package co.surumene.www.persistence;

import co.surumene.www.domain.*;
import co.surumene.wgl.api.DecoderIdentity;
import co.surumene.wgl.api.ProfileDescriptor;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class PhenotypeSnapshotCodecV1Test {
    private final PhenotypeSnapshotCodecV1 codec = new PhenotypeSnapshotCodecV1();

    @Test
    void roundTripsTheCompletePhenotypeSnapshot() {
        PhenotypeSnapshot source = snapshot();

        byte[] encoded = codec.encode(source);
        PhenotypeSnapshot restored = codec.decode(encoded);

        assertEquals(source, restored);
    }

    @Test
    void rejectsUnsupportedContainerVersion() {
        byte[] encoded = codec.encode(snapshot());
        encoded[4] = 2;

        PersistenceCodecException error = assertThrows(
                PersistenceCodecException.class,
                () -> codec.decode(encoded));

        assertTrue(error.getMessage().contains("version"));
    }

    @Test
    void rejectsTrailingBytesInsteadOfSilentlyIgnoringThem() {
        byte[] encoded = codec.encode(snapshot());

        assertThrows(
                PersistenceCodecException.class,
                () -> codec.decode(Arrays.copyOf(encoded, encoded.length + 1)));
    }

    private static PhenotypeSnapshot snapshot() {
        EnumMap<Ability, Double> abilities = new EnumMap<>(Ability.class);
        int i = 0;
        for (Ability ability : Ability.values()) abilities.put(ability, 0.1 + 0.1 * i++);

        EnumMap<PersonalityFactor, Double> personalityFactors = new EnumMap<>(PersonalityFactor.class);
        i = 0;
        for (PersonalityFactor factor : PersonalityFactor.values()) {
            personalityFactors.put(factor, 0.15 + 0.1 * i++);
        }

        EnumMap<DevelopmentFactor, Double> developmentFactors = new EnumMap<>(DevelopmentFactor.class);
        i = 0;
        for (DevelopmentFactor factor : DevelopmentFactor.values()) {
            developmentFactors.put(factor, 0.2 + 0.1 * i++);
        }

        DecoderIdentity identity = new DecoderIdentity(
                7,
                fingerprint(0x10),
                new ProfileDescriptor("wonderful-wolf", 3, fingerprint(0x40)));

        return new PhenotypeSnapshot(
                identity,
                abilities,
                new RelationshipPerformance(-17, 13),
                personalityFactors,
                Personality.VALIANT,
                List.of(
                        new ExpressedTrait(Trait.WATCHMAN, TraitStrength.WEAK),
                        new ExpressedTrait(Trait.GUARDIAN, TraitStrength.STRONG)),
                developmentFactors,
                List.of(
                        new InjuryPhenotype(Ability.MOVEMENT_SPEED, 123.25, 2.5),
                        new InjuryPhenotype(Ability.HEALTH, 456.75, 4.0)),
                1.125,
                true);
    }

    private static byte[] fingerprint(int start) {
        byte[] bytes = new byte[32];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (start + i);
        return bytes;
    }
}
