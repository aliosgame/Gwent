package alios.gwent.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigTest {

    @Test
    @DisplayName("Singleton instance provides initialized configuration")
    void testSingletonInstance() {
        GameConfig config = GameConfig.get();
        assertNotNull(config);
        assertSame(config, GameConfig.get());

        assertTrue(config.resolutionWidth >= GameConfig.MIN_WIDTH);
        assertTrue(config.resolutionHeight >= GameConfig.MIN_HEIGHT);
        assertTrue(config.masterVolume >= 0.0 && config.masterVolume <= 1.0);
        assertTrue(config.musicVolume >= 0.0 && config.musicVolume <= 1.0);
        assertTrue(config.sfxVolume >= 0.0 && config.sfxVolume <= 1.0);
    }

    @Test
    @DisplayName("Enforces minimum resolution boundaries via postLoad()")
    void testEnforcesMinimumResolution() {
        GameConfig config = GameConfig.get();
        int originalWidth = config.resolutionWidth;
        int originalHeight = config.resolutionHeight;

        try {
            config.resolutionWidth = 800;
            config.resolutionHeight = 600;
            config.postLoad();

            assertEquals(GameConfig.MIN_WIDTH, config.resolutionWidth);
            assertEquals(GameConfig.MIN_HEIGHT, config.resolutionHeight);
        } finally {
            config.resolutionWidth = originalWidth;
            config.resolutionHeight = originalHeight;
        }
    }

    @Test
    @DisplayName("ConfigFile exists in WORK_DIR")
    void testConfigFileExists() {
        GameConfig config = GameConfig.get();
        assertNotNull(config.getConfigFile());
        assertEquals("settings.json", config.getConfigFile().getName());
        assertEquals(BaseConfig.WORK_DIR, config.getConfigFile().getParentFile());
        assertTrue(config.getConfigFile().exists(), "settings.json must be created/synchronized on disk");
    }
}
