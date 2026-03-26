package main;

import main.LocalMultiplayerProtocol.MultiplayerGameSnapshot;

/**
 * Abstraction used by multiplayer UI to drive actions regardless of role.
 *
 * Implementations can be host-side or client-side, but expose the same API.
 */
public interface MultiplayerGameController {
    /** Registers live callbacks for snapshots/errors/disconnects. */
    void setListener(Listener listener);

    /** Returns latest known snapshot for this local player POV. */
    MultiplayerGameSnapshot getCurrentSnapshot();

    /** Requests to play a hand card by index (and optional chosen suit for suit-change cards). */
    void playCard(int handIndex, String chosenSuit);

    /** Requests to draw one card and pass turn. */
    void drawCard();

    /** Closes network resources and session. */
    void close();

    /** Observer receiving asynchronous controller events. */
    interface Listener {
        /** Called whenever a new game snapshot is available. */
        void onSnapshotUpdated(MultiplayerGameSnapshot snapshot);

        /** Called for recoverable action/session errors. */
        void onError(String message);

        /** Called when remote endpoint disconnects. */
        void onDisconnected(String reason);
    }
}
