package gwent.rules;

import gwent.catalog.CardCatalog;
import gwent.catalog.CardDef;
import gwent.catalog.Faction;
import gwent.catalog.Row;
import gwent.deck.StarterDecks;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchTest {
    private final CardDef infantry = CardCatalog.get(1);
    private final CardDef dijkstra = CardCatalog.get(11);
    private final CardDef elf = CardCatalog.get(156);
    private final CardDef vernon = CardCatalog.get(26);
    private final CardDef medic = CardCatalog.get(19);
    private final CardDef decoy = CardCatalog.get(166);
    private final CardDef horn = CardCatalog.get(167);
    private final CardDef scorch = CardCatalog.get(168);
    private final CardDef frost = CardCatalog.get(169);
    private final CardDef clear = CardCatalog.get(172);
    private final CardDef dragon = CardCatalog.get(149);
    private final CardDef ves = CardCatalog.get(13);
    private final CardDef siegfried = CardCatalog.get(14);
    private final CardDef redanian = CardCatalog.get(2);
    private final CardDef sheldon = CardCatalog.get(9);
    private final CardDef sabrina = CardCatalog.get(12);

    @Test
    void openingHandIsTenAndMulliganPutsCardsBack() {
        Match match = Match.start(StarterDecks.create(Faction.NORTHERN_REALMS), StarterDecks.create(Faction.NILFGAARD), new Random(1));
        assertEquals(Match.Phase.MULLIGAN, match.phase());
        assertEquals(10, match.hand(Side.PLAYER).size());
        int deck = match.deckSize(Side.PLAYER);
        CardDef first = match.hand(Side.PLAYER).get(0);
        match.mulligan(Side.PLAYER, List.of(0));
        assertEquals(10, match.hand(Side.PLAYER).size());
        assertEquals(deck, match.deckSize(Side.PLAYER));
        assertThrows(IllegalMove.class, () -> match.mulligan(Side.PLAYER, List.of(0)));
        assertTrue(match.deckSize(Side.PLAYER) >= 0);
        assertTrue(first != null);
    }

    @Test
    void spySitsOnTheOpponentAndDraws() {
        Match match = prepared(List.of(dijkstra), List.of(), List.of(infantry, infantry), List.of());
        int hand = match.hand(Side.PLAYER).size();
        match.play(Side.PLAYER, new Move.Unit(0, Row.MELEE));
        assertEquals(1, match.field().units(Side.OPPONENT, Row.MELEE).size());
        assertEquals(0, match.field().units(Side.PLAYER, Row.MELEE).size());
        assertEquals(hand - 1 + 2, match.hand(Side.PLAYER).size());
        assertEquals(4, match.field().total(Side.OPPONENT));
    }

    @Test
    void mysteriousElfDrawsOne() {
        Match match = prepared(List.of(elf), List.of(), List.of(infantry), List.of());
        match.play(Side.PLAYER, new Move.Unit(0, Row.MELEE));
        assertEquals(1, match.hand(Side.PLAYER).size());
    }

    @Test
    void decoyReturnsOwnUnitAndStealsEnemySpy() {
        Match stolen = prepared(List.of(infantry, decoy), List.of(dijkstra), List.of(), List.of());
        stolen.play(Side.PLAYER, new Move.Unit(0, Row.MELEE));
        stolen.play(Side.OPPONENT, new Move.Unit(0, Row.MELEE));
        int spyId = stolen.field().units(Side.PLAYER, Row.MELEE).get(1).placementId();
        stolen.play(Side.PLAYER, new Move.Decoy(0, spyId));
        assertEquals(dijkstra, stolen.hand(Side.PLAYER).getFirst());

        Match own = prepared(List.of(infantry, decoy), List.of(), List.of(), List.of());
        own.play(Side.PLAYER, new Move.Unit(0, Row.MELEE));
        own.play(Side.OPPONENT, new Move.Pass());
        own.play(Side.PLAYER, new Move.Decoy(0, own.field().units(Side.PLAYER, Row.MELEE).getFirst().placementId()));
        assertEquals(infantry, own.hand(Side.PLAYER).getFirst());
    }

    @Test
    void decoyDoesNotTakeHero() {
        Match match = prepared(List.of(vernon, decoy), List.of(), List.of(), List.of());
        match.play(Side.PLAYER, new Move.Unit(0, Row.MELEE));
        match.play(Side.OPPONENT, new Move.Pass());
        int id = match.field().units(Side.PLAYER, Row.MELEE).getFirst().placementId();
        assertThrows(IllegalMove.class, () -> match.play(Side.PLAYER, new Move.Decoy(0, id)));
        assertEquals(1, match.hand(Side.PLAYER).size());
    }

    @Test
    void medicRevivesNonHeroFromOwnDiscard() {
        Match match = prepared(List.of(infantry, scorch, medic), List.of(), List.of(), List.of());
        match.play(Side.PLAYER, new Move.Unit(0, Row.MELEE));
        match.play(Side.OPPONENT, new Move.Pass());
        match.play(Side.PLAYER, new Move.Scorch(0));
        int revive = match.discard(Side.PLAYER).indexOf(infantry);
        assertTrue(revive >= 0);
        match.play(Side.PLAYER, new Move.Medic(0, Row.SIEGE, revive, Row.MELEE));
        assertEquals(1, match.field().units(Side.PLAYER, Row.MELEE).size());
        assertEquals(1, match.field().units(Side.PLAYER, Row.SIEGE).size());
    }

    @Test
    void weatherHornAndScorchFollowTheBoard() {
        Match match = prepared(
                List.of(ves, frost, horn, clear, scorch),
                List.of(ves),
                List.of(),
                List.of());
        match.play(Side.PLAYER, new Move.Unit(0, Row.MELEE));
        match.play(Side.OPPONENT, new Move.Unit(0, Row.MELEE));
        match.play(Side.PLAYER, new Move.Weather(0));
        assertEquals(1, match.field().total(Side.PLAYER));
        assertEquals(1, match.field().total(Side.OPPONENT));
        match.play(Side.OPPONENT, new Move.Pass());
        match.play(Side.PLAYER, new Move.Horn(0, Row.MELEE));
        assertEquals(2, match.field().total(Side.PLAYER));
        match.play(Side.PLAYER, new Move.ClearWeather(0));
        assertEquals(10, match.field().total(Side.PLAYER));
        assertEquals(5, match.field().total(Side.OPPONENT));
        match.play(Side.PLAYER, new Move.Scorch(0));
        assertEquals(0, match.field().total(Side.PLAYER));
        assertEquals(5, match.field().total(Side.OPPONENT));
    }

    @Test
    void heroIgnoresFrost() {
        Match match = prepared(List.of(vernon, frost), List.of(), List.of(), List.of());
        match.play(Side.PLAYER, new Move.Unit(0, Row.MELEE));
        match.play(Side.OPPONENT, new Move.Pass());
        match.play(Side.PLAYER, new Move.Weather(0));
        assertEquals(10, match.field().total(Side.PLAYER));
    }

    @Test
    void dragonScorchesOpposingMeleeWhenTheRowIsHighEnough() {
        Match match = prepared(List.of(sheldon, sabrina, dragon), List.of(ves, siegfried), List.of(), List.of());
        match.play(Side.PLAYER, new Move.Unit(0, Row.RANGED));
        match.play(Side.OPPONENT, new Move.Unit(0, Row.MELEE));
        match.play(Side.PLAYER, new Move.Unit(0, Row.RANGED));
        match.play(Side.OPPONENT, new Move.Unit(0, Row.MELEE));
        assertEquals(10, match.field().rowTotal(Side.OPPONENT, Row.MELEE));
        match.play(Side.PLAYER, new Move.Unit(0, Row.MELEE));
        assertEquals(0, match.field().rowTotal(Side.OPPONENT, Row.MELEE));
        assertEquals(7, match.field().rowTotal(Side.PLAYER, Row.MELEE));
    }

    @Test
    void nilfgaardTakesTheDrawnRoundAndNorthDrawsAfterAWin() {
        Match match = prepared(List.of(ves), List.of(ves), List.of(redanian), List.of());
        match.play(Side.PLAYER, new Move.Unit(0, Row.MELEE));
        match.play(Side.OPPONENT, new Move.Unit(0, Row.MELEE));
        match.play(Side.PLAYER, new Move.Pass());
        match.play(Side.OPPONENT, new Move.Pass());
        assertEquals(0, match.gems(Side.PLAYER));
        assertEquals(1, match.gems(Side.OPPONENT));
        assertEquals(2, match.round());
        assertEquals(Side.OPPONENT, match.first());
        assertEquals(0, match.hand(Side.PLAYER).size());

        Match north = prepared(List.of(ves), List.of(redanian), List.of(redanian), List.of());
        north.play(Side.PLAYER, new Move.Unit(0, Row.MELEE));
        north.play(Side.OPPONENT, new Move.Unit(0, Row.MELEE));
        north.play(Side.PLAYER, new Move.Pass());
        north.play(Side.OPPONENT, new Move.Pass());
        assertEquals(1, north.gems(Side.PLAYER));
        assertEquals(1, north.hand(Side.PLAYER).size());
        assertEquals(0, north.deckSize(Side.PLAYER));
        assertEquals(false, north.field().weather(Row.MELEE));
    }

    @Test
    void weatherStaysAndTwoWinsEndTheMatch() {
        Match weather = prepared(List.of(frost), List.of(redanian), List.of(), List.of());
        weather.play(Side.PLAYER, new Move.Weather(0));
        weather.play(Side.OPPONENT, new Move.Pass());
        weather.play(Side.PLAYER, new Move.Pass());
        assertEquals(2, weather.round());
        assertTrue(weather.field().weather(Row.MELEE));
        assertEquals(Side.OPPONENT, weather.active());
        weather.play(Side.OPPONENT, new Move.Unit(0, Row.MELEE));
        assertEquals(1, weather.field().total(Side.OPPONENT));

        Match match = prepared(List.of(ves, siegfried), List.of(redanian, CardCatalog.get(3)), List.of(), List.of());
        match.play(Side.PLAYER, new Move.Unit(0, Row.MELEE));
        match.play(Side.OPPONENT, new Move.Unit(0, Row.MELEE));
        match.play(Side.PLAYER, new Move.Pass());
        match.play(Side.OPPONENT, new Move.Pass());
        match.play(Side.PLAYER, new Move.Unit(0, Row.MELEE));
        match.play(Side.OPPONENT, new Move.Unit(0, Row.MELEE));
        match.play(Side.PLAYER, new Move.Pass());
        match.play(Side.OPPONENT, new Move.Pass());
        assertEquals(Match.Phase.FINISHED, match.phase());
        assertEquals(Side.PLAYER, match.winner());
    }

    @Test
    void replaceOneUpdatesTheSameSeatAndWaitsForTheRest() {
        Match match = Match.start(
                StarterDecks.create(Faction.NORTHERN_REALMS),
                StarterDecks.create(Faction.NILFGAARD),
                new Random(2));
        int deck = match.deckSize(Side.PLAYER);
        match.replaceOne(Side.PLAYER, 0);
        assertEquals(Match.Phase.MULLIGAN, match.phase());
        assertEquals(1, match.mulligansLeft(Side.PLAYER));
        assertEquals(10, match.hand(Side.PLAYER).size());
        assertEquals(deck, match.deckSize(Side.PLAYER));
        match.endMulligan(Side.PLAYER);
        assertTrue(match.mulliganDone(Side.PLAYER));
        assertEquals(Match.Phase.MULLIGAN, match.phase());
    }

    @Test
    void cannotPlayOutOfTurn() {
        Match match = prepared(List.of(infantry), List.of(infantry), List.of(), List.of());
        assertThrows(IllegalMove.class, () -> match.play(Side.OPPONENT, new Move.Unit(0, Row.MELEE)));
    }

    @Test
    void twoAisFinishALegalMatch() {
        Match match = Match.start(
                StarterDecks.create(Faction.NORTHERN_REALMS),
                StarterDecks.create(Faction.NILFGAARD),
                new Random(3));
        int guard = 0;
        while (match.phase() != Match.Phase.FINISHED && guard < 400) {
            if (match.phase() == Match.Phase.MULLIGAN) {
                if (!match.mulliganDone(Side.PLAYER)) {
                    MatchAi.act(match, Side.PLAYER, MatchAi.Style.AGGRESSIVE);
                } else {
                    MatchAi.act(match, Side.OPPONENT, MatchAi.Style.CAUTIOUS);
                }
            } else {
                MatchAi.act(match, match.active(), match.active() == Side.PLAYER
                        ? MatchAi.Style.AGGRESSIVE
                        : MatchAi.Style.CAUTIOUS);
            }
            guard++;
        }
        assertEquals(Match.Phase.FINISHED, match.phase());
        assertTrue(guard < 400);
    }

    private static Match prepared(List<CardDef> playerHand, List<CardDef> opponentHand,
                                  List<CardDef> playerDeck, List<CardDef> opponentDeck) {
        return Match.prepared(
                Faction.NORTHERN_REALMS,
                Faction.NILFGAARD,
                playerHand,
                opponentHand,
                playerDeck,
                opponentDeck,
                Side.PLAYER);
    }
}
