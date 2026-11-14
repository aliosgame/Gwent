package alios.gwent.ui.screens;

import alios.gwent.audio.AudioPlayer;
import alios.gwent.config.GameConfig;
import alios.gwent.config.Resolution;
import alios.gwent.ui.navigation.BaseScreenController;
import alios.gwent.ui.navigation.ScreenManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Контроллер экрана настроек игры (видеорежим, звук, горячие клавиши).
 */
public class SettingsController extends BaseScreenController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsController.class);
    private static final String PREVIEW_SFX = "gui_gwint_dummy.wav";

    @FXML private CheckBox fullscreenCheckBox;
    @FXML private ComboBox<Resolution> resolutionComboBox;
    @FXML private Label fullscreenHintLabel;

    @FXML private CheckBox muteCheckBox;
    @FXML private Slider musicVolumeSlider;
    @FXML private Label musicVolumeValueLabel;
    @FXML private Slider sfxVolumeSlider;
    @FXML private Label sfxVolumeValueLabel;

    // =========================================================================
    // Constructors and Initialization
    // =========================================================================

    public SettingsController() {
    }

    @FXML
    private void initialize() {
        fullscreenCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            resolutionComboBox.setDisable(isSelected);
            if (fullscreenHintLabel != null) {
                fullscreenHintLabel.setVisible(isSelected);
            }
        });

        muteCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            AudioPlayer.setLiveMute(isSelected);
        });

        musicVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int percent = (int) Math.round(newVal.doubleValue());
            musicVolumeValueLabel.setText(percent + "%");
            AudioPlayer.setLiveMusicVolume(percent / 100.0);
        });

        sfxVolumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int percent = (int) Math.round(newVal.doubleValue());
            sfxVolumeValueLabel.setText(percent + "%");
            AudioPlayer.setLiveSfxVolume(percent / 100.0);
        });

        sfxVolumeSlider.setOnMouseReleased(event -> AudioPlayer.playSfx(PREVIEW_SFX));
    }

    // =========================================================================
    // Public Business Logic
    // =========================================================================

    @Override
    public void onEnter() {
        LOGGER.info("Entered SettingsScreen");
        syncFromConfig();
    }

    @Override
    public void onExit() {
        LOGGER.info("Exiting SettingsScreen");
    }

    @Override
    public void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.BACK_SPACE) {
            onCancelButton();
            event.consume();
        }
    }

    /**
     * Синхронизирует состояние элементов UI с актуальными параметрами GameConfig.
     */
    public void syncFromConfig() {
        if (!isActive()) {
            return;
        }

        GameConfig config = GameConfig.get();
        var availableResolutions = Resolution.getAvailable();
        resolutionComboBox.getItems().setAll(availableResolutions);

        Resolution currentRes = config.resolution;
        if (!availableResolutions.contains(currentRes)) {
            currentRes = availableResolutions.getLast();
        }

        fullscreenCheckBox.setSelected(config.fullscreen);
        resolutionComboBox.setValue(currentRes);

        muteCheckBox.setSelected(config.mute);
        musicVolumeSlider.setValue(Math.round(config.musicVolume * 100.0));
        sfxVolumeSlider.setValue(Math.round(config.sfxVolume * 100.0));
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    @FXML
    private void onSaveButton() {
        GameConfig config = GameConfig.get();

        boolean newFullscreen = fullscreenCheckBox.isSelected();
        Resolution selectedRes = resolutionComboBox.getValue();
        if (selectedRes == null) {
            selectedRes = config.resolution;
        }

        boolean newMute = muteCheckBox.isSelected();
        double newMusic = Math.round(musicVolumeSlider.getValue()) / 100.0;
        double newSfx = Math.round(sfxVolumeSlider.getValue()) / 100.0;

        boolean displayChanged = (config.fullscreen != newFullscreen)
                || (!newFullscreen && config.resolution != selectedRes);

        boolean audioChanged = config.mute != newMute
                || Math.abs(config.musicVolume - newMusic) >= 0.005
                || Math.abs(config.sfxVolume - newSfx) >= 0.005;

        if (displayChanged) {
            config.fullscreen = newFullscreen;
            config.resolution = selectedRes;
            ScreenManager.syncFromConfig();
        }

        if (audioChanged) {
            config.mute = newMute;
            config.musicVolume = newMusic;
            config.sfxVolume = newSfx;
            AudioPlayer.syncFromConfig();
        }

        if (displayChanged || audioChanged) {
            config.save();
            LOGGER.info("Settings saved: displayChanged={}, audioChanged={}", displayChanged, audioChanged);
        }

        ScreenManager.show(ScreenManager.MAIN_MENU);
    }

    @FXML
    private void onCancelButton() {
        AudioPlayer.syncFromConfig();
        ScreenManager.show(ScreenManager.MAIN_MENU);
    }

    // =========================================================================
    // Getters / helpers for unit tests
    // =========================================================================

    CheckBox getFullscreenCheckBox() {
        return fullscreenCheckBox;
    }

    ComboBox<Resolution> getResolutionComboBox() {
        return resolutionComboBox;
    }

    CheckBox getMuteCheckBox() {
        return muteCheckBox;
    }

    Slider getMusicVolumeSlider() {
        return musicVolumeSlider;
    }

    Slider getSfxVolumeSlider() {
        return sfxVolumeSlider;
    }

    Label getMusicVolumeValueLabel() {
        return musicVolumeValueLabel;
    }
}
