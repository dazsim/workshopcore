package com.workshopcraft.workshopcore.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 1.20.1 Block with the later split use hooks, removal side effects and component placement. */
public class CompatBlock extends Block {
    protected CompatBlock(Properties properties) {
        super(properties);
    }

    protected com.mojang.serialization.MapCodec<? extends Block> codec() {
        return null;
    }

    protected static <B extends Block> com.mojang.serialization.MapCodec<B> simpleCodec(java.util.function.Function<Properties, B> factory) {
        return null;
    }

    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return Interactions.TRY_WITH_EMPTY_HAND;
    }

    protected final InteractionResult useItemOnDefault(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return Interactions.TRY_WITH_EMPTY_HAND;
    }

    protected static <B extends Block> com.mojang.serialization.codecs.RecordCodecBuilder<B, Properties> propertiesCodec() {
        return com.mojang.serialization.MapCodec.unit(Properties::of).forGetter(b -> Properties.of());
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        final ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty()) {
            final InteractionResult r = useItemOn(stack, state, level, pos, player, hand, hit);
            if (r != Interactions.TRY_WITH_EMPTY_HAND) return r;
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        }
        return useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        Removal.onRemove(level, pos, state, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel s) affectNeighborsAfterRemoval(state, s, pos, movedByPiston);
    }

    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ComponentHolder h) {
            h.applyComponentsFrom(Comps.getter(stack, h.componentStorageKey()));
        }
    }
}
