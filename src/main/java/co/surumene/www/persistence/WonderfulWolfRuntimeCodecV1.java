package co.surumene.www.persistence;

import co.surumene.www.domain.Ability;
import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.Mode;
import co.surumene.www.domain.PhenotypeSnapshot;
import co.surumene.www.individual.*;
import co.surumene.wgl.api.DiploidGenome;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class WonderfulWolfRuntimeCodecV1 {
    private static final int MAGIC = 0x57575254; // WWRT
    public static final int CONTAINER_VERSION = 2;

    public byte[] encode(WonderfulWolfIndividual individual) {
        Objects.requireNonNull(individual, "individual");
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buffer);

            out.writeInt(MAGIC);
            out.writeByte(CONTAINER_VERSION);
            writeOptionalUuid(out, individual.ownerId());
            out.writeLong(individual.adultBiologicalTime());
            writeString(out, individual.mode().name());
            writeOptionalUuid(out, individual.commanderId());
            writeString(out, individual.actionDistance().name());
            writeOptionalPosition(out, individual.waitLocation());
            writeAffection(out, individual.affection());
            writeOptionalItem(out, individual.weapon());
            writeInventory(out, individual.inventory());
            out.writeInt(individual.generation());
            writePedigree(out, individual.pedigree());
            writeAbilityOverrides(out, individual.adminAbilityOverrides());

            out.flush();
            return buffer.toByteArray();
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    public WonderfulWolfIndividual decode(
            byte[] bytes,
            DiploidGenome genome,
            PhenotypeSnapshot phenotypeSnapshot) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(genome, "genome");
        Objects.requireNonNull(phenotypeSnapshot, "phenotypeSnapshot");

        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            if (in.readInt() != MAGIC) {
                throw new PersistenceCodecException("invalid runtime state magic");
            }
            int version = in.readUnsignedByte();
            if (version != 1 && version != CONTAINER_VERSION) {
                throw new PersistenceCodecException("unsupported runtime state container version: " + version);
            }

            Optional<UUID> ownerId = readOptionalUuid(in);
            long adultBiologicalTime = in.readLong();
            Mode mode = readEnum(in, Mode.class);
            Optional<UUID> commanderId = readOptionalUuid(in);
            ActionDistance actionDistance = readEnum(in, ActionDistance.class);
            Optional<WorldPosition> waitLocation = readOptionalPosition(in);
            Map<UUID, Long> affection = readAffection(in);
            Optional<ItemStackSnapshot> weapon = readOptionalItem(in);
            Map<Integer, ItemStackSnapshot> inventory = readInventory(in);
            int generation = in.readInt();
            PedigreeSnapshot pedigree = readPedigree(in);
            Map<Ability, Double> overrides = version >= 2
                    ? readAbilityOverrides(in) : Map.of();

            if (in.available() != 0) {
                throw new PersistenceCodecException("trailing bytes are not allowed in runtime state");
            }

            return new WonderfulWolfIndividual(
                    genome,
                    phenotypeSnapshot,
                    ownerId,
                    adultBiologicalTime,
                    mode,
                    commanderId,
                    actionDistance,
                    waitLocation,
                    affection,
                    weapon,
                    inventory,
                    generation,
                    pedigree,
                    overrides);
        } catch (PersistenceCodecException e) {
            throw e;
        } catch (EOFException e) {
            throw new PersistenceCodecException("truncated runtime state", e);
        } catch (IOException e) {
            throw new PersistenceCodecException("failed to decode runtime state", e);
        } catch (IllegalArgumentException e) {
            throw new PersistenceCodecException("malformed runtime state: " + e.getMessage(), e);
        }
    }

    private static void writeAbilityOverrides(
            DataOutputStream out, Map<Ability, Double> overrides) throws IOException {
        out.writeByte(overrides.size());
        for (Ability ability : Ability.values()) {
            if (!overrides.containsKey(ability)) continue;
            writeString(out, ability.name());
            out.writeDouble(overrides.get(ability));
        }
    }

    private static Map<Ability, Double> readAbilityOverrides(DataInputStream in)
            throws IOException {
        int count = in.readUnsignedByte();
        if (count > Ability.values().length) {
            throw new PersistenceCodecException("too many ability overrides");
        }
        EnumMap<Ability, Double> values = new EnumMap<>(Ability.class);
        for (int i = 0; i < count; i++) {
            Ability ability = readEnum(in, Ability.class);
            double value = in.readDouble();
            if (values.putIfAbsent(ability, value) != null) {
                throw new PersistenceCodecException("duplicate ability override " + ability);
            }
        }
        return values;
    }

    private static void writeOptionalUuid(DataOutputStream out, Optional<UUID> value) throws IOException {
        writeFlag(out, value.isPresent());
        if (value.isPresent()) writeUuid(out, value.orElseThrow());
    }

    private static Optional<UUID> readOptionalUuid(DataInputStream in) throws IOException {
        return readFlag(in) ? Optional.of(readUuid(in)) : Optional.empty();
    }

    private static void writeUuid(DataOutputStream out, UUID value) throws IOException {
        out.writeLong(value.getMostSignificantBits());
        out.writeLong(value.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }

    private static void writeOptionalPosition(
            DataOutputStream out,
            Optional<WorldPosition> position) throws IOException {
        writeFlag(out, position.isPresent());
        if (position.isEmpty()) return;
        WorldPosition value = position.orElseThrow();
        writeUuid(out, value.worldId());
        out.writeDouble(value.x());
        out.writeDouble(value.y());
        out.writeDouble(value.z());
    }

    private static Optional<WorldPosition> readOptionalPosition(DataInputStream in) throws IOException {
        if (!readFlag(in)) return Optional.empty();
        return Optional.of(new WorldPosition(
                readUuid(in),
                in.readDouble(),
                in.readDouble(),
                in.readDouble()));
    }

    private static void writeAffection(DataOutputStream out, Map<UUID, Long> affection) throws IOException {
        List<Map.Entry<UUID, Long>> entries = new ArrayList<>(affection.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        out.writeInt(entries.size());
        for (Map.Entry<UUID, Long> entry : entries) {
            writeUuid(out, entry.getKey());
            out.writeLong(entry.getValue());
        }
    }

    private static Map<UUID, Long> readAffection(DataInputStream in) throws IOException {
        int count = readCount(in, 24, "affection");
        Map<UUID, Long> affection = new HashMap<>();
        for (int i = 0; i < count; i++) {
            UUID playerId = readUuid(in);
            long value = in.readLong();
            if (affection.put(playerId, value) != null) {
                throw new PersistenceCodecException("duplicate affection player: " + playerId);
            }
        }
        return affection;
    }

    private static void writeOptionalItem(
            DataOutputStream out,
            Optional<ItemStackSnapshot> item) throws IOException {
        writeFlag(out, item.isPresent());
        if (item.isPresent()) writeItem(out, item.orElseThrow());
    }

    private static Optional<ItemStackSnapshot> readOptionalItem(DataInputStream in) throws IOException {
        return readFlag(in) ? Optional.of(readItem(in)) : Optional.empty();
    }

    private static void writeInventory(
            DataOutputStream out,
            Map<Integer, ItemStackSnapshot> inventory) throws IOException {
        List<Map.Entry<Integer, ItemStackSnapshot>> entries = new ArrayList<>(inventory.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        out.writeInt(entries.size());
        for (Map.Entry<Integer, ItemStackSnapshot> entry : entries) {
            out.writeInt(entry.getKey());
            writeItem(out, entry.getValue());
        }
    }

    private static Map<Integer, ItemStackSnapshot> readInventory(DataInputStream in) throws IOException {
        int count = readCount(in, 9, "inventory");
        Map<Integer, ItemStackSnapshot> inventory = new HashMap<>();
        for (int i = 0; i < count; i++) {
            int slot = in.readInt();
            ItemStackSnapshot item = readItem(in);
            if (inventory.put(slot, item) != null) {
                throw new PersistenceCodecException("duplicate inventory slot: " + slot);
            }
        }
        return inventory;
    }

    private static void writeItem(DataOutputStream out, ItemStackSnapshot item) throws IOException {
        byte[] bytes = item.bytes();
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static ItemStackSnapshot readItem(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > in.available()) {
            throw new PersistenceCodecException("invalid serialized item length: " + length);
        }
        return new ItemStackSnapshot(in.readNBytes(length));
    }

    private static void writePedigree(DataOutputStream out, PedigreeSnapshot pedigree) throws IOException {
        writeOptionalParent(out, pedigree.parentA());
        writeOptionalParent(out, pedigree.parentB());
        writeOptionalAncestor(out, pedigree.grandparentAA());
        writeOptionalAncestor(out, pedigree.grandparentAB());
        writeOptionalAncestor(out, pedigree.grandparentBA());
        writeOptionalAncestor(out, pedigree.grandparentBB());
    }

    private static PedigreeSnapshot readPedigree(DataInputStream in) throws IOException {
        return new PedigreeSnapshot(
                readOptionalParent(in),
                readOptionalParent(in),
                readOptionalAncestor(in),
                readOptionalAncestor(in),
                readOptionalAncestor(in),
                readOptionalAncestor(in));
    }

    private static void writeOptionalParent(
            DataOutputStream out,
            Optional<ParentSnapshot> parent) throws IOException {
        writeFlag(out, parent.isPresent());
        if (parent.isEmpty()) return;
        ParentSnapshot value = parent.orElseThrow();
        writeAncestor(out, value.ancestor());
        writeString(out, value.personalityId());
        out.writeByte(value.expressedTraitIds().size());
        for (String traitId : value.expressedTraitIds()) writeString(out, traitId);
        writeFlag(out, value.divineLineageExpressed());
    }

    private static Optional<ParentSnapshot> readOptionalParent(DataInputStream in) throws IOException {
        if (!readFlag(in)) return Optional.empty();
        AncestorSnapshot ancestor = readAncestor(in);
        String personalityId = readString(in);
        int traitCount = in.readUnsignedByte();
        if (traitCount > 2) {
            throw new PersistenceCodecException("parent snapshot contains too many expressed traits");
        }
        List<String> traitIds = new ArrayList<>(traitCount);
        for (int i = 0; i < traitCount; i++) traitIds.add(readString(in));
        boolean divineLineageExpressed = readFlag(in);
        return Optional.of(new ParentSnapshot(
                ancestor,
                personalityId,
                traitIds,
                divineLineageExpressed));
    }

    private static void writeOptionalAncestor(
            DataOutputStream out,
            Optional<AncestorSnapshot> ancestor) throws IOException {
        writeFlag(out, ancestor.isPresent());
        if (ancestor.isPresent()) writeAncestor(out, ancestor.orElseThrow());
    }

    private static Optional<AncestorSnapshot> readOptionalAncestor(DataInputStream in) throws IOException {
        return readFlag(in) ? Optional.of(readAncestor(in)) : Optional.empty();
    }

    private static void writeAncestor(DataOutputStream out, AncestorSnapshot ancestor) throws IOException {
        writeString(out, ancestor.displayName());
        out.writeInt(ancestor.generation());
        writeString(out, ancestor.lineageId());
    }

    private static AncestorSnapshot readAncestor(DataInputStream in) throws IOException {
        return new AncestorSnapshot(readString(in), in.readInt(), readString(in));
    }

    private static void writeFlag(DataOutputStream out, boolean value) throws IOException {
        out.writeByte(value ? 1 : 0);
    }

    private static boolean readFlag(DataInputStream in) throws IOException {
        int value = in.readUnsignedByte();
        if (value == 0) return false;
        if (value == 1) return true;
        throw new PersistenceCodecException("invalid boolean flag in runtime state: " + value);
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
        byte[] bytes = Objects.requireNonNull(value, "value").getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > in.available()) {
            throw new PersistenceCodecException("invalid string length in runtime state: " + length);
        }
        return new String(in.readNBytes(length), StandardCharsets.UTF_8);
    }

    private static int readCount(DataInputStream in, int minimumEntryBytes, String label)
            throws IOException {
        int count = in.readInt();
        if (count < 0 || count > in.available() / minimumEntryBytes) {
            throw new PersistenceCodecException("invalid " + label + " entry count: " + count);
        }
        return count;
    }
}
