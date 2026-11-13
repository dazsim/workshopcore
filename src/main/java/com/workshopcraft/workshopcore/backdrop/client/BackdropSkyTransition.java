package com.workshopcraft.workshopcore.backdrop.client;

import com.workshopcraft.workshopcore.backdrop.Backdrop;
import com.workshopcraft.workshopcore.backdrop.BackdropLayer;
import com.workshopcraft.workshopcore.backdrop.BackdropTransition;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import org.joml.Matrix4fc;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Client playback of a backdrop change. Warp morphs reuse in-flight streaks and
 * existing stars; other changes crossfade.
 */
final class BackdropSkyTransition {
    private final ResourceKey<Level> dimension;
    private final Optional<Backdrop> from;
    private final Optional<Backdrop> to;
    private final int durationTicks;
    private final boolean warpMorph;
    private final boolean enteringWarp;
    private final long startTick;
    private final float startPartial;
    private final List<BackdropRenderer.StarSample> fromStars;
    private final List<BackdropRenderer.StreakSample> fromStreaks;
    private final BackdropLayer.Warp destWarp;
    private final BackdropLayer.Warp sourceWarp;

    private BackdropSkyTransition(
            ResourceKey<Level> dimension,
            Optional<Backdrop> from,
            Optional<Backdrop> to,
            int durationTicks,
            boolean warpMorph,
            boolean enteringWarp,
            long startTick,
            float startPartial,
            List<BackdropRenderer.StarSample> fromStars,
            List<BackdropRenderer.StreakSample> fromStreaks,
            BackdropLayer.Warp destWarp,
            BackdropLayer.Warp sourceWarp
    ) {
        this.dimension = dimension;
        this.from = from;
        this.to = to;
        this.durationTicks = durationTicks;
        this.warpMorph = warpMorph;
        this.enteringWarp = enteringWarp;
        this.startTick = startTick;
        this.startPartial = startPartial;
        this.fromStars = fromStars;
        this.fromStreaks = fromStreaks;
        this.destWarp = destWarp;
        this.sourceWarp = sourceWarp;
    }

    static BackdropSkyTransition start(
            ResourceKey<Level> dimension,
            Optional<Backdrop> from,
            Optional<Backdrop> to,
            int durationTicks,
            BackdropTransition.Mode mode,
            BackdropSkyTransition previous
    ) {
        float seconds = BackdropRenderer.currentSeconds();
        float partial = BackdropRenderer.currentPartial();
        long tick = gameTick();
        List<BackdropRenderer.StarSample> stars = new ArrayList<>();
        List<BackdropRenderer.StreakSample> streaks = new ArrayList<>();
        if (previous != null && !previous.finished(tick, partial)) {
            previous.captureVisual(previous.progress(tick, partial), stars, streaks);
        } else {
            from.ifPresent(backdrop -> {
                stars.addAll(BackdropRenderer.snapshotStars(backdrop, seconds, partial));
                streaks.addAll(BackdropRenderer.snapshotWarp(backdrop, seconds));
            });
        }
        BackdropLayer.Warp destWarp = to.map(BackdropRenderer::firstWarp).orElse(null);
        BackdropLayer.Warp sourceWarp = from.map(BackdropRenderer::firstWarp).orElse(null);
        boolean fromWarp = !streaks.isEmpty() || BackdropRenderer.hasWarp(from.orElse(null));
        boolean toWarp = destWarp != null;
        boolean fromStars = !stars.isEmpty() || BackdropRenderer.hasStars(from.orElse(null));
        boolean warpMorph = switch (mode == null ? BackdropTransition.Mode.AUTO : mode) {
            case CROSSFADE -> false;
            case WARP -> fromWarp || toWarp || fromStars || BackdropRenderer.hasStars(to.orElse(null));
            case AUTO -> (toWarp && (fromStars || fromWarp)) || (fromWarp && !toWarp) || (fromWarp && toWarp);
        };
        boolean enteringWarp = warpMorph && toWarp;
        if (enteringWarp && destWarp != null && !stars.isEmpty() && streaks.isEmpty()) {
            // stars will be drawn as growing streaks from the destination vanishing point
        }
        return new BackdropSkyTransition(
                dimension, from, to, Math.max(0, durationTicks), warpMorph, enteringWarp,
                tick, partial, stars, streaks, destWarp, sourceWarp
        );
    }

    ResourceKey<Level> dimension() {
        return dimension;
    }

    boolean finished(long tick, float partial) {
        return progress(tick, partial) >= 1.0F;
    }

    boolean appliesTo(ResourceKey<Level> current) {
        return dimension.equals(current);
    }

