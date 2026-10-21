package gwent.rules;

public enum Side {
    PLAYER,
    OPPONENT;

    public Side other() {
        return this == PLAYER ? OPPONENT : PLAYER;
    }
}
