package gwent.ui.deck;

import gwent.catalog.CardCatalog;
import gwent.catalog.CardDef;
import gwent.catalog.CardKind;
import gwent.catalog.Faction;
import gwent.catalog.Row;
import gwent.deck.Deck;
import gwent.deck.DeckCheck;
import gwent.deck.DeckStore;
import gwent.ui.Navigator;
import gwent.ui.UiScale;
import gwent.ui.card.CardCrop;
import gwent.ui.card.CardFace;
import gwent.ui.card.CardStrip;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Настройка одной колоды на фракцию: слева коллекция, справа колода, в середине лидер.
 * «Начать игру» проверяет лимиты и пока только подтверждает выбор — самой партии ещё нет.
 */
public final class DeckScreen {
    /** Ширина карты в списке колоды при масштабе 1. На 2560×1440 это уже читаемый размер. */
    private static final double DECK_CARD_WIDTH = 220;
    private static final double LEADER_WIDTH = 240;
    /** Запас под вертикальную полосу: она есть только у длинного списка и иначе сбивает число карт в ряд. */
    private static final double SCROLLBAR_RESERVE = 20;
    private static final double CENTER_WIDTH = 280;

    private final Navigator navigator;
    private final DeckStore store;
    private final List<Button> factionButtons = new ArrayList<>();
    private final List<Button> collectionFilters = new ArrayList<>();
    private final List<Button> deckFilters = new ArrayList<>();
    private Faction faction = Faction.NORTHERN_REALMS;
    private Deck deck;
    private Filter collectionFilter = Filter.ALL;
    private Filter deckFilter = Filter.ALL;
    private TilePane collection;
    private TilePane chosen;
    private VBox collectionColumn;
    private VBox deckColumn;
    private VBox leaderBox;
    private Label perk;
    private Label unitsValue;
    private Label specialsValue;
    private Label strengthValue;
    private Label heroesValue;
    private Label totalValue;
    private Label problem;
    private Button start;
    private StackPane picker;
    private CardStrip leaderStrip;
    private double scale = 1;

    public DeckScreen(Navigator navigator, DeckStore store) {
        this.navigator = navigator;
        this.store = store;
    }

    public Parent create() {
        deck = store.get(faction);
        BorderPane page = new BorderPane();
        page.getStyleClass().add("deck-screen");
        page.setTop(header());
        page.setCenter(body());
        StackPane root = new StackPane(page, picker());
        root.layoutBoundsProperty().addListener((obs, was, bounds) -> {
            double next = UiScale.of(bounds.getWidth(), bounds.getHeight());
            if (Math.abs(next - scale) < 0.015) {
                return;
            }
            scale = next;
            refresh();
        });
        refresh();
        return root;
    }

    private VBox header() {
        HBox factions = new HBox(8);
        factions.setAlignment(Pos.CENTER);
        factions.setMaxWidth(Double.MAX_VALUE);
        for (Faction candidate : List.of(Faction.NORTHERN_REALMS, Faction.NILFGAARD, Faction.SCOIATAEL, Faction.MONSTERS)) {
            Button button = new Button(CardCatalog.factionName(candidate));
            button.getStyleClass().add("deck-faction");
            button.setOnAction(event -> selectFaction(candidate));
            button.setUserData(candidate);
            factionButtons.add(button);
            factions.getChildren().add(button);
        }
        perk = new Label();
        perk.getStyleClass().add("deck-perk");
        perk.setWrapText(false);
        perk.setMaxWidth(Double.MAX_VALUE);
        perk.setAlignment(Pos.CENTER);
        VBox box = new VBox(10, factions, perk);
        box.setAlignment(Pos.TOP_CENTER);
        box.getStyleClass().add("deck-header");
        return box;
    }

    private HBox columns;

    private HBox body() {
        collection = tiles();
        chosen = tiles();
        collectionColumn = column("Коллекция карт", Pos.CENTER_LEFT, filterBar(collectionFilters, true), collection);
        deckColumn = column("Карты колоды", Pos.CENTER_RIGHT, filterBar(deckFilters, false), chosen);
        VBox middle = center();
        HBox.setHgrow(middle, Priority.NEVER);
        HBox box = new HBox(16, collectionColumn, middle, deckColumn);
        box.getStyleClass().add("deck-body");
        columns = box;
        box.widthProperty().addListener((obs, was, width) -> syncColumns());
        return box;
    }

