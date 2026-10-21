package gwent.catalog;

/** Умение лидера. Здесь только ярлык: само умение сработает в партии, не в силе ряда. */
public enum LeaderAbility {
    PLAY_FROST,
    PLAY_FOG,
    PLAY_RAIN,
    PLAY_ANY_WEATHER,
    CLEAR_WEATHER,
    HORN_MELEE,
    HORN_RANGED,
    HORN_SIEGE,
    SCORCH_MELEE,
    SCORCH_RANGED,
    SCORCH_SIEGE,
    DRAW_AT_START,
    LOOK_AT_HAND,
    DRAW_OPPONENT_DISCARD,
    CANCEL_OPPONENT_LEADER,
    MEDIC_BECOMES_RANDOM,
    RETURN_FROM_DISCARD,
    DISCARD_TWO_DRAW_ONE,
    MOVE_AGILE,
    /** Пассивно удваивает шпионов у обоих игроков. В обычный подсчёт силы не входит. */
    DOUBLE_SPIES
}
