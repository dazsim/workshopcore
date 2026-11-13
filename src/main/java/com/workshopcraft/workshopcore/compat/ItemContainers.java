package com.workshopcraft.workshopcore.compat;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/** Container item lists over {@link ValueInput} / {@link ValueOutput}. */
public final class ItemContainers {
    private ItemContainers() {
    }

    public static void loadAllItems(ValueInput in, NonNullList<ItemStack> items) {
        ContainerHelper.loadAllItems(in.vanilla(), items);
    }

    public static void saveAllItems(ValueOutput out, NonNullList<ItemStack> items) {
        ContainerHelper.saveAllItems(out.vanilla(), items);
    }
}
