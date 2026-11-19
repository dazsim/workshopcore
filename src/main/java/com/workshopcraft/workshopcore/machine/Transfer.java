package com.workshopcraft.workshopcore.machine;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/** Loader neutral item and fluid movement between any blocks the loader can see. Fluid amounts are in mB. */
public final class Transfer {
    @FunctionalInterface
    public interface Lookup {
        boolean has(Level level, BlockPos pos, Direction side);
    }

    @FunctionalInterface
    public interface Mover<T> {
        /** Moves up to max from (from, fromSide) into (to, toSide). Returns the amount moved. */
        int move(Level level, BlockPos from, Direction fromSide, BlockPos to, Direction toSide, int max, Predicate<T> filter, Consumer<T> moved);
    }

    @FunctionalInterface
    public interface Probe<T> {
        boolean accepts(Level level, BlockPos pos, Direction side, T what);
    }

    @FunctionalInterface
    public interface ItemFluid {
        /** The fluid held by a container item, or null. */
        Fluid of(ItemStack stack);
    }

    @FunctionalInterface
    public interface Invalidator {
        void invalidate(Level level, BlockPos pos);
    }

    public static Lookup lookup = (level, pos, side) -> false;
    public static Mover<ItemStack> mover = (level, from, fromSide, to, toSide, max, filter, moved) -> 0;
    public static Probe<ItemStack> probe = (level, pos, side, stack) -> false;
    public static Lookup fluidLookup = (level, pos, side) -> false;
    public static Mover<Fluid> fluidMover = (level, from, fromSide, to, toSide, max, filter, moved) -> 0;
    public static Probe<Fluid> fluidProbe = (level, pos, side, fluid) -> false;
    public static ItemFluid itemFluid = stack -> null;
    public static Invalidator invalidator = (level, pos) -> { };

    private Transfer() {
    }

    /** Pushes items out of pos through side into the neighbour on that side. */
    public static int push(Level level, BlockPos pos, Direction side, int max, Predicate<ItemStack> filter) {
        return mover.move(level, pos, side, pos.relative(side), side.getOpposite(), max, filter, s -> { });
    }

    /** Pulls items from the neighbour on side into pos. */
    public static int pull(Level level, BlockPos pos, Direction side, int max, Predicate<ItemStack> filter) {
        return mover.move(level, pos.relative(side), side.getOpposite(), pos, side, max, filter, s -> { });
    }

    public static int pushFluid(Level level, BlockPos pos, Direction side, int maxMb, Predicate<Fluid> filter) {
        return fluidMover.move(level, pos, side, pos.relative(side), side.getOpposite(), maxMb, filter, f -> { });
    }

    public static int pullFluid(Level level, BlockPos pos, Direction side, int maxMb, Predicate<Fluid> filter) {
        return fluidMover.move(level, pos.relative(side), side.getOpposite(), pos, side, maxMb, filter, f -> { });
    }
}
