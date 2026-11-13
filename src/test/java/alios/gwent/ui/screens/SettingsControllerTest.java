package alios.gwent.ui.screens;

import alios.gwent.config.GameConfig;
import alios.gwent.config.Resolution;
import alios.gwent.test.JavaFxTestPlatform;
import alios.gwent.ui.navigation.ScreenManager;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingsControllerTest {

    @BeforeAll
    static void initJavaFx() {
        JavaFxTestPlatform.runOnFxThread(() -> ScreenManager.init(new Stage()));
    }

    @Test
    @DisplayName("SettingsScreen contains linked stylesheet and initialized root")
    void testSettingsLayout() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            Parent root = ScreenManager.SETTINGS.getRoot();
            assertNotNull(root);
            assertFalse(root.getStylesheets().isEmpty());
        });
    }

    @Test
    @DisplayName("onEnter synchronizes controls with GameConfig state")
    void testOnEnterSync() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            ScreenManager.show(ScreenManager.SETTINGS);
            GameConfig config = GameConfig.get();
            config.fullscreen = false;
            config.resolution = Resolution.R_1920_1080;
            config.mute = false;
            config.musicVolume = 0.75;

            ScreenManager.SETTINGS.syncFromConfig();

            assertFalse(ScreenManager.SETTINGS.getFullscreenCheckBox().isSelected());
            assertFalse(ScreenManager.SETTINGS.getResolutionComboBox().isDisabled());
            assertEquals(Resolution.R_1920_1080, ScreenManager.SETTINGS.getResolutionComboBox().getValue());
            assertFalse(ScreenManager.SETTINGS.getMuteCheckBox().isSelected());
            assertEquals(75.0, ScreenManager.SETTINGS.getMusicVolumeSlider().getValue());
            assertEquals("75%", ScreenManager.SETTINGS.getMusicVolumeValueLabel().getText());
        });
    }

    @Test
    @DisplayName("Fullscreen checkbox toggles resolution combobox disabled state")
    void testFullscreenToggle() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            ScreenManager.SETTINGS.onEnter();

            ScreenManager.SETTINGS.getFullscreenCheckBox().setSelected(true);
            assertTrue(ScreenManager.SETTINGS.getResolutionComboBox().isDisabled());

            ScreenManager.SETTINGS.getFullscreenCheckBox().setSelected(false);
            assertFalse(ScreenManager.SETTINGS.getResolutionComboBox().isDisabled());
        });
    }

    @Test
    @DisplayName("Volume slider updates percentage label dynamically")
    void testSliderLabelBinding() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            ScreenManager.SETTINGS.getMusicVolumeSlider().setValue(42.0);
            assertEquals("42%", ScreenManager.SETTINGS.getMusicVolumeValueLabel().getText());

            ScreenManager.SETTINGS.getMusicVolumeSlider().setValue(100.0);
            assertEquals("100%", ScreenManager.SETTINGS.getMusicVolumeValueLabel().getText());
        });
    }

    @Test
    @DisplayName("Save button applies settings and returns to MainMenu")
    void testSaveNavigation() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            ScreenManager.show(ScreenManager.SETTINGS);
            assertSame(ScreenManager.SETTINGS, ScreenManager.getActiveController());

            ScreenManager.SETTINGS.getMusicVolumeSlider().setValue(50.0);

            try {
                var method = SettingsController.class.getDeclaredMethod("onSaveButton");
                method.setAccessible(true);
                method.invoke(ScreenManager.SETTINGS);

                assertSame(ScreenManager.MAIN_MENU, ScreenManager.getActiveController());
                assertEquals(0.5, GameConfig.get().musicVolume, 0.01);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    @DisplayName("ESC key navigates back to Main Menu (Cancel)")
    void testEscNavigation() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            ScreenManager.show(ScreenManager.SETTINGS);
            assertSame(ScreenManager.SETTINGS, ScreenManager.getActiveController());

            KeyEvent escEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false);
            ScreenManager.getScene().getRoot().fireEvent(escEvent);

            assertSame(ScreenManager.MAIN_MENU, ScreenManager.getActiveController());
        });
    }
}
