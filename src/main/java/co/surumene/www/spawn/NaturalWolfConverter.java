package co.surumene.www.spawn;

import org.bukkit.entity.Wolf;

@FunctionalInterface
public interface NaturalWolfConverter {
    WonderfulWolfEntityCreationResult convert(Wolf wolf, long seed);
}
