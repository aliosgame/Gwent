package alios.gwent;

import alios.gwent.audio.AudioPlayer;
import alios.gwent.ui.navigation.ScreenManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Точка входа графического интерфейса JavaFX.
 */
public class GwentApp extends Application {

    private static final String APP_TITLE = "Гвинт: Карточная игра из мира «Ведьмака»";

    @Override
    public void start(Stage primaryStage) {
        ScreenManager.init(primaryStage);
        AudioPlayer.startMusic();
        ScreenManager.show(ScreenManager.MAIN_MENU);

        primaryStage.setTitle(APP_TITLE);
        primaryStage.show();
    }
}
