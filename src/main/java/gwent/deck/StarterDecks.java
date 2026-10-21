package gwent.deck;

import gwent.catalog.CardCatalog;
import gwent.catalog.CardDef;
import gwent.catalog.Faction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Первая колода фракции, если игрок ещё ничего не собирал. Она уже проходит лимиты. */
public final class StarterDecks {
    private StarterDecks() {
    }

    public static Deck create(Faction faction) {
        int leader = CardCatalog.leaders(faction).get(0).id();
        List<Integer> cards = new ArrayList<>();
        int units = 0;
        for (CardDef card : sorted(faction, true)) {
            for (int i = 0; i < card.copies() && units < DeckCheck.MIN_UNITS; i++) {
                cards.add(card.id());
                units++;
            }
        }
        int specials = 0;
        for (CardDef card : sorted(faction, false)) {
            int take = Math.min(card.copies(), 2);
            for (int i = 0; i < take && specials < 4; i++) {
                cards.add(card.id());
                specials++;
            }
        }
        return new Deck(faction, leader, cards);
    }

    private static List<CardDef> sorted(Faction faction, boolean units) {
        return CardCatalog.collection(faction).stream()
                .filter(card -> units ? card.unit() : card.special())
                .sorted(Comparator.comparingInt(CardDef::id))
                .toList();
    }
}
