package com.workshopcraft.workshopcore.screwdriver;

import com.workshopcraft.workshopcore.compat.Players;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The click that is being handled. World changes belong on the server; return SUCCESS on the client too. */
public final class ScrewdriverContext {
    private final Level level;
    private final BlockPos pos;
    private final BlockState state;
    private final Player player;
    private final ItemStack held;
    private final InteractionHand hand;
    private final BlockHitResult hit;

    ScrewdriverContext(Level level, BlockPos pos, BlockState state, Player player, ItemStack held,
                       InteractionHand hand, BlockHitResult hit) {
        this.level = level;
        this.pos = pos;
        this.state = state;
        this.player = player;
        this.held = held;
        this.hand = hand;
        this.hit = hit;
    }

    public Level level() {
        return level;
    }

    public BlockPos pos() {
        return pos;
    }

    public BlockState state() {
        return state;
    }

    public Player player() {
        return player;
    }

    public ItemStack held() {
        return held;
    }

    public InteractionHand hand() {
        return hand;
    }

    public BlockHitResult hit() {
        return hit;
    }

    public Direction face() {
        return hit.getDirection();
    }

    public Vec3 location() {
        return hit.getLocation();
    }

    public boolean sneaking() {
        return player.isSecondaryUseActive();
    }

    public boolean isClientSide() {
        return level.isClientSide();
    }

    public void click() {
        click(1.6F);
    }

    public void click(float pitch) {
        if (!level.isClientSide()) {
            level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.6F, pitch);
        }
    }

    public void ratchet() {
        ratchet(1.6F);
    }

    public void ratchet(float pitch) {
        if (!level.isClientSide()) {
            level.playSound(null, pos, SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.BLOCKS, 0.5F, pitch);
        }
    }

    public void overlay(Component message) {
        if (!level.isClientSide()) {
            Players.overlay(player, message);
        }
    }
}
