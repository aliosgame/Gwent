package alios.gwent.config;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

/**
 * Поддерживаемые разрешения экрана в соотношении 16:9.
 */
public enum Resolution {
    R_1280_720(1280, 720),
    R_1600_900(1600, 900),
    R_1920_1080(1920, 1080),
    R_2560_1440(2560, 1440),
    R_3200_1800(3200, 1800),
    R_3840_2160(3840, 2160);

    private final int width;
    private final int height;

    // =========================================================================
    // Constructors
    // =========================================================================

    Resolution(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // =========================================================================
    // Public Business Logic
    // =========================================================================

    /**
     * Автоматически определяет рекомендуемое разрешение (~70% от размеров основного монитора ОС).
     */
    public static Resolution detectDefault() {
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
                return findClosest((int) Math.round(size.width * 0.70), (int) Math.round(size.height * 0.70));
            }
        } catch (Throwable ignored) {
        }
        return R_1920_1080;
    }

    /**
     * Возвращает список поддерживаемых разрешений, которые физически помещаются на монитор пользователя.
     */
    public static List<Resolution> getAvailable() {
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
                List<Resolution> list = new ArrayList<>();
                for (Resolution res : values()) {
                    if (res.width <= size.width && res.height <= size.height) {
                        list.add(res);
                    }
                }
                return list.isEmpty() ? List.of(R_1280_720) : List.copyOf(list);
            }
        } catch (Throwable ignored) {
        }
        return List.of(values());
    }

    // =========================================================================
    // Getters and Setters
    // =========================================================================

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return width + " x " + height;
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private static Resolution findClosest(int targetWidth, int targetHeight) {
        Resolution best = R_1920_1080;
        double minDiff = Double.MAX_VALUE;

        for (Resolution candidate : values()) {
            double dw = candidate.width - targetWidth;
            double dh = candidate.height - targetHeight;
            double distSq = dw * dw + dh * dh;

            if (distSq < minDiff) {
                minDiff = distSq;
                best = candidate;
            }
        }
        return best;
    }
}
