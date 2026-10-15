package co.surumene.www.persistence;

import co.surumene.www.domain.*;
import co.surumene.wgl.api.DecoderIdentity;
import co.surumene.wgl.api.ProfileDescriptor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PhenotypeSnapshotCodecV1 {
    private static final int MAGIC = 0x57575048; // WWPH
    public static final int CONTAINER_VERSION = 1;
    private static final int FINGERPRINT_LENGTH = 32;
    private static final int MAX_STRING_BYTES = 1024;

    public byte[] encode(PhenotypeSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buffer);

            out.writeInt(MAGIC);
            out.writeByte(CONTAINER_VERSION);
            writeDecoderIdentity(out, snapshot.decoderIdentity());
            writeEnumScores(out, Ability.values(), snapshot.abilities());
            out.writeInt(snapshot.relationshipPerformance().initialAffection());
            out.writeInt(snapshot.relationshipPerformance().affectionDelta());
            writeEnumScores(out, PersonalityFactor.values(), snapshot.personalityFactors());
            writeString(out, snapshot.personality().name());

            out.writeByte(snapshot.expressedTraits().size());
            for (ExpressedTrait trait : snapshot.expressedTraits()) {
                writeString(out, trait.trait().name());
                writeString(out, trait.strength().name());
            }

            writeEnumScores(out, DevelopmentFactor.values(), snapshot.developmentFactors());

            out.writeByte(snapshot.injuries().size());
            for (InjuryPhenotype injury : snapshot.injuries()) {
                writeString(out, injury.ability().name());
                out.writeDouble(injury.onsetGameDay());
                out.writeDouble(injury.severityRank());
            }

            out.writeDouble(snapshot.divineLineageTotalScore());
            out.writeBoolean(snapshot.divineLineageExpressed());
            out.flush();
            return buffer.toByteArray();
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public PhenotypeSnapshot decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            if (in.readInt() != MAGIC) {
                throw new PersistenceCodecException("invalid phenotype snapshot magic");
            }
            int version = in.readUnsignedByte();
            if (version != CONTAINER_VERSION) {
                throw new PersistenceCodecException("unsupported phenotype snapshot container version: " + version);
            }

            DecoderIdentity identity = readDecoderIdentity(in);
            EnumMap<Ability, Double> abilities = readEnumScores(in, Ability.class);
            RelationshipPerformance relationshipPerformance =
                    new RelationshipPerformance(in.readInt(), in.readInt());
            EnumMap<PersonalityFactor, Double> personalityFactors =
                    readEnumScores(in, PersonalityFactor.class);
            Personality personality = readEnum(in, Personality.class);

            int traitCount = in.readUnsignedByte();
            if (traitCount > 2) {
                throw new PersistenceCodecException("phenotype snapshot contains too many expressed traits");
            }
            List<ExpressedTrait> traits = new ArrayList<>(traitCount);
            for (int i = 0; i < traitCount; i++) {
                traits.add(new ExpressedTrait(
                        readEnum(in, Trait.class),
                        readEnum(in, TraitStrength.class)));
            }

            EnumMap<DevelopmentFactor, Double> developmentFactors =
                    readEnumScores(in, DevelopmentFactor.class);

            int injuryCount = in.readUnsignedByte();
            if (injuryCount > Ability.values().length) {
                throw new PersistenceCodecException("phenotype snapshot contains too many injuries");
            }
            List<InjuryPhenotype> injuries = new ArrayList<>(injuryCount);
            for (int i = 0; i < injuryCount; i++) {
                injuries.add(new InjuryPhenotype(
                        readEnum(in, Ability.class),
                        in.readDouble(),
                        in.readDouble()));
            }

            double divineLineageTotalScore = in.readDouble();
            boolean divineLineageExpressed = in.readBoolean();

            if (in.available() != 0) {
                throw new PersistenceCodecException("trailing bytes are not allowed in phenotype snapshot");
            }

            return new PhenotypeSnapshot(
                    identity,
                    abilities,
                    relationshipPerformance,
                    personalityFactors,
                    personality,
                    traits,
                    developmentFactors,
                    injuries,
                    divineLineageTotalScore,
                    divineLineageExpressed);
        } catch (PersistenceCodecException e) {
            throw e;
        } catch (EOFException e) {
            throw new PersistenceCodecException("truncated phenotype snapshot", e);
        } catch (IOException e) {
            throw new PersistenceCodecException("failed to decode phenotype snapshot", e);
        } catch (IllegalArgumentException e) {
            throw new PersistenceCodecException("malformed phenotype snapshot: " + e.getMessage(), e);
        }
    }

    private static void writeDecoderIdentity(DataOutputStream out, DecoderIdentity identity) throws IOException {
        out.writeInt(identity.engineRevision());
        writeFingerprint(out, identity.engineDecoderConfigFingerprint());
        ProfileDescriptor profile = identity.profileDescriptor();
        writeString(out, profile.profileId());
        out.writeInt(profile.profileVersion());
        writeFingerprint(out, profile.semanticFingerprint());
    }

    private static DecoderIdentity readDecoderIdentity(DataInputStream in) throws IOException {
        int engineRevision = in.readInt();
        byte[] engineFingerprint = readFingerprint(in);
        String profileId = readString(in);
        int profileVersion = in.readInt();
        byte[] semanticFingerprint = readFingerprint(in);
        return new DecoderIdentity(
                engineRevision,
                engineFingerprint,
                new ProfileDescriptor(profileId, profileVersion, semanticFingerprint));
    }

    private static void writeFingerprint(DataOutputStream out, byte[] fingerprint) throws IOException {
        if (fingerprint.length != FINGERPRINT_LENGTH) {
            throw new IllegalArgumentException("fingerprint must contain 32 bytes");
        }
        out.write(fingerprint);
    }

    private static byte[] readFingerprint(DataInputStream in) throws IOException {
        byte[] fingerprint = in.readNBytes(FINGERPRINT_LENGTH);
        if (fingerprint.length != FINGERPRINT_LENGTH) {
            throw new EOFException("truncated fingerprint");
        }
        return fingerprint;
    }

    private static <E extends Enum<E>> void writeEnumScores(
            DataOutputStream out,
            E[] values,
            Map<E, Double> scores) throws IOException {
        out.writeByte(values.length);
        for (E value : values) {
            writeString(out, value.name());
            out.writeDouble(scores.get(value));
        }
    }

    private static <E extends Enum<E>> EnumMap<E, Double> readEnumScores(
            DataInputStream in,
            Class<E> enumType) throws IOException {
        E[] values = enumType.getEnumConstants();
        int count = in.readUnsignedByte();
        if (count != values.length) {
            throw new PersistenceCodecException(
                    "unexpected " + enumType.getSimpleName() + " score count: " + count);
        }

        EnumMap<E, Double> scores = new EnumMap<>(enumType);
        for (int i = 0; i < count; i++) {
            E key = readEnum(in, enumType);
            double value = in.readDouble();
            if (scores.put(key, value) != null) {
                throw new PersistenceCodecException(
                        "duplicate " + enumType.getSimpleName() + " score: " + key);
            }
        }
        return scores;
    }

    private static <E extends Enum<E>> E readEnum(DataInputStream in, Class<E> enumType)
            throws IOException {
        String name = readString(in);
        try {
            return Enum.valueOf(enumType, name);
        } catch (IllegalArgumentException e) {
            throw new PersistenceCodecException(
                    "unknown " + enumType.getSimpleName() + " value: " + name, e);
        }
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] encoded = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        if (encoded.length > MAX_STRING_BYTES) {
            throw new IllegalArgumentException("string is too long for phenotype snapshot");
        }
        out.writeInt(encoded.length);
        out.write(encoded);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > MAX_STRING_BYTES || length > in.available()) {
            throw new PersistenceCodecException("invalid string length in phenotype snapshot: " + length);
        }
        byte[] encoded = in.readNBytes(length);
        return new String(encoded, StandardCharsets.UTF_8);
    }
}