    private VBox center() {
        Label leaderTitle = new Label("Лидер");
        leaderTitle.getStyleClass().add("deck-column-title");
        leaderTitle.setMaxWidth(Double.MAX_VALUE);
        leaderTitle.setAlignment(Pos.CENTER);
        // Заголовок и фильтры списка занимают место над картами. «Лидер» начинается там же, где карты.
        Region aboveCards = new Region();
        Region listHeading = (Region) collectionColumn.getChildren().get(0);
        Region listFilters = (Region) collectionColumn.getChildren().get(1);
        aboveCards.prefHeightProperty().bind(listHeading.heightProperty().add(listFilters.heightProperty()).add(6));
        aboveCards.minHeightProperty().bind(aboveCards.prefHeightProperty());
        aboveCards.maxHeightProperty().bind(aboveCards.prefHeightProperty());
        leaderBox = new VBox(8);
        leaderBox.setAlignment(Pos.TOP_CENTER);
        unitsValue = stat();
        specialsValue = stat();
        strengthValue = stat();
        heroesValue = stat();
        totalValue = stat();
        problem = new Label();
        problem.getStyleClass().add("deck-problem");
        problem.setWrapText(true);
        start = new Button("Начать игру");
        start.getStyleClass().add("deck-action");
        start.setOnAction(event -> navigator.showMatchReady(deck));
        Button back = new Button("В меню");
        back.getStyleClass().add("deck-action");
        back.setOnAction(event -> navigator.showMainMenu());
        VBox box = new VBox(10,
                aboveCards,
                leaderTitle,
                leaderBox,
                line("Карт в колоде", totalValue),
                line("Карты отрядов", unitsValue),
                line("Особые карты", specialsValue),
                line("Сила отрядов", strengthValue),
                line("Герои", heroesValue),
                problem,
                start,
                back);
        box.getStyleClass().add("deck-center");
        box.setAlignment(Pos.TOP_CENTER);
        box.setMinWidth(260);
        box.setPrefWidth(CENTER_WIDTH);
        box.setMaxWidth(CENTER_WIDTH);
        return box;
    }

    private void selectFaction(Faction next) {
        closePicker();
        faction = next;
        deck = store.get(faction);
        collectionFilter = Filter.ALL;
        deckFilter = Filter.ALL;
        refresh();
    }

    private void refresh() {
        for (Button button : factionButtons) {
            button.getStyleClass().remove("deck-faction-selected");
            if (button.getUserData() == faction) {
                button.getStyleClass().add("deck-faction-selected");
            }
        }
        markFilters(collectionFilters, collectionFilter);
        markFilters(deckFilters, deckFilter);
        perk.setText(CardCatalog.perk(faction));
        perk.setStyle("-fx-font-size: " + Math.max(14, Math.round(16 * scale)) + "px;");
        showLeader();
        if (picker.isVisible()) {
            showLeaderStrip();
        }
        fill(collection, available(), collectionFilter, true);
        fill(chosen, inDeck(), deckFilter, false);
        int units = DeckCheck.units(deck);
        int specials = DeckCheck.specials(deck);
        totalValue.setText(Integer.toString(deck.cardIds().size()));
        unitsValue.setText(Integer.toString(units));
        specialsValue.setText(specials + "/" + DeckCheck.MAX_SPECIALS);
        strengthValue.setText(Integer.toString(DeckCheck.unitStrength(deck)));
        heroesValue.setText(Integer.toString(DeckCheck.heroes(deck)));
        unitsValue.getStyleClass().setAll(units < DeckCheck.MIN_UNITS ? "deck-bad" : "deck-stat");
        specialsValue.getStyleClass().setAll(specials > DeckCheck.MAX_SPECIALS ? "deck-bad" : "deck-stat");
        String why = DeckCheck.problem(deck);
        problem.setText(why);
        start.setDisable(!why.isEmpty());
        syncColumns();
    }

