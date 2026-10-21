package gwent.catalog;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/** Название и описание умения. Логики здесь нет, только текст для экрана. */
public record SkillTexts(String name, String text) {
    private static final Map<String, SkillTexts> ALL = load();

    public static SkillTexts get(String id) {
        return ALL.get(id);
    }

    /** Длинный текст для ленты карт. Пустая строка — блок описания не показываем. */
    public static String blurb(CardDef card) {
        StringBuilder out = new StringBuilder();
        appendText(out, card.hero() ? get("HERO") : null);
        appendText(out, card.agile() ? get("AGILE") : null);
        if (card.ability() != null && card.ability() != Ability.NONE) {
            appendText(out, get(card.ability().name()));
        } else if (card.special()) {
            appendText(out, get(card.kind().name()));
        }
        return out.toString();
    }

    public static String describe(CardDef card) {
        StringBuilder out = new StringBuilder();
        append(out, card.hero() ? get("HERO") : null);
        append(out, card.agile() ? get("AGILE") : null);
        if (card.ability() != null && card.ability() != Ability.NONE) {
            append(out, get(card.ability().name()));
        } else if (card.special()) {
            append(out, get(card.kind().name()));
        }
        if (!card.text().isBlank()) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(card.text());
        }
        return out.toString();
    }

    private static void append(StringBuilder out, SkillTexts texts) {
        appendPiece(out, texts == null ? null : texts.name());
    }

    private static void appendText(StringBuilder out, SkillTexts texts) {
        appendPiece(out, texts == null ? null : texts.text());
    }

    private static void appendPiece(StringBuilder out, String piece) {
        if (piece == null || piece.isBlank()) {
            return;
        }
        if (!out.isEmpty()) {
            out.append(' ');
        }
        out.append(piece);
    }

    private static Map<String, SkillTexts> load() {
        try (InputStream in = SkillTexts.class.getResourceAsStream("/gwent/catalog/skills.xml")) {
            if (in == null) {
                throw new IllegalStateException("Нет /gwent/catalog/skills.xml");
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            NodeList nodes = factory.newDocumentBuilder().parse(in).getElementsByTagName("skill");
            Map<String, SkillTexts> texts = new LinkedHashMap<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element element = (Element) nodes.item(i);
                texts.put(element.getAttribute("id"), new SkillTexts(element.getAttribute("name"), element.getAttribute("text")));
            }
            return Map.copyOf(texts);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Не разобрать описания умений", ex);
        }
    }
}
