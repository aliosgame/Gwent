package gwent.deck;

import gwent.catalog.Faction;

import java.util.ArrayList;
import java.util.List;

/** Одна колода одной фракции. В списке id повторяется, если копий несколько. */
public record Deck(Faction faction, int leaderId, List<Integer> cardIds) {
    public Deck {
        cardIds = List.copyOf(cardIds);
    }

    public int count(int cardId) {
        int count = 0;
        for (int id : cardIds) {
            if (id == cardId) {
                count++;
            }
        }
        return count;
    }

    public Deck withLeader(int leaderId) {
        return new Deck(faction, leaderId, cardIds);
    }

    public Deck plus(int cardId) {
        List<Integer> next = new ArrayList<>(cardIds);
        next.add(cardId);
        return new Deck(faction, leaderId, next);
    }

    public Deck minus(int cardId) {
        List<Integer> next = new ArrayList<>(cardIds);
        next.remove(Integer.valueOf(cardId));
        return new Deck(faction, leaderId, next);
    }
}
