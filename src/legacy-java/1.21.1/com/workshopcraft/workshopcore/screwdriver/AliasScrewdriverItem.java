package com.workshopcraft.workshopcore.screwdriver;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Hidden stand-in for an old screwdriver id on Fabric. Converts to {@link Screwdrivers#item()} when held or ticked. */
public final class AliasScrewdriverItem extends Item {
    public AliasScrewdriverItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity owner, int slotId, boolean isSelected) {
        if (!(owner instanceof Player player)) {
            return;
        }
        ItemStack converted = Screwdrivers.convert(stack);
        if (converted == stack) {
            return;
        }
        var inventory = player.getInventory();
        if (slotId >= 0 && slotId < inventory.getContainerSize() && inventory.getItem(slotId) == stack) {
            inventory.setItem(slotId, converted);
            return;
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i) == stack) {
                inventory.setItem(i, converted);
                return;
            }
        }
    }
}
