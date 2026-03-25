package main;

import main.LocalMultiplayerProtocol.MultiplayerGameSnapshot;

public interface MultiplayerGameController {
    void setListener(Listener listener);

    MultiplayerGameSnapshot getCurrentSnapshot();

    void playCard(int handIndex, String chosenSuit);

    void drawCard();

    void close();

    interface Listener {
        void onSnapshotUpdated(MultiplayerGameSnapshot snapshot);

        void onError(String message);

        void onDisconnected(String reason);
    }
}