    float progress(long tick, float partial) {
        if (durationTicks <= 0) {
            return 1.0F;
        }
        float elapsed = (tick + partial) - (startTick + startPartial);
        return Mth.clamp(elapsed / durationTicks, 0.0F, 1.0F);
    }

    boolean render(Matrix4fc modelView) {
        float partial = BackdropRenderer.currentPartial();
        long tick = gameTick();
        float p = progress(tick, partial);
        if (p >= 1.0F) {
            return false;
        }
        if (warpMorph) {
            renderWarpMorph(modelView, p);
        } else {
            renderCrossfade(modelView, p);
        }
        return true;
    }

    private void renderCrossfade(Matrix4fc modelView, float p) {
        if (to.isPresent()) {
            BackdropRenderer.renderBackground(to.get(), 1.0F);
            if (from.isPresent()) {
                BackdropRenderer.renderForeground(from.get(), modelView, 1.0F - p, false, false);
            }
            BackdropRenderer.renderForeground(to.get(), modelView, p, false, false);
        } else if (from.isPresent()) {
            BackdropRenderer.render(from.get(), modelView, 1.0F - p);
        }
    }

    private void renderWarpMorph(Matrix4fc modelView, float p) {
        if (to.isPresent()) {
            BackdropRenderer.renderBackground(to.get(), 1.0F);
        } else if (from.isPresent()) {
            BackdropRenderer.renderBackground(from.get(), 1.0F);
        }

        if (from.isPresent()) {
            BackdropRenderer.renderForeground(from.get(), modelView, 1.0F - p, true, true);
        }
        if (to.isPresent()) {
            Backdrop dest = to.get();
            float destStarAlpha = enteringWarp ? smooth(p, 0.45F, 1.0F) : smooth(p, 0.2F, 0.85F);
            float destWarpAlpha = enteringWarp ? smooth(p, 0.35F, 1.0F) : 0.0F;
            BackdropRenderer.renderLayers(dest, modelView, destStarAlpha, layer -> layer instanceof BackdropLayer.Stars || layer instanceof BackdropLayer.Body);
            if (destWarpAlpha > 0.0F) {
                BackdropRenderer.renderLayers(dest, modelView, destWarpAlpha, layer -> layer instanceof BackdropLayer.Warp);
            }
        }

        List<BackdropRenderer.StreakDraw> draws = new ArrayList<>();
        if (enteringWarp) {
            // Keep the original points visible at the start so the field does not go black
            // before the stars have elongated into streaks.
            float pointAlpha = 1.0F - smooth(p, 0.0F, 0.4F);
            if (pointAlpha > 0.0F && !fromStars.isEmpty()) {
                BackdropRenderer.drawStarPoints(fromStars, modelView, pointAlpha);
            }
            addEnteringStarStreaks(draws, p);
            addLeavingStreaks(draws, p, 0.35F);
        } else {
            addLeavingStreaks(draws, p, 1.0F);
        }
        BackdropRenderer.drawStreaks(draws, modelView, 1.0F);
    }

    private void addEnteringStarStreaks(List<BackdropRenderer.StreakDraw> draws, float p) {
        if (fromStars.isEmpty()) {
            return;
        }
        BackdropLayer.Warp guide = destWarp != null ? destWarp : sourceWarp;
        if (guide == null) {
            return;
        }
        float ease = p * p;
        float durationSeconds = durationTicks / 20.0F;
        float vMax = Math.max(0.0F, guide.speed()) * 40.0F;
        float travel = vMax * durationSeconds * (p * p * p) / 3.0F;
        float baseLen = Math.max(0.5F, guide.length());
        int warpColor = guide.color();
        for (BackdropRenderer.StreakSample streak : BackdropRenderer.starsAsStreaks(fromStars, guide)) {
            float dist = streak.dist() + travel;
            float tTail = dist / BackdropRenderer.vanishDistance();
            float fullLen = baseLen * (0.35F + 1.8F * Mth.clamp(tTail, 0.0F, 1.2F));
            float length = Mth.lerp(ease, streak.length(), fullLen);
            float width = Mth.lerp(ease, streak.width(), 0.06F + 0.28F * Mth.clamp(tTail, 0.0F, 1.2F));
            int color = lerpColor(streak.color(), warpColor, ease);
            float tWarp = BackdropRenderer.warpBrightness(tTail);
            // Start at full star brightness and ease into the warp brightness curve.
            float brightness = Mth.lerp(ease, 1.0F, Math.max(tWarp, 0.15F));
            draws.add(new BackdropRenderer.StreakDraw(streak.vanish(), streak.ray(), streak.side(), dist, length, width, color, brightness, false));
        }
    }

