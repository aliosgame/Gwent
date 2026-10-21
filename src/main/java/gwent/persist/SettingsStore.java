package gwent.persist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

/**
 * Читает и пишет settings.json. Битый файл не роняет запуск:
 * он переименовывается в settings.json.broken, вместо него пишутся значения по умолчанию.
 */
public final class SettingsStore {
    private static final Logger LOG = Logger.getLogger(SettingsStore.class.getName());

    private final Path file;

    public SettingsStore(Path file) {
        this.file = file;
    }

    public Settings loadOrCreate() throws IOException {
        Files.createDirectories(file.getParent());
        if (Files.notExists(file)) {
            Settings created = Settings.defaults();
            write(created);
            return created;
        }
        try {
            return SettingsJson.parse(Files.readString(file));
        } catch (SettingsFormatException ex) {
            Path broken = file.resolveSibling(file.getFileName() + ".broken");
            Files.move(file, broken, StandardCopyOption.REPLACE_EXISTING);
            LOG.warning("Файл настроек повреждён и сохранён как " + broken + ". " + ex.getMessage());
            Settings created = Settings.defaults();
            write(created);
            return created;
        }
    }

    public void save(Settings settings) throws IOException {
        Files.createDirectories(file.getParent());
        write(settings);
    }

    private void write(Settings settings) throws IOException {
        Files.writeString(file, SettingsJson.format(settings));
    }
}
