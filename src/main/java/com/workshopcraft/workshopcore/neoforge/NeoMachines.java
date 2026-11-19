package com.workshopcraft.workshopcore.neoforge;

import com.workshopcraft.workshopcore.machine.EnergyBuffer;
import com.workshopcraft.workshopcore.machine.Machines;
import com.workshopcraft.workshopcore.machine.Transfer;
import com.workshopcraft.workshopcore.machine.WorkBlockEntity;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.resource.ResourceStack;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

final class NeoMachines {
    private static final int BUCKET = 1000;

    private NeoMachines() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(NeoMachines::capabilities);
        EnergyBuffer.lookup = (level, pos, side) -> level.getCapability(Capabilities.Energy.BLOCK, pos, side) != null;
        EnergyBuffer.pusher = (level, pos, side, amount) -> {
            final EnergyHandler h = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
            if (h == null) return 0;
            try (Transaction tx = Transaction.openRoot()) {
                final int n = h.insert(amount, tx);
                tx.commit();
                return n;
            }
        };
        Transfer.lookup = (level, pos, side) -> level.getCapability(Capabilities.Item.BLOCK, pos, side) != null;
        Transfer.mover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final ResourceHandler<ItemResource> source = level.getCapability(Capabilities.Item.BLOCK, from, fromSide);
            final ResourceHandler<ItemResource> target = level.getCapability(Capabilities.Item.BLOCK, to, toSide);
            if (source == null || target == null) return 0;
            return ResourceHandlerUtil.moveStacking(source, target, r -> filter.test(r.toStack()), max, null);
        };
        Transfer.probe = (level, pos, side, stack) -> {
            final ResourceHandler<ItemResource> target = level.getCapability(Capabilities.Item.BLOCK, pos, side);
            if (target == null) return false;
            try (Transaction tx = probeTx()) {
                return target.insert(ItemResource.of(stack), 1, tx) > 0;
            }
        };
        Transfer.fluidLookup = (level, pos, side) -> level.getCapability(Capabilities.Fluid.BLOCK, pos, side) != null;
        Transfer.fluidMover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final ResourceHandler<FluidResource> source = level.getCapability(Capabilities.Fluid.BLOCK, from, fromSide);
            final ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, to, toSide);
            if (source == null || target == null) return 0;
            ResourceStack<FluidResource> done = ResourceHandlerUtil.moveFirst(source, target, r -> filter.test(r.getFluid()), max, null);
            if (done == null && max < BUCKET) done = ResourceHandlerUtil.moveFirst(source, target, r -> filter.test(r.getFluid()), BUCKET, null);
            if (done == null || done.isEmpty()) return 0;
            moved.accept(done.resource().getFluid());
            return done.amount();
        };
        Transfer.fluidProbe = (level, pos, side, fluid) -> {
            final ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
            if (target == null) return false;
            try (Transaction tx = probeTx()) {
                return target.insert(FluidResource.of(fluid), BUCKET, tx) > 0;
            }
        };
        Transfer.itemFluid = stack -> {
            final FluidStack f = FluidUtil.getFirstStackContained(stack);
            final Fluid fluid = f.isEmpty() ? null : f.getFluid();
            return fluid;
        };
        Transfer.invalidator = (level, pos) -> level.invalidateCapabilities(pos);
    }

    private static void capabilities(RegisterCapabilitiesEvent e) {
        for (Supplier<? extends BlockEntityType<? extends WorkBlockEntity>> s : Machines.exposed()) register(e, s.get());
    }

    private static <T extends WorkBlockEntity> void register(RegisterCapabilitiesEvent e, BlockEntityType<T> type) {
        e.registerBlockEntity(Capabilities.Item.BLOCK, type, WorldlyContainerWrapper::new);
        e.registerBlockEntity(Capabilities.Energy.BLOCK, type, (be, side) -> be.energyHandle(Handler::new));
    }

    @SuppressWarnings("deprecation")
    private static Transaction probeTx() {
        return Transaction.open(Transaction.getCurrentOpenedTransaction());
    }

    /** EnergyBuffer as a NeoForge energy handler. */
    static final class Handler extends SnapshotJournal<Integer> implements EnergyHandler {
        private final EnergyBuffer buffer;

        Handler(EnergyBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public long getAmountAsLong() {
            return buffer.energy;
        }

        @Override
        public long getCapacityAsLong() {
            return buffer.capacity;
        }

        @Override
        public int insert(int amount, TransactionContext tx) {
            final int n = Math.min(buffer.capacity - buffer.energy, Math.min(amount, buffer.maxIn));
            if (n <= 0) return 0;
            updateSnapshots(tx);
            buffer.energy += n;
            return n;
        }

        @Override
        public int extract(int amount, TransactionContext tx) {
            final int n = Math.min(buffer.energy, Math.min(amount, buffer.maxOut));
            if (n <= 0) return 0;
            updateSnapshots(tx);
            buffer.energy -= n;
            return n;
        }

        @Override
        protected Integer createSnapshot() {
            return buffer.energy;
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            buffer.energy = snapshot;
        }

        @Override
        protected void onRootCommit(Integer original) {
            if (buffer.energy != original) buffer.changed();
        }
    }
}
