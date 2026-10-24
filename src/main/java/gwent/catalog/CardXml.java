package gwent.catalog;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Читает карты из ресурсов. Незнакомое слово в перечислении роняет загрузку сразу. */
public final class CardXml {
    private CardXml() {
    }

    public static List<CardDef> read(InputStream in) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            NodeList nodes = factory.newDocumentBuilder().parse(in).getElementsByTagName("card");
            List<CardDef> cards = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                cards.add(card((Element) nodes.item(i)));
            }
            return List.copyOf(cards);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Не разобрать каталог карт", ex);
        }
    }

    private static CardDef card(Element element) {
        int id = integer(element, "id");
        CardKind kind = enumValue(CardKind.class, attr(element, "kind", true), id);
        Ability ability = element.hasAttribute("ability")
                ? enumValue(Ability.class, element.getAttribute("ability"), id)
                : Ability.NONE;
        String muster = element.getAttribute("muster");
        if (ability == Ability.MUSTER && muster.isBlank()) {
            muster = attr(element, "name", true);
        }
        return new CardDef(
                id,
                attr(element, "name", true),
                enumValue(Faction.class, attr(element, "faction", true), id),
                kind,
                element.hasAttribute("strength") ? integer(element, "strength") : 0,
                rows(element, id),
                element.hasAttribute("affects") ? enumValue(Row.class, element.getAttribute("affects"), id) : null,
                "true".equals(element.getAttribute("hero")),
                ability,
                muster.isBlank() ? null : muster,
                element.hasAttribute("playsOn")
                        ? enumValue(PlaysOn.class, element.getAttribute("playsOn"), id)
                        : PlaysOn.OWN,
                element.hasAttribute("leader") ? enumValue(LeaderAbility.class, element.getAttribute("leader"), id) : null,
                integer(element, "copies"),
                element.getAttribute("text"));
    }

    private static List<Row> rows(Element element, int id) {
        if (!element.hasAttribute("rows")) {
            return List.of();
        }
        List<Row> rows = new ArrayList<>();
        for (String part : element.getAttribute("rows").split(",")) {
            rows.add(enumValue(Row.class, part.strip(), id));
        }
        return rows;
    }

    private static int integer(Element element, String name) {
        try {
            return Integer.parseInt(attr(element, name, true));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Плохое число " + name + " у карты " + element.getAttribute("id"));
        }
    }

    private static String attr(Element element, String name, boolean required) {
        String value = element.getAttribute(name);
        if (required && value.isBlank()) {
            throw new IllegalArgumentException("У карты " + element.getAttribute("id") + " нет " + name);
        }
        return value;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw, int id) {
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("У карты " + id + " неизвестно значение " + raw);
        }
    }
}
