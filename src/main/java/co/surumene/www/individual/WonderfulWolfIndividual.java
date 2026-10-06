package co.surumene.www.individual;

import co.surumene.www.domain.ActionDistance;
import co.surumene.www.domain.Mode;
import co.surumene.www.domain.PhenotypeSnapshot;
import co.surumene.wgl.api.DiploidGenome;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record WonderfulWolfIndividual(
        DiploidGenome genome,
        PhenotypeSnapshot phenotypeSnapshot,
        Optional<UUID> ownerId,
        long adultBiologicalTime,
        Mode mode,
        Optional<UUID> commanderId,
        ActionDistance actionDistance,
        Optional<WorldPosition> waitLocation,
        Map<UUID, Long> affection,
        Optional<ItemStackSnapshot> weapon,
        Map<Integer, ItemStackSnapshot> inventory,
        int generation,
        PedigreeSnapshot pedigree) {

    public static final long MAX_ABSOLUTE_AFFECTION = 1_000_000_000_000_000L;

    public WonderfulWolfIndividual withAdultBiologicalTime(long value) {
        return new WonderfulWolfIndividual(
                genome, phenotypeSnapshot, ownerId, value, mode, commanderId,
                actionDistance, waitLocation, affection, weapon, inventory,
                generation, pedigree);
    }

    public WonderfulWolfIndividual withAffection(Map<UUID, Long> value) {
        return new WonderfulWolfIndividual(
                genome, phenotypeSnapshot, ownerId, adultBiologicalTime, mode,
                commanderId, actionDistance, waitLocation, value, weapon,
                inventory, generation, pedigree);
    }

    public WonderfulWolfIndividual withCommandState(
            Mode newMode,
            Optional<UUID> newCommanderId,
            ActionDistance newActionDistance,
            Optional<WorldPosition> newWaitLocation) {
        return new WonderfulWolfIndividual(
                genome, phenotypeSnapshot, ownerId, adultBiologicalTime,
                newMode, newCommanderId, newActionDistance, newWaitLocation,
                affection, weapon, inventory, generation, pedigree);
    }

    public WonderfulWolfIndividual withStorage(
            Optional<ItemStackSnapshot> newWeapon,
            Map<Integer, ItemStackSnapshot> newInventory) {
        return new WonderfulWolfIndividual(
                genome, phenotypeSnapshot, ownerId, adultBiologicalTime, mode,
                commanderId, actionDistance, waitLocation, affection,
                Objects.requireNonNull(newWeapon, "newWeapon"),
                Objects.requireNonNull(newInventory, "newInventory"),
                generation, pedigree);
    }

    public WonderfulWolfIndividual {
        Objects.requireNonNull(genome, "genome");
        Objects.requireNonNull(phenotypeSnapshot, "phenotypeSnapshot");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(mode, "mode");
        commanderId = Objects.requireNonNull(commanderId, "commanderId");
        Objects.requireNonNull(actionDistance, "actionDistance");
        waitLocation = Objects.requireNonNull(waitLocation, "waitLocation");
        weapon = Objects.requireNonNull(weapon, "weapon");
        Objects.requireNonNull(pedigree, "pedigree");

        if (adultBiologicalTime < 0) {
            throw new IllegalArgumentException("adultBiologicalTime must be >= 0");
        }
        if (generation < 0) {
            throw new IllegalArgumentException("generation must be >= 0");
        }
        if (mode == Mode.WANDER && commanderId.isPresent()) {
            throw new IllegalArgumentException("wander mode must not have a commander");
        }
        if (mode != Mode.WANDER && commanderId.isEmpty()) {
            throw new IllegalArgumentException("commanded modes require a commander");
        }
        if (mode == Mode.WAIT && waitLocation.isEmpty()) {
            throw new IllegalArgumentException("wait mode requires a wait location");
        }

        HashMap<UUID, Long> affectionCopy = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : Objects.requireNonNull(affection, "affection").entrySet()) {
            UUID player = Objects.requireNonNull(entry.getKey(), "affection player");
            Long value = Objects.requireNonNull(entry.getValue(), "affection value");
            if (value < -MAX_ABSOLUTE_AFFECTION || value > MAX_ABSOLUTE_AFFECTION) {
                throw new IllegalArgumentException("affection value is outside the supported absolute range");
            }
            affectionCopy.put(player, value);
        }
        affection = Collections.unmodifiableMap(affectionCopy);

        HashMap<Integer, ItemStackSnapshot> inventoryCopy = new HashMap<>();
        for (Map.Entry<Integer, ItemStackSnapshot> entry :
                Objects.requireNonNull(inventory, "inventory").entrySet()) {
            Integer slot = Objects.requireNonNull(entry.getKey(), "inventory slot");
            if (slot < 0) throw new IllegalArgumentException("inventory slot must be >= 0");
            inventoryCopy.put(slot, Objects.requireNonNull(entry.getValue(), "inventory item"));
        }
        inventory = Collections.unmodifiableMap(inventoryCopy);
    }
}
