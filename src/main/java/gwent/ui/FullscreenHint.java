package gwent.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Своя табличка вместо штатной JavaFX.
 * У штатной нет настройки текста и времени: она держится 4 секунды и гаснет ещё секунду.
 * Пустая {@code setFullScreenExitHint("")} её выключает.
 */
public final class FullscreenHint {
    private static final Duration HOLD = Duration.millis(2000);
    private static final Duration FADE = Duration.millis(500);

    private final StackPane layer;
    private final StackPane plate;
    private PauseTransition hold;
    private FadeTransition fade;
    private boolean showing;

    public FullscreenHint() {
        Label text = new Label("Чтобы выйти из полноэкранного режима, нажмите Esc");
        text.getStyleClass().add("fullscreen-hint-text");

        plate = new StackPane(text);
        plate.getStyleClass().add("fullscreen-hint");
        plate.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(plate, Pos.TOP_CENTER);
        StackPane.setMargin(plate, new Insets(28, 24, 0, 24));

        layer = new StackPane(plate);
        layer.setPickOnBounds(false);
        layer.setVisible(false);
        plate.setOpacity(0);
    }

    public Parent node() {
        return layer;
    }

    public void install(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, this::onMousePressed);
    }

    public void show() {
        stopMotion();
        showing = true;
        layer.setVisible(true);
        plate.setOpacity(1);
        hold = new PauseTransition(HOLD);
        hold.setOnFinished(event -> fadeOut());
        hold.play();
    }

    public void hideNow() {
        stopMotion();
        showing = false;
        plate.setOpacity(0);
        layer.setVisible(false);
    }

    private void onMousePressed(MouseEvent event) {
        if (!showing || event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        Bounds bounds = plate.localToScene(plate.getBoundsInLocal());
        if (bounds.contains(event.getSceneX(), event.getSceneY())) {
            return;
        }
        hideNow();
    }

    private void fadeOut() {
        fade = new FadeTransition(FADE, plate);
        fade.setFromValue(plate.getOpacity());
        fade.setToValue(0);
        fade.setOnFinished(event -> {
            showing = false;
            layer.setVisible(false);
        });
        fade.play();
    }

    private void stopMotion() {
        if (hold != null) {
            hold.stop();
        }
        if (fade != null) {
            fade.stop();
        }
    }
}
