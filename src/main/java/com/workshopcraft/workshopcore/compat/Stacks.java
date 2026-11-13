package com.workshopcraft.workshopcore.compat;

import com.mojang.serialization.Codec;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** ItemStack helpers that moved between versions. */
public final class Stacks {
    public static final Codec<ItemStack> OPTIONAL_CODEC = ItemStack.OPTIONAL_CODEC;

    private Stacks() {
    }

    public static void consume(ItemStack stack, int count, LivingEntity entity) {
        stack.consume(count, entity);
    }

    public static int hash(ItemStack stack) {
        return ItemStack.hashItemAndComponents(stack);
    }

    public static void limit(Container container, ItemStack stack) {
        stack.limitSize(container instanceof CompatContainerBlockEntity c ? c.getMaxStackSize(stack) : Math.min(container.getMaxStackSize(), stack.getMaxStackSize()));
    }
}
