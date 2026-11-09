package alios.gwent.config;

/**
 * Конфигурация параметров игры (разрешение экрана, громкость, полноэкранный режим).
 * Доступ к полям осуществляется напрямую без лишнего бойлерплейта.
 */
public class GameConfig extends BaseConfig {

    public static final int MIN_WIDTH = 1600;
    public static final int MIN_HEIGHT = 900;
    private static final double DEFAULT_VOLUME = 0.8;

    public volatile int resolutionWidth = 1920;
    public volatile int resolutionHeight = 1080;
    public volatile boolean fullscreen = false;

    public volatile double masterVolume = DEFAULT_VOLUME;
    public volatile double musicVolume = DEFAULT_VOLUME;
    public volatile double sfxVolume = DEFAULT_VOLUME;

    private GameConfig() {
        super("settings.json");
        init();
    }

    @Override
    protected void postLoad() {
        int width = Math.max(resolutionWidth, MIN_WIDTH);
        int height = Math.max(resolutionHeight, MIN_HEIGHT);
        double master = clampVolume(masterVolume);
        double music = clampVolume(musicVolume);
        double sfx = clampVolume(sfxVolume);

        this.resolutionWidth = width;
        this.resolutionHeight = height;
        this.masterVolume = master;
        this.musicVolume = music;
        this.sfxVolume = sfx;
    }

    private static double clampVolume(double volume) {
        if (Double.isNaN(volume)) {
            return DEFAULT_VOLUME;
        }
        return Math.clamp(volume, 0.0, 1.0);
    }

    // =========================================================================
    // Singleton
    // =========================================================================

    public static GameConfig get() {
        return Holder.INSTANCE;
    }

    private static class Holder {
        private static final GameConfig INSTANCE = new GameConfig();
    }
}
