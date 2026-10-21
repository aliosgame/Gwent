package gwent.deck;

import gwent.catalog.Faction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

/** По одной колоде на фракцию, в домашней папке рядом с настройками. */
public final class DeckStore {
    private static final Logger LOG = Logger.getLogger(DeckStore.class.getName());
    private static final Faction[] PLAYABLE = {
            Faction.NORTHERN_REALMS, Faction.NILFGAARD, Faction.SCOIATAEL, Faction.MONSTERS
    };

    private final Path file;
    private final Map<Faction, Deck> decks = new EnumMap<>(Faction.class);

    public DeckStore(Path file) {
        this.file = file;
    }

    public static DeckStore loadOrCreate(Path file) {
        DeckStore store = new DeckStore(file);
        if (Files.exists(file)) {
            try {
                store.decks.putAll(DeckJson.parse(Files.readString(file)));
            } catch (RuntimeException | IOException ex) {
                LOG.warning("Колоды не прочитались, собираю заново: " + ex.getMessage());
            }
        }
        for (Faction faction : PLAYABLE) {
            store.decks.putIfAbsent(faction, StarterDecks.create(faction));
        }
        store.saveAll();
        return store;
    }

    public Deck get(Faction faction) {
        return decks.get(faction);
    }

    public void put(Deck deck) {
        decks.put(deck.faction(), deck);
        saveAll();
    }

    private void saveAll() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, DeckJson.format(decks));
        } catch (IOException ex) {
            LOG.warning("Не удалось записать колоды: " + ex.getMessage());
        }
    }
}
