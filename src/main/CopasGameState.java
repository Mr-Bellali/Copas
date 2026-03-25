package main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CopasGameState {
    public static final int HUMAN_PLAYER_INDEX = 0;
    private static final String[] DEFAULT_PLAYER_NAMES = {"You", "AI 1", "AI 2", "AI 3"};
    private static final int STARTING_HAND_SIZE = 4;
    private static final String[] SUITS = {"Basto", "Copa", "Espada", "Oro"};

    public record AiTurnResult(int playerIndex, Card cardPlayed, boolean roundOver) {}
    public enum ActionType { PLAY_CARD, DRAW_CARD }
    public record ActionEvent(ActionType type, int actorPlayerIndex, Card card,
                              int affectedPlayerIndex, int affectedCardCount) {}
    public record PlayerActionResult(boolean success, ActionEvent event, String errorMessage) {}

    private final int playerCount;
    private final String[] playerNames;
    private final boolean[] finishedPlayers;

    private Deck drawPile;
    private final List<Card> discardPile = new ArrayList<>();
    private final List<List<Card>> playerHands = new ArrayList<>();

    private int currentPlayerIndex = HUMAN_PLAYER_INDEX;
    private String activeSuit;
    private String statusMessage = "Your turn. Play a matching card or draw from the pile.";
    private boolean roundOver;
    private boolean humanHasDrawnThisTurn;

    public CopasGameState() {
        this(DEFAULT_PLAYER_NAMES);
    }

    public CopasGameState(String[] playerNames) {
        if (playerNames == null || playerNames.length < 2) {
            throw new IllegalArgumentException("At least two players are required.");
        }

        this.playerNames = playerNames.clone();
        this.playerCount = playerNames.length;
        this.finishedPlayers = new boolean[playerCount];
        startRound();
    }

    public void startRound() {
        drawPile = Deck.createShuffledSpanishDeck();
        discardPile.clear();
        playerHands.clear();
        roundOver = false;
        humanHasDrawnThisTurn = false;
        currentPlayerIndex = HUMAN_PLAYER_INDEX;

        for (int playerIndex = 0; playerIndex < playerCount; playerIndex++) {
            finishedPlayers[playerIndex] = false;
            playerHands.add(drawPile.dealCards(STARTING_HAND_SIZE));
        }

        Card openingCard = drawFromPile();
        if (openingCard == null) {
            throw new IllegalStateException("Unable to start the round because no opening card could be drawn.");
        }

        discardPile.add(openingCard);
        activeSuit = openingCard.getType();
        statusMessage = getPlayerName(HUMAN_PLAYER_INDEX) + " turn. Top card: "
                + openingCard.getDisplayName() + ". Active suit: " + activeSuit + ".";
    }

    public List<Card> getHumanHand() {
        return getPlayerHand(HUMAN_PLAYER_INDEX);
    }

    public List<Card> getPlayerHand(int playerIndex) {
        validatePlayerIndex(playerIndex);
        return List.copyOf(playerHands.get(playerIndex));
    }

    public Card getTopCard() {
        return discardPile.getLast();
    }

    public String getActiveSuit() {
        return activeSuit;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public int getDrawPileSize() {
        return drawPile.size();
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public boolean isHumanTurn() {
        return isPlayerTurn(HUMAN_PLAYER_INDEX);
    }

    public boolean isAiTurn() {
        return !roundOver && currentPlayerIndex != HUMAN_PLAYER_INDEX;
    }

    public boolean isPlayerTurn(int playerIndex) {
        validatePlayerIndex(playerIndex);
        return !roundOver && currentPlayerIndex == playerIndex && !finishedPlayers[playerIndex];
    }

    public boolean isHumanCardPlayable(int handIndex) {
        return isPlayerCardPlayable(HUMAN_PLAYER_INDEX, handIndex);
    }

    public boolean isPlayerCardPlayable(int playerIndex, int handIndex) {
        validatePlayerIndex(playerIndex);
        List<Card> hand = playerHands.get(playerIndex);
        if (handIndex < 0 || handIndex >= hand.size()) {
            return false;
        }

        return canPlay(hand.get(handIndex));
    }

    public boolean canHumanDraw() {
        return canPlayerDraw(HUMAN_PLAYER_INDEX);
    }

    public boolean canPlayerDraw(int playerIndex) {
        validatePlayerIndex(playerIndex);
        if (!isPlayerTurn(playerIndex) || !canDrawFromPile()) {
            return false;
        }

        if (playerIndex == HUMAN_PLAYER_INDEX) {
            return !humanHasDrawnThisTurn && !hasPlayableCard(playerIndex);
        }

        return !hasPlayableCard(playerIndex);
    }

    public boolean isRoundOver() {
        return roundOver;
    }

    public boolean hasAvailableDrawPile() {
        return canDrawFromPile();
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public int getPlayerCardCount(int playerIndex) {
        validatePlayerIndex(playerIndex);
        return playerHands.get(playerIndex).size();
    }

    public String getPlayerName(int playerIndex) {
        validatePlayerIndex(playerIndex);
        return playerNames[playerIndex];
    }

    public boolean isPlayerFinished(int playerIndex) {
        validatePlayerIndex(playerIndex);
        return finishedPlayers[playerIndex];
    }

    public boolean playHumanCard(int handIndex, String chosenSuit) {
        return playCardForPlayer(HUMAN_PLAYER_INDEX, handIndex, chosenSuit).success();
    }

    public PlayerActionResult playCardForPlayer(int playerIndex, int handIndex, String chosenSuit) {
        validatePlayerIndex(playerIndex);
        if (!isPlayerTurn(playerIndex)) {
            String message = roundOver ? statusMessage : "Wait for your turn.";
            statusMessage = message;
            return new PlayerActionResult(false, null, message);
        }

        List<Card> hand = playerHands.get(playerIndex);
        if (handIndex < 0 || handIndex >= hand.size()) {
            statusMessage = "That card is no longer available.";
            return new PlayerActionResult(false, null, statusMessage);
        }

        Card card = hand.get(handIndex);
        if (!canPlay(card)) {
            statusMessage = "You can only play a card with the same number or the active suit (" + activeSuit + ").";
            return new PlayerActionResult(false, null, statusMessage);
        }

        PlayedCardResult result = playCard(playerIndex, handIndex, normalizeChosenSuit(card, chosenSuit));
        return new PlayerActionResult(
                true,
                new ActionEvent(ActionType.PLAY_CARD, playerIndex, result.card(), result.affectedPlayerIndex(), result.affectedCardCount()),
                null
        );
    }

    public void drawForHuman() {
        drawForPlayer(HUMAN_PLAYER_INDEX);
    }

    public PlayerActionResult drawForPlayer(int playerIndex) {
        validatePlayerIndex(playerIndex);
        if (!isPlayerTurn(playerIndex)) {
            String message = roundOver ? statusMessage : "Wait for your turn before drawing.";
            statusMessage = message;
            return new PlayerActionResult(false, null, message);
        }
        if (playerIndex == HUMAN_PLAYER_INDEX && humanHasDrawnThisTurn) {
            statusMessage = "You already drew this turn. Play a valid card if you can.";
            return new PlayerActionResult(false, null, statusMessage);
        }
        if (hasPlayableCard(playerIndex)) {
            statusMessage = "You already have a playable card. Play it instead of drawing.";
            return new PlayerActionResult(false, null, statusMessage);
        }

        Card drawnCard = drawFromPile();
        if (drawnCard == null) {
            statusMessage = "No cards left to draw.";
            return new PlayerActionResult(false, null, statusMessage);
        }

        playerHands.get(playerIndex).add(drawnCard);
        if (playerIndex == HUMAN_PLAYER_INDEX) {
            humanHasDrawnThisTurn = true;
        }

        if (canPlay(drawnCard)) {
            statusMessage = getPlayerName(playerIndex) + " drew " + drawnCard.getDisplayName()
                    + " and may play it or another valid card.";
        } else {
            statusMessage = getPlayerName(playerIndex) + " drew " + drawnCard.getDisplayName() + " and passed.";
            currentPlayerIndex = getNextActivePlayer(playerIndex);
            if (!roundOver && currentPlayerIndex == HUMAN_PLAYER_INDEX) {
                humanHasDrawnThisTurn = false;
            }
        }

        return new PlayerActionResult(
                true,
                new ActionEvent(ActionType.DRAW_CARD, playerIndex, drawnCard, playerIndex, 1),
                null
        );
    }

    /** Executes exactly one AI player's turn and returns what happened. GamePanel calls this
     *  after its delay timer fires so each AI move is shown with a delay and animation. */
    public AiTurnResult stepAiTurn() {
        if (!isAiTurn()) return null;

        int aiIndex = currentPlayerIndex;

        if (finishedPlayers[aiIndex]) {
            currentPlayerIndex = getNextActivePlayer(aiIndex);
            if (!roundOver && currentPlayerIndex == HUMAN_PLAYER_INDEX) humanHasDrawnThisTurn = false;
            return new AiTurnResult(aiIndex, null, roundOver);
        }

        List<Card> aiHand = playerHands.get(aiIndex);
        int playableIndex = findPlayableCardIndex(aiHand);
        Card cardPlayed = null;

        if (playableIndex >= 0) {
            Card card = aiHand.get(playableIndex);
            String chosenSuit = card.isSuitChange() ? chooseSuitForAi(aiHand, playableIndex) : null;
            cardPlayed = card;
            playCard(aiIndex, playableIndex, chosenSuit);
        } else {
            Card drawnCard = drawFromPile();
            if (drawnCard != null) {
                aiHand.add(drawnCard);
                if (canPlay(drawnCard)) {
                    String chosenSuit = drawnCard.isSuitChange() ? chooseSuitForAi(aiHand, aiHand.size() - 1) : null;
                    cardPlayed = drawnCard;
                    playCard(aiIndex, aiHand.size() - 1, chosenSuit);
                } else {
                    statusMessage = getPlayerName(aiIndex) + " drew a card and passed.";
                    currentPlayerIndex = getNextActivePlayer(aiIndex);
                }
            } else {
                statusMessage = getPlayerName(aiIndex) + " could not draw.";
                currentPlayerIndex = getNextActivePlayer(aiIndex);
            }
        }

        if (!roundOver && currentPlayerIndex == HUMAN_PLAYER_INDEX) humanHasDrawnThisTurn = false;
        return new AiTurnResult(aiIndex, cardPlayed, roundOver);
    }

    private PlayedCardResult playCard(int playerIndex, int handIndex, String chosenSuit) {
        List<Card> hand = playerHands.get(playerIndex);
        Card card = hand.remove(handIndex);
        discardPile.add(card);
        activeSuit = card.isSuitChange() ? chosenSuit : card.getType();

        StringBuilder actionMessage = new StringBuilder();
        actionMessage.append(getPlayerName(playerIndex))
                .append(" played ")
                .append(card.getDisplayName());

        if (card.isSuitChange()) {
            actionMessage.append(" and changed the suit to ").append(activeSuit);
        }

        if (hand.isEmpty()) {
            markPlayerFinished(playerIndex, actionMessage);
        }

        CardEffectResult effect = applyCardEffect(playerIndex, card, actionMessage);

        if (!roundOver) {
            statusMessage = actionMessage.append('.').toString();
        }

        return new PlayedCardResult(card, effect.affectedPlayerIndex(), effect.affectedCardCount());
    }

    private CardEffectResult applyCardEffect(int playerIndex, Card card, StringBuilder actionMessage) {
        if (remainingPlayers() <= 1) {
            finishRound();
            return new CardEffectResult(-1, 0);
        }

        if (card.isGoldenOne()) {
            int targetPlayer = getNextActivePlayer(playerIndex);
            int drawn = drawCardsForPlayer(targetPlayer, 5);
            actionMessage.append(". ").append(getPlayerName(targetPlayer)).append(" draws ").append(drawn).append(" cards and is skipped");
            currentPlayerIndex = getNextActivePlayer(targetPlayer);
            return new CardEffectResult(targetPlayer, drawn);
        }

        if (card.isDrawTwo()) {
            int targetPlayer = getNextActivePlayer(playerIndex);
            int drawn = drawCardsForPlayer(targetPlayer, 2);
            actionMessage.append(". ").append(getPlayerName(targetPlayer)).append(" draws ").append(drawn).append(" cards and is skipped");
            currentPlayerIndex = getNextActivePlayer(targetPlayer);
            return new CardEffectResult(targetPlayer, drawn);
        }

        if (card.isSkip()) {
            int skippedPlayer = getNextActivePlayer(playerIndex);
            actionMessage.append(". ").append(getPlayerName(skippedPlayer)).append(" is skipped");
            currentPlayerIndex = getNextActivePlayer(skippedPlayer);
            return new CardEffectResult(skippedPlayer, 0);
        }

        currentPlayerIndex = getNextActivePlayer(playerIndex);
        return new CardEffectResult(-1, 0);
    }

    private void markPlayerFinished(int playerIndex, StringBuilder actionMessage) {
        if (finishedPlayers[playerIndex]) {
            return;
        }

        finishedPlayers[playerIndex] = true;
        actionMessage.append(" and finished their cards");
    }

    private void finishRound() {
        roundOver = true;
        int loserIndex = -1;

        for (int playerIndex = 0; playerIndex < playerCount; playerIndex++) {
            if (!finishedPlayers[playerIndex]) {
                loserIndex = playerIndex;
                break;
            }
        }

        if (loserIndex == HUMAN_PLAYER_INDEX) {
            statusMessage = "Round over. You are the last player with cards and lose this round.";
        } else if (loserIndex >= 0) {
            statusMessage = "Round over. " + getPlayerName(loserIndex) + " is the last player with cards and loses this round.";
        } else {
            statusMessage = "Round over.";
        }
    }

    private int drawCardsForPlayer(int playerIndex, int count) {
        int drawnCards = 0;

        for (int i = 0; i < count; i++) {
            Card card = drawFromPile();
            if (card == null) {
                break;
            }
            playerHands.get(playerIndex).add(card);
            drawnCards++;
        }

        return drawnCards;
    }

    private Card drawFromPile() {
        replenishDrawPileIfNeeded();
        if (drawPile.isEmpty()) {
            return null;
        }

        return drawPile.drawCard();
    }

    private boolean canDrawFromPile() {
        return !drawPile.isEmpty() || discardPile.size() > 1;
    }

    private void replenishDrawPileIfNeeded() {
        if (!drawPile.isEmpty() || discardPile.size() <= 1) {
            return;
        }

        Card topCard = discardPile.removeLast();
        List<Card> recycledCards = new ArrayList<>(discardPile);
        discardPile.clear();
        discardPile.add(topCard);

        drawPile = new Deck(recycledCards);
        drawPile.shuffle();
    }

    private boolean canPlay(Card card) {
        return card.matches(getTopCard(), activeSuit);
    }

    private boolean hasPlayableCard(int playerIndex) {
        return playerHands.get(playerIndex).stream().anyMatch(this::canPlay);
    }

    private int findPlayableCardIndex(List<Card> hand) {
        for (int cardIndex = 0; cardIndex < hand.size(); cardIndex++) {
            if (canPlay(hand.get(cardIndex))) {
                return cardIndex;
            }
        }
        return -1;
    }

    private int getNextActivePlayer(int fromPlayerIndex) {
        int nextPlayerIndex = fromPlayerIndex;
        do {
            nextPlayerIndex = (nextPlayerIndex + 1) % playerCount;
        } while (finishedPlayers[nextPlayerIndex]);
        return nextPlayerIndex;
    }

    private int remainingPlayers() {
        int count = 0;
        for (boolean finished : finishedPlayers) {
            if (!finished) {
                count++;
            }
        }
        return count;
    }

    private String chooseSuitForAi(List<Card> hand, int playedCardIndex) {
        List<Card> remainingCards = new ArrayList<>(hand);
        remainingCards.remove(playedCardIndex);

        return remainingCards.stream()
                .filter(card -> !card.isSuitChange())
                .collect(java.util.stream.Collectors.groupingBy(Card::getType, java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .max(Comparator.comparingLong(java.util.Map.Entry::getValue))
                .map(java.util.Map.Entry::getKey)
                .orElse(SUITS[0]);
    }

    private String normalizeChosenSuit(Card card, String chosenSuit) {
        if (!card.isSuitChange()) {
            return card.getType();
        }

        for (String suit : SUITS) {
            if (suit.equals(chosenSuit)) {
                return suit;
            }
        }

        return card.getType();
    }

    private void validatePlayerIndex(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= playerCount) {
            throw new IllegalArgumentException("Unknown player index: " + playerIndex);
        }
    }

    private record CardEffectResult(int affectedPlayerIndex, int affectedCardCount) {}
    private record PlayedCardResult(Card card, int affectedPlayerIndex, int affectedCardCount) {}
}
