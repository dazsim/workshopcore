package com.workshopcraft.workshopcore.machine;

import com.workshopcraft.workshopcore.compat.CompatBlockEntity;
import com.workshopcraft.workshopcore.compat.Stacks;
import com.workshopcraft.workshopcore.compat.ValueInput;
import com.workshopcraft.workshopcore.compat.ValueOutput;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Parts fitted to a {@link ChassisBlock}. Synced to clients so renderers can show them. */
public class ChassisBlockEntity extends CompatBlockEntity {
    ItemStack core = ItemStack.EMPTY;
    ItemStack board = ItemStack.EMPTY;
    ItemStack panels = ItemStack.EMPTY;
    ItemStack worktop = ItemStack.EMPTY;
    final List<ItemStack> upgrades = new ArrayList<>();
    boolean loose;

    public ChassisBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public MachineParts parts() {
        return parts(getBlockState());
    }

    public MachineParts parts(BlockState state) {
        return new MachineParts(new ItemStack(state.getBlock()), core, board, upgrades, panels, worktop);
    }

    /** True while the last fitted upgrade still needs screwing in. */
    public boolean looseUpgrade() {
        return loose;
    }

    void restore(MachineParts p) {
        core = p.core().copy();
        board = p.board().copy();
        panels = p.panels().copy();
        worktop = p.worktop().copy();
        upgrades.clear();
        for (ItemStack s : p.upgrades()) upgrades.add(s.copy());
        loose = false;
        changed();
    }

    void clear() {
        core = board = panels = worktop = ItemStack.EMPTY;
        upgrades.clear();
        loose = false;
    }

    void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;
        for (ItemStack s : parts(state).drops()) {
            if (s.getItem() != state.getBlock().asItem()) Block.popResource(level, pos, s);
        }
        clear();
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        core = MachineParts.get(in, "core");
        board = MachineParts.get(in, "board");
        panels = MachineParts.get(in, "panels");
        worktop = MachineParts.get(in, "worktop");
        upgrades.clear();
        upgrades.addAll(in.read("upgrades", Stacks.OPTIONAL_CODEC.listOf()).orElse(List.of()));
        loose = in.getBooleanOr("loose", false);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        MachineParts.put(out, "core", core);
        MachineParts.put(out, "board", board);
        MachineParts.put(out, "panels", panels);
        MachineParts.put(out, "worktop", worktop);
        if (!upgrades.isEmpty()) out.store("upgrades", Stacks.OPTIONAL_CODEC.listOf(), upgrades);
        out.putBoolean("loose", loose);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
