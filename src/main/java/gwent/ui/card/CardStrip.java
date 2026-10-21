package gwent.ui.card;

import gwent.catalog.CardDef;
import gwent.catalog.CardKind;
import gwent.catalog.SkillTexts;
import gwent.ui.UiScale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Горизонтальный ряд полных карт. Выбранная стоит в центре окна ряда,
 * соседи видны, дальние уезжают за край. Блок описания есть только если есть текст.
 * Тот же ряд потом пойдёт в муллиган и в воскрешение медика.
 */
public final class CardStrip extends VBox {
    private final List<CardDef> cards = new ArrayList<>();
    private final Pane viewport = new Pane();
    private final HBox row = new HBox();
    private final VBox descriptionBox = new VBox(6);
    private final Label heading = new Label();
    private final Label description = new Label();
    private int selected;
    private double scale = 1;
    private double slot;
    private double gap;
    private String headingText = "";
    private IntConsumer onSelect = index -> {
    };

    public CardStrip() {
        heading.getStyleClass().add("leader-ability-title");
        description.getStyleClass().add("leader-ability-text");
        description.setWrapText(true);
        description.setAlignment(Pos.CENTER);
        description.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        descriptionBox.getStyleClass().add("strip-description");
        descriptionBox.setAlignment(Pos.CENTER);
        descriptionBox.getChildren().addAll(heading, description);
        viewport.getChildren().add(row);
        viewport.setMinHeight(0);
        setAlignment(Pos.CENTER);
        setFillWidth(true);
        getChildren().addAll(viewport, descriptionBox);
        viewport.widthProperty().addListener((obs, was, now) -> place());
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(viewport.widthProperty());
        clip.heightProperty().bind(viewport.heightProperty());
        viewport.setClip(clip);
    }

    public static String blurb(CardDef card) {
        if (card.kind() == CardKind.LEADER) {
            return card.text();
        }
        return SkillTexts.blurb(card);
    }

    public void setHeading(String headingText) {
        this.headingText = headingText == null ? "" : headingText;
        place();
    }

    public void setOnSelect(IntConsumer onSelect) {
        this.onSelect = onSelect == null ? index -> {
        } : onSelect;
    }

    public void setScale(double scale) {
        this.scale = scale;
        rebuild();
    }

    public void setCards(List<CardDef> cards, int selected) {
        this.cards.clear();
        this.cards.addAll(cards);
        this.selected = cards.isEmpty() ? 0 : Math.clamp(selected, 0, cards.size() - 1);
        rebuild();
    }

    public int selected() {
        return selected;
    }

    private void rebuild() {
        row.getChildren().clear();
        double width = UiScale.px(CardFace.SOURCE_WIDTH, scale);
        double pad = UiScale.px(6, scale);
        gap = UiScale.px(18, scale);
        slot = width + pad * 2;
        row.setSpacing(gap);
        row.setFillHeight(false);
        for (int i = 0; i < cards.size(); i++) {
            CardDef card = cards.get(i);
            StackPane frame = new StackPane(CardFace.create(card, width, CardCrop.FULL, 0));
            frame.setPadding(new Insets(pad));
            frame.getStyleClass().add("leader-frame");
            if (i == selected) {
                frame.getStyleClass().add("leader-frame-selected");
            }
            Button button = new Button();
            button.getStyleClass().add("leader-choice");
            button.setGraphic(frame);
            button.setMinWidth(slot);
            button.setPrefWidth(slot);
            button.setMaxWidth(slot);
            int index = i;
            button.setOnAction(event -> choose(index));
            row.getChildren().add(button);
        }
        double cardHeight = CardFace.height(width, CardCrop.FULL) + pad * 2 + UiScale.px(24, scale);
        viewport.setPrefHeight(cardHeight);
        viewport.setMinHeight(cardHeight);
        place();
    }

    private void choose(int index) {
        if (index == selected) {
            return;
        }
        selected = index;
        onSelect.accept(index);
        rebuild();
    }

    private void place() {
        double viewWidth = viewport.getWidth();
        if (viewWidth <= 0 || cards.isEmpty() || slot <= 0) {
            return;
        }
        double x = offset(viewWidth, slot, gap, cards.size(), selected);
        double rowWidth = cards.size() * slot + Math.max(0, cards.size() - 1) * gap;
        double y = UiScale.px(12, scale);
        row.resizeRelocate(x, y, rowWidth, viewport.getHeight() - y);
        showDescription();
    }

    /**
     * Сдвиг ряда. Если все карты влезают, ряд стоит по центру целиком.
     * Если нет — в центре окна выбранная карта, остальные уезжают за край.
     */
    public static double offset(double viewWidth, double slot, double gap, int count, int selected) {
        if (count <= 0 || slot <= 0 || viewWidth <= 0) {
            return 0;
        }
        double row = count * slot + Math.max(0, count - 1) * gap;
        if (row <= viewWidth) {
            return (viewWidth - row) / 2.0;
        }
        int index = Math.clamp(selected, 0, count - 1);
        double center = index * (slot + gap) + slot / 2.0;
        return viewWidth / 2.0 - center;
    }

    private void showDescription() {
        String text = cards.isEmpty() ? "" : blurb(cards.get(selected));
        boolean show = text != null && !text.isBlank();
        descriptionBox.setVisible(show);
        descriptionBox.setManaged(show);
        if (!show) {
            return;
        }
        heading.setText(headingText);
        heading.setVisible(!headingText.isBlank());
        heading.setManaged(!headingText.isBlank());
        description.setText(text);
        double textWidth = Math.min(viewport.getWidth() * 0.72, UiScale.px(760, scale));
        description.setMaxWidth(textWidth);
        descriptionBox.setMaxWidth(textWidth + UiScale.px(44, scale));
        double line = Math.max(36, UiScale.px(46, scale));
        description.setMinHeight(line);
        description.setPrefHeight(line);
        heading.setStyle("-fx-font-size: " + Math.max(13, Math.round(16 * scale)) + "px;");
        description.setStyle("-fx-font-size: " + Math.max(13, Math.round(15 * scale)) + "px;");
    }
}
