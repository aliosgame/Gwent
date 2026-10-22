package gwent.rules;

import gwent.catalog.Row;

/** Ход одной стороны. Тот же набор потом уйдёт по сети. */
public sealed interface Move {

    /** Пас до конца раунда. */
    record Pass() implements Move {
    }

    /** Отряд на свой ряд или, если это шпион, на ряд противника. */
    record Unit(int handIndex, Row row) implements Move {
    }

    record Horn(int handIndex, Row row) implements Move {
    }

    record Weather(int handIndex) implements Move {
    }

    record ClearWeather(int handIndex) implements Move {
    }

    record Scorch(int handIndex) implements Move {
    }

    /**
     * Чучело забирает не-героя со своей стороны в руку.
     * Чужого шпиона, лежащего у вас, чучело тоже забирает — уже в вашу руку.
     */
    record Decoy(int handIndex, int placementId) implements Move {
    }

    /**
     * Медик встаёт на свой ряд и поднимает не-героя из своего отбоя.
     * {@code discardIndex} равен -1, только если поднимать некого.
     * Ряд поднятого нужен проворной карте; у обычной он обязан совпасть с её рядом.
     */
    record Medic(int handIndex, Row row, int discardIndex, Row revivedRow) implements Move {
    }
}
