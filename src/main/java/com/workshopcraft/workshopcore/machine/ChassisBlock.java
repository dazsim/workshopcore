package com.workshopcraft.workshopcore.machine;

import com.mojang.serialization.MapCodec;
import com.workshopcraft.workshopcore.compat.CompatEntityBlock;
import com.workshopcraft.workshopcore.compat.Interactions;
import com.workshopcraft.workshopcore.screwdriver.ScrewdriverContext;
import com.workshopcraft.workshopcore.screwdriver.ScrewdriverResult;
import com.workshopcraft.workshopcore.screwdriver.ScrewdriverTarget;
import com.workshopcraft.workshopcore.screwdriver.Screwdrivers;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Empty machine frame. STEP 0..7 tracks the fitted parts, see {@link Assembly}. */
public class ChassisBlock extends CompatEntityBlock implements ScrewdriverTarget {
    public static final IntegerProperty STEP = IntegerProperty.create("step", 0, 7);

    private final ChassisTier tier;
    private final Supplier<? extends BlockEntityType<? extends ChassisBlockEntity>> type;

    public ChassisBlock(ChassisTier tier, Supplier<? extends BlockEntityType<? extends ChassisBlockEntity>> type, Properties properties) {
        super(properties);
        this.tier = tier;
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(STEP, 0).setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH));
    }

    public ChassisTier tier() {
        return tier;
    }

    //? if <26.3 {
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }
    //?}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STEP, BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    //? if >=1.21 && <1.21.2 {
    /*@Override
    protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }
    *///?}

    //? if >=1.20.5 && <1.21.2 {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return Interactions.item(fitPart(stack, state, level, pos, player));
    }
    *///?} else {
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return fitPart(stack, state, level, pos, player);
    }
    //?}

    private InteractionResult fitPart(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if (Screwdrivers.isScrewdriver(stack) || !Assembly.isPart(stack)) return Interactions.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return Assembly.fit(state, level, pos, player, stack) ? InteractionResult.SUCCESS : Interactions.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public ScrewdriverResult useScrewdriver(ScrewdriverContext ctx) {
        if (ctx.level().isClientSide()) return ScrewdriverResult.SUCCESS;
        final boolean done = ctx.player().isShiftKeyDown()
                ? Assembly.unscrew(ctx.state(), ctx.level(), ctx.pos())
                : Assembly.screw(ctx.state(), ctx.level(), ctx.pos(), ctx.player());
        return done ? ScrewdriverResult.SUCCESS : ScrewdriverResult.PASS;
    }
}
