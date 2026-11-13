package alios.gwent.ui.navigation;

import javafx.scene.Parent;
import javafx.scene.input.KeyEvent;

import java.util.Objects;

/**
 * Базовый абстрактный класс контроллера экрана.
 * <p>
 * Хранит ссылку на корневую ноду экрана ({@link Parent}) и предоставляет
 * хуки жизненного цикла для обработки активации, деактивации и клавиатурного ввода.
 */
public abstract class BaseScreenController {

    private Parent root;

    /**
     * Привязывает корневую ноду разметки к контроллеру.
     * Метод вызывается менеджером экранов сразу после загрузки FXML.
     *
     * @param root корневая нода экрана
     * @throws IllegalStateException если корневая нода уже была установлена ранее
     */
    final void initRoot(Parent root) {
        if (this.root != null) {
            throw new IllegalStateException("Root is already initialized for " + getClass().getSimpleName());
        }
        this.root = Objects.requireNonNull(root, "root must not be null");
    }

    /**
     * Возвращает корневую ноду экрана.
     *
     * @return корневая нода разметки
     * @throws IllegalStateException если корневая нода еще не инициализирована
     */
    public final Parent getRoot() {
        if (root == null) {
            throw new IllegalStateException("Root is not initialized for " + getClass().getSimpleName());
        }
        return root;
    }

    /**
     * Проверяет, является ли данный экран в данный момент активным в ScreenManager.
     *
     * @return true, если экран сейчас отображается во вьюпорте
     */
    public final boolean isActive() {
        return ScreenManager.getActiveController() == this;
    }

    /**
     * Вызывается при переходе на экран после добавления его корневой ноды во вьюпорт.
     */
    public abstract void onEnter();

    /**
     * Вызывается перед уходом с экрана при переключении на другой экран.
     */
    public abstract void onExit();

    /**
     * Обрабатывает нажатия клавиш на активном экране.
     * Базовая реализация не выполняет действий.
     *
     * @param event событие клавиатуры
     */
    public void onKeyPressed(KeyEvent event) {
        // По умолчанию экраны игнорируют клавиатурные события
    }
}
