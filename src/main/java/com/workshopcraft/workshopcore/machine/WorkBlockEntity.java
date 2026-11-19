package com.workshopcraft.workshopcore.machine;

import com.workshopcraft.workshopcore.compat.CompatContainerBlockEntity;
import com.workshopcraft.workshopcore.compat.ItemContainers;
import com.workshopcraft.workshopcore.compat.Players;
import com.workshopcraft.workshopcore.compat.ValueInput;
import com.workshopcraft.workshopcore.compat.ValueOutput;
import com.workshopcraft.workshopcore.screwdriver.ScrewdriverContext;
import com.workshopcraft.workshopcore.screwdriver.ScrewdriverResult;
import com.workshopcraft.workshopcore.screwdriver.ScrewdriverTarget;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Processing machine base: sided slots, a drive, an energy buffer, upgrades from its {@link MachineParts},
 * and auto-output on sides toggled with a screwdriver. Subclasses supply the slot roles, the job and a menu.
 */
public abstract class WorkBlockEntity extends CompatContainerBlockEntity implements WorldlyContainer, ScrewdriverTarget {
    public static final int PUSH_INTERVAL = 8;
    public static final int PUSH_COUNT = 16;
    public static final int DATA_PROGRESS = 0, DATA_DRIVE = 1, DATA_ENERGY = 2, DATA_COUNT = 3;

    private NonNullList<ItemStack> items;
    private final int[] slots;
    private final int baseCapacity;
    private final EnergyBuffer energy;
    private MachineParts parts = MachineParts.NONE;
    private Object energyHandle;
    private int outputs;
    private int pushTimer;
    public int progress;
    public int total;
    public int driveTicks;
    public int driveTotal;

