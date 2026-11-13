package com.workshopcraft.workshopcore.backdrop.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.workshopcraft.workshopcore.backdrop.Backdrop;
import com.workshopcraft.workshopcore.backdrop.BackdropLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Immediate-mode backdrop renderer for 1.21.1. */
public final class BackdropRenderer {
    private static final float WARP_VANISH_DISTANCE = 160.0F;
    private static final float WARP_START = 10.0F;
    private static final float WARP_PAST = 50.0F;

    private BackdropRenderer() {
    }

    public static void invalidate() {
    }

    public static void render(Backdrop backdrop, Matrix4fc modelView) {
        render(backdrop, modelView, 1.0F);
    }

    public static void render(Backdrop backdrop, Matrix4fc modelView, float alpha) {
        if (backdrop == null || alpha <= 0.0F) {
            return;
        }
        renderBackground(backdrop, alpha);
        renderForeground(backdrop, modelView, alpha, false, false);
    }

    static void renderBackground(Backdrop backdrop, float alpha) {
        renderLayers(backdrop, RenderSystem.getModelViewMatrix(), alpha, layer -> layer instanceof BackdropLayer.Color || layer instanceof BackdropLayer.Gradient);
    }

    static void renderForeground(Backdrop backdrop, Matrix4fc modelView, float alpha, boolean skipStars, boolean skipWarp) {
        renderLayers(backdrop, modelView, alpha, layer -> {
            if (skipStars && layer instanceof BackdropLayer.Stars) {
                return false;
            }
            if (skipWarp && layer instanceof BackdropLayer.Warp) {
                return false;
            }
            return !(layer instanceof BackdropLayer.Color) && !(layer instanceof BackdropLayer.Gradient);
        });
    }

    static void renderLayers(Backdrop backdrop, Matrix4fc modelView, float alpha, Predicate<BackdropLayer> include) {
        if (backdrop == null || alpha <= 0.0F) {
            return;
        }
        float seconds = seconds();
        float partial = currentPartial();
        Matrix4f matrix = new Matrix4f(modelView);
        for (BackdropLayer layer : backdrop.layers()) {
            if (include.test(layer)) {
                renderLayer(layer, matrix, seconds, partial, alpha);
            }
        }
    }

    private static void renderLayer(BackdropLayer layer, Matrix4f matrix, float seconds, float partial, float alpha) {
        switch (layer) {
            case BackdropLayer.Color color -> renderColor(color, matrix, alpha);
            case BackdropLayer.Gradient gradient -> renderGradient(gradient, matrix, alpha);
            case BackdropLayer.Stars stars -> renderStars(stars, matrix, seconds, partial, alpha);
            case BackdropLayer.Warp warp -> renderWarp(warp, matrix, seconds, alpha);
            case BackdropLayer.Body body -> renderBody(body, matrix, seconds, alpha);
        }
    }

    private static void renderColor(BackdropLayer.Color color, Matrix4f matrix, float alpha) {
        Vector4f tint = colorToVec(color.color(), 1.0F);
        tint.w *= alpha;
        drawSkyFan(matrix, tint);
    }

