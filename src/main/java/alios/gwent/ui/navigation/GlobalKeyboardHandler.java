package alios.gwent.ui.navigation;

import alios.gwent.audio.AudioPlayer;
import alios.gwent.config.GameConfig;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Глобальный обработчик клавиатурных сочетаний приложения (Alt+Enter, Alt+M).
 * Инкапсулирует логику перехвата системных хоткеев на уровне окна.
 */
public final class GlobalKeyboardHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalKeyboardHandler.class);

    private GlobalKeyboardHandler() {
    }

    /**
     * Обрабатывает глобальные комбинации клавиш.
     *
     * @param event событие клавиатуры
     * @return true, если событие перехвачено и обработано
     */
    public static boolean handle(KeyEvent event) {
        if (event.isAltDown() && event.getCode() == KeyCode.ENTER) {
            handleToggleFullscreen();
            return true;
        }
        if (event.isAltDown() && event.getCode() == KeyCode.M) {
            handleToggleMute();
            return true;
        }
        return false;
    }

    private static void handleToggleFullscreen() {
        GameConfig config = GameConfig.get();
        config.fullscreen = !config.fullscreen;
        config.save();

        ScreenManager.applyDisplaySettings();
        ScreenManager.SETTINGS.syncFromConfig();
        LOGGER.info("Global shortcut: fullscreen toggled to {}", config.fullscreen);
    }

    private static void handleToggleMute() {
        GameConfig config = GameConfig.get();
        config.mute = !config.mute;
        config.save();

        AudioPlayer.applyConfig();
        ScreenManager.SETTINGS.syncFromConfig();
        LOGGER.info("Global shortcut: mute toggled to {}", config.mute);
    }
}
