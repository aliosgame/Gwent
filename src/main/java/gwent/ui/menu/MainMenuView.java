package gwent.ui.menu;

import gwent.ui.Navigator;
import gwent.ui.SceneShell;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class MainMenuView {
    private final Navigator navigator;

    public MainMenuView(Navigator navigator) {
        this.navigator = navigator;
    }

    public Parent create() {
        Label title = new Label("Гвинт");
        title.getStyleClass().add("menu-title");
        title.setMinWidth(Region.USE_PREF_SIZE);

        Region rule = SceneShell.rule();
        rule.getStyleClass().add("rule-menu");

        VBox column = new VBox(16,
                title,
                rule,
                button("Одиночная игра", navigator::showSinglePlayer),
                button("Локальная сеть", navigator::showLan),
                button("Настройки", navigator::showSettings),
                button("Выход", navigator::exit));
        column.setAlignment(Pos.CENTER);
        return SceneShell.wrap(column);
    }

    private static Button button(String caption, Runnable action) {
        Button button = new Button(caption);
        button.getStyleClass().add("gwent-button");
        button.setOnAction(event -> action.run());
        return button;
    }
}
