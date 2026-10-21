package gwent.ui.card;

/**
 * Какой кусок полной картинки показывать.
 * В списке колоды картинка остаётся целиком: цитату закрывает пергамент в {@code CardFace}.
 * В слоте лидера виден титул в две строки, без цитаты (около 508 из 585).
 */
public enum CardCrop {
    FULL(1),
    DECK(494.0 / 585.0),
    LEADER_SLOT(508.0 / 585.0);

    private final double visibleFraction;

    CardCrop(double visibleFraction) {
        this.visibleFraction = visibleFraction;
    }

    public double visibleFraction() {
        return visibleFraction;
    }
}
