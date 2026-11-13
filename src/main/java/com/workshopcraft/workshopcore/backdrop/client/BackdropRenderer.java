package com.workshopcraft.workshopcore.backdrop.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.workshopcraft.workshopcore.backdrop.Backdrop;
import com.workshopcraft.workshopcore.backdrop.BackdropLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/** Draws backdrop layers with the 26.x GPU pipelines. */
public final class BackdropRenderer {
    private static final RenderSystem.AutoStorageIndexBuffer QUADS = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
    private static GpuBuffer skyDisc;
    private static final Map<String, CachedMesh> MESHES = new HashMap<>();

    private BackdropRenderer() {
    }

    public static void invalidate() {
        close(skyDisc);
        skyDisc = null;
        MESHES.values().forEach(CachedMesh::close);
        MESHES.clear();
    }

    public static void render(Backdrop backdrop, Matrix4fc modelView) {
        ensureSkyDisc();
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.set(modelView);
        float seconds = seconds();
        float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (BackdropLayer layer : backdrop.layers()) {
            switch (layer) {
                case BackdropLayer.Color color -> renderColor(color);
                case BackdropLayer.Gradient gradient -> renderGradient(gradient);
                case BackdropLayer.Stars stars -> renderStars(stars, stack, seconds, partial);
                case BackdropLayer.Warp warp -> renderWarp(warp, stack, seconds);
                case BackdropLayer.Body body -> renderBody(body, stack, seconds);
            }
        }
        stack.popMatrix();
    }

    private static void renderColor(BackdropLayer.Color color) {
        drawFan(RenderPipelines.SKY, skyDisc, 10, ARGB.vector4fFromARGB32(color.color()), RenderSystem.getModelViewMatrix());
    }

    private static void renderGradient(BackdropLayer.Gradient gradient) {
        String key = "grad:" + gradient.top() + ":" + gradient.horizon() + ":" + gradient.bottom();
        CachedMesh mesh = MESHES.computeIfAbsent(key, k -> buildGradient(gradient));
        drawIndexed(RenderPipelines.DEBUG_QUADS, mesh, new Vector4f(1, 1, 1, 1), RenderSystem.getModelViewMatrix(), null);
    }

    private static void renderStars(BackdropLayer.Stars stars, Matrix4fStack stack, float seconds, float partial) {
        String key = "stars:" + stars.count() + ":" + stars.seed();
        CachedMesh mesh = MESHES.computeIfAbsent(key, k -> buildStars(stars));
        stack.pushMatrix();
        if (stars.drift() != 0.0F) {
            stack.rotateY(stars.drift() * seconds * ((float) Math.PI / 180.0F));
        }
        if (stars.rotateWithTime()) {
            stack.rotateX(starAngle(partial));
        }
        Vector4f tint = colorToVec(stars.color(), stars.brightness());
        drawIndexed(RenderPipelines.STARS, mesh, tint, stack, null);
        stack.popMatrix();
    }

    private static void renderWarp(BackdropLayer.Warp warp, Matrix4fStack stack, float seconds) {
        CachedMesh mesh = buildWarp(warp, seconds);
        try {
            drawIndexed(RenderPipelines.DEBUG_QUADS, mesh, new Vector4f(1, 1, 1, 1), stack, null);
        } finally {
            mesh.close();
        }
    }

