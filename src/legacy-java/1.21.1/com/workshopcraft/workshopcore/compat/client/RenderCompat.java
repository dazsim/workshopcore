package com.workshopcraft.workshopcore.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.joml.Quaternionf;

/** Render calls that moved between Minecraft versions. */
public final class RenderCompat {
    private RenderCompat() {
    }

    public static RenderType cutoutBlocks() {
        return net.minecraft.client.renderer.Sheets.cutoutBlockSheet();
    }

    public static RenderType translucentBlocks() {
        return net.minecraft.client.renderer.Sheets.translucentCullBlockSheet();
    }

    public static int light(BlockAndTintGetter level, BlockPos pos) {
        return net.minecraft.client.renderer.LevelRenderer.getLightColor(level, pos);
    }

    public static void rotate(PoseStack pose, Quaternionf q) {
        pose.mulPose(q);
    }
}
