package gwent.ui.menu;

import gwent.persist.Settings;
import gwent.persist.UserPaths;
import gwent.ui.AppContext;
import gwent.ui.Navigator;
import gwent.ui.SceneShell;
import gwent.ui.WindowPlacement;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;

public final class SettingsView {
    private final Navigator navigator;
    private final AppContext context;

    public SettingsView(Navigator navigator, AppContext context) {
        this.navigator = navigator;
        this.context = context;
    }

    public Parent create() {
        Settings current = WindowPlacement.remember(context.stage(), context.settings());

        Label title = new Label("Настройки");
        title.getStyleClass().add("screen-title");
        title.setWrapText(true);

        Slider music = slider(current.musicVolume());
        Slider effects = slider(current.effectsVolume());
        Label musicValue = valueLabel(music);
        Label effectsValue = valueLabel(effects);

        CheckBox fullscreen = new CheckBox("Полный экран");
        fullscreen.setSelected(context.stage().isFullScreen());
        fullscreen.getStyleClass().add("option");
        ChangeListener<Boolean> followStage = (obs, was, now) -> fullscreen.setSelected(now);
        context.stage().fullScreenProperty().addListener(followStage);

        Label pathCaption = new Label("Файл настроек");
        pathCaption.getStyleClass().add("hint");
        Label path = new Label(UserPaths.settingsFile().toString());
        path.getStyleClass().add("path");
        path.setWrapText(true);

        Label hint = new Label("Звуки карт появятся вместе со столом. Ползунок уже запоминается.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);

        Label error = new Label();
        error.getStyleClass().add("error");
        error.setWrapText(true);

        music.valueProperty().addListener((obs, was, value) ->
                context.music().setVolume(value.doubleValue() / 100.0));

        Button save = new Button("Сохранить");
        save.getStyleClass().add("gwent-button");
        save.setOnAction(event -> save(current, music, effects, fullscreen, error));

        Button back = new Button("Назад");
        back.getStyleClass().add("gwent-button");
        back.setOnAction(event -> {
            context.music().setVolume(context.settings().musicLevel());
            navigator.showMainMenu();
        });

        VBox column = new VBox(16,
                title,
                SceneShell.rule(),
                row("Музыка", music, musicValue),
                row("Звуки", effects, effectsValue),
                hint,
                fullscreen,
                pathCaption,
                path,
                error,
                save,
                back);
        column.setAlignment(Pos.CENTER_LEFT);
        column.setPrefWidth(840);
        Parent view = SceneShell.wrap(column);
        view.sceneProperty().addListener((obs, was, scene) -> {
            if (scene == null) {
                context.stage().fullScreenProperty().removeListener(followStage);
            }
        });
        return view;
    }

    private void save(Settings current, Slider music, Slider effects, CheckBox fullscreen, Label error) {
        Settings placed = WindowPlacement.remember(context.stage(), current);
        Settings next = new Settings(
                (int) Math.round(music.getValue()),
                (int) Math.round(effects.getValue()),
                fullscreen.isSelected(),
                placed.windowWidth(),
                placed.windowHeight(),
                placed.windowX(),
                placed.windowY());
        try {
            context.save(next);
            WindowPlacement.apply(context.stage(), next);
            navigator.showMainMenu();
        } catch (IOException ex) {
            error.setText("Не удалось записать настройки: " + ex.getMessage());
        }
    }

    private static Slider slider(int value) {
        Slider slider = new Slider(0, 100, value);
        slider.setMajorTickUnit(10);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        return slider;
    }

    private static Label valueLabel(Slider slider) {
        Label label = new Label(text(slider.getValue()));
        label.getStyleClass().add("value");
        label.setMinWidth(36);
        slider.valueProperty().addListener((obs, was, value) -> label.setText(text(value.doubleValue())));
        return label;
    }

    private static String text(double value) {
        return Integer.toString((int) Math.round(value));
    }

    private static HBox row(String caption, Slider slider, Label value) {
        Label name = new Label(caption);
        name.getStyleClass().add("option");
        name.setMinWidth(110);
        slider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(slider, Priority.ALWAYS);
        HBox box = new HBox(12, name, slider, value);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }
}
