package main;

import java.io.Serializable;
import java.util.List;

public final class LocalMultiplayerProtocol {
    private LocalMultiplayerProtocol() {
    }

    public sealed interface SocketMessage extends Serializable permits HelloMessage, LobbyStateMessage,
            StartGameMessage, GameStateMessage, ActionRequestMessage, ErrorMessage, DisconnectedMessage {
    }

    public enum ClientActionType {
        PLAY_CARD,
        DRAW_CARD
    }

    public enum MultiplayerEventType {
        ROUND_START,
        PLAY_CARD,
        DRAW_CARD
    }

    public record HelloMessage(String playerName) implements SocketMessage {
    }

    public record LobbyPlayerInfo(String name, boolean host, boolean connected) implements Serializable {
    }

    public record LobbyStateMessage(List<LobbyPlayerInfo> players, boolean canStart, String statusText)
            implements SocketMessage {
    }

    public record StartGameMessage(MultiplayerGameSnapshot snapshot) implements SocketMessage {
    }

    public record GameStateMessage(MultiplayerGameSnapshot snapshot) implements SocketMessage {
    }

    public record ActionRequestMessage(ClientActionType actionType, int handIndex, String chosenSuit)
            implements SocketMessage {
    }

    public record ErrorMessage(String message) implements SocketMessage {
    }

    public record DisconnectedMessage(String reason) implements SocketMessage {
    }

    public record CardPayload(String type, int number) implements Serializable {
        public static CardPayload fromCard(Card card) {
            return card == null ? null : new CardPayload(card.getType(), card.getNumber());
        }

        public Card toCard() {
            return new Card(type, number);
        }

        public String displayName() {
            return type + " " + number;
        }
    }

    public record MultiplayerGameEvent(MultiplayerEventType type, boolean actorIsLocal, boolean targetIsLocal,
                                       CardPayload card, int affectedCardCount) implements Serializable {
    }

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

