package alios.gwent.ui.navigation;

import alios.gwent.config.GameConfig;
import alios.gwent.config.Resolution;
import alios.gwent.test.JavaFxTestPlatform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScreenManagerTest {

    @BeforeAll
    static void initJavaFx() {
        JavaFxTestPlatform.runOnFxThread(() -> ScreenManager.init(new Stage()));
    }

    @Test
    @DisplayName("Static controller constants are loaded and have initialized root nodes")
    void testControllersLoaded() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            assertNotNull(ScreenManager.MAIN_MENU);
            assertNotNull(ScreenManager.SETTINGS);

            assertNotNull(ScreenManager.MAIN_MENU.getRoot());
            assertNotNull(ScreenManager.SETTINGS.getRoot());
        });
    }

    @Test
    @DisplayName("Switches active screen and swaps root node in viewport")
    void testScreenSwitching() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            ScreenManager.show(ScreenManager.MAIN_MENU);

            assertSame(ScreenManager.MAIN_MENU, ScreenManager.getActiveController());
            assertTrue(ScreenManager.MAIN_MENU.isActive());
            assertSame(ScreenManager.MAIN_MENU.getRoot(), ScreenManager.getViewport().getChildren().getFirst());

            ScreenManager.show(ScreenManager.SETTINGS);

            assertSame(ScreenManager.SETTINGS, ScreenManager.getActiveController());
            assertTrue(ScreenManager.SETTINGS.isActive());
            assertSame(ScreenManager.SETTINGS.getRoot(), ScreenManager.getViewport().getChildren().getFirst());
        });
    }

    @Test
    @DisplayName("Routes keyboard events exclusively to the active screen")
    void testKeyboardRouting() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            ScreenManager.show(ScreenManager.SETTINGS);

            KeyEvent backspaceEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.BACK_SPACE, false, false, false, false);
            ScreenManager.getScene().getRoot().fireEvent(backspaceEvent);

            assertSame(ScreenManager.MAIN_MENU, ScreenManager.getActiveController());
        });
    }

    @Test
    @DisplayName("syncFromConfig resizes stage in windowed mode")
    void testSyncFromConfigResizesStage() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            Stage stage = ScreenManager.getStage();
            stage.show();

            GameConfig config = GameConfig.get();
            config.fullscreen = false;
            config.resolution = Resolution.R_1280_720;

            ScreenManager.syncFromConfig();

            double decorWidth = Math.max(0, stage.getWidth() - ScreenManager.getScene().getWidth());
            double decorHeight = Math.max(0, stage.getHeight() - ScreenManager.getScene().getHeight());

            assertEquals(1280 + decorWidth, stage.getWidth(), 0.1);
            assertEquals(720 + decorHeight, stage.getHeight(), 0.1);
        });
    }

    @Test
    @DisplayName("Global keyboard handler intercepts Alt+M to toggle mute")
    void testGlobalAltMute() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            boolean initialMute = GameConfig.get().mute;
            KeyEvent altMEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.M, false, false, true, false);
            ScreenManager.getScene().getRoot().fireEvent(altMEvent);

            assertNotEquals(initialMute, GameConfig.get().mute);
        });
    }
}
