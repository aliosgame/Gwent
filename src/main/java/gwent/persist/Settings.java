package gwent.persist;

/**
 * Пользовательские настройки. Громкость хранится целым числом 0–100,
 * чтобы файл можно было поправить руками.
 */
public record Settings(
        int musicVolume,
        int effectsVolume,
        boolean fullscreen,
        int windowWidth,
        int windowHeight,
        int windowX,
        int windowY
) {
    public static final int MIN_WIDTH = 1600;
    public static final int MIN_HEIGHT = 900;

    public Settings {
        musicVolume = clamp(musicVolume, 0, 100);
        effectsVolume = clamp(effectsVolume, 0, 100);
        if (windowWidth < MIN_WIDTH) {
            windowWidth = MIN_WIDTH;
        }
        if (windowHeight < MIN_HEIGHT) {
            windowHeight = MIN_HEIGHT;
        }
    }

    public static Settings defaults() {
        // -1, -1: окно ещё не ставили, при запуске центрируем на основном экране.
        return new Settings(70, 80, false, 1280, 720, -1, -1);
    }

    public double musicLevel() {
        return musicVolume / 100.0;
    }

    public double effectsLevel() {
        return effectsVolume / 100.0;
    }

    public Settings withFullscreen(boolean value) {
        return new Settings(musicVolume, effectsVolume, value, windowWidth, windowHeight, windowX, windowY);
    }

    public Settings withWindow(int width, int height, int x, int y, boolean fullscreen) {
        return new Settings(musicVolume, effectsVolume, fullscreen, width, height, x, y);
    }

    private static int clamp(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }
}
