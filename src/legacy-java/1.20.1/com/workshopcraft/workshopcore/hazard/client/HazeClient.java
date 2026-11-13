package com.workshopcraft.workshopcore.hazard.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

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

    public static float nearPlane(float current, float partialTick) {
        Visual visual = sample(partialTick);
        float mix = mix(visual);
        if (mix <= 0.0F) {
            return current;
        }
        return Mth.lerp(mix, current, 0.0F);
    }

    public static float farPlane(float current, float partialTick) {
        Visual visual = sample(partialTick);
        float mix = mix(visual);
        if (mix <= 0.0F) {
            return current;
        }
        return Math.max(0.01F, Mth.lerp(mix, current, CLOSE_FOG_END));
    }

    public static float fogRed(float current, float partialTick) {
        return lerpChannel(current, FastColor.ARGB32.red(sample(partialTick).color()) / 255.0F, partialTick);
    }

    public static float fogGreen(float current, float partialTick) {
        return lerpChannel(current, FastColor.ARGB32.green(sample(partialTick).color()) / 255.0F, partialTick);
    }

    public static float fogBlue(float current, float partialTick) {
        return lerpChannel(current, FastColor.ARGB32.blue(sample(partialTick).color()) / 255.0F, partialTick);
    }

    public static void renderTint(GuiGraphics graphics, float partialTick) {
        Visual visual = sample(partialTick);
        float amount = mix(visual) * TINT_STRENGTH;
        if (amount <= 0.001F) {
            return;
        }
        int alpha = Mth.clamp(Math.round(amount * 255.0F), 0, 255);
        int tint = FastColor.ARGB32.color(alpha, FastColor.ARGB32.red(visual.color()), FastColor.ARGB32.green(visual.color()), FastColor.ARGB32.blue(visual.color()));
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), tint);
    }

    private static float lerpChannel(float current, float target, float partialTick) {
        float mix = mix(sample(partialTick));
        if (mix <= 0.0F) {
            return current;
        }
        return Mth.lerp(mix, current, target);
    }

    private static float mix(Visual visual) {
        return visual.density() * (FastColor.ARGB32.alpha(visual.color()) / 255.0F);
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
        int from = fromDensity <= 0.0F && FastColor.ARGB32.alpha(fromColor) == 0 ? withAlpha(toColor, 0) : fromColor;
        int to = toDensity <= 0.0F && FastColor.ARGB32.alpha(toColor) == 0 ? withAlpha(fromColor, 0) : toColor;
        return new Visual(FastColor.ARGB32.lerp(t, from, to), Mth.lerp(t, fromDensity, toDensity));
    }

    private static int withAlpha(int color, int alpha) {
        return FastColor.ARGB32.color(alpha, FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color));
    }

    private static long gameTime() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        return level != null ? level.getGameTime() : 0L;
    }

    private record Visual(int color, float density) {
    }
}
