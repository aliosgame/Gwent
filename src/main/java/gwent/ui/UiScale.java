package gwent.ui;

/**
 * Один масштаб на всё окно. Опорный размер — 2560×1440:
 * там карты совпадают с файлом один к одному.
 * Windows сама переводит DPI в логические пиксели, поэтому 4К при 150 %
 * не требует отдельного пресета.
 */
public final class UiScale {
    public static final double REFERENCE_WIDTH = 2560;
    public static final double REFERENCE_HEIGHT = 1440;
    public static final double MIN = 0.55;
    public static final double MAX = 1.15;

    private UiScale() {
    }

    public static double of(double width, double height) {
        if (width <= 0 || height <= 0) {
            return 1;
        }
        double raw = Math.min(width / REFERENCE_WIDTH, height / REFERENCE_HEIGHT);
        return Math.clamp(raw, MIN, MAX);
    }

    public static double px(double base, double scale) {
        return Math.round(base * scale);
    }
}
