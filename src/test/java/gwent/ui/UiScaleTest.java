package gwent.ui;

import gwent.ui.card.CardStrip;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiScaleTest {
    @Test
    void referenceWindowIsFullSizeAndSmallWindowShrinks() {
        assertEquals(1, UiScale.of(2560, 1440), 0.001);
        assertEquals(UiScale.MIN, UiScale.of(1000, 600), 0.001);
        assertEquals(UiScale.MAX, UiScale.of(4000, 3000), 0.001);
        assertTrue(UiScale.of(1920, 1080) < 1);
    }

    @Test
    void rowThatFitsStaysCenteredAsAGroup() {
        double x = CardStrip.offset(1000, 100, 20, 5, 0);
        assertEquals(210, x, 0.001);
    }

    @Test
    void selectedCardSitsInTheMiddleWhenTheRowDoesNotFit() {
        double view = 300;
        double slot = 100;
        double gap = 0;
        double x = CardStrip.offset(view, slot, gap, 5, 2);
        double thirdCenter = x + 2 * slot + slot / 2;
        assertEquals(view / 2, thirdCenter, 0.001);
    }
}
