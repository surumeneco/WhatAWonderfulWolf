package co.surumene.www.runtime;

import co.surumene.www.ability.AbilityScale;
import co.surumene.www.behavior.AffectionPolicy;
import co.surumene.www.domain.Ability;
import co.surumene.www.domain.Trait;
import co.surumene.www.individual.ItemStackSnapshot;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.www.ui.WonderfulWolfInventoryHolder;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class WonderfulWolfScavengerRuntime {
    private static final double COLLECTION_RADIUS = 2.0;

    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfAbilityRuntime abilities;
    private final Server server;

    WonderfulWolfScavengerRuntime(
            WonderfulWolfLoadedIndividuals loaded,
            WonderfulWolfAbilityRuntime abilities,
            Server server) {
        this.loaded = Objects.requireNonNull(loaded, "loaded");
        this.abilities = Objects.requireNonNull(abilities, "abilities");
        this.server = Objects.requireNonNull(server, "server");
    }

    void tick(
            Wolf wolf,
            WonderfulWolfIndividual individual) {
        if (!ExpressedTraitLookup.has(individual, Trait.SCAVENGER)
                || managementInventoryOpen(wolf.getUniqueId())) {
            return;
        }

        int capacity = capacity(wolf, individual);
        if (capacity <= 0
                || !WonderfulWolfCargoStore.hasEmptySlot(
                        individual.inventory(),
                        capacity)) {
            return;
        }

        Map<Integer, ItemStackSnapshot> cargo =
                new HashMap<>(individual.inventory());
        WonderfulWolfIndividual updatedIndividual = individual;
        boolean cargoChanged = false;
        boolean affectionChanged = false;
        for (Entity entity : wolf.getWorld().getNearbyEntities(
                wolf.getLocation(),
                COLLECTION_RADIUS,
                COLLECTION_RADIUS,
                COLLECTION_RADIUS)) {
            if (!(entity instanceof Item dropped)
                    || dropped.isDead()
                    || !dropped.isValid()) {
                continue;
            }

            ItemStack stack = dropped.getItemStack();
            if (stack.isEmpty()) {
                continue;
            }

            if (WonderfulWolfFoodRuntime.canUseForRecovery(wolf, stack)) {
                WonderfulWolfFoodRuntime.consumeOne(wolf, dropped, stack);
                UUID commander =
                        updatedIndividual.commanderId().orElse(null);
                if (commander != null) {
                    updatedIndividual =
                            AffectionPolicy.reward(
                                    updatedIndividual,
                                    commander);
                    affectionChanged = true;
                }
                continue;
            }

            int remaining =
                    WonderfulWolfCargoStore.store(
                            cargo,
                            capacity,
                            stack);
            if (remaining == stack.getAmount()) {
                continue;
            }

            cargoChanged = true;
            if (remaining == 0) {
                dropped.remove();
            } else {
                ItemStack rest = stack.clone();
                rest.setAmount(remaining);
                dropped.setItemStack(rest);
            }

            if (!WonderfulWolfCargoStore.hasEmptySlot(
                    cargo,
                    capacity)) {
                break;
            }
        }

        if (!cargoChanged && !affectionChanged) {
            return;
        }

        loaded.saveAndRegister(
                wolf,
                updatedIndividual.withStorage(
                        updatedIndividual.weapon(),
                        cargo));
    }

    private boolean managementInventoryOpen(UUID wolfId) {
        for (Player player : server.getOnlinePlayers()) {
            if (player.getOpenInventory()
                            .getTopInventory()
                            .getHolder()
                    instanceof WonderfulWolfInventoryHolder holder
                    && holder.wolfId().equals(wolfId)) {
                return true;
            }
        }
        return false;
    }

    private int capacity(
            Wolf wolf,
            WonderfulWolfIndividual individual) {
        return abilities.find(wolf.getUniqueId())
                .map(NonAttributeAbilityAdapter::inventorySlots)
                .orElseGet(() -> (int) AbilityScale.finalizeEffective(
                        Ability.INVENTORY,
                        AbilityScale.toCanonical(
                                Ability.INVENTORY,
                                individual.phenotypeSnapshot()
                                        .abilities()
                                        .get(Ability.INVENTORY))));
    }
}
