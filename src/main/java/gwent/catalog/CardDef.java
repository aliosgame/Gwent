package gwent.catalog;

import java.util.List;

/**
 * Одна карта из каталога. Разные картинки одного имени — разные id:
 * путь к рисунку собирается из фракции и этого числа.
 * {@link #copies()} — сколько таких карт есть в полной коллекции
 * и, значит, сколько можно положить в колоду.
 */
public record CardDef(
        int id,
        String name,
        Faction faction,
        CardKind kind,
        int strength,
        List<Row> rows,
        Row affects,
        boolean hero,
        Ability ability,
        String musterGroup,
        PlaysOn playsOn,
        LeaderAbility leaderAbility,
        int copies,
        String text
) {
    public CardDef {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("У карты " + id + " нет имени");
        }
        if (strength < 0 || copies < 0) {
            throw new IllegalArgumentException("Плохие числа у карты " + id);
        }
        if (kind == CardKind.UNIT && (rows == null || rows.isEmpty())) {
            throw new IllegalArgumentException("Отряду " + id + " не задан ряд");
        }
        if (kind == CardKind.WEATHER && affects == null) {
            throw new IllegalArgumentException("Погоде " + id + " не задан ряд");
        }
        if (kind == CardKind.LEADER && leaderAbility == null) {
            throw new IllegalArgumentException("Лидеру " + id + " не задано умение");
        }
        if (ability == Ability.MUSTER && (musterGroup == null || musterGroup.isBlank())) {
            throw new IllegalArgumentException("Двойнику " + id + " не задана группа");
        }
        rows = rows == null ? List.of() : List.copyOf(rows);
        if (playsOn == null) {
            playsOn = PlaysOn.OWN;
        }
        if (ability == null) {
            ability = Ability.NONE;
        }
        if (text == null) {
            text = "";
        }
    }

    public boolean agile() {
        return rows.size() > 1;
    }

    public boolean unit() {
        return kind == CardKind.UNIT;
    }

    public boolean special() {
        return kind != CardKind.UNIT && kind != CardKind.LEADER;
    }

    public boolean spy() {
        return ability == Ability.SPY;
    }

    /** Карта, которую можно добавить в колоду. Лидеры выбираются отдельно. */
    public boolean inCollection() {
        return copies > 0 && kind != CardKind.LEADER;
    }

    /** Обрезанный рисунок для игрового поля: число силы подставляется отдельно. */
    public String imagePath() {
        String folder = special() ? "Special" : artFolder(faction);
        return "/gwent/cards/" + folder + "/" + id + ".png";
    }

    /** Полная карта для меню колоды: имя и сила уже нарисованы. У лидера отдельного полного файла нет. */
    public String fullImagePath() {
        if (kind == CardKind.LEADER) {
            return imagePath();
        }
        String path = imagePath();
        return path.substring(0, path.length() - 4) + "_full.png";
    }

    private static String artFolder(Faction faction) {
        return switch (faction) {
            case NORTHERN_REALMS -> "North";
            case NILFGAARD -> "Nilf";
            case SCOIATAEL -> "Elf";
            case MONSTERS -> "Monster";
            case NEUTRAL -> "Neutral";
        };
    }
}
