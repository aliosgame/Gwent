package alios.gwent.ui.navigation;

import alios.gwent.audio.AudioPlayer;
import alios.gwent.config.GameConfig;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Глобальный обработчик клавиатурных сочетаний приложения (Alt+Enter, Alt+M).
 * Инкапсулирует логику перехвата системных хоткеев на уровне окна.
 */
public final class GlobalKeyboardHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalKeyboardHandler.class);

    private GlobalKeyboardHandler() {
        throw new AssertionError("This class is not intended to create instances.");
    }

    // =========================================================================
    // Public Business Logic
    // =========================================================================

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

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private static void handleToggleFullscreen() {
        GameConfig config = GameConfig.get();
        config.fullscreen = !config.fullscreen;
        config.save();

        ScreenManager.syncFromConfig();
        ScreenManager.SETTINGS.syncFromConfig();
        LOGGER.info("Global shortcut: fullscreen toggled to {}", config.fullscreen);
    }

    private static void handleToggleMute() {
        GameConfig config = GameConfig.get();
        config.mute = !config.mute;
        config.save();

        AudioPlayer.syncFromConfig();
        ScreenManager.SETTINGS.syncFromConfig();
        LOGGER.info("Global shortcut: mute toggled to {}", config.mute);
    }
}
