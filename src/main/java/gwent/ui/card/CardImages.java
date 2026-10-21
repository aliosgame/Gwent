package gwent.ui.card;

import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

/** Кэш картинок. Грузим сразу, чтобы срез и рамка считались по уже известному размеру. */
public final class CardImages {
    private static final Map<String, Image> CACHE = new HashMap<>();

    private CardImages() {
    }

    public static Image get(String path) {
        Image cached = CACHE.get(path);
        if (cached != null) {
            return cached;
        }
        var url = CardImages.class.getResource(path);
        if (url == null) {
            return null;
        }
        Image image = new Image(url.toExternalForm(), false);
        CACHE.put(path, image);
        return image;
    }
}
