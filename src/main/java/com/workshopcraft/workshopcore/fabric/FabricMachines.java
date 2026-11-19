package com.workshopcraft.workshopcore.fabric;

import com.workshopcraft.workshopcore.machine.EnergyBuffer;
import com.workshopcraft.workshopcore.machine.Transfer;
import com.workshopcraft.workshopcore.machine.WorkBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import team.reborn.energy.api.EnergyStorage;

/** Machine items come from Fabric's Container fallback; energy is Team Reborn Energy. */
public final class FabricMachines {
    private static final long PER_MB = FluidConstants.BUCKET / 1000;

    private FabricMachines() {
    }

    public static void init() {
        EnergyStorage.SIDED.registerFallback((level, pos, state, be, side) ->
                be instanceof WorkBlockEntity m ? m.energyHandle(Energy::new) : null);
        EnergyBuffer.lookup = (level, pos, side) -> EnergyStorage.SIDED.find(level, pos, side) != null;
        EnergyBuffer.pusher = (level, pos, side, amount) -> {
            final EnergyStorage s = EnergyStorage.SIDED.find(level, pos, side);
            if (s == null || !s.supportsInsertion()) return 0;
            try (Transaction tx = Transaction.openOuter()) {
                final long n = s.insert(amount, tx);
                tx.commit();
                return (int) n;
            }
        };
        Transfer.lookup = (level, pos, side) -> present(ItemStorage.SIDED.find(level, pos, side));
        Transfer.mover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final Storage<ItemVariant> source = ItemStorage.SIDED.find(level, from, fromSide);
            final Storage<ItemVariant> target = ItemStorage.SIDED.find(level, to, toSide);
            if (source == null || target == null) return 0;
            return (int) StorageUtil.move(source, target, v -> filter.test(v.toStack()), max, null);
        };
        Transfer.probe = (level, pos, side, stack) -> {
            final Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos, side);
            return target != null && StorageUtil.simulateInsert(target, ItemVariant.of(stack), 1, Transaction.getCurrentUnsafe()) > 0;
        };
        Transfer.fluidLookup = (level, pos, side) -> present(FluidStorage.SIDED.find(level, pos, side));
        Transfer.fluidMover = (level, from, fromSide, to, toSide, max, filter, moved) -> {
            final Storage<FluidVariant> source = FluidStorage.SIDED.find(level, from, fromSide);
            final Storage<FluidVariant> target = FluidStorage.SIDED.find(level, to, toSide);
            if (source == null || target == null) return 0;
            final List<FluidVariant> held = new ArrayList<>();
            for (StorageView<FluidVariant> view : source) {
                if (!view.isResourceBlank() && view.getAmount() > 0 && !held.contains(view.getResource())) held.add(view.getResource());
            }
            for (FluidVariant v : held) {
                if (!filter.test(v.getFluid())) continue;
                long n = StorageUtil.move(source, target, v::equals, max * PER_MB, null);
                if (n == 0 && max < 1000) n = StorageUtil.move(source, target, v::equals, FluidConstants.BUCKET, null);
                if (n > 0) {
                    moved.accept(v.getFluid());
                    return (int) Math.max(1, n / PER_MB);
                }
            }
            return 0;
        };
        Transfer.fluidProbe = (level, pos, side, fluid) -> {
            final Storage<FluidVariant> target = FluidStorage.SIDED.find(level, pos, side);
            return target != null && StorageUtil.simulateInsert(target, FluidVariant.of(fluid), FluidConstants.BUCKET, Transaction.getCurrentUnsafe()) > 0;
        };
        Transfer.itemFluid = stack -> {
            final Storage<FluidVariant> s = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
            if (s == null) return null;
            final FluidVariant v = StorageUtil.findStoredResource(s);
            return v == null || v.isBlank() ? null : v.getFluid();
        };
    }

    private static boolean present(Storage<?> s) {
        return s != null && s != Storage.empty();
    }

    /** EnergyBuffer as a Team Reborn Energy storage. */
    public static final class Energy extends SnapshotParticipant<Integer> implements EnergyStorage {
        private final EnergyBuffer buffer;

        public Energy(EnergyBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public boolean supportsInsertion() {
            return buffer.maxIn > 0;
        }

        @Override
        public boolean supportsExtraction() {
            return buffer.maxOut > 0;
        }

        @Override
        public long insert(long amount, TransactionContext tx) {
            final int n = (int) Math.min(buffer.capacity - buffer.energy, Math.min(amount, buffer.maxIn));
            if (n <= 0) return 0;
            updateSnapshots(tx);
            buffer.energy += n;
            return n;
        }

        @Override
        public long extract(long amount, TransactionContext tx) {
            final int n = (int) Math.min(buffer.energy, Math.min(amount, buffer.maxOut));
            if (n <= 0) return 0;
            updateSnapshots(tx);
            buffer.energy -= n;
            return n;
        }

        @Override
        public long getAmount() {
            return buffer.energy;
        }

        @Override
        public long getCapacity() {
            return buffer.capacity;
        }

        @Override
        protected Integer createSnapshot() {
            return buffer.energy;
        }

        @Override
        protected void readSnapshot(Integer snapshot) {
            buffer.energy = snapshot;
        }

        @Override
        protected void onFinalCommit() {
            buffer.changed();
        }
    }
}
