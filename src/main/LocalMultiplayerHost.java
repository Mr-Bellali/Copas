package main;

import main.LocalMultiplayerProtocol.ActionRequestMessage;
import main.LocalMultiplayerProtocol.CardPayload;
import main.LocalMultiplayerProtocol.ClientActionType;
import main.LocalMultiplayerProtocol.DisconnectedMessage;
import main.LocalMultiplayerProtocol.ErrorMessage;
import main.LocalMultiplayerProtocol.HelloMessage;
import main.LocalMultiplayerProtocol.LobbyPlayerInfo;
import main.LocalMultiplayerProtocol.LobbyStateMessage;
import main.LocalMultiplayerProtocol.MultiplayerGameEvent;
import main.LocalMultiplayerProtocol.MultiplayerGameSnapshot;
import main.LocalMultiplayerProtocol.MultiplayerEventType;
import main.LocalMultiplayerProtocol.SocketMessage;
import main.LocalMultiplayerProtocol.StartGameMessage;
import main.LocalMultiplayerProtocol.GameStateMessage;

import javax.swing.SwingUtilities;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class LocalMultiplayerHost implements MultiplayerGameController, Closeable {
    public static final int DEFAULT_PORT = 34567;
    private static final int HOST_PLAYER_INDEX = 0;
    private static final int CLIENT_PLAYER_INDEX = 1;

    public interface LobbyListener {
        void onLobbyUpdated(LobbyStateMessage lobbyState);

        void onGameStarted(LocalMultiplayerHost controller, MultiplayerGameSnapshot snapshot);

        void onError(String message);

        void onDisconnected(String reason);
    }

    private final String hostPlayerName;
    private final int port;
    private final LobbyListener lobbyListener;

    private volatile MultiplayerGameSnapshot currentSnapshot;
    private volatile Listener gameListener;
    private volatile boolean running;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream clientOutput;
    private ObjectInputStream clientInput;
    private Thread acceptThread;
    private Thread clientReaderThread;

    private String clientPlayerName;
    private CopasGameState gameState;

    public LocalMultiplayerHost(String hostPlayerName, int port, LobbyListener lobbyListener) {
        this.hostPlayerName = sanitizePlayerName(hostPlayerName, "Host");
        this.port = port;
        this.lobbyListener = lobbyListener;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        notifyLobbyUpdated();

        acceptThread = new Thread(this::acceptClientLoop, "copas-host-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public String getConnectionAddress() {
        String hostAddress = "127.0.0.1";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (IOException ignored) {
        }
        return hostAddress + ":" + port;
    }

    public synchronized void startGame() {
        if (gameState != null) {
            return;
        }
        if (clientPlayerName == null) {
            publishLobbyError("Wait for another player to join before starting.");
            return;
        }

        gameState = new CopasGameState(new String[]{hostPlayerName, clientPlayerName});
        MultiplayerGameSnapshot hostSnapshot = buildSnapshotFor(HOST_PLAYER_INDEX,
                new MultiplayerGameEvent(MultiplayerEventType.ROUND_START, true, false, null, 0));
        currentSnapshot = hostSnapshot;
        sendToClient(new StartGameMessage(buildSnapshotFor(CLIENT_PLAYER_INDEX,
                new MultiplayerGameEvent(MultiplayerEventType.ROUND_START, true, false, null, 0))));
        runOnEdt(() -> lobbyListener.onGameStarted(this, hostSnapshot));
    }

    @Override
    public void setListener(Listener listener) {
        this.gameListener = listener;
    }

    @Override
    public MultiplayerGameSnapshot getCurrentSnapshot() {
        return currentSnapshot;
    }

    @Override
    public void playCard(int handIndex, String chosenSuit) {
        handleAction(HOST_PLAYER_INDEX, new ActionRequestMessage(ClientActionType.PLAY_CARD, handIndex, chosenSuit), false);
    }

    @Override
    public void drawCard() {
        handleAction(HOST_PLAYER_INDEX, new ActionRequestMessage(ClientActionType.DRAW_CARD, -1, null), false);
    }

    @Override
    public void close() {
        running = false;
        sendToClient(new DisconnectedMessage("The host closed the game."));
        closeQuietly(clientInput);
        closeQuietly(clientOutput);
        closeQuietly(clientSocket);
        closeQuietly(serverSocket);
    }

    private void acceptClientLoop() {
        try {
            while (running && clientSocket == null) {
                Socket socket = serverSocket.accept();
                setupClient(socket);
            }
        } catch (SocketException ignored) {
        } catch (IOException exception) {
            if (running) {
                publishLobbyError("Failed while waiting for a player: " + exception.getMessage());
            }
        }
    }

    private synchronized void setupClient(Socket socket) {
        if (!running) {
            closeQuietly(socket);
            return;
        }
        if (clientSocket != null) {
            try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream())) {
                output.writeObject(new ErrorMessage("Only one remote player is supported for now."));
                output.flush();
            } catch (IOException ignored) {
            } finally {
                closeQuietly(socket);
            }
            return;
        }

        try {
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            Object message = input.readObject();
            if (!(message instanceof HelloMessage helloMessage)) {
                throw new IOException("Invalid handshake received.");
            }

            clientSocket = socket;
            clientOutput = output;
            clientInput = input;
            clientPlayerName = sanitizePlayerName(helloMessage.playerName(), "Player 2");
            notifyLobbyUpdated();

            clientReaderThread = new Thread(this::readClientMessages, "copas-host-reader");
            clientReaderThread.setDaemon(true);
            clientReaderThread.start();
        } catch (IOException | ClassNotFoundException exception) {
            closeQuietly(socket);
            publishLobbyError("Could not accept player: " + exception.getMessage());
        }
    }

    private void readClientMessages() {
        try {
            while (running && clientInput != null) {
                Object object = clientInput.readObject();
                if (object instanceof ActionRequestMessage actionRequestMessage) {
                    handleAction(CLIENT_PLAYER_INDEX, actionRequestMessage, true);
                }
            }
        } catch (EOFException | SocketException ignored) {
            if (running) {
                handleClientDisconnected("The remote player disconnected.");
            }
        } catch (IOException | ClassNotFoundException exception) {
            if (running) {
                handleClientDisconnected("Connection lost: " + exception.getMessage());
            }
        }
    }

    private synchronized void handleAction(int actorIndex, ActionRequestMessage actionRequestMessage, boolean remoteActor) {
        if (gameState == null) {
            publishActionError(remoteActor, "The host has not started the match yet.");
            return;
        }

        CopasGameState.PlayerActionResult result = actionRequestMessage.actionType() == ClientActionType.PLAY_CARD
                ? gameState.playCardForPlayer(actorIndex, actionRequestMessage.handIndex(), actionRequestMessage.chosenSuit())
                : gameState.drawForPlayer(actorIndex);

        if (!result.success()) {
            publishActionError(remoteActor, result.errorMessage());
            return;
        }

        MultiplayerGameSnapshot hostSnapshot = buildSnapshotFor(HOST_PLAYER_INDEX, localizeEvent(HOST_PLAYER_INDEX, result.event()));
        MultiplayerGameSnapshot clientSnapshot = buildSnapshotFor(CLIENT_PLAYER_INDEX, localizeEvent(CLIENT_PLAYER_INDEX, result.event()));
        currentSnapshot = hostSnapshot;
        notifyGameSnapshot(hostSnapshot);
        sendToClient(new GameStateMessage(clientSnapshot));
    }

    private synchronized void handleClientDisconnected(String reason) {
        clientPlayerName = null;
        closeQuietly(clientInput);
        closeQuietly(clientOutput);
        closeQuietly(clientSocket);
        clientInput = null;
        clientOutput = null;
        clientSocket = null;
        if (gameState == null) {
            notifyLobbyUpdated();
            runOnEdt(() -> lobbyListener.onDisconnected(reason));
            return;
        }

        Listener listener = gameListener;
        if (listener != null) {
            runOnEdt(() -> listener.onDisconnected(reason));
        }
    }

    private void notifyLobbyUpdated() {
        LobbyStateMessage lobbyState = new LobbyStateMessage(buildLobbyPlayers(), clientPlayerName != null,
                clientPlayerName == null ? "Waiting for a player to join..." : clientPlayerName + " joined successfully.");
        sendToClient(lobbyState);
        runOnEdt(() -> lobbyListener.onLobbyUpdated(lobbyState));
    }

    private List<LobbyPlayerInfo> buildLobbyPlayers() {
        List<LobbyPlayerInfo> players = new ArrayList<>();
        players.add(new LobbyPlayerInfo(hostPlayerName, true, true));
        players.add(new LobbyPlayerInfo(clientPlayerName == null ? "Waiting for player 2..." : clientPlayerName,
                false, clientPlayerName != null));
        return players;
    }

    private MultiplayerGameSnapshot buildSnapshotFor(int viewerIndex, MultiplayerGameEvent recentEvent) {
        int opponentIndex = viewerIndex == HOST_PLAYER_INDEX ? CLIENT_PLAYER_INDEX : HOST_PLAYER_INDEX;
        List<CardPayload> localHand = gameState.getPlayerHand(viewerIndex).stream()
                .map(CardPayload::fromCard)
                .toList();
        List<Integer> playableCardIndexes = new ArrayList<>();
        for (int handIndex = 0; handIndex < localHand.size(); handIndex++) {
            if (gameState.isPlayerCardPlayable(viewerIndex, handIndex)) {
                playableCardIndexes.add(handIndex);
            }
        }

        return new MultiplayerGameSnapshot(
                gameState.getPlayerName(viewerIndex),
                gameState.getPlayerName(opponentIndex),
                localHand,
                gameState.getPlayerCardCount(opponentIndex),
                CardPayload.fromCard(gameState.getTopCard()),
                gameState.getActiveSuit(),
                gameState.getDrawPileSize(),
                gameState.isPlayerTurn(viewerIndex),
                gameState.getPlayerName(gameState.getCurrentPlayerIndex()),
                gameState.isRoundOver(),
                gameState.getStatusMessage(),
                gameState.canPlayerDraw(viewerIndex),
                playableCardIndexes,
                recentEvent
        );
    }

    private MultiplayerGameEvent localizeEvent(int viewerIndex, CopasGameState.ActionEvent actionEvent) {
        if (actionEvent == null) {
            return null;
        }

        CardPayload payload = switch (actionEvent.type()) {
            case PLAY_CARD -> CardPayload.fromCard(actionEvent.card());
            case DRAW_CARD -> actionEvent.actorPlayerIndex() == viewerIndex ? CardPayload.fromCard(actionEvent.card()) : null;
        };

        return new MultiplayerGameEvent(
                actionEvent.type() == CopasGameState.ActionType.PLAY_CARD ? MultiplayerEventType.PLAY_CARD : MultiplayerEventType.DRAW_CARD,
                actionEvent.actorPlayerIndex() == viewerIndex,
                actionEvent.affectedPlayerIndex() == viewerIndex,
                payload,
                actionEvent.affectedCardCount()
        );
    }

    private void notifyGameSnapshot(MultiplayerGameSnapshot snapshot) {
        Listener listener = gameListener;
        if (listener != null) {
            runOnEdt(() -> listener.onSnapshotUpdated(snapshot));
        }
    }

    private void publishLobbyError(String message) {
        runOnEdt(() -> lobbyListener.onError(message));
    }

    private void publishActionError(boolean remoteActor, String message) {
        if (remoteActor) {
            sendToClient(new ErrorMessage(message));
            return;
        }
        Listener listener = gameListener;
        if (listener != null) {
            runOnEdt(() -> listener.onError(message));
        } else {
            publishLobbyError(message);
        }
    }

    private synchronized void sendToClient(SocketMessage message) {
        if (clientOutput == null) {
            return;
        }
        try {
            clientOutput.reset();
            clientOutput.writeObject(message);
            clientOutput.flush();
        } catch (IOException exception) {
            handleClientDisconnected("Could not send data to the remote player.");
        }
    }

    private static void closeQuietly(Object resource) {
        if (resource instanceof Closeable closeable) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        } else if (resource instanceof Socket socket) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static String sanitizePlayerName(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }
}

