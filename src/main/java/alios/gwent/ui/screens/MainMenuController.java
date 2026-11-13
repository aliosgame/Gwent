package alios.gwent.ui.screens;

import alios.gwent.config.GameConfig;
import alios.gwent.ui.navigation.BaseScreenController;
import alios.gwent.ui.navigation.ScreenManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Контроллер главного меню игры.
 */
public class MainMenuController extends BaseScreenController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainMenuController.class);

    private @FXML Label resolutionLabel;
    private @FXML Label statusLabel;

    public MainMenuController() {
    }

    @FXML
    private void onStartGameButton() {
        if (statusLabel != null) {
            statusLabel.setText("Режим дуэли будет доступен в следующем обновлении");
        }
    }

    @FXML
    private void onDeckBuilderButton() {
        if (statusLabel != null) {
            statusLabel.setText("Конструктор колод будет доступен в следующем обновлении");
        }
    }

    @FXML
    private void onSettingsButton() {
        ScreenManager.show(ScreenManager.SETTINGS);
    }

    @FXML
    private void onExitButton() {
        LOGGER.info("Exit requested from MainMenu");
        Platform.exit();
    }

    @Override
    public void onEnter() {
        LOGGER.info("Entered MainMenu");
        GameConfig config = GameConfig.get();
        if (resolutionLabel != null) {
            resolutionLabel.setText(String.format("%s (Reference: 1080p)", config.resolution));
        }
        if (statusLabel != null) {
            statusLabel.setText("Главное меню активно");
        }
    }

    @Override
    public void onExit() {
        LOGGER.info("Exiting MainMenu");
    }

    @Override
    public void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            onExitButton();
            event.consume();
        }
    }
}
