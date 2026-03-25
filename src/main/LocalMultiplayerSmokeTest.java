package main;

import main.LocalMultiplayerProtocol.LobbyStateMessage;
import main.LocalMultiplayerProtocol.MultiplayerGameSnapshot;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class LocalMultiplayerSmokeTest {
    public static void main(String[] args) throws Exception {
        CountDownLatch hostLobbyReady = new CountDownLatch(1);
        CountDownLatch clientLobbyReady = new CountDownLatch(1);
        CountDownLatch hostStarted = new CountDownLatch(1);
        CountDownLatch clientStarted = new CountDownLatch(1);
        CountDownLatch updates = new CountDownLatch(2);

        AtomicReference<MultiplayerGameSnapshot> hostSnapshotRef = new AtomicReference<>();

        LocalMultiplayerHost host = new LocalMultiplayerHost("Host", LocalMultiplayerHost.DEFAULT_PORT, new LocalMultiplayerHost.LobbyListener() {
            @Override
            public void onLobbyUpdated(LobbyStateMessage lobbyState) {
                if (lobbyState.canStart()) {
                    hostLobbyReady.countDown();
                }
            }

            @Override
            public void onGameStarted(LocalMultiplayerHost controller, MultiplayerGameSnapshot snapshot) {
                hostSnapshotRef.set(snapshot);
                hostStarted.countDown();
            }

            @Override
            public void onError(String message) {
                throw new RuntimeException(message);
            }

            @Override
            public void onDisconnected(String reason) {
            }
        });
        host.start();

        LocalMultiplayerClient client = new LocalMultiplayerClient("Client", "127.0.0.1", LocalMultiplayerHost.DEFAULT_PORT, new LocalMultiplayerClient.LobbyListener() {
            @Override
            public void onLobbyUpdated(LobbyStateMessage lobbyState) {
                if (lobbyState.players().size() == 2 && lobbyState.players().get(1).connected()) {
                    clientLobbyReady.countDown();
                }
            }

            @Override
            public void onGameStarted(LocalMultiplayerClient controller, MultiplayerGameSnapshot snapshot) {
                clientStarted.countDown();
            }

            @Override
            public void onError(String message) {
                throw new RuntimeException(message);
            }

            @Override
            public void onDisconnected(String reason) {
            }
        });
        client.connect();

        if (!hostLobbyReady.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Host lobby handshake failed.");
        }
        if (!clientLobbyReady.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Client lobby handshake failed.");
        }

        host.startGame();

        if (!hostStarted.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Host did not start the game.");
        }
        if (!clientStarted.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Client did not start the game.");
        }

        host.setListener(new MultiplayerGameController.Listener() {
            @Override
            public void onSnapshotUpdated(MultiplayerGameSnapshot snapshot) {
                updates.countDown();
            }

            @Override
            public void onError(String message) {
                throw new RuntimeException(message);
            }

            @Override
            public void onDisconnected(String reason) {
                // Expected during teardown.
            }
        });
        client.setListener(new MultiplayerGameController.Listener() {
            @Override
            public void onSnapshotUpdated(MultiplayerGameSnapshot snapshot) {
                updates.countDown();
            }

            @Override
            public void onError(String message) {
                throw new RuntimeException(message);
            }

            @Override
            public void onDisconnected(String reason) {
                // Expected during teardown.
            }
        });

        MultiplayerGameSnapshot openingSnapshot = hostSnapshotRef.get();
        if (!openingSnapshot.localTurn()) {
            throw new IllegalStateException("Expected host turn on round start.");
        }

        if (!openingSnapshot.playableCardIndexes().isEmpty()) {
            int handIndex = openingSnapshot.playableCardIndexes().get(0);
            String chosenSuit = openingSnapshot.localHand().get(handIndex).number() == 7
                    ? openingSnapshot.activeSuit()
                    : null;
            host.playCard(handIndex, chosenSuit);
        } else if (openingSnapshot.canDraw()) {
            host.drawCard();
        } else {
            throw new IllegalStateException("No legal first action available.");
        }

        if (!updates.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("State update was not propagated to both sides.");
        }

        client.close();
        host.close();
        System.out.println("Local multiplayer smoke test passed.");
    }
}


