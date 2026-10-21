package gwent.rules;

import gwent.catalog.CardDef;

/** Одна выложенная копия. Две «пехтуры» — два разных экземпляра одной карты. */
public record PlacedCard(int placementId, CardDef card) {
}
