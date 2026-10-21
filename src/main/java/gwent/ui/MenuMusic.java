package gwent.ui;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/** Случайный трек из audio/music. По окончании выбирается следующий, не тот же самый. */
public final class MenuMusic {
    private static final Logger LOG = Logger.getLogger(MenuMusic.class.getName());
    private static final String MUSIC = "/gwent/audio/music/";

    private final Random random = new Random();
    private List<URL> tracks = List.of();
    private URL current;
    private double volume;
    private MediaPlayer player;

    public void play(double volume) {
        this.volume = clamp(volume);
        tracks = tracks();
        if (tracks.isEmpty()) {
            LOG.warning("В /gwent/audio/music нет треков, меню без музыки.");
            return;
        }
        start(pick(null));
    }

    public void setVolume(double volume) {
        this.volume = clamp(volume);
        if (player != null) {
            player.setVolume(this.volume);
        }
    }

    public void stop() {
        dispose();
    }

    private void start(URL track) {
        dispose();
        current = track;
        try {
            MediaPlayer next = new MediaPlayer(new Media(track.toExternalForm()));
            next.setCycleCount(1);
            next.setVolume(volume);
            next.setOnEndOfMedia(() -> Platform.runLater(this::onTrackEnded));
            next.setOnError(() -> LOG.warning("Трек не запустился: " + name(track) + ", " + next.getError()));
            player = next;
            next.play();
        } catch (RuntimeException ex) {
            LOG.warning("Трек не запустился: " + name(track) + ", " + ex.getMessage());
            player = null;
        }
    }

    private void onTrackEnded() {
        if (tracks.isEmpty()) {
            return;
        }
        start(pick(current));
    }

    /** При нескольких треках тот же самый сразу не повторяется. */
    private URL pick(URL previous) {
        if (tracks.size() == 1) {
            return tracks.getFirst();
        }
        URL next;
        do {
            next = tracks.get(random.nextInt(tracks.size()));
        } while (next.equals(previous));
        return next;
    }

    private void dispose() {
        if (player == null) {
            return;
        }
        player.stop();
        player.dispose();
        player = null;
    }

    private static List<URL> tracks() {
        URL folder = MenuMusic.class.getResource(MUSIC);
        if (folder == null) {
            return List.of();
        }
        try {
            URI uri = folder.toURI();
            if ("file".equals(uri.getScheme())) {
                try (var files = Files.list(Path.of(uri))) {
                    return files
                            .filter(path -> isTrack(path.getFileName().toString()))
                            .sorted()
                            .map(path -> {
                                try {
                                    return path.toUri().toURL();
                                } catch (IOException ex) {
                                    return null;
                                }
                            })
                            .filter(url -> url != null)
                            .toList();
                }
            }
            if ("jar".equals(uri.getScheme())) {
                return tracksInJar(uri);
            }
        } catch (IOException | URISyntaxException ex) {
            LOG.warning("Не удалось прочитать /gwent/audio/music: " + ex.getMessage());
        }
        return List.of();
    }

    private static List<URL> tracksInJar(URI folder) throws IOException {
        String spec = folder.toString();
        int separator = spec.indexOf("!/");
        if (separator < 0) {
            return List.of();
        }
        URI jar = URI.create(spec.substring("jar:".length(), separator));
        String prefix = spec.substring(separator + 2);
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        List<URL> found = new ArrayList<>();
        try (JarFile archive = new JarFile(Path.of(jar).toFile())) {
            Enumeration<JarEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.startsWith(prefix) || name.length() == prefix.length()) {
                    continue;
                }
                String file = name.substring(prefix.length());
                if (file.indexOf('/') >= 0 || !isTrack(file)) {
                    continue;
                }
                URL resource = MenuMusic.class.getResource("/" + name);
                if (resource != null) {
                    found.add(resource);
                }
            }
        }
        found.sort((left, right) -> left.toExternalForm().compareTo(right.toExternalForm()));
        return List.copyOf(found);
    }

    private static boolean isTrack(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a") || lower.endsWith(".aac");
    }

    private static String name(URL track) {
        String path = track.getPath();
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private static double clamp(double volume) {
        return Math.clamp(volume, 0, 1);
    }
}
