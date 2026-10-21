package gwent.ui;

import gwent.persist.Settings;
import gwent.persist.SettingsStore;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * То, что живёт всё время, пока открыто окно: сцена, настройки и музыка меню.
 */
public final class AppContext {
    private final Stage stage;
    private final SettingsStore store;
    private final MenuMusic music;
    private Settings settings;

    public AppContext(Stage stage, SettingsStore store, Settings settings, MenuMusic music) {
        this.stage = stage;
        this.store = store;
        this.settings = settings;
        this.music = music;
    }

    public Stage stage() {
        return stage;
    }

    public Settings settings() {
        return settings;
    }

    public MenuMusic music() {
        return music;
    }

    public void save(Settings next) throws IOException {
        store.save(next);
        settings = next;
        music.setVolume(next.musicLevel());
    }
}
