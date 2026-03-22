package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

public class Deck {
    private static final String[] CARD_TYPES = {"Basto", "Copa", "Espada", "Oro"};

    private final List<Card> cards;

    public Deck(List<Card> cards) {
        this.cards = cards;
    }

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

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card drawCard() {
        if (cards.isEmpty()) {
            throw new NoSuchElementException("The deck is empty");
        }

        return cards.remove(cards.size() - 1);
    }

    public List<Card> dealCards(int count) {
        List<Card> dealtCards = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            dealtCards.add(drawCard());
        }

        return dealtCards;
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

}