    private void addLeavingStreaks(List<BackdropRenderer.StreakDraw> draws, float p, float weight) {
        if (fromStreaks.isEmpty() || weight <= 0.0F) {
            return;
        }
        float remain = 1.0F - p;
        float fade = remain * remain;
        float durationSeconds = durationTicks / 20.0F;
        float travelled = (1.0F - remain * remain * remain) / 3.0F;
        for (BackdropRenderer.StreakSample streak : fromStreaks) {
            float dist = streak.dist() + streak.speed() * durationSeconds * travelled;
            float length = streak.length() * Mth.lerp(p, 1.0F, 0.05F);
            float width = streak.width() * Mth.lerp(p, 1.0F, 0.35F);
            float brightness = fade * weight;
            if (brightness <= 0.01F) {
                continue;
            }
            draws.add(new BackdropRenderer.StreakDraw(streak.vanish(), streak.ray(), streak.side(), dist, length, width, streak.color(), brightness));
        }
    }

    private void captureVisual(float p, List<BackdropRenderer.StarSample> stars, List<BackdropRenderer.StreakSample> streaks) {
        if (!warpMorph) {
            from.ifPresent(backdrop -> stars.addAll(BackdropRenderer.snapshotStars(backdrop, BackdropRenderer.currentSeconds(), BackdropRenderer.currentPartial())));
            from.ifPresent(backdrop -> streaks.addAll(BackdropRenderer.snapshotWarp(backdrop, BackdropRenderer.currentSeconds())));
            if (to.isPresent() && p > 0.5F) {
                stars.clear();
                streaks.clear();
                stars.addAll(BackdropRenderer.snapshotStars(to.get(), BackdropRenderer.currentSeconds(), BackdropRenderer.currentPartial()));
                streaks.addAll(BackdropRenderer.snapshotWarp(to.get(), BackdropRenderer.currentSeconds()));
            }
            return;
        }
        if (enteringWarp) {
            BackdropLayer.Warp guide = destWarp != null ? destWarp : sourceWarp;
            if (guide != null && !fromStars.isEmpty()) {
                float ease = p * p;
                float durationSeconds = durationTicks / 20.0F;
                float vMax = Math.max(0.0F, guide.speed()) * 40.0F;
                float travel = vMax * durationSeconds * (p * p * p) / 3.0F;
                float baseLen = Math.max(0.5F, guide.length());
                for (BackdropRenderer.StreakSample streak : BackdropRenderer.starsAsStreaks(fromStars, guide)) {
                    float dist = streak.dist() + travel;
                    float tTail = dist / BackdropRenderer.vanishDistance();
                    float fullLen = baseLen * (0.35F + 1.8F * Mth.clamp(tTail, 0.0F, 1.2F));
                    float length = Mth.lerp(ease, streak.length(), fullLen);
                    float width = Mth.lerp(ease, streak.width(), 0.06F + 0.28F * Mth.clamp(tTail, 0.0F, 1.2F));
                    int color = lerpColor(streak.color(), guide.color(), ease);
                    streaks.add(new BackdropRenderer.StreakSample(streak.vanish(), streak.ray(), streak.side(), dist, length, width, color, streak.speed(), baseLen));
                }
            }
            addLeavingAsSamples(streaks, p);
            if (destWarp != null && p > 0.35F) {
                streaks.addAll(BackdropRenderer.snapshotWarp(to.orElse(null), BackdropRenderer.currentSeconds()));
            }
        } else {
            addLeavingAsSamples(streaks, p);
        }
    }

    private void addLeavingAsSamples(List<BackdropRenderer.StreakSample> streaks, float p) {
        float remain = 1.0F - p;
        float durationSeconds = durationTicks / 20.0F;
        float travelled = (1.0F - remain * remain * remain) / 3.0F;
        for (BackdropRenderer.StreakSample streak : fromStreaks) {
            float dist = streak.dist() + streak.speed() * durationSeconds * travelled;
            float length = streak.length() * Mth.lerp(p, 1.0F, 0.05F);
            float width = streak.width() * Mth.lerp(p, 1.0F, 0.35F);
            streaks.add(new BackdropRenderer.StreakSample(streak.vanish(), streak.ray(), streak.side(), dist, length, width, streak.color(), streak.speed() * remain * remain, streak.growLength()));
        }
    }

    private static float smooth(float p, float start, float end) {
        if (p <= start) {
            return 0.0F;
        }
        if (p >= end) {
            return 1.0F;
        }
        float t = (p - start) / (end - start);
        return t * t * (3.0F - 2.0F * t);
    }

    private static int lerpColor(int a, int b, float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return (int) (aa + (ba - aa) * t) << 24
                | (int) (ar + (br - ar) * t) << 16
                | (int) (ag + (bg - ag) * t) << 8
                | (int) (ab + (bb - ab) * t);
    }

    private static long gameTick() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }
}
