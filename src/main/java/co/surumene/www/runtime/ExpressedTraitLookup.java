package co.surumene.www.runtime;

import co.surumene.www.domain.ExpressedTrait;
import co.surumene.www.domain.Trait;
import co.surumene.www.domain.TraitStrength;
import co.surumene.www.individual.WonderfulWolfIndividual;

import java.util.Optional;

final class ExpressedTraitLookup {
    private ExpressedTraitLookup() {}

    static Optional<TraitStrength> strength(
            WonderfulWolfIndividual individual,
            Trait trait) {
        return individual.phenotypeSnapshot().expressedTraits().stream()
                .filter(entry -> entry.trait() == trait)
                .map(ExpressedTrait::strength)
                .findFirst();
    }

    static boolean has(
            WonderfulWolfIndividual individual,
            Trait trait) {
        return strength(individual, trait).isPresent();
    }
}
