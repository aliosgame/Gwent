package gwent.rules;

import gwent.catalog.Ability;
import gwent.catalog.CardCatalog;
import gwent.catalog.CardDef;
import gwent.catalog.CardKind;
import gwent.catalog.Faction;
import gwent.catalog.PlaysOn;
import gwent.catalog.Row;
import gwent.deck.Deck;
import gwent.deck.DeckCheck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Random;

/**
 * Одна партия до двух побед. Поле считает силу, здесь — колода, рука, отбой и ходы.
 * Умения лидеров, призыв двойников и корова пока не срабатывают.
 * <p>
 * Ничья в раунде достаётся Нильфгаарду, если он один такой за столом.
 * Север после выигранного раунда берёт карту из колоды.
 * Погода не снимается в конце раунда. Кто выиграл раунд, ходит первым в следующем;
 * при чистой ничьей первым ходит тот, кто спасовал раньше.
 */
public final class Match {
    public static final int HAND = 10;
    public static final int MULLIGAN = 2;
    public static final int ROUND_WINS = 2;
    public static final int ROUNDS = 3;
    private static final int ROW_SCORCH_AT = 10;

    public enum Phase {
        MULLIGAN,
        PLAY,
        FINISHED
    }

    /** Чучело или карта рога, лежащая на ряду. На силу чучело не влияет. */
    public record Token(int id, CardDef card, Side owner, Side side, Row row) {
    }

    private final Random random;
    private final EnumMap<Side, Seat> seats = new EnumMap<>(Side.class);
    private final Battlefield field = new Battlefield();
    private final List<Token> tokens = new ArrayList<>();
    private final java.util.Map<Integer, Side> owners = new java.util.HashMap<>();

    private Phase phase = Phase.MULLIGAN;
    private int round = 1;
    private Side first;
    private Side active;
    private Side passedFirst;
    private Side winner;
    private int tokenIds = 1;

    private Match(Random random) {
        this.random = random;
    }

    /** Перемешивает обе колоды и сдаёт по десять карт. Пока оба не сделают муллиган, ходить нельзя. */
    public static Match start(Deck player, Deck opponent, Random random) {
        requireLegal(player);
        requireLegal(opponent);
        Match match = new Match(random);
        match.seats.put(Side.PLAYER, Seat.from(player, random));
        match.seats.put(Side.OPPONENT, Seat.from(opponent, random));
        match.first = random.nextBoolean() ? Side.PLAYER : Side.OPPONENT;
        match.draw(Side.PLAYER, HAND);
        match.draw(Side.OPPONENT, HAND);
        return match;
    }

    /** Уже сданные руки. Муллиган считается сделанным, ходит {@code first}. */
    public static Match prepared(Faction player, Faction opponent, List<CardDef> playerHand, List<CardDef> opponentHand,
                                 List<CardDef> playerDeck, List<CardDef> opponentDeck, Side first) {
        Match match = new Match(new Random(0));
        match.seats.put(Side.PLAYER, Seat.open(player, playerHand, playerDeck));
        match.seats.put(Side.OPPONENT, Seat.open(opponent, opponentHand, opponentDeck));
        match.seats.get(Side.PLAYER).mulliganDone = true;
        match.seats.get(Side.OPPONENT).mulliganDone = true;
        match.first = first;
        match.active = first;
        match.phase = Phase.PLAY;
        return match;
    }

    public Phase phase() {
        return phase;
    }

    public int round() {
        return round;
    }

    public Battlefield field() {
        return field;
    }

    public Side first() {
        return first;
    }

    public Side active() {
        return active;
    }

    public Side winner() {
        return winner;
    }

    public int gems(Side side) {
        return seats.get(side).gems;
    }

    public boolean passed(Side side) {
        return seats.get(side).passed;
    }

    public boolean mulliganDone(Side side) {
        return seats.get(side).mulliganDone;
    }

    public Faction faction(Side side) {
        return seats.get(side).faction;
    }

    public int leaderId(Side side) {
        return seats.get(side).leaderId;
    }

    public List<CardDef> hand(Side side) {
        return List.copyOf(seats.get(side).hand);
    }

