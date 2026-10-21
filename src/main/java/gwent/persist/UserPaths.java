package gwent.persist;

import java.nio.file.Path;

/**
 * Настройки лежат в домашней папке и не зависят от того, откуда запущен jar.
 */
public final class UserPaths {
    private UserPaths() {
    }

    public static Path root() {
        return Path.of(System.getProperty("user.home"), "Aliosgame", "Gwent");
    }

    public static Path settingsFile() {
        return root().resolve("settings.json");
    }
}
