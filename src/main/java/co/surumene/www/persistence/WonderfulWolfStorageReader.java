package co.surumene.www.persistence;

import co.surumene.www.WhatAWonderfulWolfPlugin;
import co.surumene.www.individual.WonderfulWolfIndividual;
import co.surumene.www.individual.ItemStackSnapshot;
import co.surumene.www.persistence.WonderfulWolfLoadedIndividuals;
import co.surumene.wcl.api.inventory.StorageReadResult;
import co.surumene.wcl.api.inventory.StorageSnapshot;
import co.surumene.wcl.api.inventory.StoredItem;
import co.surumene.wcl.plugin.inventory.WonderfulStorageReader;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Wolf;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

/** Owner-specific read-only provider; no chunk loading or PDC writes. */
public final class WonderfulWolfStorageReader implements WonderfulStorageReader {
    private final WhatAWonderfulWolfPlugin plugin;
    private final WonderfulWolfLoadedIndividuals loaded;
    private final WonderfulWolfRuntimeCodecV1 runtimeCodec = new WonderfulWolfRuntimeCodecV1();

    public WonderfulWolfStorageReader(WhatAWonderfulWolfPlugin plugin,
                                     WonderfulWolfLoadedIndividuals loaded) {
        this.plugin=Objects.requireNonNull(plugin);
        this.loaded=Objects.requireNonNull(loaded);
    }

    @Override public String providerId(){return "www";}

    @Override public StorageReadResult readLoaded(UUID entityId) {
        Objects.requireNonNull(entityId);
        if(!plugin.isEnabled())return new StorageReadResult.Failure("DISABLED","WWW is disabled");
        Optional<Wolf> wolf=loaded.entity(entityId);
        if(wolf.isEmpty())return new StorageReadResult.NotFound();
        Optional<WonderfulWolfIndividual> individual=loaded.find(entityId);
        if(individual.isEmpty())return new StorageReadResult.Failure("MISSING_DATA","Missing WWW loaded state");
        var entity=wolf.orElseThrow();
        var place=entity.getLocation();
        return snapshot(entityId,entity.getWorld().getUID(),
                Optional.of(new StorageSnapshot.Position(place.getX(),place.getY(),place.getZ())),
                individual.orElseThrow().ownerId(),individual.orElseThrow().commanderId(),
                individual.orElseThrow().weapon(),individual.orElseThrow().inventory());
    }

    @Override public StorageReadResult readStored(UUID entityId,UUID worldId,
            Optional<StorageSnapshot.Position> position,PersistentDataContainer pdc) {
        Objects.requireNonNull(entityId);
        Objects.requireNonNull(worldId);
        Objects.requireNonNull(position);
        Objects.requireNonNull(pdc);
        if(!plugin.isEnabled())return new StorageReadResult.Failure("DISABLED","WWW is disabled");
        try {
            if(!WonderfulWolfPdcPersistence.MARKER_VALUE.equals(
                    pdc.get(key(WonderfulWolfPdcPersistence.TYPE_KEY),PersistentDataType.STRING)))
                return new StorageReadResult.NotFound();
            Integer schema=pdc.get(key(WonderfulWolfPdcPersistence.SCHEMA_VERSION_KEY),
                    PersistentDataType.INTEGER);
            if(schema==null)return new StorageReadResult.Failure("MISSING_DATA","Missing WWW schema version");
            if(schema!=WonderfulWolfPdcPersistence.SCHEMA_VERSION &&
                    schema!=WonderfulWolfPdcPersistence.EXTERNAL_SCHEMA_VERSION)
                return new StorageReadResult.Failure("UNSUPPORTED_SCHEMA","Unsupported WWW schema "+schema);
            if(schema==WonderfulWolfPdcPersistence.EXTERNAL_SCHEMA_VERSION) {
                String ref=pdc.get(key(WonderfulWolfPdcPersistence.GENOME_REFERENCE_KEY),
                        PersistentDataType.STRING);
                if(ref==null)return new StorageReadResult.Failure("MISSING_DATA","Missing WWW genome_ref");
                UUID.fromString(ref);
            }
            byte[] runtime=pdc.get(key(WonderfulWolfPdcPersistence.RUNTIME_KEY),
                    PersistentDataType.BYTE_ARRAY);
            if(runtime==null || runtime.length==0)
                return new StorageReadResult.Failure("MISSING_DATA","Missing WWW runtime state");
            var decoded=runtimeCodec.decodeStorage(runtime);
            return snapshot(entityId,worldId,position,decoded.ownerId(),
                    decoded.commanderId(),decoded.weapon(),decoded.inventory());
        } catch(RuntimeException problem) {
            return new StorageReadResult.Failure("CORRUPT_DATA",
                    problem.getClass().getSimpleName()+": "+String.valueOf(problem.getMessage()));
        }
    }

    private StorageReadResult snapshot(UUID entityId,UUID worldId,
            Optional<StorageSnapshot.Position> position,
            Optional<UUID> owner,Optional<UUID> commander,
            Optional<ItemStackSnapshot> weapon,Map<Integer,ItemStackSnapshot> inventory) {
        Map<String,UUID> relations=new HashMap<>();
        owner.ifPresent(id->relations.put("owner",id));
        commander.ifPresent(id->relations.put("commander",id));
        List<StoredItem> items=new ArrayList<>();
        weapon.ifPresent(item->items.add(new StoredItem("weapon",item.bytes())));
        new TreeMap<>(inventory).forEach((slot,item)->items.add(new StoredItem("cargo:"+slot,item.bytes())));
        return new StorageReadResult.Success(
                new StorageSnapshot(entityId,providerId(),worldId,position,relations,items));
    }
    private NamespacedKey key(String name){return new NamespacedKey(plugin,name);}
}
