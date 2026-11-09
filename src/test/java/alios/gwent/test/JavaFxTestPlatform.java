package alios.gwent.test;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Вспомогательный класс для выполнения кода тестов в потоке JavaFX Application Thread.
 */
public final class JavaFxTestPlatform {

    private static volatile boolean started = false;

    private JavaFxTestPlatform() {
    }

    /**
     * Гарантирует однократный запуск JavaFX runtime для среды тестов.
     */
    public static synchronized void ensureStarted() {
        if (!started) {
            try {
                Platform.startup(() -> {
                });
            } catch (IllegalStateException e) {
                // Платформа уже запущена в этом процессе JVM
            }
            started = true;
        }
    }

    /**
     * Синхронно выполняет переданный код в потоке JavaFX Application Thread.
     *
     * @param action действие для выполнения
     */
    public static void runOnFxThread(Runnable action) {
        ensureStarted();
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            Platform.runLater(() -> {
                try {
                    action.run();
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            if (error.get() != null) {
                if (error.get() instanceof RuntimeException re) {
                    throw re;
                }
                throw new RuntimeException(error.get());
            }
        }
    }
}
