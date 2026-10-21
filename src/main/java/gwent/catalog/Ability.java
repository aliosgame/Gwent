package gwent.catalog;

/**
 * Способность карты. Герой и проворство — отдельные признаки:
 * у Кейрана одновременно герой, проворство и прилив сил.
 */
public enum Ability {
    NONE,
    TIGHT_BOND,
    MORALE,
    /** Отряд сам кладёт рог на свой ряд и себя не удваивает. Так устроен Лютик. */
    HORN,
    SPY,
    MEDIC,
    MUSTER,
    SCORCH_MELEE,
    SCORCH_RANGED,
    SCORCH_SIEGE,
    /** Корова: уходя с поля, зовёт другой отряд. */
    SUMMON_AVENGER
}
