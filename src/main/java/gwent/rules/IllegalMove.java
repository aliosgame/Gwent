package gwent.rules;

/** Ход нарушает правила партии. Текст можно показать игроку. */
public final class IllegalMove extends IllegalStateException {
    public IllegalMove(String message) {
        super(message);
    }
}
