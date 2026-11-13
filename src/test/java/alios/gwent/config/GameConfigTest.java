package alios.gwent.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigTest {

    @Test
    @DisplayName("Singleton instance provides initialized configuration with auto-detected resolution")
    void testSingletonInstance() {
        GameConfig config = GameConfig.get();
        assertNotNull(config);
        assertSame(config, GameConfig.get());

        assertNotNull(config.resolution);
        assertTrue(config.getResolutionWidth() >= 1280);
        assertTrue(config.getResolutionHeight() >= 720);
        assertTrue(config.musicVolume >= 0.0 && config.musicVolume <= 1.0);
        assertTrue(config.sfxVolume >= 0.0 && config.sfxVolume <= 1.0);
    }

    @Test
    @DisplayName("Resolution detectDefault and getAvailable provide valid defaults")
    void testResolutionDefaults() {
        Resolution def = Resolution.detectDefault();
        assertNotNull(def);

        var available = Resolution.getAvailable();
        assertNotNull(available);
        assertFalse(available.isEmpty());
        assertTrue(available.contains(Resolution.R_1280_720));
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
