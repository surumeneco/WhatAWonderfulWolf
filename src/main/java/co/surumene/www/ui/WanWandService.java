package co.surumene.www.ui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class WanWandService {
    private final NamespacedKey itemKey;
    private final NamespacedKey recipeKey;

    public WanWandService(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.itemKey = new NamespacedKey(plugin, "wan_wand");
        this.recipeKey = new NamespacedKey(plugin, "wan_wand_recipe");
    }

    public ItemStack create() {
        ItemStack item = ItemStack.of(Material.BONE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("ワンワンド"));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(
                itemKey,
                PersistentDataType.BYTE,
                (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isWanWand(ItemStack item) {
        if (item == null || item.isEmpty() || item.getType() != Material.BONE) {
            return false;
        }
        Byte marker = item.getItemMeta()
                .getPersistentDataContainer()
                .get(itemKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    public void registerRecipe() {
        Bukkit.removeRecipe(recipeKey);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, create());
        recipe.shape("LBL");
        recipe.setIngredient('L', Material.LEATHER);
        recipe.setIngredient('B', Material.BONE);
        Bukkit.addRecipe(recipe);
    }

    public void unregisterRecipe() {
        Bukkit.removeRecipe(recipeKey);
    }
}
