package gwent.ui;

import javafx.scene.text.Font;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public final class Fonts {
    private static final Logger LOG = Logger.getLogger(Fonts.class.getName());

    private Fonts() {
    }

    public static void load() {
        // Только статические ttf. Вариативный шрифт JavaFX рисует с огромными просветами.
        load("/gwent/ui/font/Oranienbaum-Regular.ttf");
        load("/gwent/ui/font/Philosopher-Regular.ttf");
    }

    private static void load(String path) {
        try (InputStream in = Fonts.class.getResourceAsStream(path)) {
            if (in == null) {
                LOG.warning("Нет шрифта " + path);
                return;
            }
            Font font = Font.loadFont(in, 16);
            if (font == null) {
                LOG.warning("Шрифт не разобрался: " + path);
            }
        } catch (IOException ex) {
            LOG.warning("Шрифт не прочитался: " + ex.getMessage());
        }
    }
}
