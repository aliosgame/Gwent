package gwent.persist;

import java.util.HashMap;
import java.util.Map;

/**
 * Плоский json без сторонней библиотеки: в файле только числа и один флаг.
 * Незнакомые ключи пропускаются, отсутствующие берутся из {@link Settings#defaults()}.
 */
final class SettingsJson {
    private SettingsJson() {
    }

    static String format(Settings settings) {
        return """
                {
                  "musicVolume": %d,
                  "effectsVolume": %d,
                  "fullscreen": %s,
                  "windowWidth": %d,
                  "windowHeight": %d,
                  "windowX": %d,
                  "windowY": %d
                }
                """.formatted(
                settings.musicVolume(),
                settings.effectsVolume(),
                settings.fullscreen(),
                settings.windowWidth(),
                settings.windowHeight(),
                settings.windowX(),
                settings.windowY());
    }

    static Settings parse(String text) {
        if (text == null || text.isBlank()) {
            throw new SettingsFormatException("пустой файл");
        }
        String source = stripBom(text.strip());
        if (source.length() < 2 || source.charAt(0) != '{' || source.charAt(source.length() - 1) != '}') {
            throw new SettingsFormatException("ожидался объект {...}");
        }
        Map<String, String> values = readFields(source.substring(1, source.length() - 1));
        Settings defaults = Settings.defaults();
        return new Settings(
                integer(values, "musicVolume", defaults.musicVolume()),
                integer(values, "effectsVolume", defaults.effectsVolume()),
                bool(values, "fullscreen", defaults.fullscreen()),
                integer(values, "windowWidth", defaults.windowWidth()),
                integer(values, "windowHeight", defaults.windowHeight()),
                integer(values, "windowX", defaults.windowX()),
                integer(values, "windowY", defaults.windowY()));
    }

    private static String stripBom(String source) {
        if (!source.isEmpty() && source.charAt(0) == '\uFEFF') {
            return source.substring(1).strip();
        }
        return source;
    }

    private static Map<String, String> readFields(String body) {
        Map<String, String> values = new HashMap<>();
        int index = 0;
        while (index < body.length()) {
            index = skipWhitespace(body, index);
            if (index >= body.length()) {
                break;
            }
            if (body.charAt(index) == ',') {
                index++;
                continue;
            }
            if (body.charAt(index) != '"') {
                throw new SettingsFormatException("ожидался ключ в кавычках");
            }
            int keyEnd = body.indexOf('"', index + 1);
            if (keyEnd < 0) {
                throw new SettingsFormatException("ключ не закрыт");
            }
            String key = body.substring(index + 1, keyEnd);
            index = skipWhitespace(body, keyEnd + 1);
            if (index >= body.length() || body.charAt(index) != ':') {
                throw new SettingsFormatException("ожидалось двоеточие после " + key);
            }
            index = skipWhitespace(body, index + 1);
            int valueEnd = readValue(body, index);
            values.put(key, body.substring(index, valueEnd).strip());
            index = skipWhitespace(body, valueEnd);
            if (index < body.length() && body.charAt(index) != ',') {
                throw new SettingsFormatException("лишние символы после значения " + key);
            }
        }
        return values;
    }

    private static int readValue(String body, int start) {
        if (body.startsWith("true", start) || body.startsWith("false", start)) {
            return body.startsWith("true", start) ? start + 4 : start + 5;
        }
        int index = start;
        if (index < body.length() && body.charAt(index) == '-') {
            index++;
        }
        int digits = index;
        while (index < body.length() && Character.isDigit(body.charAt(index))) {
            index++;
        }
        if (digits == index) {
            throw new SettingsFormatException("ожидалось число или true/false");
        }
        return index;
    }

    private static int skipWhitespace(String body, int index) {
        while (index < body.length() && Character.isWhitespace(body.charAt(index))) {
            index++;
        }
        return index;
    }

    private static int integer(Map<String, String> values, String key, int fallback) {
        String raw = values.get(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new SettingsFormatException("плохое число в " + key);
        }
    }

    private static boolean bool(Map<String, String> values, String key, boolean fallback) {
        String raw = values.get(key);
        if (raw == null) {
            return fallback;
        }
        if ("true".equals(raw)) {
            return true;
        }
        if ("false".equals(raw)) {
            return false;
        }
        throw new SettingsFormatException("плохое значение в " + key);
    }
}
