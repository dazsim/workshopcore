package com.workshopcraft.workshopcore.machine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Furnace fuel values on every version and loader. */
public final class Fuels {
    private Fuels() {
    }

    public static boolean isFuel(Level level, ItemStack stack) {
        //? if >=26.3 {
        /*return stack.has(net.minecraft.core.component.DataComponents.COOKING_FUEL);
        *///?} else {
        return legacyBurnTime(level, stack) > 0;
        //?}
    }

    /** Burn ticks for one item, burned by the block entity be. */
    public static int burnTime(ServerLevel level, ItemStack stack, BlockEntity be) {
        //? if >=26.3 {
        /*if (!stack.has(net.minecraft.core.component.DataComponents.COOKING_FUEL)) return 0;
        final net.minecraft.world.level.storage.loot.LootParams.Builder params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, be.getBlockState())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY, be)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(be.getBlockPos()));
        if (be instanceof net.minecraft.world.Container c) params.withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.CONTAINER, c);
        final net.minecraft.world.level.storage.loot.LootContext ctx = new net.minecraft.world.level.storage.loot.LootContext.Builder(
                params.create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CONTAINER_PROCESS)).create(java.util.Optional.empty());
        return net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt.getFromItem(stack,
                net.minecraft.core.component.DataComponents.COOKING_FUEL, net.minecraft.world.item.component.CookingFuel::burnTime, ctx, 0);
        *///?} else {
        return legacyBurnTime(level, stack);
        //?}
    }

    private static int legacyBurnTime(Level level, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        //? if >=26.3 {
        /*return 0;
        *///?} else if >=1.21.2 && neoforge {
        return level == null ? 0 : stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING, level.fuelValues());
        //?} else if >=1.21.2 {
        /*return level == null ? 0 : level.fuelValues().burnDuration(stack);
        *///?} else if !fabric {
        /*return stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING);
        *///?} else {
        /*final Integer t = net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.getFuel().get(stack.getItem());
        return t == null ? 0 : t;
        *///?}
    }

    /** Burns one item and returns what is left in the slot, including any remainder such as an empty bucket. */
    public static ItemStack consume(ItemStack fuel) {
        //? if >=26.1 && neoforge {
        final var r = fuel.getCraftingRemainder();
        fuel.shrink(1);
        return fuel.isEmpty() && r != null ? r.create() : fuel;
        //?} else if >=26.1 {
        /*final var r = fuel.getItem().getCraftingRemainder();
        fuel.shrink(1);
        return fuel.isEmpty() && r != null ? r.create() : fuel;
        *///?} else if fabric {
        /*final net.minecraft.world.item.Item r = fuel.getItem().getCraftingRemainingItem();
        fuel.shrink(1);
        return fuel.isEmpty() && r != null ? new ItemStack(r) : fuel;
        *///?} else {
        /*final ItemStack r = fuel.getCraftingRemainingItem();
        fuel.shrink(1);
        return fuel.isEmpty() ? r : fuel;
        *///?}
    }
}
