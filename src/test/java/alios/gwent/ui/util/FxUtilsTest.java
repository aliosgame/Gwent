package alios.gwent.ui.util;

import alios.gwent.test.JavaFxTestPlatform;
import javafx.scene.control.ComboBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FxUtilsTest {

    @BeforeAll
    static void initJavaFx() {
        JavaFxTestPlatform.ensureStarted();
    }

    @Test
    @DisplayName("enableComboBoxViewportScaling validates arguments strictly")
    void testArgumentValidation() {
        assertThrows(NullPointerException.class, () -> FxUtils.enableComboBoxViewportScaling(null));
    }

    @Test
    @DisplayName("enableComboBoxViewportScaling registers listener for valid ComboBox without errors")
    void testValidComboBoxRegistration() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setPrefWidth(290.0);
            comboBox.getItems().addAll("Option 1", "Option 2");

            assertDoesNotThrow(() -> FxUtils.enableComboBoxViewportScaling(comboBox));
        });
    }

    @Test
    @DisplayName("enableComboBoxViewportScaling applies scaled styles on showing")
    void testAppliesScalingOnShowing() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            javafx.scene.Scene scene = new javafx.scene.Scene(new javafx.scene.layout.StackPane());
            scene.getStylesheets().add(getClass().getResource("/alios/gwent/ui/styles/gwent.css").toExternalForm());

            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setPrefWidth(290.0);
            comboBox.getItems().addAll("Option 1", "Option 2");
            ((javafx.scene.layout.StackPane) scene.getRoot()).getChildren().add(comboBox);

            FxUtils.enableComboBoxViewportScaling(comboBox);

            // Force skin
            comboBox.skinProperty().set(new javafx.scene.control.skin.ComboBoxListViewSkin<>(comboBox));
            javafx.scene.control.skin.ComboBoxListViewSkin<?> skin = (javafx.scene.control.skin.ComboBoxListViewSkin<?>) comboBox.getSkin();
            javafx.scene.control.ListView<?> listView = (javafx.scene.control.ListView<?>) skin.getPopupContent();

            comboBox.show();

            assertNotNull(listView.getStyle());
            assertTrue(listView.getStyle().contains("-fx-pref-width:"));
            assertTrue(listView.getStyle().contains("-fx-max-width:"));

            comboBox.hide();
        });
    }
}
