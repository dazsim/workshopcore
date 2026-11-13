package com.workshopcraft.workshopcore.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Routes the 1.21.1 tag based save/load hooks to the 26.x style overloads. */
public abstract class CompatBlockEntity extends BlockEntity implements Removal {
    protected CompatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected final void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadAdditional(new ValueInput(tag, registries));
    }

    @Override
    protected final void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveAdditional(new ValueOutput(tag, registries));
    }

    @Override
    protected final void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        applyImplicitComponents(components::get);
    }

    @Override
    public final void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        removeComponentsFromTag(new ValueOutput(tag, null));
    }

    protected void loadAdditional(ValueInput input) {
    }

    protected void saveAdditional(ValueOutput output) {
    }

    protected void applyImplicitComponents(DataComponentGetter components) {
    }

    public void removeComponentsFromTag(ValueOutput output) {
    }

    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }
}
