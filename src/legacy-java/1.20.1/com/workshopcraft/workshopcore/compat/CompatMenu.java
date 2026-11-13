package com.workshopcraft.workshopcore.compat;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

/** Adds the later inventory slot helper to 1.20.1 menus. */
public abstract class CompatMenu extends AbstractContainerMenu {
    protected CompatMenu(MenuType<?> type, int id) {
        super(type, id);
    }

    protected void addStandardInventorySlots(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, x + col * 18, y + 58));
    }
}
