package co.surumene.www.domain;

import co.surumene.wgl.api.DecoderIdentity;
import co.surumene.wgl.api.ProfileDescriptor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class PhenotypeSnapshotTest {
    @Test
    void snapshotsCompletePhenotypeIntoImmutableCollections() {
        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        for (Ability ability : Ability.values()) abilities.put(ability, 0.5);

        EnumMap<PersonalityFactor, Double> personality = new EnumMap<>(PersonalityFactor.class);
        for (PersonalityFactor factor : PersonalityFactor.values()) personality.put(factor, 0.5);

        EnumMap<DevelopmentFactor, Double> development = new EnumMap<>(DevelopmentFactor.class);
        for (DevelopmentFactor factor : DevelopmentFactor.values()) development.put(factor, 0.5);

        List<ExpressedTrait> traits = new ArrayList<>(List.of(
                new ExpressedTrait(Trait.WATCHMAN, TraitStrength.WEAK)));
        List<InjuryPhenotype> injuries = new ArrayList<>(List.of(
                new InjuryPhenotype(Ability.MOVEMENT_SPEED, 120.0, 3.5)));

        DecoderIdentity identity = new DecoderIdentity(
                1,
                new byte[32],
                new ProfileDescriptor("wonderful-wolf", 1, new byte[32]));

        PhenotypeSnapshot snapshot = new PhenotypeSnapshot(
                identity,
                abilities,
                new RelationshipPerformance(0, 10),
                personality,
                Personality.SERIOUS,
                traits,
                development,
                injuries,
                0.72,
                true);

        abilities.put(Ability.HEALTH, 0.1);
        traits.clear();
        injuries.clear();

        assertEquals(0.5, snapshot.abilities().get(Ability.HEALTH));
        assertEquals(1, snapshot.expressedTraits().size());
        assertEquals(1, snapshot.injuries().size());
        assertEquals(0.72, snapshot.divineLineageTotalScore());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.abilities().put(Ability.HEALTH, 0.2));
    }

    @Test
    void rejectsIncompleteAbilitySnapshot() {
        Map<Ability, Double> incomplete = Map.of(Ability.HEALTH, 0.5);
        EnumMap<PersonalityFactor, Double> personality = new EnumMap<>(PersonalityFactor.class);
        for (PersonalityFactor factor : PersonalityFactor.values()) personality.put(factor, 0.5);
        EnumMap<DevelopmentFactor, Double> development = new EnumMap<>(DevelopmentFactor.class);
        for (DevelopmentFactor factor : DevelopmentFactor.values()) development.put(factor, 0.5);

        DecoderIdentity identity = new DecoderIdentity(
                1,
                new byte[32],
                new ProfileDescriptor("wonderful-wolf", 1, new byte[32]));

        assertThrows(IllegalArgumentException.class, () -> new PhenotypeSnapshot(
                identity,
                incomplete,
                new RelationshipPerformance(0, 10),
                personality,
                Personality.SERIOUS,
                List.of(),
                development,
                List.of(),
                0.0,
                false));
    }
}
