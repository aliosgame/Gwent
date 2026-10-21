package gwent.ui;

import gwent.ui.menu.InfoView;
import gwent.ui.menu.MainMenuView;
import gwent.ui.menu.SettingsView;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public final class Navigator {
    private final StackPane host;
    private final AppContext context;

    public Navigator(StackPane host, AppContext context) {
        this.host = host;
        this.context = context;
        host.setFocusTraversable(true);
    }

    public void showMainMenu() {
        show(new MainMenuView(this).create());
    }

    public void showSinglePlayer() {
        show(new InfoView(
                "Одиночная игра",
                "Партия против компьютера появится следующим шагом. Пока отсюда можно проверить меню, музыку и настройки.",
                this).create());
    }

    public void showLan() {
        show(new InfoView(
                "Локальная сеть",
                "Здесь появится список комнат. Комнату будет видно тем, кто сидит в той же сети — за одним роутером или в Hamachi — и за стол можно будет сесть вдвоём.",
                this).create());
    }

    public void showSettings() {
        show(new SettingsView(this, context).create());
    }

    public void exit() {
        Platform.exit();
    }

    private void show(Parent view) {
        host.getChildren().setAll(view);
        // Иначе сцена оставляет фокус на первой кнопке, и она выглядит выбранной.
        Platform.runLater(host::requestFocus);
    }
}