    public int deckSize(Side side) {
        return seats.get(side).deck.size();
    }

    public List<CardDef> discard(Side side) {
        return List.copyOf(seats.get(side).discard);
    }

    public List<Token> tokens() {
        return List.copyOf(tokens);
    }

    public void mulligan(Side side, List<Integer> handIndexes) {
        if (phase != Phase.MULLIGAN) {
            throw new IllegalMove("Муллиган уже закончен");
        }
        Seat seat = seats.get(side);
        if (seat.mulliganDone) {
            throw new IllegalMove("Эта сторона уже меняла карты");
        }
        if (handIndexes.size() > MULLIGAN) {
            throw new IllegalMove("Можно заменить не больше двух карт");
        }
        List<Integer> indexes = new ArrayList<>(handIndexes);
        indexes.sort(Collections.reverseOrder());
        Integer previous = null;
        List<CardDef> back = new ArrayList<>();
        for (int index : indexes) {
            if (previous != null && previous == index) {
                throw new IllegalMove("Одну карту нельзя заменить дважды");
            }
            previous = index;
            if (index < 0 || index >= seat.hand.size()) {
                throw new IllegalMove("В руке нет карты " + index);
            }
            back.add(seat.hand.remove(index));
        }
        seat.deck.addAll(back);
        Collections.shuffle(seat.deck, random);
        draw(side, back.size());
        seat.mulligansUsed += back.size();
        endMulligan(side);
    }

    /** Одна замена. Карта из колоды встаёт на то же место в руке. Муллиган на этом не кончается. */
    public void replaceOne(Side side, int handIndex) {
        Seat seat = openMulligan(side);
        if (seat.mulligansUsed >= MULLIGAN) {
            throw new IllegalMove("Можно заменить не больше двух карт");
        }
        if (handIndex < 0 || handIndex >= seat.hand.size()) {
            throw new IllegalMove("В руке нет карты " + handIndex);
        }
        seat.deck.add(seat.hand.remove(handIndex));
        Collections.shuffle(seat.deck, random);
        if (!seat.deck.isEmpty()) {
            seat.hand.add(handIndex, seat.deck.removeFirst());
        }
        seat.mulligansUsed++;
    }

    public void endMulligan(Side side) {
        Seat seat = openMulligan(side);
        seat.mulliganDone = true;
        if (seats.get(Side.PLAYER).mulliganDone && seats.get(Side.OPPONENT).mulliganDone) {
            phase = Phase.PLAY;
            active = first;
            passedFirst = null;
        }
    }

    public int mulligansLeft(Side side) {
        return MULLIGAN - seats.get(side).mulligansUsed;
    }

    private Seat openMulligan(Side side) {
        if (phase != Phase.MULLIGAN) {
            throw new IllegalMove("Муллиган уже закончен");
        }
        Seat seat = seats.get(side);
        if (seat.mulliganDone) {
            throw new IllegalMove("Эта сторона уже меняла карты");
        }
        return seat;
    }

    public void play(Side side, Move move) {
        if (phase != Phase.PLAY) {
            throw new IllegalMove("Сейчас нельзя ходить");
        }
        if (side != active) {
            throw new IllegalMove("Сейчас ходит другая сторона");
        }
        if (seats.get(side).passed) {
            throw new IllegalMove("Вы уже спасовали в этом раунде");
        }
        switch (move) {
            case Move.Pass ignored -> pass(side);
            case Move.Unit unit -> playUnit(side, unit.handIndex(), unit.row());
            case Move.Horn horn -> playHorn(side, horn.handIndex(), horn.row());
            case Move.Weather weather -> playWeather(side, weather.handIndex());
            case Move.ClearWeather clear -> playClear(side, clear.handIndex());
            case Move.Scorch scorch -> playScorch(side, scorch.handIndex());
            case Move.Decoy decoy -> playDecoy(side, decoy.handIndex(), decoy.placementId());
            case Move.Medic medic -> playMedic(side, medic.handIndex(), medic.row(), medic.discardIndex(), medic.revivedRow());
        }
    }

