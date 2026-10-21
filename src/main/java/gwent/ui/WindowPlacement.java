package gwent.ui;

import gwent.persist.Settings;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

public final class WindowPlacement {
    private WindowPlacement() {
    }

    public static void apply(Stage stage, Settings settings) {
        stage.setWidth(settings.windowWidth());
        stage.setHeight(settings.windowHeight());
        if (hasSavedPosition(settings) && isOnAScreen(settings)) {
            stage.setX(settings.windowX());
            stage.setY(settings.windowY());
        } else {
            center(stage, settings);
        }
        stage.setFullScreen(settings.fullscreen());
    }

    /**
     * Запоминает размер и место окна. В полном экране размер не трогаем:
     * иначе в файл попадёт размер монитора, и оконный режим потом откроется на весь стол.
     * Флаг полного экрана здесь не выключаем. При закрытии JavaFX снимает его сам,
     * уже после того как пользователь решил оставить режим включённым.
     */
    public static Settings remember(Stage stage, Settings current) {
        if (stage.isFullScreen() || current.fullscreen()) {
            return current.withFullscreen(true);
        }
        int width = (int) Math.round(stage.getWidth());
        int height = (int) Math.round(stage.getHeight());
        if (width < Settings.MIN_WIDTH || height < Settings.MIN_HEIGHT) {
            return current;
        }
        return current.withWindow(
                width,
                height,
                (int) Math.round(stage.getX()),
                (int) Math.round(stage.getY()),
                false);
    }

    private static boolean hasSavedPosition(Settings settings) {
        return settings.windowX() != -1 || settings.windowY() != -1;
    }

    private static boolean isOnAScreen(Settings settings) {
        Rectangle2D window = new Rectangle2D(
                settings.windowX(),
                settings.windowY(),
                settings.windowWidth(),
                settings.windowHeight());
        for (Screen screen : Screen.getScreens()) {
            if (screen.getVisualBounds().intersects(window)) {
                return true;
            }
        }
        return false;
    }

    private static void center(Stage stage, Settings settings) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX() + (bounds.getWidth() - settings.windowWidth()) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - settings.windowHeight()) / 2);
    }
}