    /**
     * Обе колонки делят место поровну через одинаковые min/pref и рост в {@code HBox}.
     * Число карт в ряд считается от этой общей ширины и не зависит от того, пуст список или нет.
     */
    private void syncColumns() {
        if (columns == null || collectionColumn == null) {
            return;
        }
        double total = columns.getWidth();
        if (total < 400) {
            return;
        }
        double min = columnMin();
        collectionColumn.setMinWidth(min);
        deckColumn.setMinWidth(min);
        var padding = columns.getPadding();
        double side = (total - padding.getLeft() - padding.getRight()
                - columns.getSpacing() * 2 - CENTER_WIDTH) / 2.0;
        if (side < min) {
            side = min;
        }
        int count = columnsFor(side);
        applyColumns(collection, count, side);
        applyColumns(chosen, count, side);
    }

    private double columnMin() {
        return UiScale.px(DECK_CARD_WIDTH, scale) + SCROLLBAR_RESERVE + 12;
    }

    private int columnsFor(double side) {
        double card = cardBoxWidth();
        double gap = collection.getHgap();
        double inner = side - SCROLLBAR_RESERVE;
        if (inner < card) {
            return 1;
        }
        return Math.max(1, (int) ((inner + gap) / (card + gap)));
    }

    /** Одна и та же ширина ячейки у обоих списков, чтобы полоса прокрутки не добавляла лишний столбец. */
    private void applyColumns(TilePane pane, int count, double side) {
        double cardW = cardBoxWidth();
        double cardH = CardFace.height(UiScale.px(DECK_CARD_WIDTH, scale), CardCrop.DECK) + 4;
        double gap = pane.getHgap();
        double inner = Math.max(cardW, side - SCROLLBAR_RESERVE);
        double tile = count <= 1 ? inner : (inner - gap * (count - 1)) / count;
        if (tile < cardW) {
            tile = cardW;
        }
        pane.setPrefColumns(count);
        pane.setPrefTileWidth(tile);
        pane.setPrefTileHeight(cardH);
    }

    private double cardBoxWidth() {
        return UiScale.px(DECK_CARD_WIDTH, scale) + 4;
    }

    private void showLeader() {
        leaderBox.getChildren().clear();
        CardDef leader = CardCatalog.get(deck.leaderId());
        Button card = new Button();
        card.getStyleClass().add("card-tile");
        card.setGraphic(CardFace.create(leader, UiScale.px(LEADER_WIDTH, scale), CardCrop.LEADER_SLOT, 0));
        card.setOnAction(event -> openPicker());
        leaderBox.getChildren().add(card);
    }

    private StackPane picker() {
        Label title = new Label("Выберите лидера");
        title.getStyleClass().add("leader-picker-title");
        leaderStrip = new CardStrip();
        leaderStrip.setHeading("Умение лидера");
        leaderStrip.setOnSelect(index -> {
            List<CardDef> leaders = CardCatalog.leaders(faction);
            if (index >= 0 && index < leaders.size()) {
                update(deck.withLeader(leaders.get(index).id()));
            }
        });
        leaderStrip.setMaxWidth(Double.MAX_VALUE);
        Button done = new Button("Готово");
        done.getStyleClass().add("deck-action");
        done.setOnAction(event -> closePicker());
        VBox panel = new VBox(18, title, leaderStrip, done);
        panel.setAlignment(Pos.CENTER);
        panel.setFillWidth(true);
        panel.setPickOnBounds(false);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        panel.setMaxWidth(Double.MAX_VALUE);
        picker = new StackPane(panel);
        picker.getStyleClass().add("leader-picker");
        picker.setVisible(false);
        picker.setMouseTransparent(true);
        picker.setOnMouseClicked(event -> closePicker());
        return picker;
    }

    private void openPicker() {
        showLeaderStrip();
        picker.setVisible(true);
        picker.setMouseTransparent(false);
    }

    private void closePicker() {
        picker.setVisible(false);
        picker.setMouseTransparent(true);
    }

    private void showLeaderStrip() {
        List<CardDef> leaders = CardCatalog.leaders(faction);
        int index = 0;
        for (int i = 0; i < leaders.size(); i++) {
            if (leaders.get(i).id() == deck.leaderId()) {
                index = i;
            }
        }
        leaderStrip.setScale(scale);
        leaderStrip.setCards(leaders, index);
    }