    public List<Move> legal(Side side) {
        if (phase != Phase.PLAY || side != active || seats.get(side).passed) {
            return List.of();
        }
        List<Move> moves = new ArrayList<>();
        moves.add(new Move.Pass());
        List<CardDef> hand = seats.get(side).hand;
        for (int i = 0; i < hand.size(); i++) {
            CardDef card = hand.get(i);
            switch (card.kind()) {
                case UNIT -> legalUnit(side, i, card, moves);
                case HORN -> {
                    for (Row row : Row.values()) {
                        if (!field.horn(side, row)) {
                            moves.add(new Move.Horn(i, row));
                        }
                    }
                }
                case WEATHER -> moves.add(new Move.Weather(i));
                case CLEAR_WEATHER -> moves.add(new Move.ClearWeather(i));
                case SCORCH -> moves.add(new Move.Scorch(i));
                case DECOY -> {
                    for (Row row : Row.values()) {
                        for (PlacedCard placed : field.units(side, row)) {
                            if (!placed.card().hero()) {
                                moves.add(new Move.Decoy(i, placed.placementId()));
                            }
                        }
                    }
                }
                case LEADER -> {
                }
            }
        }
        return List.copyOf(moves);
    }

    private void legalUnit(Side side, int index, CardDef card, List<Move> moves) {
        if (card.ability() == Ability.MEDIC) {
            List<Integer> revive = revivable(side);
            for (Row row : card.rows()) {
                if (revive.isEmpty()) {
                    moves.add(new Move.Medic(index, row, -1, row));
                    continue;
                }
                for (int discardIndex : revive) {
                    CardDef revived = seats.get(side).discard.get(discardIndex);
                    for (Row revivedRow : revived.rows()) {
                        moves.add(new Move.Medic(index, row, discardIndex, revivedRow));
                    }
                }
            }
            return;
        }
        Side board = boardSide(side, card);
        for (Row row : card.rows()) {
            if (card.ability() == Ability.HORN && field.horn(board, row)) {
                continue;
            }
            moves.add(new Move.Unit(index, row));
        }
    }

    private void pass(Side side) {
        seats.get(side).passed = true;
        if (passedFirst == null) {
            passedFirst = side;
        }
        if (seats.get(side.other()).passed) {
            endRound();
        } else {
            active = side.other();
        }
    }

    private void playUnit(Side side, int handIndex, Row row) {
        CardDef card = requireUnit(peek(side, handIndex));
        if (card.ability() == Ability.MEDIC) {
            throw new IllegalMove("Медика играют с выбором карты из отбоя");
        }
        Side board = boardSide(side, card);
        if (!card.rows().contains(row)) {
            throw new IllegalMove("«" + card.name() + "» не встаёт в этот ряд");
        }
        if (card.ability() == Ability.HORN && field.horn(board, row)) {
            throw new IllegalMove("В этом ряду уже есть рог");
        }
        take(side, handIndex);
        placeUnit(side, board, row, card);
        finish(side);
    }

    private void playHorn(Side side, int handIndex, Row row) {
        CardDef card = requireKind(peek(side, handIndex), CardKind.HORN);
        if (field.horn(side, row)) {
            throw new IllegalMove("В этом ряду уже есть рог");
        }
        take(side, handIndex);
        field.setHorn(side, row);
        tokens.add(new Token(tokenIds++, card, side, side, row));
        finish(side);
    }

    private void playWeather(Side side, int handIndex) {
        CardDef card = requireKind(peek(side, handIndex), CardKind.WEATHER);
        take(side, handIndex);
        field.setWeather(card.affects());
        seats.get(side).discard.add(card);
        finish(side);
    }

    private void playClear(Side side, int handIndex) {
        CardDef card = requireKind(peek(side, handIndex), CardKind.CLEAR_WEATHER);
        take(side, handIndex);
        field.clearWeather();
        seats.get(side).discard.add(card);
        finish(side);
    }

    private void playScorch(Side side, int handIndex) {
        CardDef card = requireKind(peek(side, handIndex), CardKind.SCORCH);
        take(side, handIndex);
        seats.get(side).discard.add(card);
        bury(field.scorchTargets());
        finish(side);
    }

