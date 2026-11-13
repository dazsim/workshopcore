package com.workshopcraft.workshopcore.compat;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Menu with {@link #addStandardInventorySlots(Inventory, int, int)} on every version. */
public abstract class CompatMenu extends AbstractContainerMenu {
    protected CompatMenu(MenuType<?> type, int id) {
        super(type, id);
    }
}
