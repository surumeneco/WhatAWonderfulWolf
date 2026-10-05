package co.surumene.www.breeding;

import co.surumene.www.individual.WonderfulWolfIndividual;

public interface WonderfulWolfBreedingOutcome {
    record Success(
            WonderfulWolfIndividual child,
            String lineageId) implements WonderfulWolfBreedingOutcome {}

    record Fallback(
            String detail) implements WonderfulWolfBreedingOutcome {}
}
