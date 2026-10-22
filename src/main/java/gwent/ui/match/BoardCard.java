package gwent.ui.match;

import gwent.catalog.CardDef;
import gwent.ui.card.CardImages;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * Обрезанная карта для руки и поля. Кружок силы на рисунке пустой.
 * У обычной карты он светлый, у героя чёрный, поэтому цифра героя белая.
 */
public final class BoardCard {
    public static final double CROP_WIDTH = 310;
    public static final double CROP_HEIGHT = 430;
    /** Центр пустого кружка на файле 310×430. */
    private static final double CIRCLE_X = 46;
    private static final double CIRCLE_Y = 44;
    private static final double CIRCLE_DIAMETER = 62;

    private BoardCard() {
    }

    public static Pane create(CardDef card, double width, Integer strength) {
        return create(card, width, strength, strength);
    }

    /**
     * {@code baseStrength} — сила с карты. Если на поле она выше, цифра зелёная, если ниже — красная.
     * У героя цифра всегда белая: эффекты его не меняют.
     */
    public static Pane create(CardDef card, double width, Integer strength, Integer baseStrength) {
        double height = width * CROP_HEIGHT / CROP_WIDTH;
        Pane art = new Pane();
        art.getStyleClass().add("board-card");
        art.setMinSize(width, height);
        art.setPrefSize(width, height);
        art.setMaxSize(width, height);
        Image picture = card == null ? null : CardImages.get(card.imagePath());
        if (picture == null) {
            Label missing = new Label(card == null ? "?" : card.name());
            missing.getStyleClass().add("deck-stat-name");
            missing.setWrapText(true);
            missing.setLayoutX(8);
            missing.setLayoutY(height * 0.4);
            missing.setMaxWidth(width - 16);
            art.getChildren().add(missing);
            return art;
        }
        ImageView view = new ImageView(picture);
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setSmooth(true);
        view.setMouseTransparent(true);
        if (picture.getWidth() > 2 && picture.getHeight() > 2) {
            view.setViewport(new Rectangle2D(0, 1, picture.getWidth(), picture.getHeight() - 1));
        }
        art.getChildren().add(view);
        if (strength != null) {
            int base = baseStrength == null ? strength : baseStrength;
            art.getChildren().add(strengthLabel(width, height, strength, ink(card, strength, base)));
        }
        if (card != null) {
            art.getChildren().add(nameLabel(card, width, height));
        }
        return art;
    }

    /** 1 — выше базы, -1 — ниже, 0 — как на карте. Герой всегда 0: цвет у него отдельный. */
    static int tone(CardDef card, int actual, int base) {
        if (card != null && card.hero()) {
            return 0;
        }
        return Integer.compare(actual, base);
    }

    private static Color ink(CardDef card, int actual, int base) {
        if (card != null && card.hero()) {
            return Color.web("#f7f4ee");
        }
        return switch (tone(card, actual, base)) {
            case 1 -> Color.web("#1b7a32");
            case -1 -> Color.web("#b42318");
            default -> Color.web("#2a2118");
        };
    }

    /** Та же цифра, что в кружке карты: размер от диаметра, для двузначных чуть меньше. */
    public static Text strengthNumber(int strength, double diameter) {
        return strengthNumber(strength, diameter, Color.web("#2a2118"));
    }

    private static Text strengthNumber(int strength, double diameter, Color ink) {
        double size = Math.max(14, diameter * 0.62);
        if (strength >= 10) {
            size *= 0.86;
        }
        Text text = new Text(Integer.toString(strength));
        text.setFont(Font.font("Philosopher", size));
        text.setFill(ink);
        text.setStroke(ink);
        text.setStrokeWidth(Math.max(0.8, size * 0.07));
        text.setMouseTransparent(true);
        return text;
    }

    private static StackPane strengthLabel(double width, double height, int strength, Color ink) {
        double diameter = width * CIRCLE_DIAMETER / CROP_WIDTH;
        Text text = strengthNumber(strength, diameter, ink);
        StackPane box = new StackPane(text);
        box.setMinSize(diameter, diameter);
        box.setPrefSize(diameter, diameter);
        box.setMaxSize(diameter, diameter);
        box.setMouseTransparent(true);
        double centerX = width * CIRCLE_X / CROP_WIDTH;
        double centerY = height * CIRCLE_Y / CROP_HEIGHT;
        box.setLayoutX(centerX - diameter / 2.0);
        box.setLayoutY(centerY - diameter / 2.0);
        return box;
    }

    /** На обрезанном рисунке имени нет, а без него ряд не прочитать. */
    private static Label nameLabel(CardDef card, double width, double height) {
        double bar = Math.max(16, width * 0.16);
        Label name = new Label(card.name());
        name.setAlignment(Pos.CENTER);
        name.setTextAlignment(TextAlignment.CENTER);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setFont(Font.font("Philosopher", Math.max(10, bar * 0.58)));
        name.setTextFill(Color.web("#f6edd8"));
        name.setPrefSize(width, bar);
        name.setMinSize(width, bar);
        name.setMaxSize(width, bar);
        name.setLayoutY(height - bar);
        name.setStyle("-fx-background-color: rgba(16, 10, 7, 0.82);");
        name.setMouseTransparent(true);
        return name;
    }
}
