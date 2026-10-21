package gwent.deck;

import gwent.catalog.Faction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Четыре объекта в одном файле. Чужие ключи пропускаются. */
final class DeckJson {
    private DeckJson() {
    }

    static String format(Map<Faction, Deck> decks) {
        StringBuilder out = new StringBuilder("{\n");
        Faction[] order = {Faction.NORTHERN_REALMS, Faction.NILFGAARD, Faction.SCOIATAEL, Faction.MONSTERS};
        for (int i = 0; i < order.length; i++) {
            Deck deck = decks.get(order[i]);
            out.append("  \"").append(order[i].name()).append("\": {\n");
            out.append("    \"leader\": ").append(deck.leaderId()).append(",\n");
            out.append("    \"cards\": [");
            for (int c = 0; c < deck.cardIds().size(); c++) {
                if (c > 0) {
                    out.append(", ");
                }
                out.append(deck.cardIds().get(c));
            }
            out.append("]\n  }");
            if (i < order.length - 1) {
                out.append(',');
            }
            out.append('\n');
        }
        out.append("}\n");
        return out.toString();
    }

    static Map<Faction, Deck> parse(String text) {
        Map<Faction, Deck> decks = new EnumMap<>(Faction.class);
        String source = text.strip();
        if (source.startsWith("\uFEFF")) {
            source = source.substring(1).strip();
        }
        for (Faction faction : List.of(Faction.NORTHERN_REALMS, Faction.NILFGAARD, Faction.SCOIATAEL, Faction.MONSTERS)) {
            String key = "\"" + faction.name() + "\"";
            int at = source.indexOf(key);
            if (at < 0) {
                continue;
            }
            int open = source.indexOf('{', at + key.length());
            int close = source.indexOf('}', open);
            if (open < 0 || close < 0) {
                throw new IllegalArgumentException("Сломана колода " + faction);
            }
            String body = source.substring(open, close + 1);
            decks.put(faction, new Deck(faction, leader(body), cards(body)));
        }
        return decks;
    }

    private static int leader(String body) {
        int at = body.indexOf("\"leader\"");
        int colon = body.indexOf(':', at);
        int end = colon + 1;
        while (end < body.length() && !Character.isDigit(body.charAt(end)) && body.charAt(end) != '-') {
            end++;
        }
        int stop = end;
        while (stop < body.length() && (Character.isDigit(body.charAt(stop)) || body.charAt(stop) == '-')) {
            stop++;
        }
        return Integer.parseInt(body.substring(end, stop));
    }

    private static List<Integer> cards(String body) {
        int open = body.indexOf('[');
        int close = body.indexOf(']', open);
        if (open < 0 || close < 0) {
            throw new IllegalArgumentException("В колоде нет списка карт");
        }
        String inside = body.substring(open + 1, close).trim();
        List<Integer> ids = new ArrayList<>();
        if (inside.isEmpty()) {
            return ids;
        }
        for (String part : inside.split(",")) {
            ids.add(Integer.parseInt(part.trim()));
        }
        return ids;
    }
}
