package com.workshopcraft.workshopcore.screwdriver;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Hidden stand-in for an old screwdriver id on Fabric. Converts to {@link Screwdrivers#item()} when held or ticked. */
public final class AliasScrewdriverItem extends Item {
    public AliasScrewdriverItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof Player player)) {
            return;
        }
        ItemStack converted = Screwdrivers.convert(stack);
        if (converted == stack) {
            return;
        }
        if (slot != null) {
            player.setItemSlot(slot, converted);
            return;
        }
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i) == stack) {
                inventory.setItem(i, converted);
                return;
            }
        }
    }
}
