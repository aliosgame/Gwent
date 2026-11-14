package alios.gwent;

import javafx.application.Application;

/**
 * Класс-трамплин для запуска JavaFX приложения без необходимости предварительной модульной инициализации.
 */
public final class AppLauncher {

    private AppLauncher() {
        throw new AssertionError("This class is not intended to create instances.");
    }

    public static void main(String[] args) {
        Application.launch(GwentApp.class, args);
    }
}
