package gwent.launch;

import javafx.application.Application;

/**
 * Точка входа для запуска игры из автономного Fat JAR.
 * <p>
 * Класс намеренно не наследует {@link Application}, чтобы JVM-загрузчик
 * при запуске {@code java -jar} обходил проверку наличия JavaFX на module-path
 * и передавал управление JavaFX рантайму, упакованному внутри JAR.
 */
public final class GwentLauncher {
    private GwentLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(GwentApp.class, args);
    }
}
