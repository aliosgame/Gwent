package alios.gwent.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class BaseConfigTest {

    private static final String TEST_FILE_NAME = "test-schema-config.json";

    private static class OldConfigSchema extends BaseConfig {
        public String oldField = "deprecated";
        public int keepField = 100;

        OldConfigSchema() {
            super(TEST_FILE_NAME);
            init();
        }
    }

    private static class NewConfigSchema extends BaseConfig {
        public int keepField = 100;
        public String newField = "brand_new";

        NewConfigSchema() {
            super(TEST_FILE_NAME);
            init();
        }
    }

    public static class NestedDeck {
        public int leader = 1;
        public String oldDeckField = "to_be_removed";
    }

    public static class NewNestedDeck {
        public int leader = 1;
        public int sleeveId = 99;
    }

    private static class NestedOldConfig extends BaseConfig {
        public NestedDeck deck = new NestedDeck();

        NestedOldConfig() {
            super(TEST_FILE_NAME);
            init();
        }
    }

    private static class NestedNewConfig extends BaseConfig {
        public NewNestedDeck deck = new NewNestedDeck();

        NestedNewConfig() {
            super(TEST_FILE_NAME);
            init();
        }
    }

    @AfterEach
    void cleanup() {
        File testFile = new File(BaseConfig.WORK_DIR, TEST_FILE_NAME);
        if (testFile.exists()) {
            testFile.delete();
        }
        File backupFile = new File(BaseConfig.WORK_DIR, TEST_FILE_NAME + ".bak");
        if (backupFile.exists()) {
            backupFile.delete();
        }
    }

    @Test
    @DisplayName("Automatically migrates schema: removes obsolete fields and adds newly added fields")
    void testSchemaMigration() throws IOException {
        OldConfigSchema oldConfig = new OldConfigSchema();
        oldConfig.keepField = 777;
        oldConfig.save();

        File file = oldConfig.getConfigFile();
        assertTrue(file.exists());
        String initialJson = Files.readString(file.toPath());
        assertTrue(initialJson.contains("oldField"));
        assertTrue(initialJson.contains("777"));

        NewConfigSchema newConfig = new NewConfigSchema();

        assertEquals(777, newConfig.keepField);
        assertEquals("brand_new", newConfig.newField);

        String updatedJson = Files.readString(file.toPath());
        assertFalse(updatedJson.contains("oldField"), "Obsolete field must be removed from disk");
        assertTrue(updatedJson.contains("newField"), "New field must be added to disk");
        assertTrue(updatedJson.contains("777"), "Existing field value must be preserved");
    }

    @Test
    @DisplayName("Deep schema migration handles nested objects (adds new nested fields, removes obsolete)")
    void testDeepNestedSchemaMigration() throws IOException {
        NestedOldConfig oldConfig = new NestedOldConfig();
        oldConfig.deck.leader = 205;
        oldConfig.deck.oldDeckField = "legacy";
        oldConfig.save();

        File file = oldConfig.getConfigFile();
        String oldJson = Files.readString(file.toPath());
        assertTrue(oldJson.contains("oldDeckField"));

        NestedNewConfig newConfig = new NestedNewConfig();
        assertEquals(205, newConfig.deck.leader);
        assertEquals(99, newConfig.deck.sleeveId);

        String newJson = Files.readString(file.toPath());
        assertFalse(newJson.contains("oldDeckField"), "Nested obsolete field must be removed");
        assertTrue(newJson.contains("sleeveId"), "Nested new field must be added to disk");
        assertTrue(newJson.contains("205"), "Existing nested field value must be preserved");
    }

    @Test
    @DisplayName("Recovers from corrupted JSON file by creating .bak and restoring defaults")
    void testRecoversFromCorruptedFile() throws IOException {
        File file = new File(BaseConfig.WORK_DIR, TEST_FILE_NAME);
        Files.writeString(file.toPath(), "{ totally broken json syntax: true ");

        NewConfigSchema config = new NewConfigSchema();
        assertEquals(100, config.keepField);
        assertEquals("brand_new", config.newField);

        File backup = new File(BaseConfig.WORK_DIR, TEST_FILE_NAME + ".bak");
        assertTrue(backup.exists(), "Backup of corrupted file (.bak) must be created");
        assertEquals("{ totally broken json syntax: true ", Files.readString(backup.toPath()));

        String healedJson = Files.readString(file.toPath());
        assertTrue(healedJson.contains("\"keepField\" : 100"));
    }
}
