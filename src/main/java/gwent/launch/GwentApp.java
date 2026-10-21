package gwent.launch;

import gwent.deck.DeckStore;
import gwent.persist.Settings;
import gwent.persist.SettingsStore;
import gwent.persist.UserPaths;
import gwent.ui.AppContext;
import gwent.ui.Fonts;
import gwent.ui.FullscreenHint;
import gwent.ui.MenuMusic;
import gwent.ui.Navigator;
import gwent.ui.WindowPlacement;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

public final class GwentApp extends Application {
    private static final Logger LOG = Logger.getLogger(GwentApp.class.getName());

    private AppContext context;
    private boolean closing;

    @Override
    public void start(Stage stage) {
        SettingsStore store = new SettingsStore(UserPaths.settingsFile());
        Settings settings;
        try {
            settings = store.loadOrCreate();
        } catch (IOException ex) {
            showStartupError(ex);
            Platform.exit();
            return;
        }

        Fonts.load();
        MenuMusic music = new MenuMusic();
        music.play(settings.musicLevel());

        StackPane screens = new StackPane();
        FullscreenHint hint = new FullscreenHint();
        StackPane root = new StackPane(screens, hint.node());
        root.getStylesheets().add(stylesheet());
        Scene scene = new Scene(root);
        scene.setFill(Color.web("#120e0b"));

        stage.setTitle("Гвинт");
        stage.setScene(scene);
        stage.setMinWidth(Settings.MIN_WIDTH);
        stage.setMinHeight(Settings.MIN_HEIGHT);
        stage.setFullScreenExitHint("");

        context = new AppContext(stage, store, settings, music);
        DeckStore decks = DeckStore.loadOrCreate(UserPaths.decksFile());
        Navigator navigator = new Navigator(screens, context, decks);
        navigator.showMainMenu();
        hint.install(scene);

        stage.fullScreenProperty().addListener((obs, was, now) -> onFullscreenChanged(now, hint));
        stage.setOnCloseRequest(event -> beginClose());
        stage.setOnHiding(event -> closing = true);

        WindowPlacement.apply(stage, settings);
        stage.show();
    }

    @Override
    public void stop() {
        if (context != null) {
            beginClose();
            context.music().stop();
        }
    }

    private void onFullscreenChanged(boolean fullscreen, FullscreenHint hint) {
        if (fullscreen) {
            hint.show();
        } else {
            hint.hideNow();
        }
        if (closing) {
            return;
        }
        if (fullscreen) {
            rememberFullscreen(true);
            return;
        }
        // Выключение приходит и от Esc, и от закрытия окна: JavaFX сначала снимает
        // полный экран, и только потом шлёт запрос закрытия. Запись false откладываем
        // на следующий пульс — если это был выход из игры, флаг closing уже стоит.
        Platform.runLater(() -> {
            if (!closing) {
                rememberFullscreen(false);
            }
        });
    }

    private void beginClose() {
        closing = true;
        rememberWindow();
    }

    private void rememberFullscreen(boolean fullscreen) {
        if (context.settings().fullscreen() == fullscreen) {
            return;
        }
        try {
            context.save(context.settings().withFullscreen(fullscreen));
        } catch (IOException ex) {
            LOG.warning("Не удалось сохранить полный экран: " + ex.getMessage());
        }
    }

    private void rememberWindow() {
        try {
            context.save(WindowPlacement.remember(context.stage(), context.settings()));
        } catch (IOException ex) {
            LOG.warning("Не удалось сохранить окно: " + ex.getMessage());
        }
    }

    private static String stylesheet() {
        return Objects.requireNonNull(
                GwentApp.class.getResource("/gwent/ui/gwent.css"),
                "Нет /gwent/ui/gwent.css").toExternalForm();
    }

    private static void showStartupError(IOException ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Гвинт");
        alert.setHeaderText("Не удалось подготовить папку настроек");
        alert.setContentText(UserPaths.root() + System.lineSeparator() + ex.getMessage());
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
