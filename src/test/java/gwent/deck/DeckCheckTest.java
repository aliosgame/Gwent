package gwent.deck;

import gwent.catalog.Faction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckCheckTest {
    @TempDir
    Path dir;

    @Test
    void starterDecksAreLegalAndOnePerFaction() {
        for (Faction faction : new Faction[] {
                Faction.NORTHERN_REALMS, Faction.NILFGAARD, Faction.SCOIATAEL, Faction.MONSTERS}) {
            Deck deck = StarterDecks.create(faction);
            assertEquals(faction, deck.faction());
            assertTrue(DeckCheck.ok(deck), DeckCheck.problem(deck));
            assertTrue(DeckCheck.units(deck) >= DeckCheck.MIN_UNITS);
            assertTrue(DeckCheck.specials(deck) <= DeckCheck.MAX_SPECIALS);
        }
    }

    @Test
    void rejectsAShortDeckAndTooManyCopies() {
        Deck shortDeck = new Deck(Faction.NORTHERN_REALMS, 201, java.util.List.of(1));
        assertFalse(DeckCheck.ok(shortDeck));

        Deck crowded = StarterDecks.create(Faction.NORTHERN_REALMS).plus(1).plus(1).plus(1);
        assertFalse(DeckCheck.problem(crowded).isEmpty());
    }

    @Test
    void storeKeepsTheEditedDeck() {
        DeckStore store = DeckStore.loadOrCreate(dir.resolve("decks.json"));
        Deck edited = store.get(Faction.NILFGAARD).withLeader(208);
        store.put(edited);

        DeckStore again = DeckStore.loadOrCreate(dir.resolve("decks.json"));
        assertEquals(208, again.get(Faction.NILFGAARD).leaderId());
        assertEquals(edited.cardIds(), again.get(Faction.NILFGAARD).cardIds());
    }
}
