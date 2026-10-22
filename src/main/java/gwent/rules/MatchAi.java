package gwent.rules;

import gwent.catalog.CardDef;
import gwent.catalog.PlaysOn;
import gwent.catalog.Row;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Две манеры без просчёта наперёд.
 * Задира кладёт самую сильную карту и пасует, только когда противник уже спасовал и мы впереди.
 * Осторожный сначала сбрасывает мелочь и пасует, как только выигрывает с запасом.
 */
public final class MatchAi {
    public enum Style {
        AGGRESSIVE,
        CAUTIOUS
    }

    private MatchAi() {
    }

    public static void act(Match match, Side side, Style style) {
        if (match.phase() == Match.Phase.MULLIGAN) {
            match.mulligan(side, mulligan(match, side, style));
            return;
        }
        match.play(side, nextMove(match, side, style));
    }

    /** Ход, который ИИ сделает сейчас. Карта ещё в руке, её имя можно показать до розыгрыша. */
    public static Move nextMove(Match match, Side side, Style style) {
        return choose(match, side, style);
    }

    private static List<Integer> mulligan(Match match, Side side, Style style) {
        int weakAt = style == Style.AGGRESSIVE ? 2 : 5;
        List<Integer> weak = new ArrayList<>();
        List<CardDef> hand = match.hand(side);
        for (int i = 0; i < hand.size(); i++) {
            CardDef card = hand.get(i);
            if (!card.hero() && card.strength() <= weakAt) {
                weak.add(i);
            }
        }
        weak.sort(Comparator.comparingInt(index -> hand.get(index).strength()));
        if (weak.size() > Match.MULLIGAN) {
            return List.copyOf(weak.subList(0, Match.MULLIGAN));
        }
        return List.copyOf(weak);
    }

    private static Move choose(Match match, Side side, Style style) {
        int own = match.field().total(side);
        int foe = match.field().total(side.other());
        boolean foePassed = match.passed(side.other());
        if (style == Style.CAUTIOUS && own > foe && (foePassed || own >= foe + 10)) {
            return new Move.Pass();
        }
        if (style == Style.AGGRESSIVE && foePassed && own > foe) {
            return new Move.Pass();
        }
        Move chosen = null;
        int best = style == Style.AGGRESSIVE ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (Move move : match.legal(side)) {
            if (move instanceof Move.Pass) {
                continue;
            }
            int value = value(match, side, move);
            boolean better = style == Style.AGGRESSIVE ? value > best : value < best;
            if (chosen == null || better) {
                chosen = move;
                best = value;
            }
        }
        return chosen == null ? new Move.Pass() : chosen;
    }

    private static int value(Match match, Side side, Move move) {
        return switch (move) {
            case Move.Pass ignored -> 0;
            case Move.Unit unit -> power(match.hand(side).get(unit.handIndex()));
            case Move.Medic medic -> {
                int body = power(match.hand(side).get(medic.handIndex()));
                if (medic.discardIndex() < 0) {
                    yield body;
                }
                yield body + power(match.discard(side).get(medic.discardIndex()));
            }
            case Move.Horn ignored -> 8;
            case Move.Weather weather -> weatherGain(match, side, match.hand(side).get(weather.handIndex()).affects());
            case Move.ClearWeather ignored -> weathered(match) ? 6 : 1;
            case Move.Scorch ignored -> 7;
            case Move.Decoy decoy -> decoyValue(match, side, decoy.placementId());
        };
    }

    private static int power(CardDef card) {
        int bonus = card.playsOn() == PlaysOn.OPPONENT ? 6 : 0;
        return card.strength() + bonus;
    }

    private static int weatherGain(Match match, Side side, Row row) {
        return nonHeroes(match, side.other(), row) - nonHeroes(match, side, row);
    }

    private static int nonHeroes(Match match, Side side, Row row) {
        int count = 0;
        for (PlacedCard placed : match.field().units(side, row)) {
            if (!placed.card().hero()) {
                count++;
            }
        }
        return count;
    }

    private static boolean weathered(Match match) {
        for (Row row : Row.values()) {
            if (match.field().weather(row)) {
                return true;
            }
        }
        return false;
    }

    private static int decoyValue(Match match, Side side, int placementId) {
        for (Row row : Row.values()) {
            for (PlacedCard placed : match.field().units(side, row)) {
                if (placed.placementId() == placementId) {
                    return placed.card().playsOn() == PlaysOn.OPPONENT ? 9 : placed.card().strength();
                }
            }
        }
        return 0;
    }
}
