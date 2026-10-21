package gwent.persist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsStoreTest {
    @TempDir
    Path dir;

    @Test
    void createsDefaultFileWhenMissing() throws IOException {
        Path file = dir.resolve("settings.json");
        Settings loaded = new SettingsStore(file).loadOrCreate();

        assertEquals(Settings.defaults(), loaded);
        assertTrue(Files.exists(file));
        assertEquals(loaded, new SettingsStore(file).loadOrCreate());
    }

    @Test
    void roundTripKeepsValues() throws IOException {
        Path file = dir.resolve("settings.json");
        Settings custom = new Settings(10, 20, true, 1600, 900, 40, 50);

        new SettingsStore(file).save(custom);

        assertEquals(custom, new SettingsStore(file).loadOrCreate());
        assertTrue(Files.readString(file).contains("\"musicVolume\": 10"));
    }

    @Test
    void clampsVolumeAndWindow() {
        Settings settings = new Settings(140, -5, false, 100, 100, 0, 0);

        assertEquals(100, settings.musicVolume());
        assertEquals(0, settings.effectsVolume());
        assertEquals(Settings.MIN_WIDTH, settings.windowWidth());
        assertEquals(Settings.MIN_HEIGHT, settings.windowHeight());
    }

    @Test
    void missingKeysAndTrailingCommaUseDefaults() throws IOException {
        Path file = dir.resolve("settings.json");
        Files.writeString(file, """
                {
                  "musicVolume": 15,
                }
                """);

        Settings loaded = new SettingsStore(file).loadOrCreate();

        assertEquals(15, loaded.musicVolume());
        assertEquals(Settings.defaults().effectsVolume(), loaded.effectsVolume());
        assertEquals(Settings.defaults().windowWidth(), loaded.windowWidth());
        assertEquals(-1, loaded.windowX());
    }

    @Test
    void brokenFileIsReplacedWithDefaults() throws IOException {
        Path file = dir.resolve("settings.json");
        Files.writeString(file, "это не json");

        Settings loaded = new SettingsStore(file).loadOrCreate();

        assertEquals(Settings.defaults(), loaded);
        assertEquals("это не json", Files.readString(dir.resolve("settings.json.broken")).strip());
        assertTrue(Files.readString(file).contains("\"fullscreen\": false"));
    }

    @Test
    void saveCreatesMissingDirectory() throws IOException {
        Path file = dir.resolve("nested").resolve("settings.json");
        Settings custom = new Settings(5, 6, false, 1280, 720, 8, 9);

        new SettingsStore(file).save(custom);

        assertEquals(custom, new SettingsStore(file).loadOrCreate());
    }
}
