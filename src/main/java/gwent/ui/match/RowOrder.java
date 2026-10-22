package gwent.ui.match;

import gwent.catalog.CardDef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Порядок карт на ряду: чучело впереди, отряды по текущей силе, одинаковые имена рядом. */
public final class RowOrder {
    public record Entry(CardDef card, int shownStrength) {
    }

    private RowOrder() {
    }

    public static List<Integer> indices(List<Entry> entries) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator
                .comparing((Integer index) -> entries.get(index).card().unit())
                .thenComparingInt(index -> entries.get(index).card().unit() ? entries.get(index).shownStrength() : 0)
                .thenComparing(index -> entries.get(index).card().name())
                .thenComparingInt(index -> entries.get(index).card().id()));
        return order;
    }
}
