package alios.gwent.config;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Конфигурация параметров игры (разрешение экрана, громкость, полноэкранный режим, mute).
 * Доступ к полям осуществляется напрямую без лишнего бойлерплейта.
 */
public class GameConfig extends BaseConfig {

    private static final double DEFAULT_VOLUME = 0.8;

    public Resolution resolution = Resolution.detectDefault();
    public boolean fullscreen = false;

    public boolean mute = false;
    public double musicVolume = DEFAULT_VOLUME;
    public double sfxVolume = DEFAULT_VOLUME;

    // =========================================================================
    // Constructors and Initialization
    // =========================================================================

    private GameConfig() {
        super("settings.json");
        init();
    }

    @Override
    protected void postLoad() {
        this.musicVolume = clampVolume(musicVolume);
        this.sfxVolume = clampVolume(sfxVolume);
    }

    // =========================================================================
    // Public Business Logic
    // =========================================================================

    @JsonIgnore
    public int getResolutionWidth() {
        return resolution != null ? resolution.getWidth() : Resolution.R_1920_1080.getWidth();
    }

    @JsonIgnore
    public int getResolutionHeight() {
        return resolution != null ? resolution.getHeight() : Resolution.R_1920_1080.getHeight();
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

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
        return SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final GameConfig INSTANCE = new GameConfig();
    }
}
