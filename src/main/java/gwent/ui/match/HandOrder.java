package gwent.ui.match;

import gwent.catalog.CardDef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Порядок руки и окон выбора: особые карты, затем отряды по базовой силе, одинаковые имена рядом. */
public final class HandOrder {
    private HandOrder() {
    }

    public static List<Integer> indices(List<CardDef> cards) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator
                .comparing((Integer index) -> cards.get(index).unit())
                .thenComparingInt(index -> cards.get(index).unit() ? cards.get(index).strength() : 0)
                .thenComparing(index -> cards.get(index).name())
                .thenComparingInt(index -> cards.get(index).id()));
        return order;
    }
}
