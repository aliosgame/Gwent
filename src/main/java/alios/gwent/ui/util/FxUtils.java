package alios.gwent.ui.util;

import alios.gwent.ui.navigation.ScreenManager;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ComboBoxListViewSkin;

import java.util.Locale;
import java.util.Objects;

/**
 * Общие вспомогательные утилиты для JavaFX интерфейса приложения.
 */
public final class FxUtils {

    private FxUtils() {
        throw new AssertionError("This utility class is not intended to create instances.");
    }

    // =========================================================================
    // Public Business Logic
    // =========================================================================

    /**
     * Адаптирует ширину выпадающего списка {@link ComboBox} под текущий масштаб Viewport.
     * <p>
     * <b>Контракт использования:</b> Комбобокс должен быть свёрстан под 1080p с положительной шириной
     * (через {@code prefWidth} в FXML или вычисленной {@code width}).
     *
     * @param comboBox     целевой комбобокс
     * @throws IllegalStateException если у комбобокса не задана положительная ширина
     */
    public static void enableComboBoxViewportScaling(ComboBox<?> comboBox) {
        Objects.requireNonNull(comboBox, "comboBox must not be null");

        comboBox.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing && comboBox.getSkin() instanceof ComboBoxListViewSkin<?> skin) {
                if (skin.getPopupContent() instanceof ListView<?> listView) {
                    double width = comboBox.getWidth() > 0 ? comboBox.getWidth() : comboBox.getPrefWidth();
                    if (width <= 0) {
                        throw new IllegalStateException(
                                "ComboBox must have a positive width or prefWidth defined in 1080p layout: " + comboBox.getId()
                        );
                    }

                    double scale = ScreenManager.getScale();
                    double scaledWidth = width * scale;

                    listView.setStyle(String.format(
                            Locale.ROOT,
                            "-fx-pref-width: %.1fpx; -fx-max-width: %.1fpx;",
                            scaledWidth, scaledWidth
                    ));
                }
            }
        });
    }
}
