package gwent.ui;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/** Общий фон меню: картинка, затемнение слева, колонка с текстом. */
public final class SceneShell {
    private SceneShell() {
    }

    public static Parent wrap(Region content) {
        StackPane stack = new StackPane();
        stack.getStyleClass().add("shell");

        Region veil = new Region();
        veil.getStyleClass().add("veil");

        content.getStyleClass().add("column");
        content.setMaxWidth(Region.USE_PREF_SIZE);
        content.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(content, Pos.CENTER_LEFT);
        stack.getChildren().addAll(veil, content);
        return stack;
    }

    public static Region rule() {
        Region rule = new Region();
        rule.getStyleClass().add("rule");
        return rule;
    }
}
