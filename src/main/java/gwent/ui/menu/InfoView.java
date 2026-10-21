package gwent.ui.menu;

import gwent.ui.Navigator;
import gwent.ui.SceneShell;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Экран с заголовком, коротким текстом и возвратом в меню. */
public final class InfoView {
    private final String title;
    private final String message;
    private final Navigator navigator;

    public InfoView(String title, String message, Navigator navigator) {
        this.title = title;
        this.message = message;
        this.navigator = navigator;
    }

    public Parent create() {
        Label heading = new Label(title);
        heading.getStyleClass().add("screen-title");
        heading.setWrapText(true);

        Label body = new Label(message);
        body.getStyleClass().add("body");
        body.setWrapText(true);

        Button back = new Button("Назад");
        back.getStyleClass().add("gwent-button");
        back.setOnAction(event -> navigator.showMainMenu());

        VBox column = new VBox(18, heading, SceneShell.rule(), body, back);
        column.setAlignment(Pos.CENTER_LEFT);
        column.setPrefWidth(840);
        return SceneShell.wrap(column);
    }
}
