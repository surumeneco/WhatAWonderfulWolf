package co.surumene.www.behavior;

import co.surumene.www.domain.*;
import co.surumene.www.individual.*;
import co.surumene.wgl.api.*;

import java.util.*;

final class BehaviorTestIndividuals {
    private BehaviorTestIndividuals() {}

    static WonderfulWolfIndividual individual(
            int initialAffection,
            int affectionDelta) {
        BitSequence bits = BitSequence.fromBits("10101010");
        DiploidGenome genome = new DiploidGenome(
                1,
                List.of(new ChromosomePair(bits, bits)));

        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) abilities.put(ability, 0.5);
        EnumMap<PersonalityFactor, Double> personality =
                new EnumMap<>(PersonalityFactor.class);
        for (PersonalityFactor factor : PersonalityFactor.values()) {
            personality.put(factor, 0.5);
        }
        EnumMap<DevelopmentFactor, Double> development =
                new EnumMap<>(DevelopmentFactor.class);
        for (DevelopmentFactor factor : DevelopmentFactor.values()) {
            development.put(factor, 0.5);
        }

        PhenotypeSnapshot phenotype = new PhenotypeSnapshot(
                new DecoderIdentity(
                        1,
                        new byte[32],
                        new ProfileDescriptor("wonderful-wolf", 1, new byte[32])),
                abilities,
                new RelationshipPerformance(initialAffection, affectionDelta),
                personality,
                Personality.SERIOUS,
                List.of(),
                development,
                List.of(),
                0.0,
                false);

        return new WonderfulWolfIndividual(
                genome,
                phenotype,
                Optional.empty(),
                0L,
                Mode.WANDER,
                Optional.empty(),
                ActionDistance.NORMAL,
                Optional.empty(),
                Map.of(),
                Optional.empty(),
                Map.of(),
                0,
                PedigreeSnapshot.founder());
    }
}
