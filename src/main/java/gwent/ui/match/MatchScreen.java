package gwent.ui.match;

import gwent.catalog.Ability;
import gwent.catalog.CardCatalog;
import gwent.catalog.CardDef;
import gwent.catalog.CardKind;
import gwent.catalog.Faction;
import gwent.catalog.PlaysOn;
import gwent.catalog.Row;
import gwent.catalog.SkillTexts;
import gwent.deck.Deck;
import gwent.deck.StarterDecks;
import gwent.rules.IllegalMove;
import gwent.rules.Match;
import gwent.rules.MatchAi;
import gwent.rules.Move;
import gwent.rules.PlacedCard;
import gwent.rules.Side;
import gwent.ui.UiScale;
import gwent.ui.card.CardCrop;
import gwent.ui.card.CardFace;
import gwent.ui.card.CardImages;
import gwent.ui.card.CardStrip;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Стол партии.
 * Витрина — место справа, куда карта выезжает крупной полной версией,
 * прежде чем игрок кликнет ряд. Оттуда же она уезжает на поле уже обрезанной.
 */
public final class MatchScreen {
    private static final double FLY_MS = 480;
    private static final double SHOWCASE_HOLD_MS = 500;
    private static final double PASS_MS = 700;
    private static final double ROUND_MS = 1400;

    private final Match match;
    private final MatchAi.Style opponentStyle;
    private final Runnable back;
    private final StackPane overlay = new StackPane();
    private final CardStrip strip = new CardStrip();
    private final Label pickerTitle = new Label();
    private final Button pickerAct = new Button();
    private final Button pickerDone = new Button("Дальше");

    private final Map<Side, EnumMap<Row, Node>> rowPlates = new EnumMap<>(Side.class);
    private final Map<Side, EnumMap<Row, Node>> hornSlots = new EnumMap<>(Side.class);
    private final Map<Integer, Node> unitNodes = new HashMap<>();
    private final Map<Integer, Node> handNodes = new HashMap<>();
    private final Map<Integer, Node> arrivalSlots = new HashMap<>();
    private final Map<Integer, Node> gapNodes = new HashMap<>();
    private final Set<Integer> arriving = new HashSet<>();
    private final Set<Integer> heldGaps = new HashSet<>();

    private StackPane root;
    private BorderPane table;
    private Pane flight;
    private StackPane showcaseHolder;
    private Label showcaseNote;
    private Node weatherSlot;
    private Node rowsBox;
    private Node opponentDeckPile;
    private Node playerDeckPile;

    private Integer showcaseIndex;
    private Side showcaseSide;
    private boolean showcaseReady;
    private boolean flying;
    private Row medicRow;
    private Integer reviveIndex;
    private boolean waitingReviveRow;
    private boolean locked;
    private String note = "";
    private List<Integer> pickerOrder = List.of();

    private double scale = 1;
    private double leftWidth = 200;
    private double rightWidth = 280;
    private double boardCardWidth = 78;
    private double boardCardHeight = 108;
    private double rowBand = 78;
    private double gapAboveHand;
    private double handBand = 160;
    private double deckWidth = 84;
    private double showcaseWidth = 210;
    private double handCardWidth = 78;

    private MatchScreen(Match match, MatchAi.Style opponentStyle, Runnable back) {
        this.match = match;
        this.opponentStyle = opponentStyle;
        this.back = back;
    }

    public static MatchScreen versusPreset(Deck player, Runnable back) {
        Faction foe = player.faction() == Faction.NILFGAARD ? Faction.NORTHERN_REALMS : Faction.NILFGAARD;
        Match match = Match.start(player, StarterDecks.create(foe), new Random());
        return new MatchScreen(match, MatchAi.Style.CAUTIOUS, back);
    }

    public Parent create() {
        table = new BorderPane();
        table.setPadding(new Insets(8, 10, 8, 10));
        flight = new Pane();
        flight.setMouseTransparent(true);
        flight.setPickOnBounds(false);
        buildOverlay();
        root = new StackPane(table, flight, overlay);
        root.getStyleClass().add("match-screen");
        // Иначе минимальная высота детей раздвигает окно на каждом перестроении.
        root.setMinSize(0, 0);
        table.setMinSize(0, 0);
        root.layoutBoundsProperty().addListener((obs, was, bounds) -> measure(bounds.getWidth(), bounds.getHeight()));
        root.sceneProperty().addListener((obs, was, scene) -> {
            if (scene != null) {
                scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
            }
        });
        redraw();
        if (match.phase() == Match.Phase.MULLIGAN) {
            showMulligan();
        }
        return root;
    }

    private void onKey(KeyEvent event) {
        if (event.getCode() != KeyCode.ESCAPE || match.phase() == Match.Phase.MULLIGAN) {
            return;
        }
        if (overlay.isVisible() || (showcaseIndex != null && showcaseSide == Side.PLAYER)) {
            cancelShowcase();
            event.consume();
        }
    }

    private void measure(double width, double height) {
        if (width < 200 || height < 200) {
            return;
        }
        double next = UiScale.of(width, height);
        double left = clamp(width * 0.17, 168, 280);
        double right = 320;
        double band = clamp(height * 0.21, 112, 240);
        double deck = clamp(right * 0.40, 58, 124);
        double room = Math.max(170, height - deck * 1.6 * 2 - 150);
        double showW = Math.min(right - 44, room * CardFace.SOURCE_WIDTH / CardFace.SOURCE_HEIGHT);
        double rowsRoom = Math.max(420, height - band - 28);
        double bandHeight = clamp(rowsRoom * 0.85 / 6.0, 88, 190);
        double cardH = Math.max(70, bandHeight - 10);
        double cardW = cardH * BoardCard.CROP_WIDTH / BoardCard.CROP_HEIGHT;
        double maxW = Math.max(44, (width - left - right - 96) / 7.0);
        if (cardW > maxW) {
            cardW = maxW;
            cardH = cardW * BoardCard.CROP_HEIGHT / BoardCard.CROP_WIDTH;
        }
        boolean same = near(next, scale) && near(cardW, boardCardWidth) && near(showW, showcaseWidth)
                && near(left, leftWidth) && near(band, handBand) && near(bandHeight, rowBand);
        scale = next;
        leftWidth = left;
        rightWidth = right;
        handBand = band;
        deckWidth = deck;
        showcaseWidth = showW;
        boardCardWidth = cardW;
        boardCardHeight = cardH;
        rowBand = bandHeight;
        handBand = rowBand;
        handCardWidth = boardCardWidth;
        double rowsBlock = 6 * bandHeight + 10;
        double slack = Math.max(0, height - rowsBlock - band - 40);
        gapAboveHand = slack / 2.0;
        if (overlay.isVisible()) {
            strip.setScale(scale);
        }
        if (!same && !locked) {
            redraw();
        }
    }

    private void redraw() {
        rowPlates.clear();
        hornSlots.clear();
        unitNodes.clear();
        handNodes.clear();
        arrivalSlots.clear();
        gapNodes.clear();
        opponentDeckPile = null;
        playerDeckPile = null;
        rowsBox = null;
        for (Side side : Side.values()) {
            rowPlates.put(side, new EnumMap<>(Row.class));
            hornSlots.put(side, new EnumMap<>(Row.class));
        }
        table.setTop(null);
        table.setLeft(leftColumn());
        table.setCenter(center());
        table.setRight(rightColumn());
    }