    private static void renderGradient(BackdropLayer.Gradient gradient, Matrix4f matrix, float alpha) {
        beginColor();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int rings = 16;
        int segs = 16;
        for (int r = 0; r < rings; r++) {
            float t0 = r / (float) rings;
            float t1 = (r + 1) / (float) rings;
            float y0 = 1.0F - 2.0F * t0;
            float y1 = 1.0F - 2.0F * t1;
            float r0 = (float) Math.sqrt(Math.max(0.0F, 1.0F - y0 * y0));
            float r1 = (float) Math.sqrt(Math.max(0.0F, 1.0F - y1 * y1));
            int c0 = scaleColor(sampleGradient(gradient, y0), alpha);
            int c1 = scaleColor(sampleGradient(gradient, y1), alpha);
            for (int s = 0; s < segs; s++) {
                float a0 = s * ((float) Math.PI * 2.0F) / segs;
                float a1 = (s + 1) * ((float) Math.PI * 2.0F) / segs;
                float x00 = r0 * Mth.cos(a0) * 100.0F, z00 = r0 * Mth.sin(a0) * 100.0F;
                float x01 = r0 * Mth.cos(a1) * 100.0F, z01 = r0 * Mth.sin(a1) * 100.0F;
                float x10 = r1 * Mth.cos(a0) * 100.0F, z10 = r1 * Mth.sin(a0) * 100.0F;
                float x11 = r1 * Mth.cos(a1) * 100.0F, z11 = r1 * Mth.sin(a1) * 100.0F;
                quad(builder, matrix, x00, y0 * 100.0F, z00, c0, x01, y0 * 100.0F, z01, c0, x11, y1 * 100.0F, z11, c1, x10, y1 * 100.0F, z10, c1);
            }
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void renderStars(BackdropLayer.Stars stars, Matrix4f matrix, float seconds, float partial, float alpha) {
        Matrix4f pose = new Matrix4f(matrix);
        if (stars.drift() != 0.0F) {
            pose.rotateY(stars.drift() * seconds * ((float) Math.PI / 180.0F));
        }
        if (stars.rotateWithTime()) {
            pose.rotateX(starAngle(partial));
        }
        List<StarSample> samples = new ArrayList<>();
        snapshotStars(stars, 0.0F, 0.0F, samples);
        // snapshot applies its own rotation when seconds/partial passed; we already rotated the pose, so sample unrotated
        samples.clear();
        int count = Math.max(0, Math.min(stars.count(), 8000));
        RandomSource random = RandomSource.create(stars.seed());
        int color = scaleColor(stars.color(), stars.brightness() * alpha);
        beginColor();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < count; i++) {
            float x = random.nextFloat() * 2.0F - 1.0F;
            float y = random.nextFloat() * 2.0F - 1.0F;
            float z = random.nextFloat() * 2.0F - 1.0F;
            float len2 = x * x + y * y + z * z;
            if (len2 < 0.01F || len2 > 1.0F) {
                i--;
                continue;
            }
            float size = 0.15F + random.nextFloat() * 0.1F;
            float rot = random.nextFloat() * (float) Math.PI * 2.0F;
            Vector3f center = new Vector3f(x, y, z).normalize(100.0F);
            Matrix3f rotation = new Matrix3f().rotateTowards(new Vector3f(center).negate(), new Vector3f(0.0F, 1.0F, 0.0F)).rotateZ(-rot);
            Vector3f a = new Vector3f(size, -size, 0.0F).mul(rotation).add(center);
            Vector3f b = new Vector3f(size, size, 0.0F).mul(rotation).add(center);
            Vector3f c = new Vector3f(-size, size, 0.0F).mul(rotation).add(center);
            Vector3f d = new Vector3f(-size, -size, 0.0F).mul(rotation).add(center);
            quad(builder, pose, a, color, b, color, c, color, d, color);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void renderWarp(BackdropLayer.Warp warp, Matrix4f matrix, float seconds, float alpha) {
        List<StreakSample> samples = snapshotWarp(warp, seconds);
        List<StreakDraw> draws = new ArrayList<>(samples.size());
        for (StreakSample sample : samples) {
            draws.add(new StreakDraw(sample.vanish(), sample.ray(), sample.side(), sample.dist(), sample.length(), sample.width(), sample.color(), 1.0F));
        }
        drawStreaks(draws, matrix, alpha);
    }

    private static void renderBody(BackdropLayer.Body body, Matrix4f matrix, float seconds, float alpha) {
        PoseStack pose = new PoseStack();
        pose.mulPose(matrix);
        if (body.orbitSpeed() != 0.0F) {
            Vector3f axis = body.orbitAxis().vector();
            pose.mulPose(com.mojang.math.Axis.of(axis).rotationDegrees(body.orbitSpeed() * seconds));
        }
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-body.azimuth()));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F - body.altitude()));
        if (body.spin() != 0.0F) {
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(body.spin() * 360.0F * seconds));
        }
        pose.translate(0.0F, 100.0F, 0.0F);
        float scale = 2.0F * 100.0F * (float) Math.tan(Math.toRadians(Math.max(0.1, body.size())) / 2.0);
        pose.scale(scale, 1.0F, scale);
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(body.texture());
        RenderSystem.setShaderTexture(0, texture.getId());
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f m = pose.last().pose();
        int color = scaleColor(0xFFFFFFFF, alpha);
        builder.addVertex(m, -1.0F, 0.0F, -1.0F).setUv(0.0F, 0.0F).setColor(color);
        builder.addVertex(m, 1.0F, 0.0F, -1.0F).setUv(1.0F, 0.0F).setColor(color);
        builder.addVertex(m, 1.0F, 0.0F, 1.0F).setUv(1.0F, 1.0F).setColor(color);
        builder.addVertex(m, -1.0F, 0.0F, 1.0F).setUv(0.0F, 1.0F).setColor(color);
        BufferUploader.drawWithShader(builder.buildOrThrow());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void drawSkyFan(Matrix4f matrix, Vector4f tint) {
        beginColor();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        int color = argb(tint);
        builder.addVertex(matrix, 0.0F, 16.0F, 0.0F).setColor(color);
        float x = 512.0F;
        for (int i = -180; i <= 180; i += 45) {
            builder.addVertex(matrix, x * Mth.cos(i * ((float) Math.PI / 180.0F)), 16.0F, 512.0F * Mth.sin(i * ((float) Math.PI / 180.0F))).setColor(color);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    static void drawStreaks(List<StreakDraw> streaks, Matrix4fc modelView, float alpha) {
        if (streaks.isEmpty() || alpha <= 0.0F) {
            return;
        }
        Matrix4f matrix = new Matrix4f(modelView);
        beginColor();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int drawn = 0;
        for (StreakDraw streak : streaks) {
            if (streak.length() <= 0.01F || streak.brightness() <= 0.0F) {
                continue;
            }
            Vector3f start = new Vector3f(streak.vanish()).fma(streak.dist(), streak.ray());
            Vector3f end = new Vector3f(streak.vanish()).fma(streak.dist() + streak.length(), streak.ray());
            Vector3f tailSide = new Vector3f(streak.side()).mul(streak.width() * 0.35F);
            Vector3f headSide = new Vector3f(streak.side()).mul(streak.width());
            float tTail = streak.dist() / WARP_VANISH_DISTANCE;
            float tHead = (streak.dist() + streak.length()) / WARP_VANISH_DISTANCE;
            float tailB = streak.brightness();
            float headB = streak.brightness();
            if (streak.warpCurve()) {
                tailB *= warpBrightness(tTail);
                headB *= warpBrightness(Math.max(tHead, tTail));
            }
            int tail = scaleColor(streak.color(), tailB * alpha);
            int head = scaleColor(streak.color(), headB * alpha);
            quad(builder, matrix,
                    start.x - tailSide.x, start.y - tailSide.y, start.z - tailSide.z, tail,
                    start.x + tailSide.x, start.y + tailSide.y, start.z + tailSide.z, tail,
                    end.x + headSide.x, end.y + headSide.y, end.z + headSide.z, head,
                    end.x - headSide.x, end.y - headSide.y, end.z - headSide.z, head);
            drawn++;
        }
        if (drawn == 0) {
            builder.build();
            return;
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    static void drawStarPoints(List<StarSample> stars, Matrix4fc modelView, float alpha) {
        if (stars.isEmpty() || alpha <= 0.0F) {
            return;
        }
        Matrix4f matrix = new Matrix4f(modelView);
        beginColor();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (StarSample star : stars) {
            Vector3f center = star.pos();
            float size = star.size();
            Vector3f toward = new Vector3f(center).negate().normalize();
            Vector3f side = new Vector3f();
            toward.cross(Math.abs(toward.y) > 0.99F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F), side);
            if (side.lengthSquared() < 1.0E-6F) {
                toward.cross(new Vector3f(1.0F, 0.0F, 0.0F), side);
            }
            side.normalize();
            Vector3f up = new Vector3f(side).cross(toward).normalize();
            Vector3f a = new Vector3f(center).fma(size, side).fma(-size, up);
            Vector3f b = new Vector3f(center).fma(size, side).fma(size, up);
            Vector3f c = new Vector3f(center).fma(-size, side).fma(size, up);
            Vector3f d = new Vector3f(center).fma(-size, side).fma(-size, up);
            int color = scaleColor(star.color(), alpha);
            quad(builder, matrix, a, color, b, color, c, color, d, color);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void beginColor() {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
    }

    private static void quad(BufferBuilder builder, Matrix4f matrix, Vector3f a, int ca, Vector3f b, int cb, Vector3f c, int cc, Vector3f d, int cd) {
        quad(builder, matrix, a.x, a.y, a.z, ca, b.x, b.y, b.z, cb, c.x, c.y, c.z, cc, d.x, d.y, d.z, cd);
    }

    private static void quad(BufferBuilder builder, Matrix4f matrix, float x0, float y0, float z0, int c0, float x1, float y1, float z1, int c1, float x2, float y2, float z2, int c2, float x3, float y3, float z3, int c3) {
        builder.addVertex(matrix, x0, y0, z0).setColor(c0);
        builder.addVertex(matrix, x1, y1, z1).setColor(c1);
        builder.addVertex(matrix, x2, y2, z2).setColor(c2);
        builder.addVertex(matrix, x3, y3, z3).setColor(c3);
    }

    private static int argb(Vector4f tint) {
        return scaleColor(((int) (tint.w * 255.0F) << 24) | ((int) (tint.x * 255.0F) << 16) | ((int) (tint.y * 255.0F) << 8) | (int) (tint.z * 255.0F), 1.0F);
    }
    private static int sampleGradient(BackdropLayer.Gradient gradient, float y) {
        if (y >= 0.0F) {
            return lerpColor(gradient.horizon(), gradient.top(), y);
        }
        return lerpColor(gradient.horizon(), gradient.bottom(), -y);
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

    private static Vector3f cone(Vector3f axis, float spreadDeg, RandomSource random) {
        float maxAngle = (float) Math.toRadians(Math.max(0.0F, spreadDeg));
        float cosMax = (float) Math.cos(maxAngle);
        float cosTheta = 1.0F - random.nextFloat() * (1.0F - cosMax);
        float sinTheta = (float) Math.sqrt(Math.max(0.0F, 1.0F - cosTheta * cosTheta));
        float phi = random.nextFloat() * ((float) Math.PI * 2.0F);
        Vector3f up = Math.abs(axis.y) > 0.99F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f right = new Vector3f(axis).cross(up).normalize();
        up = new Vector3f(right).cross(axis).normalize();
        return new Vector3f(axis).mul(cosTheta)
                .add(right.mul(sinTheta * (float) Math.cos(phi), new Vector3f()))
                .add(up.mul(sinTheta * (float) Math.sin(phi), new Vector3f()))
                .normalize();
    }

    static float warpBrightness(float t) {
        if (t <= 0.05F) {
            return 0.0F;
        }
        if (t < 1.0F) {
            return t * t;
        }
        return Mth.clamp(1.6F - t, 0.0F, 1.0F);
    }

    private static int scaleColor(int argb, float brightness) {
        brightness = Mth.clamp(brightness, 0.0F, 1.0F);
        int a = (argb >>> 24) & 255;
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        return (int) (a * brightness) << 24
                | (int) (r * brightness) << 16
                | (int) (g * brightness) << 8
                | (int) (b * brightness);
    }

    static boolean hasWarp(Backdrop backdrop) {
        if (backdrop == null) {
            return false;
        }
        for (BackdropLayer layer : backdrop.layers()) {
            if (layer instanceof BackdropLayer.Warp) {
                return true;
            }
        }
        return false;
    }

    static boolean hasStars(Backdrop backdrop) {
        if (backdrop == null) {
            return false;
        }
        for (BackdropLayer layer : backdrop.layers()) {
            if (layer instanceof BackdropLayer.Stars) {
                return true;
            }
        }
        return false;
    }

    static BackdropLayer.Warp firstWarp(Backdrop backdrop) {
        if (backdrop == null) {
            return null;
        }
        for (BackdropLayer layer : backdrop.layers()) {
            if (layer instanceof BackdropLayer.Warp warp) {
                return warp;
            }
        }
        return null;
    }

    static List<StarSample> snapshotStars(Backdrop backdrop, float seconds, float partial) {
        List<StarSample> out = new ArrayList<>();
        if (backdrop == null) {
            return out;
        }
        for (BackdropLayer layer : backdrop.layers()) {
            if (layer instanceof BackdropLayer.Stars stars) {
                snapshotStars(stars, seconds, partial, out);
            }
        }
        return out;
    }

    private static void snapshotStars(BackdropLayer.Stars stars, float seconds, float partial, List<StarSample> out) {
        int count = Math.max(0, Math.min(stars.count(), 8000));
        RandomSource random = RandomSource.create(stars.seed());
        Matrix4f rotation = new Matrix4f();
        if (stars.drift() != 0.0F) {
            rotation.rotateY(stars.drift() * seconds * ((float) Math.PI / 180.0F));
        }
        if (stars.rotateWithTime()) {
            rotation.rotateX(starAngle(partial));
        }
        int color = scaleColor(stars.color(), stars.brightness());
        for (int i = 0; i < count; i++) {
            float x = random.nextFloat() * 2.0F - 1.0F;
            float y = random.nextFloat() * 2.0F - 1.0F;
            float z = random.nextFloat() * 2.0F - 1.0F;
            float len2 = x * x + y * y + z * z;
            if (len2 < 0.01F || len2 > 1.0F) {
                i--;
                continue;
            }
            float size = 0.15F + random.nextFloat() * 0.1F;
            // consume the same rotation sample as buildStars
            random.nextFloat();
            Vector3f center = new Vector3f(x, y, z).normalize(100.0F);
            rotation.transformPosition(center);
            out.add(new StarSample(center, size, color));
        }
    }

    static List<StreakSample> snapshotWarp(Backdrop backdrop, float seconds) {
        List<StreakSample> out = new ArrayList<>();
        if (backdrop == null) {
            return out;
        }
        for (BackdropLayer layer : backdrop.layers()) {
            if (layer instanceof BackdropLayer.Warp warp) {
                snapshotWarp(warp, seconds, out);
            }
        }
        return out;
    }

    static List<StreakSample> starsAsStreaks(List<StarSample> stars, BackdropLayer.Warp warp) {
        List<StreakSample> out = new ArrayList<>();
        if (warp == null) {
            return out;
        }
        Vector3f axis = warp.direction().vector().normalize();
        Vector3f vanish = new Vector3f(axis).mul(WARP_VANISH_DISTANCE);
        float speed = Math.max(0.0F, warp.speed()) * 40.0F;
        float baseLen = Math.max(0.5F, warp.length());
        for (StarSample star : stars) {
            Vector3f ray = new Vector3f(star.pos()).sub(vanish);
            if (ray.lengthSquared() < 1.0E-6F) {
                ray.set(axis).negate();
            }
            ray.normalize();
            Vector3f side = new Vector3f();
            ray.cross(Math.abs(ray.y) > 0.99F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F), side);
            if (side.lengthSquared() < 1.0E-6F) {
                ray.cross(new Vector3f(1.0F, 0.0F, 0.0F), side);
            }
            side.normalize();
            float dist = star.pos().distance(vanish);
            out.add(new StreakSample(new Vector3f(vanish), ray, side, dist, Math.max(star.size(), 0.2F), Math.max(star.size() * 0.5F, 0.08F), star.color(), speed, baseLen));
        }
        return out;
    }

    record StarSample(Vector3f pos, float size, int color) {
    }

    record StreakSample(Vector3f vanish, Vector3f ray, Vector3f side, float dist, float length, float width, int color, float speed, float growLength) {
        StreakSample(Vector3f vanish, Vector3f ray, Vector3f side, float dist, float length, float width, int color, float speed) {
            this(vanish, ray, side, dist, length, width, color, speed, length);
        }
    }

    record StreakDraw(Vector3f vanish, Vector3f ray, Vector3f side, float dist, float length, float width, int color, float brightness, boolean warpCurve) {
        StreakDraw(Vector3f vanish, Vector3f ray, Vector3f side, float dist, float length, float width, int color, float brightness) {
            this(vanish, ray, side, dist, length, width, color, brightness, true);
        }
    }


    private static Vector4f colorToVec(int argb, float brightness) {
        float a = ((argb >>> 24) & 255) / 255.0F * brightness;
        float r = ((argb >> 16) & 255) / 255.0F * brightness;
        float g = ((argb >> 8) & 255) / 255.0F * brightness;
        float b = (argb & 255) / 255.0F * brightness;
        return new Vector4f(r, g, b, a);
    }

    static float currentSeconds() {
        return seconds();
    }


    static float currentPartial() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }

    static float vanishDistance() {
        return WARP_VANISH_DISTANCE;
    }

    private static void snapshotWarp(BackdropLayer.Warp warp, float seconds, List<StreakSample> out) {
        int count = Math.max(0, Math.min(warp.count(), 400));
        RandomSource random = RandomSource.create(42L);
        Vector3f axis = warp.direction().vector().normalize();
        Vector3f vanish = new Vector3f(axis).mul(WARP_VANISH_DISTANCE);
        Vector3f towardCamera = new Vector3f(axis).negate();
        int color = warp.color();
        float travel = WARP_VANISH_DISTANCE + WARP_PAST - WARP_START;
        float speed = Math.max(0.0F, warp.speed()) * 40.0F;
        float baseLen = Math.max(0.5F, warp.length());
        for (int i = 0; i < count; i++) {
            Vector3f ray = cone(towardCamera, warp.spread(), random);
            Vector3f side = new Vector3f();
            ray.cross(Math.abs(ray.y) > 0.99F ? new Vector3f(1.0F, 0.0F, 0.0F) : new Vector3f(0.0F, 1.0F, 0.0F), side);
            if (side.lengthSquared() < 1.0E-6F) {
                ray.cross(new Vector3f(1.0F, 0.0F, 0.0F), side);
            }
            side.normalize();
            float dist = (seconds * speed + random.nextFloat() * travel) % travel + WARP_START;
            float tTail = dist / WARP_VANISH_DISTANCE;
            float segLen = baseLen * (0.35F + 1.8F * Mth.clamp(tTail, 0.0F, 1.2F));
            float width = 0.06F + 0.28F * Mth.clamp(tTail, 0.0F, 1.2F);
            out.add(new StreakSample(new Vector3f(vanish), new Vector3f(ray), new Vector3f(side), dist, segLen, width, color, speed));
        }
    }

    private static List<StreakSample> snapshotWarp(BackdropLayer.Warp warp, float seconds) {
        List<StreakSample> out = new ArrayList<>();
        snapshotWarp(warp, seconds, out);
        return out;
    }

    private static float starAngle(float partial) {
        var level = Minecraft.getInstance().level;
        float time = level == null ? 0.0F : level.getTimeOfDay(partial);
        return time * ((float) Math.PI * 2.0F);
    }

    private static float seconds() {
        var level = Minecraft.getInstance().level;
        float partial = currentPartial();
        long time = level == null ? 0L : level.getGameTime();
        return (time + partial) / 20.0F;
    }
}