    private void fill(TilePane pane, List<CardDef> cards, Filter active, boolean adding) {
        pane.getChildren().clear();
        for (CardDef card : cards) {
            if (!active.accepts(card)) {
                continue;
            }
            int count = adding ? card.copies() - deck.count(card.id()) : deck.count(card.id());
            Button tile = new Button();
            tile.getStyleClass().add("card-tile");
            tile.setGraphic(CardFace.create(card, UiScale.px(DECK_CARD_WIDTH, scale), CardCrop.DECK, count));
            tile.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            tile.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            tile.setOnAction(event -> update(adding ? deck.plus(card.id()) : deck.minus(card.id())));
            pane.getChildren().add(tile);
        }
    }

    private List<CardDef> available() {
        return CardCatalog.collection(faction).stream()
                .filter(card -> deck.count(card.id()) < card.copies())
                .toList();
    }

    private List<CardDef> inDeck() {
        Map<Integer, CardDef> order = new LinkedHashMap<>();
        for (int id : deck.cardIds()) {
            order.putIfAbsent(id, CardCatalog.get(id));
        }
        return List.copyOf(order.values());
    }

    private void update(Deck next) {
        deck = next;
        store.put(deck);
        refresh();
    }

    private static void markFilters(List<Button> buttons, Filter active) {
        for (Button button : buttons) {
            button.getStyleClass().remove("deck-filter-selected");
            if (button.getUserData() == active) {
                button.getStyleClass().add("deck-filter-selected");
            }
        }
    }

    /** Свои кнопки фильтра у коллекции и у колоды: каждый список отбирается отдельно. */
    private FlowPane filterBar(List<Button> into, boolean collectionSide) {
        FlowPane bar = new FlowPane(4, 4);
        bar.setAlignment(collectionSide ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT);
        bar.setMaxWidth(Double.MAX_VALUE);
        for (Filter candidate : Filter.values()) {
            Button button = new Button(candidate.caption);
            button.getStyleClass().add("deck-filter");
            button.setUserData(candidate);
            button.setOnAction(event -> {
                if (collectionSide) {
                    collectionFilter = candidate;
                } else {
                    deckFilter = candidate;
                }
                refresh();
            });
            into.add(button);
            bar.getChildren().add(button);
        }
        return bar;
    }

    private static VBox column(String title, Pos align, FlowPane filters, TilePane tiles) {
        Label heading = new Label(title);
        heading.getStyleClass().add("deck-column-title");
        heading.setMinWidth(0);
        heading.setMaxWidth(Double.MAX_VALUE);
        heading.setAlignment(align);
        ScrollPane scroll = new ScrollPane(tiles);
        scroll.setFitToWidth(true);
        scroll.setMinWidth(0);
        scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("deck-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox box = new VBox(8, heading, filters, scroll);
        box.setMinWidth(0);
        box.setPrefWidth(480);
        box.setMaxWidth(Double.MAX_VALUE);
        box.getStyleClass().add("deck-column");
        HBox.setHgrow(box, Priority.ALWAYS);
        filters.prefWrapLengthProperty().bind(box.widthProperty());
        return box;
    }

    private TilePane tiles() {
        TilePane pane = new TilePane();
        pane.setHgap(10);
        pane.setVgap(12);
        pane.setPrefColumns(3);
        pane.setMinWidth(0);
        pane.setTileAlignment(Pos.TOP_LEFT);
        return pane;
    }

    private static Label stat() {
        Label label = new Label("0");
        label.getStyleClass().add("deck-stat");
        return label;
    }

    private static HBox line(String caption, Label value) {
        Label name = new Label(caption);
        name.getStyleClass().add("deck-stat-name");
        name.setMinWidth(120);
        HBox box = new HBox(8, name, value);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private enum Filter {
        ALL("Все"),
        MELEE("Рукопашные"),
        RANGED("Дальние"),
        SIEGE("Осада"),
        SPECIAL("Особые"),
        HERO("Герои");

        private final String caption;

        Filter(String caption) {
            this.caption = caption;
        }

        boolean accepts(CardDef card) {
            return switch (this) {
                case ALL -> true;
                case MELEE -> card.rows().contains(Row.MELEE);
                case RANGED -> card.rows().contains(Row.RANGED);
                case SIEGE -> card.rows().contains(Row.SIEGE);
                case SPECIAL -> card.special();
                case HERO -> card.hero();
            };
        }
    }
}
