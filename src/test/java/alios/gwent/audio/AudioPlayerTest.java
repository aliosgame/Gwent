package alios.gwent.audio;

import alios.gwent.config.GameConfig;
import alios.gwent.test.JavaFxTestPlatform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AudioPlayerTest {

    @BeforeAll
    static void initJavaFx() {
        JavaFxTestPlatform.ensureStarted();
    }

    @Test
    @DisplayName("AudioPlayer discovers 10 mp3 music tracks and ignores subdirectories")
    void testDiscoveredTracks() {
        List<String> tracks = AudioPlayer.getMusicTracks();

        assertNotNull(tracks);
        assertEquals(10, tracks.size(), "Root music directory should contain exactly 10 tracks");
        for (String track : tracks) {
            assertTrue(track.toLowerCase().endsWith(".mp3") || track.toLowerCase().endsWith(".wav"));
            assertFalse(track.contains("/"), "Subdirectory names must not be included");
        }
    }

    @Test
    @DisplayName("AudioPlayer applies volume and mute from GameConfig")
    void testApplyConfig() {
        GameConfig config = GameConfig.get();
        config.musicVolume = 0.65;
        config.sfxVolume = 0.45;
        config.mute = false;

        AudioPlayer.applyConfig();

        assertEquals(0.65, AudioPlayer.getMusicVolume(), 0.001);
        assertEquals(0.45, AudioPlayer.getSfxVolume(), 0.001);
        assertFalse(AudioPlayer.isMute());
        assertEquals(0.65, AudioPlayer.getEffectiveMusicVolume(), 0.001);
        assertEquals(0.45, AudioPlayer.getEffectiveSfxVolume(), 0.001);
    }

    @Test
    @DisplayName("Mute mode zeroes effective volumes without resetting saved levels")
    void testMuteBehavior() {
        AudioPlayer.setLiveMusicVolume(0.8);
        AudioPlayer.setLiveSfxVolume(0.7);
        AudioPlayer.setLiveMute(true);

        assertTrue(AudioPlayer.isMute());
        assertEquals(0.8, AudioPlayer.getMusicVolume(), 0.001);
        assertEquals(0.7, AudioPlayer.getSfxVolume(), 0.001);
        assertEquals(0.0, AudioPlayer.getEffectiveMusicVolume(), 0.001);
        assertEquals(0.0, AudioPlayer.getEffectiveSfxVolume(), 0.001);

        AudioPlayer.setLiveMute(false);
        assertFalse(AudioPlayer.isMute());
        assertEquals(0.8, AudioPlayer.getEffectiveMusicVolume(), 0.001);
        assertEquals(0.7, AudioPlayer.getEffectiveSfxVolume(), 0.001);
    }

    @Test
    @DisplayName("Volume clamping restricts values to range [0.0, 1.0]")
    void testVolumeClamping() {
        AudioPlayer.setLiveMusicVolume(1.5);
        assertEquals(1.0, AudioPlayer.getMusicVolume(), 0.001);

        AudioPlayer.setLiveMusicVolume(-0.5);
        assertEquals(0.0, AudioPlayer.getMusicVolume(), 0.001);

        AudioPlayer.setLiveSfxVolume(2.0);
        assertEquals(1.0, AudioPlayer.getSfxVolume(), 0.001);

        AudioPlayer.setLiveSfxVolume(-1.0);
        assertEquals(0.0, AudioPlayer.getSfxVolume(), 0.001);
    }

    @Test
    @DisplayName("SFX playback can be triggered on FX Thread")
    void testPlaySfx() {
        JavaFxTestPlatform.runOnFxThread(() -> {
            AudioPlayer.setLiveMute(false);
            AudioPlayer.setLiveSfxVolume(0.5);

            assertDoesNotThrow(() -> AudioPlayer.playSfx("gui_gwint_dummy.wav"));
            assertDoesNotThrow(() -> AudioPlayer.playSfx("non_existing_file.wav"));
        });
    }
}
