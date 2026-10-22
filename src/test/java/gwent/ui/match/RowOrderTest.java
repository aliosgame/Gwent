package gwent.ui.match;

import gwent.catalog.CardCatalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RowOrderTest {
    @Test
    void decoyFirstThenCurrentStrengthAndSameNamesTogether() {
        var decoy = new RowOrder.Entry(CardCatalog.get(166), 0);
        var weak = new RowOrder.Entry(CardCatalog.get(10), 1);
        var twin = new RowOrder.Entry(CardCatalog.get(10), 8);
        var other = new RowOrder.Entry(CardCatalog.get(11), 8);
        List<RowOrder.Entry> entries = List.of(other, twin, decoy, weak);
        List<Integer> order = RowOrder.indices(entries);
        assertEquals(166, entries.get(order.get(0)).card().id());
        assertEquals(1, entries.get(order.get(1)).shownStrength());
        assertEquals(10, entries.get(order.get(2)).card().id());
        assertEquals(11, entries.get(order.get(3)).card().id());
    }
}
