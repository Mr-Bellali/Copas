package main;

import main.LocalMultiplayerProtocol.ActionRequestMessage;
import main.LocalMultiplayerProtocol.ClientActionType;
import main.LocalMultiplayerProtocol.DisconnectedMessage;
import main.LocalMultiplayerProtocol.ErrorMessage;
import main.LocalMultiplayerProtocol.GameStateMessage;
import main.LocalMultiplayerProtocol.HelloMessage;
import main.LocalMultiplayerProtocol.LobbyStateMessage;
import main.LocalMultiplayerProtocol.MultiplayerGameSnapshot;
import main.LocalMultiplayerProtocol.SocketMessage;
import main.LocalMultiplayerProtocol.StartGameMessage;

import javax.swing.SwingUtilities;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

public class LocalMultiplayerClient implements MultiplayerGameController, Closeable {
    private static final int CONNECT_TIMEOUT_MS = 3500;
    public interface LobbyListener {
        void onLobbyUpdated(LobbyStateMessage lobbyState);

        void onGameStarted(LocalMultiplayerClient controller, MultiplayerGameSnapshot snapshot);

        void onError(String message);

        void onDisconnected(String reason);
    }

    private final String playerName;
    private final String hostAddress;
    private final int port;
    private final LobbyListener lobbyListener;

    private volatile Listener gameListener;
    private volatile MultiplayerGameSnapshot currentSnapshot;
    private volatile boolean running;

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Thread readerThread;

    public LocalMultiplayerClient(String playerName, String hostAddress, int port, LobbyListener lobbyListener) {
        this.playerName = sanitizePlayerName(playerName, "Player 2");
        this.hostAddress = hostAddress == null || hostAddress.isBlank() ? "127.0.0.1" : hostAddress.trim();
        this.port = port;
        this.lobbyListener = lobbyListener;
    }

    public void connect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(hostAddress, port), CONNECT_TIMEOUT_MS);
        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());
        running = true;
        send(new HelloMessage(playerName));

        readerThread = new Thread(this::readMessages, "copas-client-reader");
        readerThread.setDaemon(true);
        readerThread.start();
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
        send(new ActionRequestMessage(ClientActionType.PLAY_CARD, handIndex, chosenSuit));
    }

    @Override
    public void drawCard() {
        send(new ActionRequestMessage(ClientActionType.DRAW_CARD, -1, null));
    }

    @Override
    public void close() {
        running = false;
        send(new DisconnectedMessage("The player left the game."));
        closeQuietly(input);
        closeQuietly(output);
        closeQuietly(socket);
    }

    private void readMessages() {
        try {
            while (running && input != null) {
                Object object = input.readObject();
                if (object instanceof LobbyStateMessage lobbyStateMessage) {
                    runOnEdt(() -> lobbyListener.onLobbyUpdated(lobbyStateMessage));
                } else if (object instanceof StartGameMessage startGameMessage) {
                    currentSnapshot = startGameMessage.snapshot();
                    runOnEdt(() -> lobbyListener.onGameStarted(this, startGameMessage.snapshot()));
                } else if (object instanceof GameStateMessage gameStateMessage) {
                    currentSnapshot = gameStateMessage.snapshot();
                    Listener listener = gameListener;
                    if (listener != null) {
                        runOnEdt(() -> listener.onSnapshotUpdated(gameStateMessage.snapshot()));
                    }
                } else if (object instanceof ErrorMessage errorMessage) {
                    Listener listener = gameListener;
                    if (listener != null) {
                        runOnEdt(() -> listener.onError(errorMessage.message()));
                    } else {
                        runOnEdt(() -> lobbyListener.onError(errorMessage.message()));
                    }
                } else if (object instanceof DisconnectedMessage disconnectedMessage) {
                    notifyDisconnected(disconnectedMessage.reason());
                    break;
                }
            }
        } catch (EOFException | SocketException ignored) {
            if (running) {
                notifyDisconnected("Disconnected from host.");
            }
        } catch (IOException | ClassNotFoundException exception) {
            if (running) {
                notifyDisconnected("Connection lost: " + exception.getMessage());
            }
        }
    }

    private synchronized void send(SocketMessage message) {
        if (!running || output == null) {
            return;
        }
        try {
            output.reset();
            output.writeObject(message);
            output.flush();
        } catch (IOException exception) {
            notifyDisconnected("Failed to send data to the host.");
        }
    }

    private void notifyDisconnected(String reason) {
        running = false;
        Listener listener = gameListener;
        if (listener != null) {
            runOnEdt(() -> listener.onDisconnected(reason));
        } else {
            runOnEdt(() -> lobbyListener.onDisconnected(reason));
        }
        closeQuietly(input);
        closeQuietly(output);
        closeQuietly(socket);
    }

    private static void closeQuietly(Object resource) {
        if (resource instanceof Closeable closeable) {
            try {
                closeable.close();
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

