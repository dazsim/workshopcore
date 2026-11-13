package com.workshopcraft.workshopcore.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Container block entity that loads and saves through {@link ValueInput} / {@link ValueOutput}. */
public abstract class CompatContainerBlockEntity extends BaseContainerBlockEntity {
    protected CompatContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected final void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        loadAdditional(ValueInput.of(input));
    }

    @Override
    protected final void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        saveAdditional(ValueOutput.of(output));
    }

    @Override
    public final void removeComponentsFromTag(net.minecraft.world.level.storage.ValueOutput output) {
        super.removeComponentsFromTag(output);
        removeComponentsFromTag(ValueOutput.of(output));
    }

    protected void loadAdditional(ValueInput input) {
    }

    protected void saveAdditional(ValueOutput output) {
    }

    public void removeComponentsFromTag(ValueOutput output) {
    }
}
