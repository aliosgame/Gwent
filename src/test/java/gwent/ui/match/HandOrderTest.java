package gwent.ui.match;

import gwent.catalog.CardCatalog;
import gwent.catalog.CardDef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandOrderTest {
    @Test
    void specialsFirstThenStrengthAndSameNamesStayTogether() {
        CardDef dijkstra = CardCatalog.get(11);
        CardDef decoy = CardCatalog.get(166);
        CardDef stripes = CardCatalog.get(10);
        CardDef infantry = CardCatalog.get(1);
        CardDef horn = CardCatalog.get(167);
        List<CardDef> cards = List.of(dijkstra, decoy, stripes, infantry, stripes, horn);
        List<CardDef> sorted = new ArrayList<>();
        for (int index : HandOrder.indices(cards)) {
            sorted.add(cards.get(index));
        }
        int firstUnit = 0;
        while (firstUnit < sorted.size() && !sorted.get(firstUnit).unit()) {
            firstUnit++;
        }
        assertTrue(firstUnit >= 2);
        for (int i = 0; i < firstUnit; i++) {
            assertFalse(sorted.get(i).unit());
        }
        for (int i = firstUnit + 1; i < sorted.size(); i++) {
            assertTrue(sorted.get(i).strength() >= sorted.get(i - 1).strength());
        }
        assertEquals(0, BoardCard.tone(CardCatalog.get(26), 15, 10));
        assertEquals(1, BoardCard.tone(CardCatalog.get(1), 8, 4));
        assertEquals(-1, BoardCard.tone(CardCatalog.get(1), 1, 4));
        assertEquals(0, BoardCard.tone(CardCatalog.get(1), 4, 4));
        int stripesAt = -1;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).id() == stripes.id()) {
                if (stripesAt < 0) {
                    stripesAt = i;
                } else {
                    assertTrue(i == stripesAt + 1);
                }
            }
        }
    }
}
