package alios.gwent.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Базовый класс для конфигурационных JSON-файлов приложения.
 * <p>
 * Автоматически гарантирует существование рабочей директории, загружает настройки
 * напрямую в публичные поля без геттеров/сеттеров, синхронизирует схему на диске
 * (включая вложенные объекты) и создает бэкап при повреждении файла.
 */
public abstract class BaseConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseConfig.class);

    public static final File WORK_DIR = ensureWorkDirectoryExists();

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);

    @JsonIgnore
    private final File configFile;

    // =========================================================================
    // Constructors and Initialization
    // =========================================================================

    protected BaseConfig(String fileName) {
        Objects.requireNonNull(fileName, "fileName must not be null");
        this.configFile = new File(WORK_DIR, fileName);
    }

    /**
     * Инициализирует конфигурацию: если файл отсутствует — создает со значениями по умолчанию,
     * если существует — загружает данные и синхронизирует структуру.
     */
    protected final void init() {
        if (!configFile.exists()) {
            save();
        } else {
            load();
        }
    }

    /**
     * Хук пост-обработки для валидации или корректировки значений после загрузки.
     */
    protected void postLoad() {
    }

    // =========================================================================
    // Public Business Logic
    // =========================================================================

    /**
     * Сохраняет текущие значения полей в JSON-файл.
     */
    public synchronized void save() {
        try {
            MAPPER.writeValue(configFile, this);
        } catch (IOException e) {
            LOGGER.error("Failed to save config to {}", configFile, e);
        }
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private static File ensureWorkDirectoryExists() {
        String customDir = System.getProperty("gwent.workdir"); // Используется только в юнит-тестах
        Path workDirPath = customDir != null
                ? Path.of(customDir)
                : Path.of(System.getProperty("user.home"), "Aliosgame", "Gwent");

        try {
            Files.createDirectories(workDirPath);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать рабочую директорию: " + workDirPath, e);
        }

        return workDirPath.toFile();
    }

    private void load() {
        try {
            JsonNode fileTree = MAPPER.readTree(configFile);
            JsonNode currentModelTree = MAPPER.valueToTree(this);

            MAPPER.readerForUpdating(this).readValue(fileTree);
            postLoad();

            // Глубокая проверка схемы: выявляет изменения как на верхнем уровне, так и во вложенных объектах
            if (!hasSameSchema(fileTree, currentModelTree)) {
                LOGGER.info("Config {} schema mismatch detected. Updating file on disk.", configFile.getName());
                save();
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to read config from {}: {}. Reverting to defaults.", configFile.getName(), e.getMessage());
            createBackupFile();
            save();
        }
    }

    /**
     * Рекурсивно проверяет соответствие структуры ключей двух JSON-деревьев
     * независимо от значений полей.
     */
    private boolean hasSameSchema(JsonNode fileNode, JsonNode modelNode) {
        if (fileNode == null || modelNode == null) {
            return fileNode == modelNode;
        }

        if (fileNode.isObject() && modelNode.isObject()) {
            Set<String> fileKeys = new HashSet<>();
            fileNode.fieldNames().forEachRemaining(fileKeys::add);

            Set<String> modelKeys = new HashSet<>();
            modelNode.fieldNames().forEachRemaining(modelKeys::add);

            if (!fileKeys.equals(modelKeys)) {
                return false;
            }

            for (String key : modelKeys) {
                if (!hasSameSchema(fileNode.get(key), modelNode.get(key))) {
                    return false;
                }
            }
            return true;
        }

        return fileNode.isObject() == modelNode.isObject();
    }

    private void createBackupFile() {
        if (configFile.exists()) {
            File backup = new File(configFile.getParentFile(), configFile.getName() + ".bak");
            try {
                Files.copy(configFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Created backup of corrupted config at {}", backup.getName());
            } catch (IOException ex) {
                LOGGER.warn("Failed to create backup for {}: {}", configFile.getName(), ex.getMessage());
            }
        }
    }

    // =========================================================================
    // Getters / helpers for unit tests
    // =========================================================================

    @JsonIgnore
    File getConfigFile() {
        return configFile;
    }
}