    private VBox leftColumn() {
        VBox box = new VBox();
        box.getStyleClass().add("match-rail");
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8, 10, 8, 6));
        box.setMinWidth(leftWidth);
        box.setPrefWidth(leftWidth);
        box.setMaxWidth(leftWidth);
        box.setMinHeight(0);
        Label round = new Label("Раунд " + match.round());
        round.getStyleClass().add("match-round");
        VBox middle = new VBox(8, round, weatherSlot());
        middle.setAlignment(Pos.CENTER);
        box.getChildren().add(flexibleGap());
        box.getChildren().add(leaderPortrait(Side.OPPONENT));
        box.getChildren().add(flexibleGap());
        box.getChildren().add(stats(Side.OPPONENT));
        box.getChildren().add(flexibleGap());
        box.getChildren().add(middle);
        box.getChildren().add(flexibleGap());
        box.getChildren().add(stats(Side.PLAYER));
        box.getChildren().add(flexibleGap());
        box.getChildren().add(leaderPortrait(Side.PLAYER));
        box.getChildren().add(flexibleGap());
        return box;
    }

    /** Лишняя высота делится поровну, а не собирается одной дырой по центру. */
    private Region flexibleGap() {
        Region gap = new Region();
        gap.setMinHeight(12);
        gap.setPrefHeight(18);
        VBox.setVgrow(gap, Priority.ALWAYS);
        return gap;
    }

    private Node leaderPortrait(Side side) {
        CardDef leader = CardCatalog.get(match.leaderId(side));
        Image picture = CardImages.get(leader.fullImagePath());
        double width = Math.min(boardCardWidth, leftWidth - 28);
        double cropHeight = picture == null ? width * 1.5 : picture.getHeight() * 442.0 / 585.0;
        double height = picture == null ? width * 1.5 : width * cropHeight / picture.getWidth();
        StackPane frame = new StackPane();
        frame.getStyleClass().add("match-leader");
        frame.setMinSize(width, height);
        frame.setMaxSize(width, height);
        if (picture != null) {
            ImageView view = new ImageView(picture);
            view.setFitWidth(width);
            view.setFitHeight(height);
            view.setSmooth(true);
            view.setMouseTransparent(true);
            view.setViewport(new Rectangle2D(0, 0, picture.getWidth(), cropHeight));
            frame.getChildren().add(view);
        }
        return frame;
    }

    private BorderPane stats(Side side) {
        BorderPane box = new BorderPane();
        box.getStyleClass().add("match-stats");
        double upperBlock = 38;
        double lowerBlock = Math.max(36, 36 * scale);
        double half = 15 + Math.max(upperBlock, lowerBlock) + 15;
        double statsH = half * 2;
        box.setMinHeight(statsH);
        box.setPrefHeight(statsH);
        box.setMaxHeight(statsH);
        box.setMinWidth(leftWidth - 36);
        box.setMaxWidth(leftWidth - 20);
        if (match.phase() == Match.Phase.PLAY && match.active() == side) {
            box.getStyleClass().add("match-stats-active");
        }
        VBox upper = new VBox(2);
        upper.setAlignment(Pos.TOP_LEFT);
        String title = side == Side.PLAYER ? "Вы" : "Противник";
        Label name = new Label(title);
        name.getStyleClass().add("match-side-name");
        Label faction = new Label(CardCatalog.factionName(match.faction(side)));
        faction.getStyleClass().add("match-faction");
        upper.getChildren().add(name);
        upper.getChildren().add(faction);
        if (match.passed(side)) {
            Label passed = new Label(match.hand(side).isEmpty() ? "Карты кончились" : "Пас");
            passed.getStyleClass().add("match-passed");
            upper.getChildren().add(passed);
        }
        HBox info = new HBox(10);
        info.setAlignment(Pos.CENTER_LEFT);
        info.getChildren().add(handCount(side));
        info.getChildren().add(gems(side));
        VBox topHalf = new VBox(upper);
        topHalf.setAlignment(Pos.BOTTOM_LEFT);
        topHalf.setPadding(new Insets(15, 0, 15, 2));
        topHalf.setMinHeight(half);
        topHalf.setPrefHeight(half);
        topHalf.setMaxHeight(half);
        VBox bottomHalf = new VBox(info);
        bottomHalf.setAlignment(Pos.TOP_LEFT);
        bottomHalf.setPadding(new Insets(15, 0, 15, 2));
        bottomHalf.setMinHeight(half);
        bottomHalf.setPrefHeight(half);
        bottomHalf.setMaxHeight(half);
        VBox text = new VBox(topHalf, bottomHalf);
        text.setMaxHeight(Double.MAX_VALUE);
        box.setCenter(text);
        StackPane badge = totalBadge(side);
        BorderPane.setAlignment(badge, Pos.CENTER);
        BorderPane.setMargin(badge, new Insets(0, 2, 0, 8));
        box.setRight(badge);
        return box;
    }

    private HBox handCount(Side side) {
        HBox box = new HBox(4);
        box.setAlignment(Pos.CENTER_LEFT);
        Image icon = CardImages.get("/gwent/ui/board/cards-icon.png");
        if (icon != null) {
            ImageView view = new ImageView(icon);
            double height = Math.max(28, 36 * scale);
            view.setFitHeight(height);
            view.setFitWidth(height * icon.getWidth() / icon.getHeight());
            view.setSmooth(true);
            box.getChildren().add(view);
        }
        Label count = new Label(Integer.toString(match.hand(side).size()));
        count.getStyleClass().add("match-hand-count");
        count.setFont(javafx.scene.text.Font.font("Philosopher", Math.max(20, 26 * scale)));
        box.getChildren().add(count);
        return box;
    }

    private HBox gems(Side side) {
        HBox box = new HBox(3);
        box.setAlignment(Pos.CENTER_LEFT);
        Image picture = CardImages.get("/gwent/ui/board/life.png");
        double size = Math.max(26, 34 * scale);
        int lit = livesLeft(side);
        for (int i = 0; i < Match.ROUND_WINS; i++) {
            if (picture == null) {
                box.getChildren().add(new Label(i < lit ? "●" : "○"));
                continue;
            }
            ImageView view = new ImageView(picture);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setSmooth(true);
            if (i >= lit) {
                view.setOpacity(0.30);
            }
            box.getChildren().add(view);
        }
        return box;
    }

    private StackPane totalBadge(Side side) {
        double size = statsBadgeSize();
        StackPane badge = new StackPane();
        badge.setMinSize(size, size);
        badge.setMaxSize(size, size);
        String path = side == Side.PLAYER ? "/gwent/ui/board/power_my.png" : "/gwent/ui/board/power_enemy.png";
        Image picture = CardImages.get(path);
        if (picture != null) {
            ImageView view = new ImageView(picture);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setSmooth(true);
            badge.getChildren().add(view);
        }
        badge.getChildren().add(BoardCard.strengthNumber(match.field().total(side), size));
        return badge;
    }

    private double statsBadgeSize() {
        return Math.max(52, 62 * scale);
    }

    private double rowBadgeSize() {
        return statsBadgeSize() * 0.7;
    }

    /** Жизни горят с начала партии и гаснут, когда раунд взят противником. */
    private int livesLeft(Side side) {
        return Math.max(0, Match.ROUND_WINS - match.gems(side.other()));
    }

    private StackPane weatherSlot() {
        StackPane slot = new StackPane();
        slot.getStyleClass().add("match-weather-slot");
        slot.setPickOnBounds(true);
        double height = clamp(leftWidth * 0.62, 110, 168);
        double width = height * 309.0 / 174.0;
        if (width > leftWidth - 8) {
            width = leftWidth - 8;
            height = width * 174.0 / 309.0;
        }
        slot.setMinHeight(0);
        slot.setPrefSize(width, height);
        slot.setMaxSize(width, height);
        HBox cards = new HBox(4);
        cards.setAlignment(Pos.CENTER);
        cards.setMouseTransparent(true);
        for (Row row : List.of(Row.MELEE, Row.RANGED, Row.SIEGE)) {
            if (!match.field().weather(row)) {
                continue;
            }
            double cardWidth = Math.min(boardCardWidth * 0.72, (width - 36) / 3.2);
            cards.getChildren().add(BoardCard.create(weatherArt(row), cardWidth, null));
        }
        slot.getChildren().add(cards);
        if (weatherReady()) {
            slot.getStyleClass().add("match-target");
            slot.setOnMouseClicked(event -> onWeatherSlot());
        }
        weatherSlot = slot;
        return slot;
    }

    private HBox center() {
        HBox body = new HBox(0);
        body.setFillHeight(true);
        body.setMinHeight(0);
        VBox rows = rows();
        rows.setMinHeight(0);
        VBox main = new VBox(0);
        main.getStyleClass().add("match-rows");
        main.setMinHeight(0);
        main.getChildren().add(rows);
        Region between = new Region();
        between.setMinHeight(0);
        between.setPrefHeight(gapAboveHand);
        between.setMaxHeight(gapAboveHand);
        main.getChildren().add(between);
        if (match.phase() != Match.Phase.MULLIGAN) {
            main.getChildren().add(handBar());
        }
        Region below = new Region();
        below.setMinHeight(0);
        VBox.setVgrow(below, Priority.ALWAYS);
        main.getChildren().add(below);
        HBox.setHgrow(main, Priority.ALWAYS);
        body.getChildren().addAll(powerColumn(), main);
        return body;
    }

    private StackPane powerColumn() {
        StackPane column = new StackPane();
        column.getStyleClass().add("match-column");
        double columnWidth = Math.max(50, rowBadgeSize() + 8);
        column.setMinWidth(columnWidth);
        column.setPrefWidth(columnWidth);
        column.setMaxWidth(columnWidth);
        column.setMaxHeight(Double.MAX_VALUE);
        VBox badges = new VBox(2);
        badges.setAlignment(Pos.TOP_CENTER);
        badges.setFillWidth(true);
        badges.setMouseTransparent(true);
        for (Row row : List.of(Row.SIEGE, Row.RANGED, Row.MELEE)) {
            badges.getChildren().add(powerCell(Side.OPPONENT, row));
        }
        for (Row row : List.of(Row.MELEE, Row.RANGED, Row.SIEGE)) {
            badges.getChildren().add(powerCell(Side.PLAYER, row));
        }
        StackPane.setAlignment(badges, Pos.TOP_CENTER);
        column.getChildren().add(badges);
        return column;
    }

    private StackPane powerCell(Side side, Row row) {
        StackPane cell = new StackPane(rowTotal(side, row));
        cell.setMinHeight(rowBand);
        cell.setPrefHeight(rowBand);
        cell.setMaxHeight(rowBand);
        cell.setMouseTransparent(true);
        return cell;
    }

    private VBox rows() {
        VBox box = new VBox(2);
        box.getStyleClass().add("match-rows");
        box.setFillWidth(true);
        rowsBox = box;
        for (Row row : List.of(Row.SIEGE, Row.RANGED, Row.MELEE)) {
            box.getChildren().add(rowLine(Side.OPPONENT, row));
        }
        for (Row row : List.of(Row.MELEE, Row.RANGED, Row.SIEGE)) {
            box.getChildren().add(rowLine(Side.PLAYER, row));
        }
        return box;
    }

    private HBox rowLine(Side side, Row row) {
        HBox line = new HBox(0);
        line.setAlignment(Pos.CENTER_LEFT);
        line.getStyleClass().add("match-row");
        line.setMinHeight(rowBand);
        line.setPrefHeight(rowBand);
        line.setMaxHeight(rowBand);
        line.getChildren().add(hornSlot(side, row));

        StackPane plate = new StackPane();
        plate.getStyleClass().add("match-row-plate");
        plate.setPickOnBounds(true);
        HBox.setHgrow(plate, Priority.ALWAYS);
        plate.setPrefHeight(rowBand);
        plate.setMaxWidth(Double.MAX_VALUE);
        plate.setMaxHeight(rowBand);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(plate.widthProperty());
        clip.heightProperty().bind(plate.heightProperty());
        plate.setClip(clip);
        if (match.field().weather(row)) {
            Region wash = new Region();
            wash.setMouseTransparent(true);
            wash.setStyle("-fx-background-color: " + weatherWash(row) + ";");
            wash.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            plate.getChildren().add(wash);
        }
        Image icon = CardImages.get(rowIcon(row));
        if (icon != null) {
            ImageView mark = new ImageView(icon);
            double markSize = Math.max(28, boardCardHeight * 0.72);
            mark.setFitWidth(markSize);
            mark.setFitHeight(markSize);
            mark.setSmooth(true);
            mark.setOpacity(0.42);
            mark.setMouseTransparent(true);
            plate.getChildren().add(mark);
        }
        List<RowPiece> pieces = rowPieces(side, row);
        HBox cards = new HBox(spacing(pieces.size()));
        cards.setAlignment(Pos.CENTER);
        cards.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        for (int index : RowOrder.indices(pieces.stream().map(RowPiece::entry).toList())) {
            cards.getChildren().add(pieceNode(side, row, pieces.get(index)));
        }
        plate.getChildren().add(cards);
        StackPane.setAlignment(cards, Pos.CENTER);
        rowPlates.get(side).put(row, plate);
        if (rowReady(side, row)) {
            line.getStyleClass().add("match-row-ready");
            plate.setOnMouseClicked(event -> onRow(side, row));
        }
        line.getChildren().add(plate);
        return line;
    }

    private List<RowPiece> rowPieces(Side side, Row row) {
        List<RowPiece> pieces = new ArrayList<>();
        for (PlacedCard placed : match.field().units(side, row)) {
            int shown = match.field().strength(side, row, placed);
            pieces.add(new RowPiece(new RowOrder.Entry(placed.card(), shown), placed));
        }
        for (Match.Token token : match.tokens()) {
            if (token.side() == side && token.row() == row && token.card().kind() != CardKind.HORN) {
                pieces.add(new RowPiece(new RowOrder.Entry(token.card(), 0), null));
            }
        }
        return pieces;
    }

    private StackPane hornSlot(Side side, Row row) {
        double slot = rowBand;
        StackPane box = new StackPane();
        box.getStyleClass().add("match-horn-slot");
        box.setPickOnBounds(true);
        box.setMinSize(slot, slot);
        box.setPrefSize(slot, slot);
        box.setMaxSize(slot, slot);
        Image icon = CardImages.get("/gwent/ui/board/field_upgrade.png");
        boolean horn = match.field().horn(side, row);
        if (icon != null) {
            ImageView mark = new ImageView(icon);
            mark.setFitWidth(slot * 0.78);
            mark.setFitHeight(slot * 0.78);
            mark.setSmooth(true);
            mark.setOpacity(horn ? 1 : 0.55);
            mark.setMouseTransparent(true);
            box.getChildren().add(mark);
        }
        Match.Token token = hornToken(side, row);
        if (token != null) {
            double cardH = slot - 2;
            double cardW = cardH * BoardCard.CROP_WIDTH / BoardCard.CROP_HEIGHT;
            Pane face = BoardCard.create(token.card(), cardW, null);
            face.setMouseTransparent(true);
            box.getChildren().add(face);
        } else if (horn) {
            box.getStyleClass().add("match-horn-on");
        }
        if (hornReady(side, row)) {
            box.getStyleClass().add("match-target");
            box.setOnMouseClicked(event -> {
                event.consume();
                onHorn(side, row);
            });
        }
        hornSlots.get(side).put(row, box);
        return box;
    }

    private Match.Token hornToken(Side side, Row row) {
        for (Match.Token token : match.tokens()) {
            if (token.side() == side && token.row() == row && token.card().kind() == CardKind.HORN) {
                return token;
            }
        }
        return null;
    }

    private StackPane rowTotal(Side side, Row row) {
        double size = rowBadgeSize();
        StackPane badge = new StackPane();
        badge.setMinSize(size, size);
        badge.setMaxSize(size, size);
        String path = side == Side.PLAYER ? "/gwent/ui/board/power_my.png" : "/gwent/ui/board/power_enemy.png";
        Image picture = CardImages.get(path);
        if (picture != null) {
            ImageView view = new ImageView(picture);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setSmooth(true);
            view.setMouseTransparent(true);
            badge.getChildren().add(view);
        }
        Text value = BoardCard.strengthNumber(match.field().rowTotal(side, row), size);
        value.setMouseTransparent(true);
        badge.getChildren().add(value);
        return badge;
    }

    private Node pieceNode(Side side, Row row, RowPiece piece) {
        if (piece.placed() == null) {
            Pane face = BoardCard.create(piece.entry().card(), boardCardWidth, null);
            face.setMouseTransparent(true);
            return face;
        }
        return unitNode(side, row, piece.placed());
    }

    private Node unitNode(Side side, Row row, PlacedCard placed) {
        int actual = match.field().strength(side, row, placed);
        Pane face = BoardCard.create(placed.card(), boardCardWidth, actual, placed.card().strength());
        boolean decoyTarget = decoyReady() && side == Side.PLAYER && !placed.card().hero();
        if (!decoyTarget) {
            face.setMouseTransparent(true);
            unitNodes.put(placed.placementId(), face);
            return face;
        }
        Button button = new Button();
        button.getStyleClass().add("card-tile");
        button.setGraphic(face);
        button.setOnMouseClicked(event -> {
            event.consume();
            onDecoy(placed.placementId());
        });
        unitNodes.put(placed.placementId(), button);
        return button;
    }

    private Node handBar() {
        HBox cards = new HBox(6);
        cards.setAlignment(Pos.CENTER);
        cards.setPadding(new Insets(0, 8, 0, 8));
        cards.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        if (match.phase() == Match.Phase.PLAY) {
            List<CardDef> hand = match.hand(Side.PLAYER);
            boolean canPick = playerCanChoose();
            for (int index : HandOrder.indices(hand)) {
                CardDef card = hand.get(index);
                if (arriving.contains(index)) {
                    cards.getChildren().add(handSpacer(card, index, true));
                    continue;
                }
                if (heldGaps.contains(index)) {
                    cards.getChildren().add(handSpacer(card, index, false));
                    continue;
                }
                if (showcaseSide == Side.PLAYER && showcaseIndex != null && showcaseIndex == index) {
                    continue;
                }
                Button button = new Button();
                button.getStyleClass().addAll("card-tile", "board-hand");
                button.setGraphic(BoardCard.create(card, handCardWidth, shownStrength(card)));
                if (canPick) {
                    int handIndex = index;
                    button.setOnAction(event -> beginShowcase(handIndex, button));
                    // toFront() в HBox переставляет карту в конец ряда, а не рисует её сверху.
                    // Курсор остаётся на месте, под ним оказывается следующая, и все правее неё меняются местами.
                    button.setOnMouseEntered(event -> {
                        button.setViewOrder(-1);
                        lift(button.getGraphic(), true);
                    });
                    button.setOnMouseExited(event -> {
                        button.setViewOrder(0);
                        lift(button.getGraphic(), false);
                    });
                }
                handNodes.put(index, button);
                cards.getChildren().add(button);
            }
        }
        StackPane wrap = new StackPane(cards);
        wrap.setAlignment(Pos.CENTER);
        ScrollPane scroll = new ScrollPane(wrap);
        scroll.setFitToHeight(true);
        scroll.setPannable(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().addAll("deck-scroll", "match-hand-wood");
        scroll.setFitToWidth(true);
        scroll.setMinHeight(0);
        scroll.setPrefHeight(handBand);
        scroll.setMaxHeight(handBand);
        return scroll;
    }

    private void lift(Node node, boolean up) {
        if (node == null) {
            return;
        }
        Object running = node.getProperties().get("lift");
        if (running instanceof TranslateTransition previous) {
            previous.stop();
        }
        TranslateTransition move = new TranslateTransition(Duration.millis(120), node);
        move.setToY(up ? -14 * scale : 0);
        node.getProperties().put("lift", move);
        move.play();
    }

    private VBox rightColumn() {
        VBox box = new VBox(8);
        box.getStyleClass().add("match-rail-right");
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(8, 12, 8, 18));
        box.setMinWidth(rightWidth);
        box.setPrefWidth(rightWidth);
        box.setMaxWidth(rightWidth);
        box.setMinHeight(0);
        box.getChildren().add(piles(Side.OPPONENT));
        showcaseNote = new Label(note);
        showcaseNote.getStyleClass().add("match-note");
        showcaseNote.setWrapText(true);
        showcaseNote.setMaxWidth(rightWidth - 48);
        showcaseNote.setAlignment(Pos.CENTER);
        showcaseNote.setVisible(!note.isBlank());
        showcaseNote.setManaged(!note.isBlank());
        box.getChildren().add(showcaseNote);

        double fullH = CardFace.height(showcaseWidth, CardCrop.FULL);
        StackPane holder = new StackPane();
        holder.setMinSize(0, 0);
        holder.setPrefSize(showcaseWidth, fullH);
        holder.setMaxWidth(showcaseWidth);
        holder.setMaxHeight(Double.MAX_VALUE);
        holder.setAlignment(Pos.CENTER);
        if (showcaseIndex != null && !flying) {
            CardDef card = cardOf(showcaseSide, showcaseIndex);
            if (card != null) {
                Parent full = CardFace.create(card, showcaseWidth, CardCrop.FULL, 0);
                holder.getChildren().add(full);
                if (showcaseSide == Side.PLAYER && showcaseReady) {
                    full.setOnMouseClicked(event -> cancelShowcase());
                }
                Caption caption = Caption.of(card);
                if (caption.present()) {
                    holder.setUserData(caption);
                }
            }
        }
        VBox.setVgrow(holder, Priority.ALWAYS);
        showcaseHolder = holder;
        box.getChildren().add(holder);

        Caption caption = holder.getUserData() instanceof Caption found ? found : null;
        if (caption != null && showcaseIndex != null && !flying) {
            box.getChildren().add(skillBox(caption));
        }
        box.getChildren().add(piles(Side.PLAYER));
        Button action = new Button(match.phase() == Match.Phase.FINISHED ? "К колоде" : "Пас");
        action.getStyleClass().add("deck-action");
        if (match.phase() == Match.Phase.FINISHED) {
            action.setOnAction(event -> back.run());
        } else {
            action.setDisable(locked || !playerCanChoose());
            action.setOnAction(event -> onPass());
        }
        box.getChildren().add(action);
        return box;
    }

    private VBox skillBox(Caption caption) {
        double textWidth = rightWidth - 52;
        Label title = new Label(caption.title());
        title.getStyleClass().add("match-skill-title");
        title.setWrapText(true);
        title.setPrefWidth(textWidth);
        title.setMaxWidth(textWidth);
        title.setAlignment(Pos.CENTER);
        Label body = new Label(caption.body());
        body.getStyleClass().add("match-skill-body");
        body.setWrapText(true);
        body.setPrefWidth(textWidth);
        body.setMaxWidth(textWidth);
        body.setAlignment(Pos.CENTER);
        VBox box = new VBox(4, title, body);
        box.getStyleClass().add("match-skill");
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(rightWidth - 44);
        return box;
    }

    private HBox piles(Side side) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER);
        box.getChildren().add(grave(side));
        box.getChildren().add(skull());
        box.getChildren().add(deckPile(side));
        return box;
    }

    private double[] pileBox() {
        double graveH = Math.max(70, deckWidth * 1.02);
        double graveW = graveH * 114.0 / 149.0;
        return new double[] {graveW, graveH, graveW * 1.1, graveH * 1.1};
    }

    private Node skull() {
        double size = pileBox()[1] * 0.42;
        Image picture = CardImages.get("/gwent/ui/board/retreat-skull.png");
        if (picture == null) {
            return new Region();
        }
        ImageView view = new ImageView(picture);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    private StackPane grave(Side side) {
        double[] box = pileBox();
        double width = box[0];
        double height = box[1];
        StackPane slot = new StackPane();
        slot.getStyleClass().add("match-grave");
        slot.setMinSize(0, 0);
        slot.setPrefSize(width, height);
        slot.setMaxSize(width, height);
        List<CardDef> discard = match.discard(side);
        if (!discard.isEmpty()) {
            CardDef top = discard.getLast();
            double cardH = height - 8;
            double cardW = cardH * BoardCard.CROP_WIDTH / BoardCard.CROP_HEIGHT;
            Pane face = BoardCard.create(top, cardW, shownStrength(top));
            face.setMouseTransparent(true);
            slot.getChildren().add(face);
            Label count = new Label(Integer.toString(discard.size()));
            count.getStyleClass().add("match-pile-count");
            StackPane.setAlignment(count, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(count, new Insets(0, 4, 4, 0));
            slot.getChildren().add(count);
        }
        return slot;
    }

    private StackPane deckPile(Side side) {
        Image picture = CardImages.get(coverPath(match.faction(side)));
        double[] box = pileBox();
        double width = box[2];
        double height = box[3];
        StackPane pile = new StackPane();
        pile.setMinSize(0, 0);
        pile.setPrefSize(width, height);
        pile.setMaxSize(width, height);
        if (picture != null) {
            ImageView view = new ImageView(picture);
            view.setFitWidth(width);
            view.setFitHeight(height);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            pile.getChildren().add(view);
        } else {
            pile.getStyleClass().add("match-grave");
        }
        Label count = new Label(Integer.toString(match.deckSize(side)));
        count.getStyleClass().add("match-deck-count");
        StackPane.setAlignment(count, Pos.BOTTOM_CENTER);
        StackPane.setMargin(count, new Insets(0, 0, 6, 0));
        pile.getChildren().add(count);
        if (side == Side.OPPONENT) {
            opponentDeckPile = pile;
        } else {
            playerDeckPile = pile;
        }
        return pile;
    }

    /** Невидимая карта того же размера. Пустота в руке или место, куда летит взятая карта. */
    private Node handSpacer(CardDef card, int index, boolean arrival) {
        Button gap = new Button();
        gap.getStyleClass().addAll("card-tile", "board-hand");
        gap.setGraphic(BoardCard.create(card, handCardWidth, shownStrength(card)));
        gap.setOpacity(0);
        gap.setMouseTransparent(true);
        gap.setFocusTraversable(false);
        if (arrival) {
            arrivalSlots.put(index, gap);
        } else {
            gapNodes.put(index, gap);
        }
        return gap;
    }

    private void beginShowcase(int index, Node source) {
        if (!playerCanChoose() || (showcaseIndex != null && showcaseIndex == index)) {
            return;
        }
        if (showcaseIndex != null && showcaseSide == Side.PLAYER) {
            swapShowcase(index, source);
            return;
        }
        locked = true;
        showcaseReady = false;
        medicRow = null;
        reviveIndex = null;
        waitingReviveRow = false;
        Bounds from = sceneBounds(source);
        heldGaps.add(index);
        showcaseIndex = index;
        showcaseSide = Side.PLAYER;
        flying = true;
        redraw();
        settle();
        CardDef card = match.hand(Side.PLAYER).get(index);
        fly(from, sceneBounds(showcaseHolder), card, shownStrength(card), true, handCardWidth, () -> {
            flying = false;
            showcaseReady = true;
            locked = false;
            note = "";
            redraw();
        });
    }

    /** Карта с витрины возвращается в свою пустоту, а новая одновременно выезжает на витрину. */
    private void swapShowcase(int index, Node source) {
        locked = true;
        showcaseReady = false;
        medicRow = null;
        reviveIndex = null;
        waitingReviveRow = false;
        int previous = showcaseIndex;
        CardDef returning = cardOf(Side.PLAYER, previous);
        Bounds backFrom = sceneBounds(showcaseHolder);
        Bounds backTo = sceneBounds(gapNodes.get(previous));
        Bounds forwardFrom = sceneBounds(source);
        heldGaps.add(index);
        showcaseIndex = index;
        showcaseSide = Side.PLAYER;
        flying = true;
        note = "";
        redraw();
        settle();
        int[] pending = {2};
        Runnable step = () -> {
            pending[0]--;
            if (pending[0] > 0) {
                return;
            }
            flying = false;
            showcaseReady = true;
            locked = false;
            redraw();
        };
        fly(backFrom, backTo, returning, shownStrength(returning), false, handCardWidth, () -> {
            heldGaps.remove(previous);
            redraw();
            step.run();
        });
        CardDef incoming = match.hand(Side.PLAYER).get(index);
        fly(forwardFrom, sceneBounds(showcaseHolder), incoming, shownStrength(incoming), true, handCardWidth, step);
    }

    private void cancelShowcase() {
        if (locked) {
            return;
        }
        if (match.phase() == Match.Phase.MULLIGAN) {
            return;
        }
        if (overlay.isVisible()) {
            cancelRevive();
            return;
        }
        if (showcaseIndex == null || showcaseSide != Side.PLAYER) {
            return;
        }
        locked = true;
        showcaseReady = false;
        flying = true;
        note = "";
        int returning = showcaseIndex;
        CardDef card = cardOf(Side.PLAYER, returning);
        redraw();
        settle();
        fly(sceneBounds(showcaseHolder), sceneBounds(gapNodes.get(returning)), card, shownStrength(card), false, handCardWidth, () -> {
            flying = false;
            heldGaps.remove(returning);
            showcaseIndex = null;
            showcaseSide = null;
            medicRow = null;
            reviveIndex = null;
            waitingReviveRow = false;
            locked = false;
            redraw();
        });
    }

    private void onPass() {
        if (locked || !playerCanChoose()) {
            return;
        }
        heldGaps.clear();
        showcaseIndex = null;
        showcaseSide = null;
        showcaseReady = false;
        medicRow = null;
        waitingReviveRow = false;
        locked = true;
        note = "Вы пасуете.";
        redraw();
        hold(PASS_MS, () -> commit(Side.PLAYER, new Move.Pass()));
    }

    private boolean playerCanChoose() {
        return !locked && match.phase() == Match.Phase.PLAY && match.active() == Side.PLAYER && !match.passed(Side.PLAYER)
                && !overlay.isVisible();
    }

    private boolean rowReady(Side side, Row row) {
        if (!showcaseReady || locked || showcaseSide != Side.PLAYER || showcaseIndex == null) {
            return false;
        }
        if (waitingReviveRow) {
            CardDef revived = match.discard(Side.PLAYER).get(reviveIndex);
            return side == boardSide(Side.PLAYER, revived) && revived.rows().contains(row);
        }
        CardDef card = match.hand(Side.PLAYER).get(showcaseIndex);
        if (card.ability() == Ability.MEDIC && !revivable().isEmpty()) {
            return side == Side.PLAYER && card.rows().contains(row);
        }
        if (card.kind() == CardKind.WEATHER) {
            return card.affects() == row;
        }
        return !movesForRow(side, row).isEmpty();
    }

    private boolean hornReady(Side side, Row row) {
        if (!showcaseReady || locked || showcaseSide != Side.PLAYER || showcaseIndex == null || waitingReviveRow) {
            return false;
        }
        CardDef card = match.hand(Side.PLAYER).get(showcaseIndex);
        return card.kind() == CardKind.HORN && side == Side.PLAYER && !match.field().horn(side, row);
    }

    private boolean weatherReady() {
        if (!showcaseReady || locked || showcaseSide != Side.PLAYER || showcaseIndex == null || waitingReviveRow) {
            return false;
        }
        CardKind kind = match.hand(Side.PLAYER).get(showcaseIndex).kind();
        return kind == CardKind.WEATHER || kind == CardKind.CLEAR_WEATHER;
    }

    private boolean decoyReady() {
        if (!showcaseReady || locked || showcaseSide != Side.PLAYER || showcaseIndex == null || waitingReviveRow) {
            return false;
        }
        return match.hand(Side.PLAYER).get(showcaseIndex).kind() == CardKind.DECOY;
    }

    private void onRow(Side side, Row row) {
        if (!rowReady(side, row)) {
            return;
        }
        if (waitingReviveRow) {
            launch(new Move.Medic(showcaseIndex, medicRow, reviveIndex, row), rowPlates.get(Side.PLAYER).get(medicRow));
            return;
        }
        CardDef card = match.hand(Side.PLAYER).get(showcaseIndex);
        if (card.ability() == Ability.MEDIC && !revivable().isEmpty()) {
            medicRow = row;
            showRevive();
            return;
        }
        if (card.kind() == CardKind.HORN) {
            onHorn(side, row);
            return;
        }
        List<Move> found = movesForRow(side, row);
        if (found.size() == 1) {
            launch(found.getFirst(), landing(Side.PLAYER, found.getFirst()));
        }
    }

    private void onHorn(Side side, Row row) {
        if (!hornReady(side, row) && !(rowReady(side, row) && match.hand(Side.PLAYER).get(showcaseIndex).kind() == CardKind.HORN)) {
            return;
        }
        for (Move move : match.legal(Side.PLAYER)) {
            if (move instanceof Move.Horn horn && horn.handIndex() == showcaseIndex && horn.row() == row) {
                launch(horn, hornSlots.get(side).get(row));
                return;
            }
        }
    }

    private void onWeatherSlot() {
        if (!weatherReady()) {
            return;
        }
        for (Move move : match.legal(Side.PLAYER)) {
            if (handIndex(move) != showcaseIndex) {
                continue;
            }
            if (move instanceof Move.Weather || move instanceof Move.ClearWeather) {
                launch(move, weatherSlot);
                return;
            }
        }
    }

    private void onDecoy(int placementId) {
        if (!decoyReady()) {
            return;
        }
        launch(new Move.Decoy(showcaseIndex, placementId), unitNodes.get(placementId));
    }

    private List<Move> movesForRow(Side side, Row row) {
        List<Move> found = new ArrayList<>();
        if (showcaseIndex == null) {
            return found;
        }
        CardDef card = match.hand(Side.PLAYER).get(showcaseIndex);
        for (Move move : match.legal(Side.PLAYER)) {
            if (handIndex(move) != showcaseIndex) {
                continue;
            }
            switch (move) {
                case Move.Unit unit -> {
                    if (unit.row() == row && side == boardSide(Side.PLAYER, card)) {
                        found.add(unit);
                    }
                }
                case Move.Horn horn -> {
                    if (horn.row() == row && side == Side.PLAYER) {
                        found.add(horn);
                    }
                }
                case Move.Weather weather -> {
                    if (card.affects() == row) {
                        found.add(weather);
                    }
                }
                case Move.Medic medic -> {
                    if (medic.row() == row && side == Side.PLAYER && medic.discardIndex() < 0) {
                        found.add(medic);
                    }
                }
                case Move.ClearWeather clear -> found.add(clear);
                case Move.Scorch scorch -> found.add(scorch);
                default -> {
                }
            }
        }
        return found;
    }

    private void launch(Move move, Node target) {
        if (locked || showcaseIndex == null) {
            return;
        }
        locked = true;
        showcaseReady = false;
        settle();
        Bounds from = sceneBounds(showcaseHolder);
        Bounds to = sceneBounds(target);
        CardDef card = cardOf(Side.PLAYER, move);
        heldGaps.clear();
        flying = true;
        redraw();
        fly(from, to, card, shownStrength(card), false, boardCardWidth, () -> {
            flying = false;
            showcaseIndex = null;
            showcaseSide = null;
            commit(Side.PLAYER, move);
        });
    }

    private void commit(Side side, Move move) {
        int roundBefore = match.round();
        int scorePlayer = match.field().total(Side.PLAYER);
        int scoreOpponent = match.field().total(Side.OPPONENT);
        int gemsPlayer = match.gems(Side.PLAYER);
        int gemsOpponent = match.gems(Side.OPPONENT);
        int handBefore = match.hand(side).size();
        CardDef played = cardOf(side, move);
        try {
            match.play(side, move);
        } catch (IllegalMove ex) {
            locked = false;
            heldGaps.clear();
            showcaseIndex = null;
            showcaseSide = null;
            showcaseReady = false;
            note = ex.getMessage();
            redraw();
            return;
        }
        if (side == Side.PLAYER) {
            heldGaps.clear();
            showcaseIndex = null;
            showcaseSide = null;
            showcaseReady = false;
            medicRow = null;
            reviveIndex = null;
            waitingReviveRow = false;
        }
        int drawn = 0;
        if (side == Side.PLAYER && played != null && played.spy() && handIndex(move) >= 0) {
            drawn = Math.max(0, match.hand(side).size() - (handBefore - 1));
        }
        boolean ended = match.round() != roundBefore || match.phase() == Match.Phase.FINISHED;
        if (ended) {
            note = roundLine(scorePlayer, scoreOpponent, gemsPlayer, gemsOpponent);
            if (showcaseNote != null) {
                showcaseNote.setText(note);
                showcaseNote.setVisible(true);
                showcaseNote.setManaged(true);
            }
            hold(ROUND_MS, () -> {
                redraw();
                opponentSequence();
            });
            return;
        }
        Runnable next = () -> {
            redraw();
            if (side == Side.OPPONENT) {
                hold(280, this::opponentSequence);
            } else {
                opponentSequence();
            }
        };
        if (drawn > 0) {
            arriveFromDeck(drawn, next);
            return;
        }
        next.run();
    }

    private void opponentSequence() {
        if (match.phase() != Match.Phase.PLAY || match.active() != Side.OPPONENT) {
            locked = false;
            redraw();
            return;
        }
        locked = true;
        int cards = match.hand(Side.OPPONENT).size();
        Move move = MatchAi.nextMove(match, Side.OPPONENT, opponentStyle);
        if (move instanceof Move.Pass) {
            note = cards == 0 ? "У противника не осталось карт. Он пасует." : "Противник пасует.";
            redraw();
            hold(PASS_MS, () -> commit(Side.OPPONENT, move));
            return;
        }
        settle();
        CardDef card = cardOf(Side.OPPONENT, move);
        Bounds from = sceneBounds(opponentDeckPile);
        Bounds to = sceneBounds(showcaseHolder);
        fly(from, to, card, shownStrength(card), true, boardCardWidth, () -> {
            showcaseIndex = handIndex(move);
            showcaseSide = Side.OPPONENT;
            showcaseReady = true;
            flying = false;
            note = "";
            redraw();
            hold(SHOWCASE_HOLD_MS, () -> playOpponent(move));
        });
    }

    private void playOpponent(Move move) {
        settle();
        Bounds from = sceneBounds(showcaseHolder);
        Bounds to = sceneBounds(landing(Side.OPPONENT, move));
        CardDef card = cardOf(Side.OPPONENT, move);
        flying = true;
        redraw();
        fly(from, to, card, shownStrength(card), false, boardCardWidth, () -> {
            flying = false;
            showcaseIndex = null;
            showcaseSide = null;
            showcaseReady = false;
            commit(Side.OPPONENT, move);
        });
    }

    private void arriveFromDeck(int drawn, Runnable then) {
        int size = match.hand(Side.PLAYER).size();
        arriving.clear();
        for (int index = size - drawn; index < size; index++) {
            arriving.add(index);
        }
        locked = true;
        redraw();
        settle();
        List<Integer> indexes = List.copyOf(arriving);
        if (indexes.isEmpty()) {
            arriving.clear();
            then.run();
            return;
        }
        int[] left = {indexes.size()};
        for (Integer index : indexes) {
            CardDef card = match.hand(Side.PLAYER).get(index);
            flyCrop(sceneBounds(playerDeckPile), sceneBounds(arrivalSlots.get(index)), card, shownStrength(card), () -> {
                left[0]--;
                if (left[0] == 0) {
                    arriving.clear();
                    then.run();
                }
            });
        }
    }

    private void flyCrop(Bounds fromScene, Bounds toScene, CardDef card, Integer strength, Runnable then) {
        if (card == null) {
            then.run();
            return;
        }
        double width = Math.max(36, handCardWidth);
        double height = width * BoardCard.CROP_HEIGHT / BoardCard.CROP_WIDTH;
        Pane crop = BoardCard.create(card, width, strength);
        crop.setManaged(false);
        crop.setMouseTransparent(true);
        crop.resize(width, height);
        Bounds from = flight.sceneToLocal(fromScene);
        Bounds to = flight.sceneToLocal(toScene);
        double startScale = Math.max(0.35, from.getWidth() / width);
        crop.setScaleX(startScale);
        crop.setScaleY(startScale);
        double startX = from.getMinX() + from.getWidth() / 2.0;
        double startY = from.getMinY() + from.getHeight() / 2.0;
        double endX = to.getMinX() + to.getWidth() / 2.0;
        double endY = to.getMinY() + to.getHeight() / 2.0;
        crop.setLayoutX(startX - width / 2.0);
        crop.setLayoutY(startY - height / 2.0);
        flight.getChildren().add(crop);
        TranslateTransition move = new TranslateTransition(Duration.millis(FLY_MS), crop);
        move.setToX(endX - startX);
        move.setToY(endY - startY);
        move.setInterpolator(Interpolator.EASE_BOTH);
        ScaleTransition scale = new ScaleTransition(Duration.millis(FLY_MS), crop);
        scale.setFromX(startScale);
        scale.setFromY(startScale);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_BOTH);
        ParallelTransition all = new ParallelTransition(move, scale);
        all.setOnFinished(event -> {
            flight.getChildren().remove(crop);
            then.run();
        });
        all.play();
    }

    private void fly(Bounds fromScene, Bounds toScene, CardDef card, Integer strength, boolean becomeFull, double landWidth, Runnable then) {
        if (card == null || showcaseWidth < 20) {
            then.run();
            return;
        }
        Parent full = CardFace.create(card, showcaseWidth, CardCrop.FULL, 0);
        Pane crop = BoardCard.create(card, showcaseWidth, strength);
        full.setMouseTransparent(true);
        crop.setMouseTransparent(true);
        full.setOpacity(becomeFull ? 0 : 1);
        crop.setOpacity(becomeFull ? 1 : 0);
        StackPane flyer = new StackPane(crop, full);
        flyer.setManaged(false);
        flyer.setMouseTransparent(true);
        double stackW = showcaseWidth;
        double stackH = CardFace.height(showcaseWidth, CardCrop.FULL);
        Bounds from = flight.sceneToLocal(fromScene);
        Bounds to = flight.sceneToLocal(toScene);
        double startScale = becomeFull ? Math.max(0.18, from.getWidth() / stackW) : 1;
        double endScale = becomeFull ? 1 : Math.max(0.18, landWidth / stackW);
        flyer.setScaleX(startScale);
        flyer.setScaleY(startScale);
        double startX = from.getMinX() + from.getWidth() / 2.0;
        double startY = from.getMinY() + from.getHeight() / 2.0;
        double endX = to.getMinX() + to.getWidth() / 2.0;
        double endY = to.getMinY() + to.getHeight() / 2.0;
        flyer.setLayoutX(startX - stackW / 2.0);
        flyer.setLayoutY(startY - stackH / 2.0);
        flyer.resize(stackW, stackH);
        flight.getChildren().add(flyer);
        flyer.layout();
        TranslateTransition move = new TranslateTransition(Duration.millis(FLY_MS), flyer);
        move.setToX(endX - startX);
        move.setToY(endY - startY);
        move.setInterpolator(Interpolator.EASE_BOTH);
        ScaleTransition scale = new ScaleTransition(Duration.millis(FLY_MS), flyer);
        scale.setFromX(startScale);
        scale.setFromY(startScale);
        scale.setToX(endScale);
        scale.setToY(endScale);
        scale.setInterpolator(Interpolator.EASE_BOTH);
        FadeTransition fadeFull = new FadeTransition(Duration.millis(FLY_MS), full);
        fadeFull.setToValue(becomeFull ? 1 : 0);
        FadeTransition fadeCrop = new FadeTransition(Duration.millis(FLY_MS), crop);
        fadeCrop.setToValue(becomeFull ? 0 : 1);
        ParallelTransition all = new ParallelTransition(move, scale, fadeFull, fadeCrop);
        all.setOnFinished(event -> {
            flight.getChildren().remove(flyer);
            then.run();
        });
        all.play();
    }

    private void hold(double millis, Runnable then) {
        PauseTransition pause = new PauseTransition(Duration.millis(millis));
        pause.setOnFinished(event -> then.run());
        pause.play();
    }

    private void settle() {
        if (root.getScene() != null) {
            root.applyCss();
            root.layout();
        }
    }

    private Bounds sceneBounds(Node node) {
        if (node != null && node.getScene() != null) {
            Bounds bounds = node.localToScene(node.getBoundsInLocal());
            if (bounds.getWidth() > 2 && bounds.getHeight() > 2) {
                return bounds;
            }
        }
        double x = root.getWidth() > 0 ? root.getWidth() * 0.78 : 700;
        double y = root.getHeight() > 0 ? root.getHeight() * 0.35 : 200;
        return new BoundingBox(x, y, showcaseWidth, CardFace.height(showcaseWidth, CardCrop.FULL));
    }

    private Node landing(Side actor, Move move) {
        CardDef card = cardOf(actor, move);
        if (card == null) {
            return rowsBox;
        }
        return switch (move) {
            case Move.Unit unit -> plate(boardSide(actor, card), unit.row());
            case Move.Horn horn -> hornSlots.get(actor).get(horn.row());
            case Move.Medic medic -> plate(actor, medic.row());
            case Move.Weather ignored -> weatherSlot;
            case Move.ClearWeather ignored -> weatherSlot;
            case Move.Decoy decoy -> {
                Node unit = unitNodes.get(decoy.placementId());
                yield unit != null ? unit : plate(actor, Row.MELEE);
            }
            case Move.Scorch ignored -> rowsBox;
            case Move.Pass ignored -> rowsBox;
        };
    }

    private Node plate(Side side, Row row) {
        EnumMap<Row, Node> plates = rowPlates.get(side);
        if (plates == null || plates.get(row) == null) {
            return rowsBox;
        }
        return plates.get(row);
    }

    private String roundLine(int scorePlayer, int scoreOpponent, int gemsPlayer, int gemsOpponent) {
        String score = scorePlayer + " : " + scoreOpponent;
        String head;
        if (scorePlayer > scoreOpponent || (match.gems(Side.PLAYER) > gemsPlayer && scorePlayer == scoreOpponent)) {
            head = "Раунд ваш, " + score + ".";
        } else if (scoreOpponent > scorePlayer
                || (match.gems(Side.OPPONENT) > gemsOpponent && scorePlayer == scoreOpponent)) {
            head = "Раунд за противником, " + score + ".";
        } else {
            head = "Ничья в раунде, " + score + ".";
        }
        if (match.phase() == Match.Phase.FINISHED) {
            return head + " " + result();
        }
        if (match.active() == Side.PLAYER) {
            return head + " Дальше ваш ход.";
        }
        return head + " Дальше ходит противник.";
    }

    private String result() {
        String score = match.gems(Side.PLAYER) + ":" + match.gems(Side.OPPONENT);
        if (match.winner() == Side.PLAYER) {
            return "Победа. Счёт " + score + ".";
        }
        if (match.winner() == Side.OPPONENT) {
            return "Поражение. Счёт " + score + ".";
        }
        return "Ничья. Счёт " + score + ".";
    }

    private void buildOverlay() {
        overlay.getStyleClass().add("leader-picker");
        overlay.setVisible(false);
        overlay.setMouseTransparent(true);
        pickerTitle.getStyleClass().add("leader-picker-title");
        strip.setMaxWidth(Double.MAX_VALUE);
        pickerAct.getStyleClass().add("deck-action");
        pickerDone.getStyleClass().add("deck-action");
        HBox actions = new HBox(12, pickerAct, pickerDone);
        actions.setAlignment(Pos.CENTER);
        VBox panel = new VBox(18, pickerTitle, strip, actions);
        panel.setAlignment(Pos.CENTER);
        panel.setFillWidth(true);
        panel.setPickOnBounds(false);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        panel.setMaxWidth(Double.MAX_VALUE);
        overlay.getChildren().add(panel);
        StackPane.setAlignment(panel, Pos.CENTER);
    }

    private void showMulligan() {
        List<CardDef> hand = match.hand(Side.PLAYER);
        pickerOrder = HandOrder.indices(hand);
        pickerTitle.setText("Замена карт");
        strip.setHeading("Какую карту вернуть в колоду");
        strip.setOnSelect(index -> {
        });
        strip.setScale(scale);
        strip.setCards(ordered(hand, pickerOrder), 0);
        pickerAct.setText("Заменить");
        pickerAct.setDisable(match.mulligansLeft(Side.PLAYER) == 0);
        pickerAct.setOnAction(event -> replaceSelected());
        pickerDone.setText("Дальше");
        pickerDone.setVisible(true);
        pickerDone.setManaged(true);
        pickerDone.setOnAction(event -> finishMulligan());
        note = "Можно сменить до двух карт, каждую отдельно.";
        overlay.setVisible(true);
        overlay.setMouseTransparent(false);
    }

    private void replaceSelected() {
        if (match.mulligansLeft(Side.PLAYER) == 0 || pickerOrder.isEmpty()) {
            return;
        }
        int handIndex = pickerOrder.get(strip.selected());
        match.replaceOne(Side.PLAYER, handIndex);
        List<CardDef> hand = match.hand(Side.PLAYER);
        pickerOrder = HandOrder.indices(hand);
        int selected = Math.max(0, pickerOrder.indexOf(handIndex));
        strip.setCards(ordered(hand, pickerOrder), selected);
        pickerAct.setDisable(match.mulligansLeft(Side.PLAYER) == 0);
        pickerTitle.setText("Замена карт. Осталось " + match.mulligansLeft(Side.PLAYER));
    }

    private void finishMulligan() {
        overlay.setVisible(false);
        overlay.setMouseTransparent(true);
        match.endMulligan(Side.PLAYER);
        if (match.phase() == Match.Phase.MULLIGAN) {
            MatchAi.act(match, Side.OPPONENT, opponentStyle);
        }
        note = "";
        redraw();
        hold(80, this::opponentSequence);
    }

    private void showRevive() {
        List<Integer> discardIndexes = revivable();
        List<CardDef> cards = new ArrayList<>();
        for (int index : discardIndexes) {
            cards.add(match.discard(Side.PLAYER).get(index));
        }
        pickerOrder = new ArrayList<>();
        List<CardDef> view = new ArrayList<>();
        for (int index : HandOrder.indices(cards)) {
            pickerOrder.add(discardIndexes.get(index));
            view.add(cards.get(index));
        }
        pickerTitle.setText("Кликните карту в отбое");
        strip.setHeading("Кого поднять");
        strip.setScale(scale);
        strip.setOnSelect(index -> {
            pickerAct.setDisable(false);
            pickerTitle.setText("Поднять «" + view.get(index).name() + "»");
        });
        strip.setCards(view, 0);
        pickerAct.setText("Поднять");
        pickerAct.setDisable(true);
        pickerAct.setOnAction(event -> confirmRevive(pickerOrder.get(strip.selected())));
        pickerDone.setText("Назад");
        pickerDone.setVisible(true);
        pickerDone.setManaged(true);
        pickerDone.setOnAction(event -> cancelRevive());
        overlay.setVisible(true);
        overlay.setMouseTransparent(false);
    }

    private void confirmRevive(int discardIndex) {
        reviveIndex = discardIndex;
        overlay.setVisible(false);
        overlay.setMouseTransparent(true);
        waitingReviveRow = true;
        note = "Куда положить «" + match.discard(Side.PLAYER).get(discardIndex).name() + "».";
        redraw();
    }

    private void cancelRevive() {
        medicRow = null;
        overlay.setVisible(false);
        overlay.setMouseTransparent(true);
        note = "";
        redraw();
    }

    private List<Integer> revivable() {
        List<Integer> indexes = new ArrayList<>();
        List<CardDef> discard = match.discard(Side.PLAYER);
        for (int i = 0; i < discard.size(); i++) {
            CardDef card = discard.get(i);
            if (card.unit() && !card.hero()) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    private static List<CardDef> ordered(List<CardDef> cards, List<Integer> order) {
        List<CardDef> view = new ArrayList<>();
        for (int index : order) {
            view.add(cards.get(index));
        }
        return view;
    }

    private CardDef cardOf(Side side, Move move) {
        return cardOf(side, handIndex(move));
    }

    private CardDef cardOf(Side side, Integer index) {
        if (side == null || index == null) {
            return null;
        }
        List<CardDef> hand = match.hand(side);
        if (index < 0 || index >= hand.size()) {
            return null;
        }
        return hand.get(index);
    }

    private static Integer shownStrength(CardDef card) {
        if (card != null && card.unit()) {
            return card.strength();
        }
        return null;
    }

    private double spacing(int count) {
        if (count >= 8) {
            return -boardCardWidth * 0.42;
        }
        if (count >= 5) {
            return -boardCardWidth * 0.16;
        }
        return 4;
    }

    private static String weatherWash(Row row) {
        return switch (row) {
            case MELEE -> "rgba(150, 190, 220, 0.34)";
            case RANGED -> "rgba(186, 186, 180, 0.36)";
            case SIEGE -> "rgba(60, 100, 150, 0.38)";
        };
    }

    private static String rowIcon(Row row) {
        return switch (row) {
            case MELEE -> "/gwent/ui/board/field_melee.png";
            case RANGED -> "/gwent/ui/board/field_range.png";
            case SIEGE -> "/gwent/ui/board/field_art.png";
        };
    }

    private static CardDef weatherArt(Row row) {
        int id = switch (row) {
            case MELEE -> 169;
            case RANGED -> 170;
            case SIEGE -> 171;
        };
        return CardCatalog.get(id);
    }

    private static String coverPath(Faction faction) {
        return switch (faction) {
            case NORTHERN_REALMS -> "/gwent/ui/board/cover-north.png";
            case NILFGAARD -> "/gwent/ui/board/cover-nilf.png";
            case SCOIATAEL -> "/gwent/ui/board/cover-elf.png";
            case MONSTERS -> "/gwent/ui/board/cover-monster.png";
            case NEUTRAL -> "/gwent/ui/board/cover-north.png";
        };
    }

    private static Side boardSide(Side owner, CardDef card) {
        return card.playsOn() == PlaysOn.OPPONENT ? owner.other() : owner;
    }

    private static int handIndex(Move move) {
        return switch (move) {
            case Move.Pass ignored -> -1;
            case Move.Unit unit -> unit.handIndex();
            case Move.Horn horn -> horn.handIndex();
            case Move.Weather weather -> weather.handIndex();
            case Move.ClearWeather clear -> clear.handIndex();
            case Move.Scorch scorch -> scorch.handIndex();
            case Move.Decoy decoy -> decoy.handIndex();
            case Move.Medic medic -> medic.handIndex();
        };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean near(double left, double right) {
        return Math.abs(left - right) < 1.5;
    }

    private record RowPiece(RowOrder.Entry entry, PlacedCard placed) {
    }

    private record Caption(String title, String body) {
        static Caption of(CardDef card) {
            List<SkillTexts> parts = new ArrayList<>();
            add(parts, card.hero() ? "HERO" : null);
            add(parts, card.agile() ? "AGILE" : null);
            if (card.ability() != null && card.ability() != Ability.NONE) {
                add(parts, card.ability().name());
            } else if (card.special()) {
                add(parts, card.kind().name());
            }
            String title = "";
            String body = "";
            for (SkillTexts part : parts) {
                if (!title.isEmpty()) {
                    title += ". ";
                }
                title += part.name();
                if (!body.isEmpty()) {
                    body += " ";
                }
                body += part.text();
            }
            return new Caption(title, body);
        }

        boolean present() {
            return !title.isBlank();
        }

        private static void add(List<SkillTexts> parts, String id) {
            if (id == null) {
                return;
            }
            SkillTexts texts = SkillTexts.get(id);
            if (texts != null) {
                parts.add(texts);
            }
        }
    }
}