    /** Synced 0..1000 values: progress, drive level and energy level. */
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case DATA_PROGRESS -> total <= 0 ? 0 : progress * 1000 / total;
                case DATA_DRIVE -> drive().level(WorkBlockEntity.this);
                case DATA_ENERGY -> energy.permille();
                default -> 0;
            };
        }

        @Override
        public void set(int i, int v) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    protected WorkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size, int energyCapacity, int maxEnergyIn) {
        super(type, pos, state);
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        this.slots = new int[size];
        for (int i = 0; i < size; i++) slots[i] = i;
        this.baseCapacity = energyCapacity;
        this.energy = new EnergyBuffer(energyCapacity, maxEnergyIn, 0, this::setChanged);
    }

    protected abstract boolean isInput(int slot);

    protected abstract boolean isOutput(int slot);

    /** Base ticks for the job the inputs allow right now, or 0 if there is nothing to do. */
    protected abstract int workTime(ServerLevel level);

    /** Called when progress reaches the job time: consume inputs, produce outputs. */
    protected abstract void finishWork(ServerLevel level);

    /** Extra input filter, e.g. "has a recipe". */
    protected boolean accepts(int slot, ItemStack stack) {
        return true;
    }

    /** Slot burned by {@link Drives#FUEL}, or -1. */
    public int fuelSlot() {
        return -1;
    }

    /** Energy used per working tick by {@link Drives#ENERGY}, before speed scaling. */
    public int energyPerTick() {
        return 10;
    }

    /** Drive when the parts supply none, e.g. a machine placed without assembly. */
    protected Drive fallbackDrive() {
        return Drives.CRANK;
    }

    public Drive drive() {
        final Drive d = parts.drive();
        return d != null ? d : fallbackDrive();
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public MachineParts parts() {
        return parts;
    }

    /** One cached loader handle per machine, used by the loader adapters. */
    @SuppressWarnings("unchecked")
    public <T> T energyHandle(Function<EnergyBuffer, T> factory) {
        if (energyHandle == null) energyHandle = factory.apply(energy);
        return (T) energyHandle;
    }

    public void install(MachineParts parts) {
        this.parts = parts;
        applyParts();
        sync();
    }

    void forgetParts() {
        parts = MachineParts.NONE;
    }

    protected void applyParts() {
        energy.capacity = (int) ((long) baseCapacity * parts.storage() / 100);
        energy.energy = Math.min(energy.energy, energy.capacity);
    }

    public boolean crank(Player player) {
        return drive().crank(this, player);
    }

    public boolean outputs(Direction side) {
        return (outputs & (1 << side.ordinal())) != 0;
    }

    public void toggleOutput(Direction side) {
        outputs ^= 1 << side.ordinal();
        sync();
    }

    public void tick(ServerLevel level) {
        final int time = workTime(level);
        if (time <= 0) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
        } else {
            final int speed = parts.speed();
            total = Math.max(1, (int) ((long) time * 100 / speed));
            if (drive().run(this, level, (int) ((long) energyPerTick() * speed / 100))) {
                if (++progress >= total) {
                    progress = 0;
                    finishWork(level);
                }
                setChanged();
            }
        }
        if (outputs != 0 && ++pushTimer >= PUSH_INTERVAL) {
            pushTimer = 0;
            for (Direction d : Direction.values()) if (outputs(d)) Transfer.push(level, worldPosition, d, PUSH_COUNT, s -> true);
        }
    }

    /** True if stack fits entirely into slot. */
    public boolean fits(int slot, ItemStack stack) {
        final ItemStack cur = getItem(slot);
        if (cur.isEmpty()) return true;
        //? if >=1.20.5 {
        final boolean same = ItemStack.isSameItemSameComponents(cur, stack);
        //?} else {
        /*final boolean same = ItemStack.isSameItemSameTags(cur, stack);
        *///?}
        return same && cur.getCount() + stack.getCount() <= Math.min(getMaxStackSize(), cur.getMaxStackSize());
    }

    /** Adds stack to slot, assuming {@link #fits}. */
    public void store(int slot, ItemStack stack) {
        final ItemStack cur = getItem(slot);
        if (cur.isEmpty()) setItem(slot, stack.copy());
        else cur.grow(stack.getCount());
        setChanged();
    }

    public void dropContents() {
        if (level != null) Containers.dropContents(level, worldPosition, this);
        clearContent();
    }

    protected void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;
        Containers.dropContents(level, pos, this);
        for (ItemStack s : parts.drops()) Block.popResource(level, pos, s);
        parts = MachineParts.NONE;
    }

    @Override
    public ScrewdriverResult useScrewdriver(ScrewdriverContext ctx) {
        if (ctx.level().isClientSide()) return ScrewdriverResult.SUCCESS;
        if (ctx.player().isShiftKeyDown()) {
            return Assembly.disassemble(ctx.level(), ctx.pos(), this) ? ScrewdriverResult.SUCCESS : ScrewdriverResult.PASS;
        }
        final Direction side = ctx.face();
        toggleOutput(side);
        Players.overlay(ctx.player(), Component.translatable(outputs(side) ? "message.workshopcore.machine.output_on" : "message.workshopcore.machine.output_off",
                Component.translatable("direction.workshopcore." + side.getName())));
        return ScrewdriverResult.SUCCESS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == fuelSlot()) return Fuels.isFuel(level, stack);
        return isInput(slot) && accepts(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isOutput(slot);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        items = NonNullList.withSize(slots.length, ItemStack.EMPTY);
        ItemContainers.loadAllItems(in, items);
        parts = MachineParts.load(in, "parts_");
        applyParts();
        energy.load(in, "energy");
        progress = in.getIntOr("progress", 0);
        total = in.getIntOr("total", 0);
        driveTicks = in.getIntOr("drive_ticks", 0);
        driveTotal = in.getIntOr("drive_total", 0);
        outputs = in.getIntOr("outputs", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        ItemContainers.saveAllItems(out, items);
        parts.save(out, "parts_");
        energy.save(out, "energy");
        out.putInt("progress", progress);
        out.putInt("total", total);
        out.putInt("drive_ticks", driveTicks);
        out.putInt("drive_total", driveTotal);
        out.putInt("outputs", outputs);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    //? if forge {
    /*private final com.workshopcraft.workshopcore.forge.ForgeMachines.Caps forgeCaps = new com.workshopcraft.workshopcore.forge.ForgeMachines.Caps(this);

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, Direction side) {
        final net.minecraftforge.common.util.LazyOptional<T> r = forgeCaps.get(cap, side);
        return r != null ? r : super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        forgeCaps.invalidate();
    }
    *///?}
}
