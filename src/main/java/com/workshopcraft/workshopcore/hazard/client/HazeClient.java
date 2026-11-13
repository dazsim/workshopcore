package com.workshopcraft.workshopcore.hazard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Vector4f;

/** Client haze fade, fog mutation, and screen tint. */
public final class HazeClient {
    private static final float CLOSE_FOG_END = 8.0F;
    private static final float TINT_STRENGTH = 0.40F;

    private static int fromColor;
    private static float fromDensity;
    private static int toColor;
    private static float toDensity;
    private static long startTick;
    private static int fadeTicks;

    private HazeClient() {
    }

    public static void accept(int color, float density, int ticks) {
        Visual current = sample(0.0F);
        fromColor = current.color();
        fromDensity = current.density();
        toColor = color;
        toDensity = Mth.clamp(density, 0.0F, 1.0F);
        fadeTicks = Math.max(0, ticks);
        startTick = gameTime();
        if (toDensity <= 0.0F && fromDensity <= 0.0F) {
            fadeTicks = 0;
        }
    }

    public static void clear() {
        fromColor = 0;
        fromDensity = 0.0F;
        toColor = 0;
        toDensity = 0.0F;
        fadeTicks = 0;
        startTick = 0L;
    }

    public static boolean hasHaze(float partialTick) {
        return sample(partialTick).density() > 0.0F;
    }

    public static void apply(FogData fog, float partialTick) {
        Visual visual = sample(partialTick);
        if (visual.density() <= 0.0F) {
            return;
        }
        float mix = mix(visual);
        if (mix <= 0.0F) {
            return;
        }
        Vector4f color = fog.color;
        color.x = Mth.lerp(mix, color.x, ARGB.red(visual.color()) / 255.0F);
        color.y = Mth.lerp(mix, color.y, ARGB.green(visual.color()) / 255.0F);
        color.z = Mth.lerp(mix, color.z, ARGB.blue(visual.color()) / 255.0F);
        fog.environmentalStart = Mth.lerp(mix, fog.environmentalStart, 0.0F);
        fog.environmentalEnd = Math.max(fog.environmentalStart + 0.01F, Mth.lerp(mix, fog.environmentalEnd, CLOSE_FOG_END));
        fog.renderDistanceStart = Mth.lerp(mix, fog.renderDistanceStart, 0.0F);
        fog.renderDistanceEnd = Math.max(fog.renderDistanceStart + 0.01F, Mth.lerp(mix, fog.renderDistanceEnd, CLOSE_FOG_END));
        fog.skyEnd = Math.max(fog.environmentalEnd, Mth.lerp(mix, fog.skyEnd, CLOSE_FOG_END));
        fog.cloudEnd = Math.max(fog.environmentalEnd, Mth.lerp(mix, fog.cloudEnd, CLOSE_FOG_END));
    }

    public static void renderTint(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        Visual visual = sample(delta.getGameTimeDeltaPartialTick(false));
        float mix = mix(visual) * TINT_STRENGTH;
        if (mix <= 0.001F) {
            return;
        }
        int alpha = Mth.clamp(Math.round(mix * 255.0F), 0, 255);
        int tint = ARGB.color(alpha, ARGB.red(visual.color()), ARGB.green(visual.color()), ARGB.blue(visual.color()));
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), tint);
    }

    private static float mix(Visual visual) {
        return visual.density() * (ARGB.alpha(visual.color()) / 255.0F);
    }

    private static Visual sample(float partialTick) {
        if (fadeTicks <= 0) {
            return new Visual(toColor, toDensity);
        }
        float elapsed = (gameTime() - startTick) + partialTick;
        float t = Mth.clamp(elapsed / fadeTicks, 0.0F, 1.0F);
        if (t >= 1.0F) {
            return new Visual(toColor, toDensity);
        }
        int from = fromDensity <= 0.0F && ARGB.alpha(fromColor) == 0 ? withAlpha(toColor, 0) : fromColor;
        int to = toDensity <= 0.0F && ARGB.alpha(toColor) == 0 ? withAlpha(fromColor, 0) : toColor;
        return new Visual(ARGB.srgbLerp(t, from, to), Mth.lerp(t, fromDensity, toDensity));
    }

    private static int withAlpha(int color, int alpha) {
        return ARGB.color(alpha, ARGB.red(color), ARGB.green(color), ARGB.blue(color));
    }

    private static long gameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        return level != null ? level.getGameTime() : 0L;
    }

    private record Visual(int color, float density) {
    }
}