    private void playDecoy(Side side, int handIndex, int placementId) {
        CardDef card = requireKind(peek(side, handIndex), CardKind.DECOY);
        Found found = find(side, placementId);
        if (found.placed().card().hero()) {
            throw new IllegalMove("Чучело не забирает героя");
        }
        take(side, handIndex);
        field.remove(found.placed());
        owners.remove(placementId);
        seats.get(side).hand.add(found.placed().card());
        tokens.add(new Token(tokenIds++, card, side, side, found.row()));
        finish(side);
    }

    private void playMedic(Side side, int handIndex, Row row, int discardIndex, Row revivedRow) {
        CardDef card = requireUnit(peek(side, handIndex));
        if (card.ability() != Ability.MEDIC) {
            throw new IllegalMove("Это не медик");
        }
        if (!card.rows().contains(row)) {
            throw new IllegalMove("Медик не встаёт в этот ряд");
        }
        List<Integer> revive = revivable(side);
        CardDef revived = null;
        Side revivedBoard = side;
        if (!revive.isEmpty()) {
            if (!revive.contains(discardIndex)) {
                throw new IllegalMove("Эту карту медик не поднимет");
            }
            revived = seats.get(side).discard.get(discardIndex);
            if (revivedRow == null || !revived.rows().contains(revivedRow)) {
                throw new IllegalMove("Поднятой карте не подходит этот ряд");
            }
            revivedBoard = boardSide(side, revived);
            if (revived.ability() == Ability.HORN && field.horn(revivedBoard, revivedRow)) {
                throw new IllegalMove("В этом ряду уже есть рог");
            }
        }
        take(side, handIndex);
        placeUnit(side, side, row, card);
        if (revived != null) {
            seats.get(side).discard.remove(discardIndex);
            placeUnit(side, revivedBoard, revivedRow, revived);
        }
        finish(side);
    }

    private void placeUnit(Side owner, Side board, Row row, CardDef card) {
        PlacedCard placed = field.place(board, row, card);
        owners.put(placed.placementId(), owner);
        if (card.ability() == Ability.SPY || card.playsOn() == PlaysOn.OPPONENT) {
            draw(owner, card.spyDraw());
        }
        Row scorched = scorchRow(card.ability());
        if (scorched != null) {
            bury(field.rowScorchTargets(board.other(), scorched, ROW_SCORCH_AT));
        }
    }

    private void bury(List<PlacedCard> targets) {
        for (PlacedCard target : targets) {
            Side owner = owners.remove(target.placementId());
            field.remove(target);
            if (owner != null) {
                seats.get(owner).discard.add(target.card());
            }
        }
    }

    private void finish(Side side) {
        if (seats.get(side.other()).passed) {
            return;
        }
        active = side.other();
    }

    private void endRound() {
        int player = field.total(Side.PLAYER);
        int opponent = field.total(Side.OPPONENT);
        Side roundWinner = null;
        if (player > opponent) {
            roundWinner = Side.PLAYER;
        } else if (opponent > player) {
            roundWinner = Side.OPPONENT;
        } else if (isNilfgaard(Side.PLAYER) && !isNilfgaard(Side.OPPONENT)) {
            roundWinner = Side.PLAYER;
        } else if (isNilfgaard(Side.OPPONENT) && !isNilfgaard(Side.PLAYER)) {
            roundWinner = Side.OPPONENT;
        }
        if (roundWinner != null) {
            seats.get(roundWinner).gems++;
        }
        for (Side side : Side.values()) {
            for (Row row : Row.values()) {
                for (PlacedCard placed : field.units(side, row)) {
                    Side owner = owners.get(placed.placementId());
                    if (owner != null) {
                        seats.get(owner).discard.add(placed.card());
                    }
                }
            }
        }
        for (Token token : tokens) {
            seats.get(token.owner()).discard.add(token.card());
        }
        tokens.clear();
        owners.clear();
        field.clearUnits();
        for (Seat seat : seats.values()) {
            seat.passed = false;
        }
        boolean over = gems(Side.PLAYER) >= ROUND_WINS || gems(Side.OPPONENT) >= ROUND_WINS || round >= ROUNDS;
        if (roundWinner != null && seats.get(roundWinner).faction == Faction.NORTHERN_REALMS && !over) {
            draw(roundWinner, 1);
        }
        if (over) {
            phase = Phase.FINISHED;
            active = null;
            if (gems(Side.PLAYER) > gems(Side.OPPONENT)) {
                winner = Side.PLAYER;
            } else if (gems(Side.OPPONENT) > gems(Side.PLAYER)) {
                winner = Side.OPPONENT;
            }
            return;
        }
        first = roundWinner != null ? roundWinner : passedFirst;
        passedFirst = null;
        active = first;
        round++;
    }

