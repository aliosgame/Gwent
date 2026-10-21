package gwent.deck;

import gwent.catalog.CardCatalog;
import gwent.catalog.CardDef;
import gwent.catalog.CardKind;
import gwent.catalog.Faction;

import java.util.LinkedHashMap;
import java.util.Map;

/** Можно ли сесть с этой колодой за стол. Пустая строка — можно. */
public final class DeckCheck {
    public static final int MIN_UNITS = 22;
    public static final int MAX_SPECIALS = 10;

    private DeckCheck() {
    }

    public static String problem(Deck deck) {
        CardDef leader;
        try {
            leader = CardCatalog.get(deck.leaderId());
        } catch (IllegalArgumentException ex) {
            return "Не выбран лидер";
        }
        if (leader.kind() != CardKind.LEADER || leader.faction() != deck.faction()) {
            return "Лидер не от этой фракции";
        }
        int units = 0;
        int specials = 0;
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (int id : deck.cardIds()) {
            CardDef card;
            try {
                card = CardCatalog.get(id);
            } catch (IllegalArgumentException ex) {
                return "В колоде есть неизвестная карта";
            }
            if (!card.inCollection()) {
                return "«" + card.name() + "» нельзя класть в колоду";
            }
            if (card.faction() != deck.faction() && card.faction() != Faction.NEUTRAL) {
                return "«" + card.name() + "» из другой фракции";
            }
            int have = counts.merge(id, 1, Integer::sum);
            if (have > card.copies()) {
                return "«" + card.name() + "» больше, чем есть в коллекции";
            }
            if (card.unit()) {
                units++;
            } else if (card.special()) {
                specials++;
            }
        }
        if (units < MIN_UNITS) {
            return "Отрядов " + units + ", нужно хотя бы " + MIN_UNITS;
        }
        if (specials > MAX_SPECIALS) {
            return "Особых карт " + specials + ", можно не больше " + MAX_SPECIALS;
        }
        return "";
    }

    public static boolean ok(Deck deck) {
        return problem(deck).isEmpty();
    }

    public static int units(Deck deck) {
        return count(deck, true);
    }

    public static int specials(Deck deck) {
        return count(deck, false);
    }

    public static int unitStrength(Deck deck) {
        int sum = 0;
        for (int id : deck.cardIds()) {
            CardDef card = CardCatalog.get(id);
            if (card.unit()) {
                sum += card.strength();
            }
        }
        return sum;
    }

    public static int heroes(Deck deck) {
        int count = 0;
        for (int id : deck.cardIds()) {
            if (CardCatalog.get(id).hero()) {
                count++;
            }
        }
        return count;
    }

    private static int count(Deck deck, boolean units) {
        int count = 0;
        for (int id : deck.cardIds()) {
            CardDef card = CardCatalog.get(id);
            if (units ? card.unit() : card.special()) {
                count++;
            }
        }
        return count;
    }
}
