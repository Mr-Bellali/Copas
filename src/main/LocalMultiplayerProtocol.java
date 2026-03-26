package main;

import java.io.Serializable;
import java.util.List;

/**
 * Serializable DTO protocol used over object streams for local/LAN multiplayer.
 */
public final class LocalMultiplayerProtocol {
    private LocalMultiplayerProtocol() {
    }

    /** Base marker for all messages exchanged between client and host. */
    public sealed interface SocketMessage extends Serializable permits HelloMessage, LobbyStateMessage,
            StartGameMessage, GameStateMessage, ActionRequestMessage, ErrorMessage, DisconnectedMessage {
    }

    /** Client-side action requests sent to host authority. */
    public enum ClientActionType {
        PLAY_CARD,
        DRAW_CARD
    }

    /** Event categories used for UI animations. */
    public enum MultiplayerEventType {
        ROUND_START,
        PLAY_CARD,
        DRAW_CARD
    }

    /** First handshake packet sent by joining peer. */
    public record HelloMessage(String playerName) implements SocketMessage {
    }

    /** Lightweight lobby row description. */
    public record LobbyPlayerInfo(String name, boolean host, boolean connected) implements Serializable {
    }

    /** Lobby state broadcast from host to both participants. */
    public record LobbyStateMessage(List<LobbyPlayerInfo> players, boolean canStart, String statusText)
            implements SocketMessage {
    }

    /** Signals game start with first per-player snapshot. */
    public record StartGameMessage(MultiplayerGameSnapshot snapshot) implements SocketMessage {
    }

    /** Standard gameplay state push after each accepted action. */
    public record GameStateMessage(MultiplayerGameSnapshot snapshot) implements SocketMessage {
    }

    /** Client asks host to execute one action on authoritative game state. */
    public record ActionRequestMessage(ClientActionType actionType, int handIndex, String chosenSuit)
            implements SocketMessage {
    }

    /** Recoverable protocol/gameplay error message. */
    public record ErrorMessage(String message) implements SocketMessage {
    }

    /** Remote endpoint indicates a graceful shutdown. */
    public record DisconnectedMessage(String reason) implements SocketMessage {
    }

    /** Serializable card payload (no image state) for transport and reconstruction. */
    public record CardPayload(String type, int number) implements Serializable {
        /** Converts domain card into transport payload. */
        public static CardPayload fromCard(Card card) {
            return card == null ? null : new CardPayload(card.getType(), card.getNumber());
        }

        /** Reconstructs a renderable Card from payload data. */
        public Card toCard() {
            return new Card(type, number);
        }

        /** Human-readable card label. */
        public String displayName() {
            return type + " " + number;
        }
    }

    /** Compact animation/event info for the last action in a snapshot. */
    public record MultiplayerGameEvent(MultiplayerEventType type, boolean actorIsLocal, boolean targetIsLocal,
                                       CardPayload card, int affectedCardCount) implements Serializable {
    }

    /**
     * Full per-player game snapshot.
     *
     * Host sends this after each valid action so UI can render without replaying diffs.
     */
    public record MultiplayerGameSnapshot(String localPlayerName,
                                          String opponentName,
                                          List<CardPayload> localHand,
                                          int opponentCardCount,
                                          CardPayload topCard,
                                          String activeSuit,
                                          int drawPileSize,
                                          boolean localTurn,
                                          String currentTurnName,
                                          boolean roundOver,
                                          String statusMessage,
                                          boolean canDraw,
                                          List<Integer> playableCardIndexes,
                                          MultiplayerGameEvent recentEvent) implements Serializable {
    }
}