    private boolean isNilfgaard(Side side) {
        return seats.get(side).faction == Faction.NILFGAARD;
    }

    private void draw(Side side, int count) {
        Seat seat = seats.get(side);
        for (int i = 0; i < count && !seat.deck.isEmpty(); i++) {
            seat.hand.add(seat.deck.removeFirst());
        }
    }

    private List<Integer> revivable(Side side) {
        List<Integer> indexes = new ArrayList<>();
        List<CardDef> discard = seats.get(side).discard;
        for (int i = 0; i < discard.size(); i++) {
            CardDef card = discard.get(i);
            if (card.unit() && !card.hero()) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    private Found find(Side boardSide, int placementId) {
        for (Row row : Row.values()) {
            for (PlacedCard placed : field.units(boardSide, row)) {
                if (placed.placementId() == placementId) {
                    return new Found(placed, row);
                }
            }
        }
        throw new IllegalMove("На вашей стороне нет такой карты");
    }

    private CardDef peek(Side side, int handIndex) {
        List<CardDef> hand = seats.get(side).hand;
        if (handIndex < 0 || handIndex >= hand.size()) {
            throw new IllegalMove("В руке нет такой карты");
        }
        return hand.get(handIndex);
    }

    private CardDef take(Side side, int handIndex) {
        return seats.get(side).hand.remove(handIndex);
    }

    private static CardDef requireUnit(CardDef card) {
        if (!card.unit()) {
            throw new IllegalMove("«" + card.name() + "» — не отряд");
        }
        return card;
    }

    private static CardDef requireKind(CardDef card, CardKind kind) {
        if (card.kind() != kind) {
            throw new IllegalMove("«" + card.name() + "» так сыграть нельзя");
        }
        return card;
    }

    private static Side boardSide(Side owner, CardDef card) {
        return card.playsOn() == PlaysOn.OPPONENT ? owner.other() : owner;
    }

    private static Row scorchRow(Ability ability) {
        return switch (ability) {
            case SCORCH_MELEE -> Row.MELEE;
            case SCORCH_RANGED -> Row.RANGED;
            case SCORCH_SIEGE -> Row.SIEGE;
            default -> null;
        };
    }

    private static void requireLegal(Deck deck) {
        String problem = DeckCheck.problem(deck);
        if (!problem.isEmpty()) {
            throw new IllegalArgumentException(problem);
        }
    }

    private record Found(PlacedCard placed, Row row) {
    }

    private static final class Seat {
        private final Faction faction;
        private final int leaderId;
        private final List<CardDef> deck = new ArrayList<>();
        private final List<CardDef> hand = new ArrayList<>();
        private final List<CardDef> discard = new ArrayList<>();
        private boolean passed;
        private boolean mulliganDone;
        private int mulligansUsed;
        private int gems;

        private Seat(Faction faction, int leaderId) {
            this.faction = faction;
            this.leaderId = leaderId;
        }

        private static Seat from(Deck source, Random random) {
            Seat seat = new Seat(source.faction(), source.leaderId());
            for (int id : source.cardIds()) {
                seat.deck.add(CardCatalog.get(id));
            }
            Collections.shuffle(seat.deck, random);
            return seat;
        }

        private static Seat open(Faction faction, List<CardDef> hand, List<CardDef> deck) {
            int leader = CardCatalog.leaders(faction).getFirst().id();
            Seat seat = new Seat(faction, leader);
            seat.hand.addAll(hand);
            seat.deck.addAll(deck);
            return seat;
        }
    }
}
