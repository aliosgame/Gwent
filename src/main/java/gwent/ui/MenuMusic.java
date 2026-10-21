package gwent.ui;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.logging.Logger;

/** Зацикленная музыка меню. Если файл не читается, меню всё равно открывается. */
public final class MenuMusic {
    private static final Logger LOG = Logger.getLogger(MenuMusic.class.getName());

    private MediaPlayer player;

    public void play(double volume) {
        URL resource = MenuMusic.class.getResource("/gwent/audio/menu.mp3");
        if (resource == null) {
            LOG.warning("Нет файла /gwent/audio/menu.mp3, меню без музыки.");
            return;
        }
        try {
            player = new MediaPlayer(new Media(resource.toExternalForm()));
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setVolume(clamp(volume));
            player.setOnError(() -> LOG.warning("Музыка меню не запустилась: " + player.getError()));
            player.play();
        } catch (RuntimeException ex) {
            LOG.warning("Музыка меню не запустилась: " + ex.getMessage());
            player = null;
        }
    }

    public void setVolume(double volume) {
        if (player != null) {
            player.setVolume(clamp(volume));
        }
    }

    public void stop() {
        if (player != null) {
            player.dispose();
            player = null;
        }
    }

    private static double clamp(double volume) {
        return Math.clamp(volume, 0, 1);
    }
}