    private static void renderBody(BackdropLayer.Body body, Matrix4fStack stack, float seconds) {
        CachedMesh mesh = MESHES.computeIfAbsent("body", k -> buildUnitQuad());
        PoseStack pose = new PoseStack();
        if (body.orbitSpeed() != 0.0F) {
            Vector3f axis = body.orbitAxis().vector();
            pose.mulPose(com.mojang.math.Axis.of(axis).rotationDegrees(body.orbitSpeed() * seconds));
        }
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-body.azimuth()));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F - body.altitude()));
        if (body.spin() != 0.0F) {
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(body.spin() * 360.0F * seconds));
        }
        stack.pushMatrix();
        stack.mul(pose.last().pose());
        stack.translate(0.0F, 100.0F, 0.0F);
        float scale = 2.0F * 100.0F * (float) Math.tan(Math.toRadians(Math.max(0.1, body.size())) / 2.0);
        stack.scale(scale, 1.0F, scale);
        AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(body.texture());
        drawIndexed(RenderPipelines.CELESTIAL, mesh, new Vector4f(1, 1, 1, 1), stack, texture);
        stack.popMatrix();
    }

    private static void ensureSkyDisc() {
        if (skyDisc != null) {
            return;
        }
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(10 * DefaultVertexFormat.POSITION.getVertexSize())) {
            BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
            builder.addVertex(0.0F, 16.0F, 0.0F);
            float x = 512.0F;
            for (int i = -180; i <= 180; i += 45) {
                builder.addVertex(x * Mth.cos(i * ((float) Math.PI / 180.0F)), 16.0F, 512.0F * Mth.sin(i * ((float) Math.PI / 180.0F)));
            }
            try (MeshData mesh = builder.buildOrThrow()) {
                skyDisc = RenderSystem.getDevice().createBuffer(() -> "workshopcore sky disc", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            }
        }
    }

    private static CachedMesh buildGradient(BackdropLayer.Gradient gradient) {
        int rings = 16;
        int segs = 16;
        int quads = rings * segs;
        int vtxSize = DefaultVertexFormat.POSITION_COLOR.getVertexSize();
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(quads * 4 * vtxSize)) {
            BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int r = 0; r < rings; r++) {
                float t0 = r / (float) rings;
                float t1 = (r + 1) / (float) rings;
                float y0 = 1.0F - 2.0F * t0;
                float y1 = 1.0F - 2.0F * t1;
                float r0 = (float) Math.sqrt(Math.max(0.0F, 1.0F - y0 * y0));
                float r1 = (float) Math.sqrt(Math.max(0.0F, 1.0F - y1 * y1));
                int c0 = sampleGradient(gradient, y0);
                int c1 = sampleGradient(gradient, y1);
                for (int s = 0; s < segs; s++) {
                    float a0 = s * ((float) Math.PI * 2) / segs;
                    float a1 = (s + 1) * ((float) Math.PI * 2) / segs;
                    float x00 = r0 * Mth.cos(a0) * 100.0F, z00 = r0 * Mth.sin(a0) * 100.0F;
                    float x01 = r0 * Mth.cos(a1) * 100.0F, z01 = r0 * Mth.sin(a1) * 100.0F;
                    float x10 = r1 * Mth.cos(a0) * 100.0F, z10 = r1 * Mth.sin(a0) * 100.0F;
                    float x11 = r1 * Mth.cos(a1) * 100.0F, z11 = r1 * Mth.sin(a1) * 100.0F;
                    builder.addVertex(x00, y0 * 100.0F, z00).setColor(c0);
                    builder.addVertex(x10, y1 * 100.0F, z10).setColor(c1);
                    builder.addVertex(x11, y1 * 100.0F, z11).setColor(c1);
                    builder.addVertex(x01, y0 * 100.0F, z01).setColor(c0);
                }
            }
            return cached("workshopcore gradient", builder, quads * 6);
        }
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

    private static CachedMesh buildStars(BackdropLayer.Stars stars) {
        int count = Math.max(0, Math.min(stars.count(), 8000));
        RandomSource random = RandomSource.create(stars.seed());
        int vtxSize = DefaultVertexFormat.POSITION.getVertexSize();
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(vtxSize * count * 4)) {
            BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
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
                Vector3f center = new Vector3f(x, y, z).normalize(100.0F);
                float rot = random.nextFloat() * (float) Math.PI * 2.0F;
                Matrix3f rotation = new Matrix3f().rotateTowards(new Vector3f(center).negate(), new Vector3f(0.0F, 1.0F, 0.0F)).rotateZ(-rot);
                builder.addVertex(new Vector3f(size, -size, 0.0F).mul(rotation).add(center));
                builder.addVertex(new Vector3f(size, size, 0.0F).mul(rotation).add(center));
                builder.addVertex(new Vector3f(-size, size, 0.0F).mul(rotation).add(center));
                builder.addVertex(new Vector3f(-size, -size, 0.0F).mul(rotation).add(center));
            }
            return cached("workshopcore stars", builder, count * 6);
        }
    }

    private static final float WARP_VANISH_DISTANCE = 160.0F;
    private static final float WARP_START = 10.0F;
    private static final float WARP_PAST = 50.0F;

    /**
     * Streaks lie on rays from a fixed vanishing point along {@code warp.direction()}.
     * They travel from that point toward the viewer, growing longer and brighter as they pass.
     * {@code spread} is the cone half-angle in degrees around that direction.
     */
    private static CachedMesh buildWarp(BackdropLayer.Warp warp, float seconds) {
        int count = Math.max(0, Math.min(warp.count(), 400));
        RandomSource random = RandomSource.create(42L);
        int vtxSize = DefaultVertexFormat.POSITION_COLOR.getVertexSize();
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(vtxSize * count * 4)) {
            BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
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
                float tHead = (dist + segLen) / WARP_VANISH_DISTANCE;
                float width = 0.06F + 0.28F * Mth.clamp(tTail, 0.0F, 1.2F);
                int tail = scaleColor(color, warpBrightness(tTail));
                int head = scaleColor(color, warpBrightness(tHead));
                Vector3f start = new Vector3f(vanish).fma(dist, ray);
                Vector3f end = new Vector3f(vanish).fma(dist + segLen, ray);
                Vector3f tailSide = new Vector3f(side).mul(width * 0.35F);
                Vector3f headSide = new Vector3f(side).mul(width);
                builder.addVertex(start.x - tailSide.x, start.y - tailSide.y, start.z - tailSide.z).setColor(tail);
                builder.addVertex(start.x + tailSide.x, start.y + tailSide.y, start.z + tailSide.z).setColor(tail);
                builder.addVertex(end.x + headSide.x, end.y + headSide.y, end.z + headSide.z).setColor(head);
                builder.addVertex(end.x - headSide.x, end.y - headSide.y, end.z - headSide.z).setColor(head);
            }
            return cached("workshopcore warp", builder, count * 6);
        }
    }

    /** Unit vector in a cone of half-angle {@code spreadDeg} around {@code axis}. */
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

    private static float warpBrightness(float t) {
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

    private static CachedMesh buildUnitQuad() {
        int vtxSize = DefaultVertexFormat.POSITION_TEX.getVertexSize();
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(4 * vtxSize)) {
            BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            builder.addVertex(-1.0F, 0.0F, -1.0F).setUv(0.0F, 0.0F);
            builder.addVertex(1.0F, 0.0F, -1.0F).setUv(1.0F, 0.0F);
            builder.addVertex(1.0F, 0.0F, 1.0F).setUv(1.0F, 1.0F);
            builder.addVertex(-1.0F, 0.0F, 1.0F).setUv(0.0F, 1.0F);
            return cached("workshopcore body quad", builder, 6);
        }
    }

    private static CachedMesh cached(String name, BufferBuilder builder, int indexCount) {
        try (MeshData mesh = builder.buildOrThrow()) {
            GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> name, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            return new CachedMesh(buffer, indexCount);
        }
    }

    private static void drawFan(RenderPipeline pipeline, GpuBuffer buffer, int vertexCount, Vector4f color, Matrix4fc modelView) {
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(modelView, color, new Vector3f(), new Matrix4f());
        GpuTextureView colorView = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
        GpuTextureView depthView = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "workshopcore sky", colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.setVertexBuffer(0, buffer);
            pass.draw(0, vertexCount);
        }
    }

    private static void drawIndexed(RenderPipeline pipeline, CachedMesh mesh, Vector4f color, Matrix4fc modelView, AbstractTexture texture) {
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(modelView, color, new Vector3f(), new Matrix4f());
        GpuTextureView colorView = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
        GpuTextureView depthView = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
        GpuBuffer indices = QUADS.getBuffer(mesh.indexCount);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "workshopcore sky layer", colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            if (texture != null) {
                pass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
            }
            pass.setVertexBuffer(0, mesh.buffer);
            pass.setIndexBuffer(indices, QUADS.type());
            pass.drawIndexed(0, 0, mesh.indexCount, 1);
        }
    }

    private static Vector4f colorToVec(int argb, float brightness) {
        float a = ((argb >>> 24) & 255) / 255.0F * brightness;
        float r = ((argb >> 16) & 255) / 255.0F * brightness;
        float g = ((argb >> 8) & 255) / 255.0F * brightness;
        float b = (argb & 255) / 255.0F * brightness;
        return new Vector4f(r, g, b, a);
    }

    private static float starAngle(float partial) {
        return Minecraft.getInstance().gameRenderer.getMainCamera().attributeProbe()
                .getValue(EnvironmentAttributes.STAR_ANGLE, partial) * ((float) Math.PI / 180.0F);
    }

    private static float seconds() {
        var level = Minecraft.getInstance().level;
        float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        long time = level == null ? 0L : level.getGameTime();
        return (time + partial) / 20.0F;
    }

    private static void close(GpuBuffer buffer) {
        if (buffer != null) {
            buffer.close();
        }
    }

    private record CachedMesh(GpuBuffer buffer, int indexCount) {
        void close() {
            buffer.close();
        }
    }
}
