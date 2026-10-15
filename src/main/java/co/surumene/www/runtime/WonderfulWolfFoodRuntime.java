package co.surumene.www.runtime;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.FoodProperties;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Item;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;

final class WonderfulWolfFoodRuntime {
    private WonderfulWolfFoodRuntime() {}

    static boolean canUseForRecovery(
            Wolf wolf,
            ItemStack stack) {
        return wolf.getHealth() < maximumHealth(wolf)
                && Tag.ITEMS_WOLF_FOOD.isTagged(stack.getType())
                && nutrition(stack) > 0;
    }

    static void consumeOne(
            Wolf wolf,
            Item dropped,
            ItemStack stack) {
        int nutrition = nutrition(stack);
        if (nutrition <= 0) {
            return;
        }

        wolf.setHealth(Math.min(
                maximumHealth(wolf),
                wolf.getHealth() + nutrition));
        if (stack.getAmount() <= 1) {
            dropped.remove();
            return;
        }

        ItemStack rest = stack.clone();
        rest.setAmount(stack.getAmount() - 1);
        dropped.setItemStack(rest);
    }

    private static int nutrition(ItemStack stack) {
        FoodProperties food =
                stack.getData(DataComponentTypes.FOOD);
        return food == null ? 0 : food.nutrition();
    }

    private static double maximumHealth(Wolf wolf) {
        AttributeInstance maximum =
                wolf.getAttribute(Attribute.MAX_HEALTH);
        return maximum == null
                ? Math.max(1.0, wolf.getHealth())
                : Math.max(1.0, maximum.getValue());
    }
}
