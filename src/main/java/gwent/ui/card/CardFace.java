package gwent.ui.card;

import gwent.catalog.CardDef;
import java.util.IdentityHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Картинка карты.
 * В колоде полная карта остаётся со своими скруглёнными углами,
 * буквы цитаты заменяются пергаментом той же карты, а счётчик копий лежит на этом пергаменте.
 * Границы сняты с файлов 310×585: имя, даже в две строки, кончается к строке 493,
 * хвост флага — около 516, цитата начинается с 525 и у длинных цитат доходит до 580.
 */
public final class CardFace {
    public static final double SOURCE_WIDTH = 310;
    public static final double SOURCE_HEIGHT = 585;
    /** Чистая строка пергамента между флагом и цитатой. */
    private static final int DONOR_Y = 508;
    /** Цитата начинается около 525, имя кончается к 493. */
    private static final int QUOTE_Y0 = 520;
    private static final int QUOTE_Y1 = 581;
    /** Насколько пиксель должен быть темнее пергамента над ним, чтобы считаться чернилами. */
    private static final int INK_DELTA = 45;
    private static final int PAPER_SUM = 540;

    private static final Map<Image, Image> WITHOUT_QUOTE = new IdentityHashMap<>();

    private CardFace() {
    }

    public static Parent create(CardDef card, double width, CardCrop crop, int copies) {
        double height = height(width, crop);
        Image picture = CardImages.get(card.fullImagePath());
        ImageView view = new ImageView(picture);
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        if (picture != null && picture.getWidth() > 0 && crop == CardCrop.LEADER_SLOT) {
            double visible = picture.getHeight() * crop.visibleFraction();
            view.setViewport(new Rectangle2D(0, 0, picture.getWidth(), visible));
            Rectangle clip = new Rectangle(width, height);
            double arc = 32 * width / SOURCE_WIDTH;
            clip.setArcWidth(arc);
            clip.setArcHeight(arc);
            view.setClip(clip);
        }

        StackPane art = new StackPane(view);
        art.getStyleClass().add("card-art");
        art.setMinSize(width, height);
        art.setMaxSize(width, height);
        art.setMouseTransparent(true);
        if (crop == CardCrop.DECK && copies > 0 && picture != null) {
            view.setImage(withoutQuote(picture));
            paintCount(art, width, copies);
        }
        return art;
    }

    /**
     * Стирает цитату пикселями пергамента той же карты.
     * Плоский прямоугольник поверх картинки давал шов, поэтому темнеющие буквы
     * заменяются цветом чистой строки над цитатой, а светлый пергамент остаётся как есть.
     */
    private static Image withoutQuote(Image source) {
        Image cached = WITHOUT_QUOTE.get(source);
        if (cached != null) {
            return cached;
        }
        PixelReader reader = source.getPixelReader();
        int width = (int) source.getWidth();
        int height = (int) source.getHeight();
        if (reader == null || width < 20 || height < QUOTE_Y1) {
            WITHOUT_QUOTE.put(source, source);
            return source;
        }
        WritableImage copy = new WritableImage(reader, width, height);
        PixelWriter writer = copy.getPixelWriter();
        int donorY = Math.min(DONOR_Y, height - 1);
        int y0 = Math.min(QUOTE_Y0, height - 1);
        int y1 = Math.min(QUOTE_Y1, height - 1);
        for (int y = y0; y <= y1; y++) {
            for (int x = 12; x < width - 12; x++) {
                int argb = reader.getArgb(x, y);
                int alpha = (argb >>> 24) & 0xff;
                if (alpha < 40 || blueRibbon(argb)) {
                    continue;
                }
                int donor = parchmentAt(reader, x, donorY, width);
                if (channelSum(argb) + INK_DELTA >= channelSum(donor)) {
                    continue;
                }
                int red = (donor >> 16) & 0xff;
                int green = (donor >> 8) & 0xff;
                int blue = donor & 0xff;
                writer.setArgb(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        WITHOUT_QUOTE.put(source, copy);
        return copy;
    }

    private static int parchmentAt(PixelReader reader, int x, int donorY, int width) {
        int donor = reader.getArgb(x, donorY);
        if (((donor >>> 24) & 0xff) >= 200 && channelSum(donor) >= PAPER_SUM) {
            return donor;
        }
        int fallbackX = Math.min(width - 20, Math.max(120, x));
        donor = reader.getArgb(fallbackX, donorY);
        if (((donor >>> 24) & 0xff) >= 200 && channelSum(donor) >= PAPER_SUM) {
            return donor;
        }
        return reader.getArgb(Math.min(width - 20, 160), donorY);
    }

    private static int channelSum(int argb) {
        return ((argb >> 16) & 0xff) + ((argb >> 8) & 0xff) + (argb & 0xff);
    }

    private static boolean blueRibbon(int argb) {
        int red = (argb >> 16) & 0xff;
        int blue = argb & 0xff;
        return blue > red + 25 && blue > 70;
    }

    /** Счётчик копий снизу слева, на пергаменте, под флагом. */
    private static void paintCount(StackPane art, double width, int copies) {
        double scale = width / SOURCE_WIDTH;
        Image picture = CardImages.get("/gwent/ui/copy-stack.png");
        ImageView icon = new ImageView(picture);
        icon.setMouseTransparent(true);
        // Высота в пикселях исходной карты 310×585. Ширина берётся из файла, а не из старой иконки.
        double iconHeight = 56 * scale;
        double iconWidth = iconHeight;
        if (picture != null && picture.getHeight() > 0) {
            iconWidth = iconHeight * picture.getWidth() / picture.getHeight();
        }
        icon.setFitHeight(iconHeight);
        icon.setFitWidth(iconWidth);
        icon.setPreserveRatio(true);
        icon.setSmooth(true);
        double textSize = Math.max(13, 32 * scale);
        Text count = new Text("x" + copies);
        count.setFont(Font.font("Philosopher", textSize));
        count.setFill(Color.web("#5a3514"));
        count.setMouseTransparent(true);
        HBox badge = new HBox(Math.max(3, 4.8 * scale), icon, count);
        badge.setAlignment(Pos.CENTER_LEFT);
        badge.setMouseTransparent(true);
        badge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(badge, Pos.BOTTOM_LEFT);
        // Ниже хвоста флага (он кончается около строки 516 из 585).
        StackPane.setMargin(badge, new Insets(0, 0, 6 * scale, 18 * scale));
        art.getChildren().add(badge);
    }

    public static double height(double width, CardCrop crop) {
        double fraction = crop == CardCrop.DECK ? 1 : crop.visibleFraction();
        return width * SOURCE_HEIGHT / SOURCE_WIDTH * fraction;
    }

}
