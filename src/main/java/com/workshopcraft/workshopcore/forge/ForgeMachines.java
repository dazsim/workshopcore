package com.workshopcraft.workshopcore.forge;

import com.workshopcraft.workshopcore.machine.EnergyBuffer;
import com.workshopcraft.workshopcore.machine.Transfer;
import com.workshopcraft.workshopcore.machine.WorkBlockEntity;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

/** Forge capabilities for {@link WorkBlockEntity}, plus the Transfer and energy hooks. */
public final class ForgeMachines {
    private static final int BUCKET = 1000;

    private ForgeMachines() {
    }

    static void init() {
        EnergyBuffer.lookup = (level, pos, side) -> cap(level, pos, side, ForgeCapabilities.ENERGY) != null;
        EnergyBuffer.pusher = (level, pos, side, amount) -> {
            final IEnergyStorage s = cap(level, pos, side, ForgeCapabilities.ENERGY);
            return s == null || !s.canReceive() ? 0 : s.receiveEnergy(amount, false);
        };
        Transfer.lookup = (level, pos, side) -> cap(level, pos, side, ForgeCapabilities.ITEM_HANDLER) != null;
        Transfer.mover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final IItemHandler src = cap(level, from, fromSide, ForgeCapabilities.ITEM_HANDLER);
            final IItemHandler dst = cap(level, to, toSide, ForgeCapabilities.ITEM_HANDLER);
            return src == null || dst == null ? 0 : moveItems(src, dst, max, filter);
        };
        Transfer.probe = (level, pos, side, stack) -> {
            final IItemHandler dst = cap(level, pos, side, ForgeCapabilities.ITEM_HANDLER);
            return dst != null && ItemHandlerHelper.insertItemStacked(dst, ItemHandlerHelper.copyStackWithSize(stack, 1), true).isEmpty();
        };
        Transfer.fluidLookup = (level, pos, side) -> cap(level, pos, side, ForgeCapabilities.FLUID_HANDLER) != null;
        Transfer.fluidMover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final IFluidHandler src = cap(level, from, fromSide, ForgeCapabilities.FLUID_HANDLER);
            final IFluidHandler dst = cap(level, to, toSide, ForgeCapabilities.FLUID_HANDLER);
            if (src == null || dst == null) return 0;
            final FluidStack peek = src.drain(Math.max(max, BUCKET), IFluidHandler.FluidAction.SIMULATE);
            if (peek.isEmpty() || !filter.test(peek.getFluid())) return 0;
            FluidStack done = FluidUtil.tryFluidTransfer(dst, src, max, true);
            if (done.isEmpty() && max < BUCKET) done = FluidUtil.tryFluidTransfer(dst, src, BUCKET, true);
            if (done.isEmpty()) return 0;
            moved.accept(done.getFluid());
            return done.getAmount();
        };
        Transfer.fluidProbe = (level, pos, side, fluid) -> {
            final IFluidHandler dst = cap(level, pos, side, ForgeCapabilities.FLUID_HANDLER);
            return dst != null && dst.fill(new FluidStack(fluid, BUCKET), IFluidHandler.FluidAction.SIMULATE) > 0;
        };
        Transfer.itemFluid = stack -> FluidUtil.getFluidContained(stack).filter(f -> !f.isEmpty()).map(FluidStack::getFluid).orElse(null);
    }

    private static <T> T cap(Level level, BlockPos pos, Direction side, Capability<T> cap) {
        final BlockEntity be = level.getBlockEntity(pos);
        return be == null ? null : be.getCapability(cap, side).resolve().orElse(null);
    }

    static int moveItems(IItemHandler src, IItemHandler dst, int max, Predicate<ItemStack> filter) {
        int moved = 0;
        for (int i = 0; i < src.getSlots() && moved < max; i++) {
            final ItemStack sim = src.extractItem(i, max - moved, true);
            if (sim.isEmpty() || !filter.test(sim)) continue;
            final int n = sim.getCount() - ItemHandlerHelper.insertItemStacked(dst, sim, true).getCount();
            if (n <= 0) continue;
            final ItemStack taken = src.extractItem(i, n, false);
            final ItemStack left = ItemHandlerHelper.insertItemStacked(dst, taken, false);
            if (!left.isEmpty()) src.insertItem(i, left, false);
            moved += taken.getCount() - left.getCount();
        }
        return moved;
    }

    /** Lazily built, cached capabilities of one machine. */
    public static final class Caps {
        private final WorkBlockEntity be;
        private final LazyOptional<?>[] items = new LazyOptional<?>[7];
        private LazyOptional<IEnergyStorage> energy;

        public Caps(WorkBlockEntity be) {
            this.be = be;
        }

        public <T> LazyOptional<T> get(Capability<T> cap, Direction side) {
            if (cap == ForgeCapabilities.ITEM_HANDLER) {
                final int i = side == null ? 6 : side.ordinal();
                if (items[i] == null) items[i] = LazyOptional.of(() -> side == null ? new InvWrapper(be) : new SidedInvWrapper(be, side));
                return items[i].cast();
            }
            if (cap == ForgeCapabilities.ENERGY) {
                if (energy == null) energy = LazyOptional.of(() -> be.energyHandle(Energy::new));
                return energy.cast();
            }
            return null;
        }

        public void invalidate() {
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null) items[i].invalidate();
                items[i] = null;
            }
            if (energy != null) energy.invalidate();
            energy = null;
        }
    }

    /** EnergyBuffer as Forge Energy. */
    public static final class Energy implements IEnergyStorage {
        private final EnergyBuffer buffer;

        public Energy(EnergyBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public int receiveEnergy(int max, boolean simulate) {
            return buffer.receive(max, simulate);
        }

        @Override
        public int extractEnergy(int max, boolean simulate) {
            return buffer.extract(max, simulate);
        }

        @Override
        public int getEnergyStored() {
            return buffer.energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return buffer.capacity;
        }

        @Override
        public boolean canExtract() {
            return buffer.maxOut > 0;
        }

        @Override
        public boolean canReceive() {
            return buffer.maxIn > 0;
        }
    }
}
