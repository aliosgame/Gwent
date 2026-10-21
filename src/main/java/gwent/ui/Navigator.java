package gwent.ui;

import gwent.catalog.CardCatalog;
import gwent.catalog.CardDef;
import gwent.deck.Deck;
import gwent.deck.DeckCheck;
import gwent.deck.DeckStore;
import gwent.ui.deck.DeckScreen;
import gwent.ui.menu.InfoView;
import gwent.ui.menu.MainMenuView;
import gwent.ui.menu.SettingsView;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public final class Navigator {
    private final StackPane host;
    private final AppContext context;
    private final DeckStore decks;

    public Navigator(StackPane host, AppContext context, DeckStore decks) {
        this.host = host;
        this.context = context;
        this.decks = decks;
        host.setFocusTraversable(true);
    }

    public void showMainMenu() {
        show(new MainMenuView(this).create());
    }

    public void showSinglePlayer() {
        show(new DeckScreen(this, decks).create());
    }

    public void showMatchReady(Deck deck) {
        CardDef leader = CardCatalog.get(deck.leaderId());
        show(new InfoView(
                "Колода готова",
                "Сесть за стол можно будет следующим шагом. Сейчас выбраны "
                        + CardCatalog.factionName(deck.faction())
                        + ", лидер «" + leader.name() + "», отрядов "
                        + DeckCheck.units(deck) + ", особых "
                        + DeckCheck.specials(deck) + ".",
                "К колоде",
                this::showSinglePlayer).create());
    }

    public void showLan() {
        show(new InfoView(
                "Локальная сеть",
                "Здесь появится список комнат. Комнату будет видно тем, кто сидит в той же сети — за одним роутером или в Hamachi — и за стол можно будет сесть вдвоём.",
                this).create());
    }

    public void showSettings() {
        show(new SettingsView(this, context).create());
    }

    public void exit() {
        Platform.exit();
    }

    private void show(Parent view) {
        host.getChildren().setAll(view);
        // Иначе сцена оставляет фокус на первой кнопке, и она выглядит выбранной.
        Platform.runLater(host::requestFocus);
    }
}
