package com.workshopcraft.workshopcore.compat;

import com.workshopcraft.workshopcore.compat.component.DataComponentMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Routes 1.20.1 NBT save/load to the 26.x style overloads. Implicit components use {@link #componentStorageKey()}. */
public abstract class CompatBlockEntity extends BlockEntity implements Removal, ComponentHolder {
    protected CompatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Item/block-entity NBT compound key for implicit components, e.g. "SimpleMachines". Null skips them. */
    public String componentStorageKey() {
        return null;
    }

    @Override
    public final void load(CompoundTag tag) {
        super.load(tag);
        loadAdditional(new ValueInput(tag, null));
        final String key = componentStorageKey();
        if (key != null && tag.contains(key)) applyImplicitComponents(Comps.getterFrom(tag.getCompound(key)));
    }

    @Override
    protected final void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveAdditional(new ValueOutput(tag, null));
        final String key = componentStorageKey();
        if (key != null) {
            final DataComponentMap.Builder b = new DataComponentMap.Builder();
            collectImplicitComponents(b);
            if (!b.values().isEmpty()) {
                final CompoundTag data = new CompoundTag();
                Comps.write(data, b);
                tag.put(key, data);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return getUpdateTag((HolderLookup.Provider) null);
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag();
    }

    public CompoundTag saveWithoutMetadata(HolderLookup.Provider registries) {
        return saveWithoutMetadata();
    }

    public CompoundTag saveCustomOnly(HolderLookup.Provider registries) {
        return saveWithoutMetadata();
    }

    public void applyComponentsFrom(DataComponentGetter components) {
        applyImplicitComponents(components);
        setChanged();
    }

    protected void loadAdditional(ValueInput input) {
    }

    protected void saveAdditional(ValueOutput output) {
    }

    protected void applyImplicitComponents(DataComponentGetter components) {
    }

    protected void collectImplicitComponents(DataComponentMap.Builder components) {
    }

    public void removeComponentsFromTag(ValueOutput output) {
    }

    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }
}
