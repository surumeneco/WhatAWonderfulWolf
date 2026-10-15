package co.surumene.www.breeding;

import co.surumene.www.individual.WonderfulWolfIndividual;

import java.util.Objects;

public record BreedingParent(
        String displayName,
        WonderfulWolfIndividual individual) {

    public BreedingParent {
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(individual, "individual");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }
}
