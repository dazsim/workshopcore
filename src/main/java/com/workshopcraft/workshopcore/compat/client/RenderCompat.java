package com.workshopcraft.workshopcore.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import org.joml.Quaternionf;

/** Render calls that moved between Minecraft versions. */
public final class RenderCompat {
    private RenderCompat() {
    }

    public static RenderType cutoutBlocks() {
        //? if >=26.2 {
        /*return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
        *///?} else {
        return net.minecraft.client.renderer.Sheets.cutoutBlockSheet();
        //?}
    }

    public static RenderType translucentBlocks() {
        //? if >=26.3 {
        /*return net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucentCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
        *///?} else if >=26.2 {
        /*return net.minecraft.client.renderer.Sheets.translucentBlockItemSheet();
        *///?} else {
        return net.minecraft.client.renderer.Sheets.translucentBlockSheet();
        //?}
    }

    public static int light(BlockAndLightGetter level, BlockPos pos) {
        //? if >=26.2 {
        /*return net.minecraft.util.LightCoordsUtil.getLightCoords(level, pos);
        *///?} else {
        return net.minecraft.client.renderer.LevelRenderer.getLightCoords(level, pos);
        //?}
    }

    public static void rotate(PoseStack pose, Quaternionf q) {
        //? if >=26.3 {
        /*pose.rotate(q);
        *///?} else {
        pose.mulPose(q);
        //?}
    }
}
