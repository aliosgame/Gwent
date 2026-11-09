package alios.gwent.ui.navigation;

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

            assertSame(ScreenManager.MAIN_MENU, ScreenManager.getCurrentController());
            assertTrue(ScreenManager.isCurrent(ScreenManager.MAIN_MENU));
            assertSame(ScreenManager.MAIN_MENU.getRoot(), ScreenManager.getViewport().getChildren().getFirst());

            ScreenManager.show(ScreenManager.SETTINGS);

            assertSame(ScreenManager.SETTINGS, ScreenManager.getCurrentController());
            assertTrue(ScreenManager.isCurrent(ScreenManager.SETTINGS));
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

            assertSame(ScreenManager.MAIN_MENU, ScreenManager.getCurrentController());
        });
    }
}
