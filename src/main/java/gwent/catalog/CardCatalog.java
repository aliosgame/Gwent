package gwent.catalog;

import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CardCatalog {
    private static final List<CardDef> ALL = load();
    private static final Map<Integer, CardDef> BY_ID = index(ALL);

    private CardCatalog() {
    }

    public static List<CardDef> all() {
        return ALL;
    }

    public static CardDef get(int id) {
        CardDef card = BY_ID.get(id);
        if (card == null) {
            throw new IllegalArgumentException("Нет карты " + id);
        }
        return card;
    }

    public static List<CardDef> of(Faction faction) {
        return ALL.stream().filter(card -> card.faction() == faction).toList();
    }

    public static List<CardDef> units(Faction faction) {
        return of(faction).stream().filter(CardDef::unit).toList();
    }

    public static List<CardDef> leaders(Faction faction) {
        return of(faction).stream().filter(card -> card.kind() == CardKind.LEADER).toList();
    }

    /** Всё, что можно положить в колоду этой фракции: свои карты и нейтральные. */
    public static List<CardDef> collection(Faction faction) {
        return ALL.stream()
                .filter(CardDef::inCollection)
                .filter(card -> card.faction() == faction || card.faction() == Faction.NEUTRAL)
                .toList();
    }

    public static String perk(Faction faction) {
        return switch (faction) {
            case NORTHERN_REALMS -> "Королевства Севера берут карту из своей колоды после каждого выигранного раунда.";
            case NILFGAARD -> "Ничья в раунде засчитывается как победа Нильфгаарда.";
            case SCOIATAEL -> "Скоя'таэли решают, кто ходит первым.";
            case MONSTERS -> "После раунда чудовища оставляют на столе один случайный отряд.";
            case NEUTRAL -> "";
        };
    }

    public static String factionName(Faction faction) {
        return switch (faction) {
            case NORTHERN_REALMS -> "Королевства Севера";
            case NILFGAARD -> "Нильфгаард";
            case SCOIATAEL -> "Скоя'таэли";
            case MONSTERS -> "Чудовища";
            case NEUTRAL -> "Нейтральные";
        };
    }

    private static List<CardDef> load() {
        try (InputStream in = CardCatalog.class.getResourceAsStream("/gwent/catalog/cards.xml")) {
            if (in == null) {
                throw new IllegalStateException("Нет /gwent/catalog/cards.xml");
            }
            List<CardDef> cards = CardXml.read(in);
            Map<Integer, CardDef> seen = new LinkedHashMap<>();
            for (CardDef card : cards) {
                if (seen.put(card.id(), card) != null) {
                    throw new IllegalStateException("Повторный id карты: " + card.id());
                }
            }
            return List.copyOf(cards);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Не прочитать каталог карт", ex);
        }
    }

    private static Map<Integer, CardDef> index(Collection<CardDef> cards) {
        Map<Integer, CardDef> index = new LinkedHashMap<>();
        for (CardDef card : cards) {
            index.put(card.id(), card);
        }
        return Map.copyOf(index);
    }
}
