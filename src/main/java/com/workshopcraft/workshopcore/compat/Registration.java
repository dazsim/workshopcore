package com.workshopcraft.workshopcore.compat;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/** Registry object constructors that changed shape between 1.21.1 and 26.x. */
public final class Registration {
    private Registration() {
    }

    @FunctionalInterface
    public interface BlockEntityFactory<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    public static <T extends Recipe<?>> RecipeSerializer<T> serializer(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> stream) {
        //? if >=26.1 {
        return new RecipeSerializer<>(codec, stream);
        //?} else {
        /*return new RecipeSerializer<>() {
            @Override
            public MapCodec<T> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
                return stream;
            }
        };
        *///?}
    }

    public static <T extends BlockEntity> BlockEntityType<T> blockEntity(BlockEntityFactory<T> factory, Set<Block> blocks) {
        //? if >=26.1 {
        return new BlockEntityType<>(factory::create, blocks);
        //?} else {
        /*return BlockEntityType.Builder.of(factory::create, blocks.toArray(new Block[0])).build(null);
        *///?}
    }

    /**
     * Same as {@link #blockEntity} but accepts vanilla {@link BlockEntityType.BlockEntitySupplier}.
     * A separate name avoids ambiguous method references against {@link BlockEntityFactory}.
     */
    public static <T extends BlockEntity> BlockEntityType<T> blockEntitySupplier(BlockEntityType.BlockEntitySupplier<T> supplier, Set<Block> blocks) {
        //? if >=26.1 {
        return new BlockEntityType<>(supplier, blocks);
        //?} else {
        /*return BlockEntityType.Builder.of(supplier, blocks.toArray(new Block[0])).build(null);
        *///?}
    }
}
