package gwent.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardCatalogTest {
    @Test
    void loadsSkinsAsSeparateCardsAndKeepsCollectionCounts() {
        assertEquals(3, CardCatalog.get(1).copies());
        assertEquals("Гребаная пехтура", CardCatalog.get(1).name());
        assertEquals(1, CardCatalog.get(4).copies());
        assertEquals(1, CardCatalog.get(5).copies());
        assertEquals(1, CardCatalog.get(6).copies());
        assertEquals("Вес", CardCatalog.get(13).name());
        assertEquals(1, CardCatalog.get(164).copies());
        assertEquals(0, CardCatalog.get(165).copies());
        assertFalse(CardCatalog.get(165).inCollection());
        assertFalse(CardCatalog.get(155).inCollection());
        assertEquals("/gwent/cards/North/10.png", CardCatalog.get(10).imagePath());
        assertEquals("/gwent/cards/North/10_full.png", CardCatalog.get(10).fullImagePath());
        assertEquals("/gwent/cards/Special/169.png", CardCatalog.get(169).imagePath());
        assertEquals("/gwent/cards/North/201.png", CardCatalog.get(201).fullImagePath());
    }

    @Test
    void neutralsAreNotAPlayableFaction() {
        assertTrue(CardCatalog.leaders(Faction.NEUTRAL).isEmpty());
        assertTrue(CardCatalog.collection(Faction.NORTHERN_REALMS).stream()
                .anyMatch(card -> card.faction() == Faction.NEUTRAL));
        assertTrue(CardCatalog.collection(Faction.NORTHERN_REALMS).stream()
                .noneMatch(card -> card.faction() == Faction.NILFGAARD));
        assertEquals(5, CardCatalog.leaders(Faction.NORTHERN_REALMS).size());
    }

    @Test
    void musterGroupsAndSpiesComeFromTheFile() {
        assertEquals("crones", CardCatalog.get(98).musterGroup());
        assertEquals("crones", CardCatalog.get(99).musterGroup());
        assertEquals("vampires", CardCatalog.get(86).musterGroup());
        assertEquals("arachas", CardCatalog.get(103).musterGroup());
        assertEquals(PlaysOn.OPPONENT, CardCatalog.get(11).playsOn());
        assertEquals(2, CardCatalog.get(11).spyDraw());
        assertEquals(1, CardCatalog.get(156).spyDraw());
        assertTrue(CardCatalog.get(105).agile());
        assertTrue(CardCatalog.get(105).hero());
        assertEquals(Ability.MORALE, CardCatalog.get(105).ability());
        assertEquals("Прочная связь", SkillTexts.get("TIGHT_BOND").name());
        assertThrows(IllegalArgumentException.class, () -> CardCatalog.get(9999));
    }
}
