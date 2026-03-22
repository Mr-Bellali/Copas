package main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CopasGameState {
    public static final int HUMAN_PLAYER_INDEX = 0;
    private static final int PLAYER_COUNT = 4;
    private static final int STARTING_HAND_SIZE = 4;
    private static final String[] PLAYER_NAMES = {"You", "AI 1", "AI 2", "AI 3"};
    private static final String[] SUITS = {"Basto", "Copa", "Espada", "Oro"};

    private Deck drawPile;
    private final List<Card> discardPile = new ArrayList<>();
    private final List<List<Card>> playerHands = new ArrayList<>();
    private final boolean[] finishedPlayers = new boolean[PLAYER_COUNT];

    private int currentPlayerIndex = HUMAN_PLAYER_INDEX;
    private String activeSuit;
    private String statusMessage = "Your turn. Play a matching card or draw from the pile.";
    private boolean roundOver;
    private boolean humanHasDrawnThisTurn;

    public CopasGameState() {
        startRound();
    }

    public void startRound() {
        drawPile = Deck.createShuffledSpanishDeck();
        discardPile.clear();
        playerHands.clear();
        roundOver = false;
        humanHasDrawnThisTurn = false;
        currentPlayerIndex = HUMAN_PLAYER_INDEX;

        for (int playerIndex = 0; playerIndex < PLAYER_COUNT; playerIndex++) {
            finishedPlayers[playerIndex] = false;
            playerHands.add(drawPile.dealCards(STARTING_HAND_SIZE));
        }

        Card openingCard = drawFromPile();
        if (openingCard == null) {
            throw new IllegalStateException("Unable to start the round because no opening card could be drawn.");
        }

        discardPile.add(openingCard);
        activeSuit = openingCard.getType();
        statusMessage = "Your turn. Top card: " + openingCard.getDisplayName() + ". Active suit: " + activeSuit + ".";
    }

    public List<Card> getHumanHand() {
        return List.copyOf(playerHands.get(HUMAN_PLAYER_INDEX));
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

    public boolean isHumanTurn() {
        return !roundOver && currentPlayerIndex == HUMAN_PLAYER_INDEX && !finishedPlayers[HUMAN_PLAYER_INDEX];
    }

    public boolean isHumanCardPlayable(int handIndex) {
        List<Card> humanHand = playerHands.get(HUMAN_PLAYER_INDEX);
        if (handIndex < 0 || handIndex >= humanHand.size()) {
            return false;
        }

        return canPlay(humanHand.get(handIndex));
    }

    public boolean canHumanDraw() {
        return isHumanTurn() && !humanHasDrawnThisTurn && !hasPlayableCard() && canDrawFromPile();
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
        return playerHands.get(playerIndex).size();
    }

    public String getPlayerName(int playerIndex) {
        return PLAYER_NAMES[playerIndex];
    }

    public boolean isPlayerFinished(int playerIndex) {
        return finishedPlayers[playerIndex];
    }

    public boolean playHumanCard(int handIndex, String chosenSuit) {
        if (!isHumanTurn()) {
            statusMessage = roundOver ? statusMessage : "Wait for your turn.";
            return false;
        }

        List<Card> humanHand = playerHands.get(HUMAN_PLAYER_INDEX);
        if (handIndex < 0 || handIndex >= humanHand.size()) {
            statusMessage = "That card is no longer available.";
            return false;
        }

        Card card = humanHand.get(handIndex);
        if (!canPlay(card)) {
            statusMessage = "You can only play a card with the same number or the active suit (" + activeSuit + ").";
            return false;
        }

        playCard(HUMAN_PLAYER_INDEX, handIndex, normalizeChosenSuit(card, chosenSuit));
        if (!roundOver) {
            runAiTurns();
        }
        return true;
    }

    public void drawForHuman() {
        if (!isHumanTurn()) {
            statusMessage = roundOver ? statusMessage : "Wait for your turn before drawing.";
            return;
        }
        if (humanHasDrawnThisTurn) {
            statusMessage = "You already drew this turn. Play a valid card if you can.";
            return;
        }
        if (hasPlayableCard()) {
            statusMessage = "You already have a playable card. Play it instead of drawing.";
            return;
        }

        Card drawnCard = drawFromPile();
        if (drawnCard == null) {
            statusMessage = "No cards left to draw.";
            return;
        }

        playerHands.get(HUMAN_PLAYER_INDEX).add(drawnCard);
        humanHasDrawnThisTurn = true;

        if (canPlay(drawnCard)) {
            statusMessage = "You drew " + drawnCard.getDisplayName() + ". You may play it or another valid card.";
        } else {
            statusMessage = "You drew " + drawnCard.getDisplayName() + " and it cannot be played. Turn passes.";
            endTurnAfterAction(drawnCard.getDisplayName() + " was not playable.");
            if (!roundOver) {
                runAiTurns();
            }
        }
    }

    private void runAiTurns() {
        while (!roundOver && currentPlayerIndex != HUMAN_PLAYER_INDEX) {
            int aiIndex = currentPlayerIndex;
            if (finishedPlayers[aiIndex]) {
                currentPlayerIndex = getNextActivePlayer(aiIndex);
                continue;
            }

            takeAiTurn(aiIndex);
        }

        if (!roundOver) {
            humanHasDrawnThisTurn = false;
            if (!hasPlayableCard()) {
                statusMessage = statusMessage + " Your turn: no playable card, draw from the pile.";
            } else if (!statusMessage.contains("Your turn")) {
                statusMessage = statusMessage + " Your turn.";
            }
        }
    }

    private void takeAiTurn(int aiIndex) {
        List<Card> aiHand = playerHands.get(aiIndex);
        int playableIndex = findPlayableCardIndex(aiHand);

        if (playableIndex >= 0) {
            Card card = aiHand.get(playableIndex);
            String chosenSuit = card.isSuitChange() ? chooseSuitForAi(aiHand, playableIndex) : null;
            playCard(aiIndex, playableIndex, chosenSuit);
            return;
        }

        Card drawnCard = drawFromPile();
        if (drawnCard == null) {
            statusMessage = getPlayerName(aiIndex) + " could not draw. ";
            currentPlayerIndex = getNextActivePlayer(aiIndex);
            return;
        }

        aiHand.add(drawnCard);
        if (canPlay(drawnCard)) {
            String chosenSuit = drawnCard.isSuitChange() ? chooseSuitForAi(aiHand, aiHand.size() - 1) : null;
            playCard(aiIndex, aiHand.size() - 1, chosenSuit);
        } else {
            statusMessage = getPlayerName(aiIndex) + " drew a card and passed.";
            currentPlayerIndex = getNextActivePlayer(aiIndex);
        }
    }

    private void playCard(int playerIndex, int handIndex, String chosenSuit) {
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

        applyCardEffect(playerIndex, card, actionMessage);

        if (!roundOver) {
            statusMessage = actionMessage.append('.').toString();
        }
    }

    private void applyCardEffect(int playerIndex, Card card, StringBuilder actionMessage) {
        if (remainingPlayers() <= 1) {
            finishRound();
            return;
        }

        if (card.isGoldenOne()) {
            int targetPlayer = getNextActivePlayer(playerIndex);
            int drawn = drawCardsForPlayer(targetPlayer, 5);
            actionMessage.append(". ").append(getPlayerName(targetPlayer)).append(" draws ").append(drawn).append(" cards and is skipped");
            currentPlayerIndex = getNextActivePlayer(targetPlayer);
            return;
        }

        if (card.isDrawTwo()) {
            int targetPlayer = getNextActivePlayer(playerIndex);
            int drawn = drawCardsForPlayer(targetPlayer, 2);
            actionMessage.append(". ").append(getPlayerName(targetPlayer)).append(" draws ").append(drawn).append(" cards and is skipped");
            currentPlayerIndex = getNextActivePlayer(targetPlayer);
            return;
        }

        if (card.isSkip()) {
            int skippedPlayer = getNextActivePlayer(playerIndex);
            actionMessage.append(". ").append(getPlayerName(skippedPlayer)).append(" is skipped");
            currentPlayerIndex = getNextActivePlayer(skippedPlayer);
            return;
        }

        currentPlayerIndex = getNextActivePlayer(playerIndex);
    }

    private void endTurnAfterAction(String detail) {
        currentPlayerIndex = getNextActivePlayer(HUMAN_PLAYER_INDEX);
        statusMessage = detail;
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

        for (int playerIndex = 0; playerIndex < PLAYER_COUNT; playerIndex++) {
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

    private boolean hasPlayableCard() {
        return playerHands.get(HUMAN_PLAYER_INDEX).stream().anyMatch(this::canPlay);
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
            nextPlayerIndex = (nextPlayerIndex + 1) % PLAYER_COUNT;
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
}




