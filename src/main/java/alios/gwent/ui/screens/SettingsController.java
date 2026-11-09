package alios.gwent.ui.screens;

import alios.gwent.config.GameConfig;
import alios.gwent.ui.navigation.BaseScreenController;
import alios.gwent.ui.navigation.ScreenManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Контроллер экрана настроек игры.
 */
public class SettingsController extends BaseScreenController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsController.class);

    private @FXML Label resolutionValueLabel;
    private @FXML Label minResolutionValueLabel;
    private @FXML Label masterVolumeLabel;
    private @FXML Label musicVolumeLabel;
    private @FXML Label sfxVolumeLabel;
    private @FXML Label infoLabel;

    public SettingsController() {
    }

    @FXML
    private void onBackButton() {
        ScreenManager.show(ScreenManager.MAIN_MENU);
    }

    @Override
    public void onEnter() {
        LOGGER.info("Entered SettingsScreen");
        updateDisplayedSettings();
    }

    @Override
    public void onExit() {
        LOGGER.info("Exiting SettingsScreen");
    }

    @Override
    public void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.BACK_SPACE) {
            onBackButton();
            event.consume();
        }
    }

    private void updateDisplayedSettings() {
        GameConfig config = GameConfig.get();
        if (resolutionValueLabel != null) {
            resolutionValueLabel.setText(String.format("%d x %d", config.resolutionWidth, config.resolutionHeight));
        }
        if (minResolutionValueLabel != null) {
            minResolutionValueLabel.setText(String.format("%d x %d", GameConfig.MIN_WIDTH, GameConfig.MIN_HEIGHT));
        }
        if (masterVolumeLabel != null) {
            masterVolumeLabel.setText(String.format("%d%%", Math.round(config.masterVolume * 100)));
        }
        if (musicVolumeLabel != null) {
            musicVolumeLabel.setText(String.format("%d%%", Math.round(config.musicVolume * 100)));
        }
        if (sfxVolumeLabel != null) {
            sfxVolumeLabel.setText(String.format("%d%%", Math.round(config.sfxVolume * 100)));
        }
        if (infoLabel != null) {
            infoLabel.setText("Файл настроек: " + config.getConfigFile());
        }
    }
}
