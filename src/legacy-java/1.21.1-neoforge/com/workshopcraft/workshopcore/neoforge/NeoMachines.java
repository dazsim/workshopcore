package com.workshopcraft.workshopcore.neoforge;

import com.workshopcraft.workshopcore.machine.EnergyBuffer;
import com.workshopcraft.workshopcore.machine.Transfer;
import com.workshopcraft.workshopcore.machine.WorkBlockEntity;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import com.workshopcraft.workshopcore.machine.Machines;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** 1.21.1 NeoForge capabilities for {@link WorkBlockEntity}, plus the Transfer and energy hooks. */
final class NeoMachines {
    private static final int BUCKET = 1000;

    private NeoMachines() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(NeoMachines::capabilities);
        EnergyBuffer.lookup = (level, pos, side) -> cap(level, pos, side, Capabilities.EnergyStorage.BLOCK) != null;
        EnergyBuffer.pusher = (level, pos, side, amount) -> {
            final IEnergyStorage s = cap(level, pos, side, Capabilities.EnergyStorage.BLOCK);
            return s == null || !s.canReceive() ? 0 : s.receiveEnergy(amount, false);
        };
        Transfer.lookup = (level, pos, side) -> cap(level, pos, side, Capabilities.ItemHandler.BLOCK) != null;
        Transfer.mover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final IItemHandler src = cap(level, from, fromSide, Capabilities.ItemHandler.BLOCK);
            final IItemHandler dst = cap(level, to, toSide, Capabilities.ItemHandler.BLOCK);
            return src == null || dst == null ? 0 : moveItems(src, dst, max, filter);
        };
        Transfer.probe = (level, pos, side, stack) -> {
            final IItemHandler dst = cap(level, pos, side, Capabilities.ItemHandler.BLOCK);
            return dst != null && ItemHandlerHelper.insertItemStacked(dst, stack.copyWithCount(1), true).isEmpty();
        };
        Transfer.fluidLookup = (level, pos, side) -> cap(level, pos, side, Capabilities.FluidHandler.BLOCK) != null;
        Transfer.fluidMover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final IFluidHandler src = cap(level, from, fromSide, Capabilities.FluidHandler.BLOCK);
            final IFluidHandler dst = cap(level, to, toSide, Capabilities.FluidHandler.BLOCK);
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
            final IFluidHandler dst = cap(level, pos, side, Capabilities.FluidHandler.BLOCK);
            return dst != null && dst.fill(new FluidStack(fluid, BUCKET), IFluidHandler.FluidAction.SIMULATE) > 0;
        };
        Transfer.itemFluid = stack -> FluidUtil.getFluidContained(stack).filter(f -> !f.isEmpty()).map(FluidStack::getFluid).orElse(null);
        Transfer.invalidator = (level, pos) -> level.invalidateCapabilities(pos);
    }

    private static <T> T cap(Level level, BlockPos pos, Direction side, BlockCapability<T, Direction> cap) {
        return level.getCapability(cap, pos, side);
    }

    private static void capabilities(RegisterCapabilitiesEvent e) {
        for (Supplier<? extends BlockEntityType<? extends WorkBlockEntity>> s : Machines.exposed()) register(e, s.get());
    }

    private static <T extends WorkBlockEntity> void register(RegisterCapabilitiesEvent e, BlockEntityType<T> type) {
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> side == null ? new InvWrapper(be) : new SidedInvWrapper(be, side));
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (be, side) -> be.energyHandle(Energy::new));
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

    /** EnergyBuffer as NeoForge energy. */
    static final class Energy implements IEnergyStorage {
        private final EnergyBuffer buffer;

        Energy(EnergyBuffer buffer) {
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
