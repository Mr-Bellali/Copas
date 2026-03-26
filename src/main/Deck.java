package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Mutable deck abstraction used for draw pile and recycled piles.
 */
public class Deck {
    private static final String[] CARD_TYPES = {"Basto", "Copa", "Espada", "Oro"}; // suits available in this game

    private final List<Card> cards; // top of deck is the last element for cheap remove()

    /** Wraps an existing card list as a deck instance. */
    public Deck(List<Card> cards) {
        this.cards = cards;
    }

    /** Builds a full 40-card Spanish deck and shuffles it. */
    public static Deck createShuffledSpanishDeck() {
        List<Card> cards = new ArrayList<>();

        for (String cardType : CARD_TYPES) {
            for (int number = 1; number <= 10; number++) {
                cards.add(new Card(cardType, number));
            }
        }

        Deck deck = new Deck(cards);
        deck.shuffle();
        return deck;
    }

    /** Randomizes card order in-place. */
    public void shuffle() {
        Collections.shuffle(cards);
    }

    /** Draws the top card from the deck. */
    public Card drawCard() {
        if (cards.isEmpty()) {
            throw new NoSuchElementException("The deck is empty");
        }

        return cards.remove(cards.size() - 1);
    }

    /** Draws multiple cards preserving draw order. */
    public List<Card> dealCards(int count) {
        List<Card> dealtCards = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            dealtCards.add(drawCard());
        }

        return dealtCards;
    }

    /** Current remaining card count. */
    public int size() {
        return cards.size();
    }

    /** Convenience emptiness check. */
    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
