package gwent.rules;

import gwent.catalog.Ability;
import gwent.catalog.CardDef;
import gwent.catalog.CardKind;
import gwent.catalog.Row;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Поле для подсчёта силы. Партии здесь нет: только ряды, погода и рог.
 * <p>
 * Порядок как в мини-игре «Ведьмака 3»:
 * погода ставит не-герою 1, прочная связь умножает это число,
 * рог удваивает, прилив сил добавляет +1 уже после рога.
 * Герой не меняется, но его прилив сил действует на остальных.
 */
public final class Battlefield {
    private final EnumMap<Row, Boolean> weather = new EnumMap<>(Row.class);
    private final EnumMap<Side, EnumMap<Row, Line>> lines = new EnumMap<>(Side.class);
    private int nextId = 1;

    public Battlefield() {
        for (Row row : Row.values()) {
            weather.put(row, false);
        }
        for (Side side : Side.values()) {
            EnumMap<Row, Line> rows = new EnumMap<>(Row.class);
            for (Row row : Row.values()) {
                rows.put(row, new Line());
            }
            lines.put(side, rows);
        }
    }

    public PlacedCard place(Side side, Row row, CardDef card) {
        if (card.kind() != CardKind.UNIT) {
            throw new IllegalArgumentException("На ряд кладётся только отряд: " + card.id());
        }
        if (!accepts(card, row)) {
            throw new IllegalArgumentException("Карта " + card.id() + " не встаёт в ряд " + row);
        }
        Line line = line(side, row);
        if (card.ability() == Ability.HORN && line.horn) {
            throw new IllegalStateException("В этом ряду уже есть командирский рог");
        }
        PlacedCard placed = new PlacedCard(nextId++, card);
        line.units.add(placed);
        if (card.ability() == Ability.HORN) {
            line.horn = true;
            line.hornSource = placed.placementId();
        }
        return placed;
    }

    public void setHorn(Side side, Row row) {
        Line line = line(side, row);
        if (line.horn) {
            throw new IllegalStateException("В этом ряду уже есть командирский рог");
        }
        line.horn = true;
        line.hornSource = -1;
    }

    public void setWeather(Row row) {
        weather.put(row, true);
    }

    public void clearWeather() {
        for (Row row : Row.values()) {
            weather.put(row, false);
        }
    }

    public boolean weather(Row row) {
        return weather.get(row);
    }

    public void remove(PlacedCard placed) {
        for (Side side : Side.values()) {
            for (Row row : Row.values()) {
                Line line = line(side, row);
                if (line.units.remove(placed)) {
                    if (line.hornSource == placed.placementId()) {
                        line.horn = false;
                        line.hornSource = -1;
                    }
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Этой карты нет на поле");
    }

    public List<PlacedCard> units(Side side, Row row) {
        return List.copyOf(line(side, row).units);
    }

    public int strength(Side side, Row row, PlacedCard placed) {
        Line line = line(side, row);
        if (!line.units.contains(placed)) {
            throw new IllegalArgumentException("Карта не в этом ряду");
        }
        return strength(placed, line, weather.get(row));
    }

    public int rowTotal(Side side, Row row) {
        Line line = line(side, row);
        boolean weathered = weather.get(row);
        int sum = 0;
        for (PlacedCard placed : line.units) {
            sum += strength(placed, line, weathered);
        }
        return sum;
    }

    public int total(Side side) {
        int sum = 0;
        for (Row row : Row.values()) {
            sum += rowTotal(side, row);
        }
        return sum;
    }

    /** Казнь: все не-герои с наибольшей текущей силой на всём поле. */
    public List<PlacedCard> scorchTargets() {
        int max = Integer.MIN_VALUE;
        List<PlacedCard> targets = new ArrayList<>();
        for (Side side : Side.values()) {
            for (Row row : Row.values()) {
                Line line = line(side, row);
                boolean weathered = weather.get(row);
                for (PlacedCard placed : line.units) {
                    if (placed.card().hero()) {
                        continue;
                    }
                    int power = strength(placed, line, weathered);
                    if (power > max) {
                        max = power;
                        targets.clear();
                        targets.add(placed);
                    } else if (power == max) {
                        targets.add(placed);
                    }
                }
            }
        }
        return List.copyOf(targets);
    }

    /**
     * Рядовая казнь, как у Виллентретенмерта и части лидеров.
     * Порог считается по всему ряду, включая героев. Снимаются только не-герои.
     */
    public List<PlacedCard> rowScorchTargets(Side side, Row row, int minimum) {
        if (rowTotal(side, row) < minimum) {
            return List.of();
        }
        Line line = line(side, row);
        boolean weathered = weather.get(row);
        int max = Integer.MIN_VALUE;
        List<PlacedCard> targets = new ArrayList<>();
        for (PlacedCard placed : line.units) {
            if (placed.card().hero()) {
                continue;
            }
            int power = strength(placed, line, weathered);
            if (power > max) {
                max = power;
                targets.clear();
                targets.add(placed);
            } else if (power == max) {
                targets.add(placed);
            }
        }
        return List.copyOf(targets);
    }

    private static boolean accepts(CardDef card, Row row) {
        return card.rows().contains(row);
    }

    private int strength(PlacedCard placed, Line line, boolean weathered) {
        CardDef card = placed.card();
        int power = card.hero() ? card.strength() : (weathered ? 1 : card.strength());
        if (!card.hero() && card.ability() == Ability.TIGHT_BOND) {
            power *= bondCount(line, card);
        }
        if (!card.hero() && line.horn && line.hornSource != placed.placementId()) {
            power *= 2;
        }
        if (!card.hero()) {
            power += moraleFromOthers(line, placed);
        }
        return power;
    }

    private static int bondCount(Line line, CardDef card) {
        int count = 0;
        for (PlacedCard other : line.units) {
            if (other.card().ability() == Ability.TIGHT_BOND && other.card().name().equals(card.name())) {
                count++;
            }
        }
        return count;
    }

    private static int moraleFromOthers(Line line, PlacedCard placed) {
        int bonus = 0;
        for (PlacedCard other : line.units) {
            if (other.placementId() != placed.placementId() && other.card().ability() == Ability.MORALE) {
                bonus++;
            }
        }
        return bonus;
    }

    private Line line(Side side, Row row) {
        return lines.get(side).get(row);
    }

    private static final class Line {
        private final List<PlacedCard> units = new ArrayList<>();
        private boolean horn;
        private int hornSource = -1;
    }
}
