package alios.gwent.ui.navigation;

import alios.gwent.config.GameConfig;
import alios.gwent.ui.screens.MainMenuController;
import alios.gwent.ui.screens.SettingsController;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Централизованный менеджер экранов приложения.
 * <p>
 * Реализует паттерн Single Scene + Swappable Root с прямой типизацией контроллеров,
 * предварительной загрузкой экранов и масштабированием под референсное разрешение 1920x1080.
 */
public final class ScreenManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenManager.class);

    private static final double REFERENCE_WIDTH = 1920.0;
    private static final double REFERENCE_HEIGHT = 1080.0;

    public static final MainMenuController MAIN_MENU =
            loadController("/alios/gwent/ui/screens/MainMenuScreen.fxml", MainMenuController.class);
    public static final SettingsController SETTINGS =
            loadController("/alios/gwent/ui/screens/SettingsScreen.fxml", SettingsController.class);

    private static Stage stage;
    private static Scene scene;
    private static StackPane rootContainer;
    private static StackPane viewport;

    private static BaseScreenController currentController;

    private ScreenManager() {
    }

    /**
     * Инициализирует менеджер экранов: настраивает окно, сцену, масштабируемый вьюпорт
     * и централизованный фильтр клавиатурных событий.
     *
     * @param primaryStage главное окно JavaFX
     */
    public static void init(Stage primaryStage) {
        stage = Objects.requireNonNull(primaryStage, "primaryStage must not be null");

        GameConfig config = GameConfig.get();
        stage.setMinWidth(GameConfig.MIN_WIDTH);
        stage.setMinHeight(GameConfig.MIN_HEIGHT);
        stage.setWidth(config.resolutionWidth);
        stage.setHeight(config.resolutionHeight);

        rootContainer = new StackPane();
        rootContainer.setStyle("-fx-background-color: #0b0a08;");

        viewport = new StackPane();
        viewport.setMinSize(REFERENCE_WIDTH, REFERENCE_HEIGHT);
        viewport.setPrefSize(REFERENCE_WIDTH, REFERENCE_HEIGHT);
        viewport.setMaxSize(REFERENCE_WIDTH, REFERENCE_HEIGHT);

        // Трансформационное масштабирование под 1920x1080 с сохранением пропорций 16:9
        InvalidationListener scaleListener = obs -> {
            double containerWidth = rootContainer.getWidth();
            double containerHeight = rootContainer.getHeight();
            if (containerWidth > 0 && containerHeight > 0) {
                double scale = Math.min(containerWidth / REFERENCE_WIDTH, containerHeight / REFERENCE_HEIGHT);
                viewport.setScaleX(scale);
                viewport.setScaleY(scale);
            }
        };

        rootContainer.widthProperty().addListener(scaleListener);
        rootContainer.heightProperty().addListener(scaleListener);
        rootContainer.getChildren().add(viewport);

        scene = new Scene(rootContainer, config.resolutionWidth, config.resolutionHeight);

        // Централизованная изоляция клавиатурных событий: доставка строго активному контроллеру
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (currentController != null) {
                currentController.onKeyPressed(event);
            }
        });

        stage.setScene(scene);
        LOGGER.info("ScreenManager initialized successfully");
    }

    /**
     * Переключает отображаемый экран приложения на указанный контроллер.
     *
     * @param nextController контроллер экрана, на который выполняется переход
     */
    public static void show(BaseScreenController nextController) {
        Objects.requireNonNull(nextController, "nextController must not be null");

        if (viewport == null) {
            throw new IllegalStateException("ScreenManager must be initialized before showing screens");
        }

        if (nextController == currentController) {
            return;
        }

        if (currentController != null) {
            try {
                currentController.onExit();
            } catch (Exception e) {
                LOGGER.error("Error during onExit in {}", currentController.getClass().getSimpleName(), e);
            }
        }

        Parent nextRoot = nextController.getRoot();
        viewport.getChildren().setAll(nextRoot);

        currentController = nextController;

        nextRoot.setFocusTraversable(true);
        if (Platform.isFxApplicationThread()) {
            nextRoot.requestFocus();
        } else {
            Platform.runLater(nextRoot::requestFocus);
        }

        try {
            nextController.onEnter();
        } catch (Exception e) {
            LOGGER.error("Error during onEnter in {}", nextController.getClass().getSimpleName(), e);
        }

        LOGGER.info("Active screen switched to: {}", nextController.getClass().getSimpleName());
    }

    /**
     * Загружает FXML-разметку экрана, связывает с ней полученный контроллер
     * и возвращает типизированный экземпляр контроллера.
     *
     * @param fxmlPath путь к файлу разметки на classpath
     * @param <T>      тип конкретного контроллера экрана
     * @return инициализированный контроллер экрана
     */
    private static <T extends BaseScreenController> T loadController(String fxmlPath, Class<T> controllerClass) {
        Objects.requireNonNull(controllerClass, "controllerClass must not be null");
        URL resource = ScreenManager.class.getResource(fxmlPath);
        if (resource == null) {
            throw new IllegalStateException("FXML resource not found: " + fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(resource);
        try {
            Parent root = loader.load();
            Object controllerObj = loader.getController();

            if (!controllerClass.isInstance(controllerObj)) {
                throw new IllegalStateException(
                        "Controller in " + fxmlPath + " must be an instance of " + controllerClass.getName()
                );
            }

            T controller = controllerClass.cast(controllerObj);
            controller.initRoot(root);
            LOGGER.info("Loaded screen controller: {}", controller.getClass().getSimpleName());
            return controller;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    public static BaseScreenController getCurrentController() {
        return currentController;
    }

    public static boolean isCurrent(BaseScreenController controller) {
        return currentController == controller;
    }

    public static Scene getScene() {
        return scene;
    }

    public static Pane getViewport() {
        return viewport;
    }
}
