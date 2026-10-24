package gwent.rules;

import gwent.catalog.CardCatalog;
import gwent.catalog.CardDef;
import gwent.catalog.Row;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RowStrengthTest {
    private final CardDef stripes = CardCatalog.get(10);
    private final CardDef infantry = CardCatalog.get(1);
    private final CardDef ballista = CardCatalog.get(23);
    private final CardDef expert = CardCatalog.get(4);
    private final CardDef yarpen = CardCatalog.get(8);
    private final CardDef dandelion = CardCatalog.get(141);
    private final CardDef vernon = CardCatalog.get(26);
    private final CardDef kayran = CardCatalog.get(105);
    private final CardDef harpy = CardCatalog.get(80);
    private final CardDef cow = CardCatalog.get(154);
    private final CardDef elf = CardCatalog.get(156);
    private final CardDef dijkstra = CardCatalog.get(11);

    @Test
    void threeBlueStripesAndHornFollowTheWitcherNumbers() {
        Battlefield clear = fieldWithStripes(false, false);
        assertEquals(12, unit(clear, 0));
        assertEquals(36, clear.rowTotal(Side.PLAYER, Row.MELEE));

        Battlefield horn = fieldWithStripes(false, true);
        assertEquals(24, unit(horn, 0));
        assertEquals(72, horn.rowTotal(Side.PLAYER, Row.MELEE));

        Battlefield frost = fieldWithStripes(true, false);
        assertEquals(3, unit(frost, 0));
        assertEquals(9, frost.rowTotal(Side.PLAYER, Row.MELEE));

        Battlefield frostHorn = fieldWithStripes(true, true);
        assertEquals(6, unit(frostHorn, 0));
        assertEquals(18, frostHorn.rowTotal(Side.PLAYER, Row.MELEE));
    }

    @Test
    void tightBondCountsSkinsOfTheSameName() {
        Battlefield field = new Battlefield();
        field.place(Side.PLAYER, Row.MELEE, CardCatalog.get(124));
        field.place(Side.PLAYER, Row.MELEE, CardCatalog.get(125));
        assertEquals(20, field.rowTotal(Side.PLAYER, Row.MELEE));
    }

    @Test
    void loneTightBondCardIsNotMultiplied() {
        Battlefield field = new Battlefield();
        field.place(Side.PLAYER, Row.MELEE, infantry);
        assertEquals(1, field.rowTotal(Side.PLAYER, Row.MELEE));

        field.place(Side.PLAYER, Row.MELEE, infantry);
        assertEquals(4, field.rowTotal(Side.PLAYER, Row.MELEE));
    }

    @Test
    void tightBondDoesNotCrossRowsOrPlayers() {
        Battlefield field = new Battlefield();
        field.place(Side.PLAYER, Row.MELEE, infantry);
        field.place(Side.OPPONENT, Row.MELEE, infantry);
        assertEquals(1, field.rowTotal(Side.PLAYER, Row.MELEE));
        assertEquals(1, field.rowTotal(Side.OPPONENT, Row.MELEE));
    }

    @Test
    void hornDoublesAfterMoraleIsAdded() {
        Battlefield field = new Battlefield();
        PlacedCard siege = field.place(Side.PLAYER, Row.SIEGE, ballista);
        field.place(Side.PLAYER, Row.SIEGE, expert);
        field.setHorn(Side.PLAYER, Row.SIEGE);

        // В оригинале рог удваивает ряд после морали: (6 + 1) * 2 = 14
        assertEquals(14, field.strength(Side.PLAYER, Row.SIEGE, siege));
        assertEquals(2, strengthOf(field, Side.PLAYER, Row.SIEGE, expert));
        assertEquals(16, field.rowTotal(Side.PLAYER, Row.SIEGE));
    }

    @Test
    void severalMoraleCardsBuffEachOtherButNotThemselves() {
        Battlefield field = new Battlefield();
        field.place(Side.PLAYER, Row.SIEGE, expert);
        field.place(Side.PLAYER, Row.SIEGE, expert);
        field.place(Side.PLAYER, Row.SIEGE, expert);
        assertEquals(9, field.rowTotal(Side.PLAYER, Row.SIEGE));
    }

    @Test
    void moraleDoesNotBuffAHeroAndDoesNotCrossRows() {
        Battlefield field = new Battlefield();
        PlacedCard hero = field.place(Side.PLAYER, Row.MELEE, vernon);
        field.place(Side.PLAYER, Row.SIEGE, expert);
        PlacedCard cannon = field.place(Side.PLAYER, Row.SIEGE, ballista);

        assertEquals(10, field.strength(Side.PLAYER, Row.MELEE, hero));
        assertEquals(7, field.strength(Side.PLAYER, Row.SIEGE, cannon));
    }

    @Test
    void heroIgnoresWeatherButHisMoraleStillLands() {
        Battlefield field = new Battlefield();
        field.setWeather(Row.MELEE);
        PlacedCard hero = field.place(Side.PLAYER, Row.MELEE, kayran);
        PlacedCard bird = field.place(Side.PLAYER, Row.MELEE, harpy);

        assertEquals(8, field.strength(Side.PLAYER, Row.MELEE, hero));
        assertEquals(2, field.strength(Side.PLAYER, Row.MELEE, bird));
    }

    @Test
    void dandelionIsTheHornAndDoesNotDoubleHimself() {
        Battlefield field = new Battlefield();
        PlacedCard bard = field.place(Side.PLAYER, Row.MELEE, dandelion);
        PlacedCard dwarf = field.place(Side.PLAYER, Row.MELEE, yarpen);

        assertEquals(2, field.strength(Side.PLAYER, Row.MELEE, bard));
        assertEquals(4, field.strength(Side.PLAYER, Row.MELEE, dwarf));

        field.setWeather(Row.MELEE);
        assertEquals(1, field.strength(Side.PLAYER, Row.MELEE, bard));
        assertEquals(2, field.strength(Side.PLAYER, Row.MELEE, dwarf));
        assertThrows(IllegalStateException.class, () -> field.setHorn(Side.PLAYER, Row.MELEE));
    }

    @Test
    void weatherSetsZeroStrengthUnitsToOneAndLeavesAHeroSpyAtZero() {
        Battlefield field = new Battlefield();
        PlacedCard animal = field.place(Side.PLAYER, Row.RANGED, cow);
        PlacedCard spy = field.place(Side.PLAYER, Row.MELEE, elf);
        assertEquals(0, field.strength(Side.PLAYER, Row.RANGED, animal));
        assertEquals(0, field.strength(Side.PLAYER, Row.MELEE, spy));

        field.setWeather(Row.RANGED);
        field.setWeather(Row.MELEE);
        assertEquals(1, field.strength(Side.PLAYER, Row.RANGED, animal));
        assertEquals(0, field.strength(Side.PLAYER, Row.MELEE, spy));
    }

    @Test
    void weatherIsSharedAndClearWeatherLiftsIt() {
        Battlefield field = new Battlefield();
        field.setWeather(Row.MELEE);
        PlacedCard ours = field.place(Side.PLAYER, Row.MELEE, yarpen);
        PlacedCard theirs = field.place(Side.OPPONENT, Row.MELEE, yarpen);
        PlacedCard cannon = field.place(Side.PLAYER, Row.SIEGE, ballista);

        assertEquals(1, field.strength(Side.PLAYER, Row.MELEE, ours));
        assertEquals(1, field.strength(Side.OPPONENT, Row.MELEE, theirs));
        assertEquals(6, field.strength(Side.PLAYER, Row.SIEGE, cannon));

        field.clearWeather();
        assertEquals(2, field.strength(Side.PLAYER, Row.MELEE, ours));
    }

    @Test
    void spyScoresForTheSideItWasPlacedOn() {
        Battlefield field = new Battlefield();
        field.place(Side.OPPONENT, Row.MELEE, dijkstra);
        assertEquals(0, field.total(Side.PLAYER));
        assertEquals(4, field.total(Side.OPPONENT));
    }

    @Test
    void scorchTakesEveryNonHeroTiedForTheHighestStrength() {
        Battlefield field = new Battlefield();
        field.place(Side.PLAYER, Row.MELEE, vernon);
        PlacedCard left = field.place(Side.PLAYER, Row.SIEGE, ballista);
        PlacedCard right = field.place(Side.OPPONENT, Row.SIEGE, ballista);
        field.place(Side.OPPONENT, Row.MELEE, yarpen);

        assertEquals(List.of(left, right), field.scorchTargets());
    }

    @Test
    void rowScorchRespectsTheThresholdAndSparesHeroes() {
        Battlefield field = new Battlefield();
        PlacedCard dwarf = field.place(Side.OPPONENT, Row.MELEE, yarpen);
        assertTrue(field.rowScorchTargets(Side.OPPONENT, Row.MELEE, 10).isEmpty());

        field.place(Side.OPPONENT, Row.MELEE, vernon);
        assertEquals(List.of(dwarf), field.rowScorchTargets(Side.OPPONENT, Row.MELEE, 10));
    }

    @Test
    void agileCardCanChooseALegalRowOnly() {
        Battlefield field = new Battlefield();
        field.place(Side.PLAYER, Row.RANGED, harpy);
        assertThrows(IllegalArgumentException.class, () -> field.place(Side.PLAYER, Row.SIEGE, harpy));
        assertThrows(IllegalArgumentException.class, () -> field.place(Side.PLAYER, Row.RANGED, yarpen));
    }

    private Battlefield fieldWithStripes(boolean frost, boolean horn) {
        Battlefield field = new Battlefield();
        if (frost) {
            field.setWeather(Row.MELEE);
        }
        field.place(Side.PLAYER, Row.MELEE, stripes);
        field.place(Side.PLAYER, Row.MELEE, stripes);
        field.place(Side.PLAYER, Row.MELEE, stripes);
        if (horn) {
            field.setHorn(Side.PLAYER, Row.MELEE);
        }
        return field;
    }

    private static int unit(Battlefield field, int index) {
        return field.strength(Side.PLAYER, Row.MELEE, field.units(Side.PLAYER, Row.MELEE).get(index));
    }

    private static int strengthOf(Battlefield field, Side side, Row row, CardDef card) {
        return field.units(side, row).stream()
                .filter(placed -> placed.card() == card)
                .mapToInt(placed -> field.strength(side, row, placed))
                .findFirst()
                .orElseThrow();
    }
}
