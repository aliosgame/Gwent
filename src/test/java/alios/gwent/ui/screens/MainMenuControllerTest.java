package alios.gwent.ui.screens;

import alios.gwent.test.JavaFxTestPlatform;
import alios.gwent.ui.navigation.ScreenManager;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MainMenuControllerTest {

    @BeforeAll
    static void initJavaFx() {
        JavaFxTestPlatform.runOnFxThread(() -> ScreenManager.init(new Stage()));
    }

    @Test
    @DisplayName("MainMenuScreen contains linked stylesheet")
    void testMainMenuStylesheet() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            Parent root = ScreenManager.MAIN_MENU.getRoot();
            assertNotNull(root);
            assertFalse(root.getStylesheets().isEmpty());
        });
    }
}
