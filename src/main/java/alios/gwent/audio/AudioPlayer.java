package alios.gwent.audio;

import alios.gwent.config.GameConfig;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Централизованный статический аудиоплеер приложения.
 * <p>
 * Управляет фоновой музыкой (MediaPlayer) и звуковыми эффектами (AudioClip).
 * Реализует бесконечный случайный плейлист без двух повторений трека подряд,
 * параллельное воспроизведение SFX, регулировку громкости в реальном времени и режим Mute.
 */
public final class AudioPlayer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AudioPlayer.class);

    private static final String MUSIC_RESOURCE_DIR = "/alios/gwent/assets/audio/music";
    private static final String SFX_RESOURCE_DIR = "/alios/gwent/assets/audio/sfx/";

    private static final List<String> MUSIC_TRACKS = discoverMusicTracks();
    private static final Map<String, AudioClip> SFX_CACHE = new HashMap<>();
    private static final Random RANDOM = new Random();

    private static MediaPlayer currentMusicPlayer;
    private static int currentTrackIndex = -1;

    private static double musicVolume = GameConfig.get().musicVolume;
    private static double sfxVolume = GameConfig.get().sfxVolume;
    private static boolean mute = GameConfig.get().mute;

    private AudioPlayer() {
    }

    // =========================================================================
    // Public Business Logic
    // =========================================================================

    /**
     * Запускает воспроизведение фоновой музыки со случайного трека.
     */
    public static void startMusic() {
        if (MUSIC_TRACKS.isEmpty()) {
            LOGGER.warn("No music tracks available to play in {}", MUSIC_RESOURCE_DIR);
            return;
        }
        playNextTrack();
    }

    /**
     * Воспроизводит звуковой эффект по относительному пути внутри assets/audio/sfx/.
     * Звук играет параллельно фоновой музыке без её прерывания.
     *
     * @param relativePath относительный путь к файлу (например, "gui_gwint_dummy.wav")
     */
    public static void playSfx(String relativePath) {
        double effectiveSfxVolume = getEffectiveSfxVolume();
        if (effectiveSfxVolume <= 0.0) {
            return;
        }

        AudioClip clip = getOrCreateClip(relativePath);
        if (clip != null) {
            clip.play(effectiveSfxVolume);
        }
    }

    /**
     * Устанавливает громкость музыки в реальном времени без сохранения в конфиг.
     *
     * @param volume громкость от 0.0 до 1.0
     */
    public static void setLiveMusicVolume(double volume) {
        musicVolume = Math.clamp(volume, 0.0, 1.0);
        if (currentMusicPlayer != null) {
            currentMusicPlayer.setVolume(getEffectiveMusicVolume());
        }
    }

    /**
     * Устанавливает громкость звуковых эффектов в реальном времени без сохранения в конфиг.
     *
     * @param volume громкость от 0.0 до 1.0
     */
    public static void setLiveSfxVolume(double volume) {
        sfxVolume = Math.clamp(volume, 0.0, 1.0);
    }

    /**
     * Устанавливает состояние Mute в реальном времени.
     * Трек продолжает воспроизведение без паузы, но с нулевой громкостью.
     *
     * @param muteState true для выключения звука
     */
    public static void setLiveMute(boolean muteState) {
        mute = muteState;
        if (currentMusicPlayer != null) {
            currentMusicPlayer.setVolume(getEffectiveMusicVolume());
        }
    }

    /**
     * Синхронизирует параметры громкости и mute из текущего GameConfig.
     */
    public static void applyConfig() {
        GameConfig config = GameConfig.get();
        musicVolume = config.musicVolume;
        sfxVolume = config.sfxVolume;
        mute = config.mute;

        if (currentMusicPlayer != null) {
            currentMusicPlayer.setVolume(getEffectiveMusicVolume());
        }
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    static double getEffectiveMusicVolume() {
        return mute ? 0.0 : musicVolume;
    }

    static double getEffectiveSfxVolume() {
        return mute ? 0.0 : sfxVolume;
    }

    /**
     * Переключает воспроизведение на следующий случайный трек,
     * гарантируя отсутствие двух одинаковых треков подряд.
     */
    private static void playNextTrack() {
        if (MUSIC_TRACKS.isEmpty()) {
            return;
        }

        int nextIndex = pickNextTrackIndex();
        String trackName = MUSIC_TRACKS.get(nextIndex);
        currentTrackIndex = nextIndex;

        stopCurrentMusic();

        URL resource = AudioPlayer.class.getResource(MUSIC_RESOURCE_DIR + "/" + trackName);
        if (resource == null) {
            LOGGER.error("Music track resource not found: {}", trackName);
            return;
        }

        try {
            Media media = new Media(resource.toURI().toString());
            currentMusicPlayer = new MediaPlayer(media);
            currentMusicPlayer.setVolume(getEffectiveMusicVolume());
            currentMusicPlayer.setOnEndOfMedia(AudioPlayer::playNextTrack);
            currentMusicPlayer.setOnError(() -> {
                MediaException error = currentMusicPlayer.getError();
                LOGGER.error("Error occurred while playing track {}", trackName, error);
                playNextTrack();
            });
            currentMusicPlayer.play();
            LOGGER.info("Now playing music track [{}]: {}", nextIndex + 1, trackName);
        } catch (Exception e) {
            LOGGER.error("Failed to play track {}: {}", trackName, e.getMessage(), e);
        }
    }

    /**
     * Останавливает текущий музыкальный трек и освобождает его медиа-ресурсы.
     */
    private static void stopCurrentMusic() {
        if (currentMusicPlayer != null) {
            currentMusicPlayer.stop();
            currentMusicPlayer.dispose();
            currentMusicPlayer = null;
        }
    }

    private static int pickNextTrackIndex() {
        if (MUSIC_TRACKS.size() <= 1) {
            return 0;
        }
        int nextIndex;
        do {
            nextIndex = RANDOM.nextInt(MUSIC_TRACKS.size());
        } while (nextIndex == currentTrackIndex);
        return nextIndex;
    }

    private static AudioClip getOrCreateClip(String relativePath) {
        return SFX_CACHE.computeIfAbsent(relativePath, path -> {
            String fullPath = SFX_RESOURCE_DIR + path;
            URL url = AudioPlayer.class.getResource(fullPath);
            if (url == null) {
                LOGGER.warn("SFX asset not found: {}", fullPath);
                return null;
            }
            try {
                return new AudioClip(url.toExternalForm());
            } catch (Exception e) {
                LOGGER.error("Failed to load AudioClip for {}: {}", fullPath, e.getMessage(), e);
                return null;
            }
        });
    }

    private static List<String> discoverMusicTracks() {
        URL folderUrl = AudioPlayer.class.getResource(MUSIC_RESOURCE_DIR);
        if (folderUrl == null) {
            LOGGER.warn("Music directory not found: {}", MUSIC_RESOURCE_DIR);
            return List.of();
        }

        try {
            if ("file".equals(folderUrl.getProtocol())) {
                try (Stream<Path> stream = Files.list(Path.of(folderUrl.toURI()))) {
                    List<String> tracks = stream
                            .filter(Files::isRegularFile)
                            .map(p -> p.getFileName().toString())
                            .filter(AudioPlayer::isSupportedAudioFile)
                            .sorted()
                            .toList();
                    LOGGER.info("Discovered {} music tracks in {} (file protocol)", tracks.size(), MUSIC_RESOURCE_DIR);
                    return tracks;
                }
            } else if ("jar".equals(folderUrl.getProtocol())) {
                JarURLConnection jarConnection = (JarURLConnection) folderUrl.openConnection();
                try (JarFile jarFile = jarConnection.getJarFile()) {
                    String prefix = jarConnection.getEntryName();
                    if (!prefix.endsWith("/")) {
                        prefix += "/";
                    }
                    List<String> tracks = new ArrayList<>();
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith(prefix) && !name.equals(prefix)) {
                            String relativeName = name.substring(prefix.length());
                            if (!entry.isDirectory() && !relativeName.contains("/") && isSupportedAudioFile(relativeName)) {
                                tracks.add(relativeName);
                            }
                        }
                    }
                    Collections.sort(tracks);
                    LOGGER.info("Discovered {} music tracks in {} (jar protocol)", tracks.size(), MUSIC_RESOURCE_DIR);
                    return Collections.unmodifiableList(tracks);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to discover music tracks: {}", e.getMessage(), e);
        }
        return List.of();
    }

    private static boolean isSupportedAudioFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".wav");
    }

    // =========================================================================
    // Getters / helpers for unit tests
    // =========================================================================

    static double getMusicVolume() {
        return musicVolume;
    }

    static double getSfxVolume() {
        return sfxVolume;
    }

    static boolean isMute() {
        return mute;
    }

    static List<String> getMusicTracks() {
        return Collections.unmodifiableList(MUSIC_TRACKS);
    }
}
