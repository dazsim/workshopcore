package com.workshopcraft.workshopcore.compat;

import com.mojang.serialization.Codec;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/** 1.20.1 ItemStack helpers. */
public final class Stacks {
    public static final Codec<ItemStack> OPTIONAL_CODEC = ItemStack.CODEC;

    private Stacks() {
    }

    public static void consume(ItemStack stack, int count, LivingEntity entity) {
        if (!(entity instanceof Player p) || !p.getAbilities().instabuild) stack.shrink(count);
    }

    public static int hash(ItemStack stack) {
        return Objects.hash(stack.getItem(), stack.getTag());
    }

    public static void limit(Container container, ItemStack stack) {
        final int max = container instanceof CompatContainerBlockEntity c ? c.getMaxStackSize(stack) : Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
        if (stack.getCount() > max) stack.setCount(max);
    }
}
