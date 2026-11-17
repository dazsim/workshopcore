package com.workshopcraft.workshopcore.ctm.client;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
//? if >=26.1 {
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.joml.Vector3f;
import org.joml.Vector3fc;
//?} else {
/*import net.minecraft.client.renderer.block.model.BakedQuad;
*///?}

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a face quad into quarters and maps each quarter onto one of the five tiles. Tiles are laid out by
 * block position on the face, the same way vanilla maps a full face with no rotation.
 */
public final class CtmQuads {
    private static final float EPS = 1.0E-4F;

    private CtmQuads() {
    }

    /** Applies connected textures to one face list of a model. cull is the list's cull face, null for unculled quads. */
    public static List<BakedQuad> transform(List<BakedQuad> quads, Direction cull, long mask, CtmDef def) {
        if (quads.isEmpty()) {
            return quads;
        }
        if (cull != null && def.hideInnerFaces && (CtmMask.face(mask, cull) & CtmMask.ACROSS) != 0) {
            return List.of();
        }
        List<BakedQuad> out = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad q = quads.get(i);
            List<BakedQuad> r = apply(q, CtmMask.face(mask, direction(q)), def);
            if (r == null) {
                if (out != null) {
                    out.add(q);
                }
                continue;
            }
            if (out == null) {
                out = new ArrayList<>(quads.size() + 4);
                out.addAll(quads.subList(0, i));
            }
            out.addAll(r);
        }
        return out == null ? quads : out;
    }

    /** Quads to draw instead of q, or null to keep q as it is. */
    public static List<BakedQuad> apply(BakedQuad q, int bits, CtmDef def) {
        TextureAtlasSprite[] tiles = def.sprites();
        if (tiles == null || !def.matches(sprite(q))) {
            return null;
        }
        Direction face = direction(q);
        int axis = face.getAxis().ordinal();
        float[][] p = positions(q);
        float plane = p[0][axis];
        float[] u = new float[4], v = new float[4];
        float u0 = Float.MAX_VALUE, u1 = -Float.MAX_VALUE, v0 = Float.MAX_VALUE, v1 = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            if (Math.abs(p[i][axis] - plane) > EPS) {
                return null;
            }
            u[i] = texU(face, p[i]);
            v[i] = texV(face, p[i]);
            u0 = Math.min(u0, u[i]);
            u1 = Math.max(u1, u[i]);
            v0 = Math.min(v0, v[i]);
            v1 = Math.max(v1, v[i]);
        }
        if (u1 - u0 < EPS || v1 - v0 < EPS) {
            return null;
        }
        int[] t = new int[4];
        for (int k = 0; k < 4; k++) {
            t[k] = CtmMask.tile(bits, (k & 1) != 0, (k & 2) != 0);
        }
        List<BakedQuad> out = new ArrayList<>(4);
        if (t[0] == t[1] && t[0] == t[2] && t[0] == t[3]) {
            out.add(build(q, face, plane, u, v, u0, u1, v0, v1, tiles[t[0]]));
            return out;
        }
        for (int k = 0; k < 4; k++) {
            float a = (k & 1) != 0 ? 0.5F : 0.0F, b = (k & 2) != 0 ? 0.5F : 0.0F;
            float ru0 = Math.max(u0, a), ru1 = Math.min(u1, a + 0.5F);
            float rv0 = Math.max(v0, b), rv1 = Math.min(v1, b + 0.5F);
            if (ru1 - ru0 < EPS || rv1 - rv0 < EPS) {
                continue;
            }
            out.add(build(q, face, plane, u, v, ru0, ru1, rv0, rv1, tiles[t[k]]));
        }
        return out;
    }

    private static float texU(Direction face, float[] p) {
        return switch (face) {
            case DOWN, UP, SOUTH -> p[0];
            case NORTH -> 1.0F - p[0];
            case WEST -> p[2];
            case EAST -> 1.0F - p[2];
        };
    }

    private static float texV(Direction face, float[] p) {
        return switch (face) {
            case DOWN -> 1.0F - p[2];
            case UP -> p[2];
            default -> 1.0F - p[1];
        };
    }

    private static float[] position(Direction face, float plane, float u, float v) {
        return switch (face) {
            case DOWN -> new float[]{u, plane, 1.0F - v};
            case UP -> new float[]{u, plane, v};
            case NORTH -> new float[]{1.0F - u, 1.0F - v, plane};
            case SOUTH -> new float[]{u, 1.0F - v, plane};
            case WEST -> new float[]{plane, 1.0F - v, u};
            case EAST -> new float[]{plane, 1.0F - v, 1.0F - u};
        };
    }

    private static float clamp(float x, float lo, float hi) {
        return x < lo ? lo : Math.min(x, hi);
    }

    private static BakedQuad build(BakedQuad q, Direction face, float plane, float[] u, float[] v,
                                   float ru0, float ru1, float rv0, float rv1, TextureAtlasSprite sprite) {
        float du = sprite.getU1() - sprite.getU0(), dv = sprite.getV1() - sprite.getV0();
        float[][] pos = new float[4][];
        float[] su = new float[4], sv = new float[4];
        for (int i = 0; i < 4; i++) {
            float nu = clamp(u[i], ru0, ru1), nv = clamp(v[i], rv0, rv1);
            pos[i] = position(face, plane, nu, nv);
            su[i] = sprite.getU0() + nu * du;
            sv[i] = sprite.getV0() + nv * dv;
        }
        return make(q, pos, su, sv, sprite);
    }

    //? if >=26.1 {
    private static TextureAtlasSprite sprite(BakedQuad q) {
        return q.materialInfo().sprite();
    }

    private static Direction direction(BakedQuad q) {
        return q.direction();
    }

    private static float[][] positions(BakedQuad q) {
        float[][] p = new float[4][];
        for (int i = 0; i < 4; i++) {
            Vector3fc v = q.position(i);
            p[i] = new float[]{v.x(), v.y(), v.z()};
        }
        return p;
    }

    private static long[] packUv(float[] su, float[] sv) {
        long[] uv = new long[4];
        for (int i = 0; i < 4; i++) {
            uv[i] = UVPair.pack(su[i], sv[i]);
        }
        return uv;
    }

    private static Vector3f[] vectors(float[][] pos) {
        Vector3f[] p = new Vector3f[4];
        for (int i = 0; i < 4; i++) {
            p[i] = new Vector3f(pos[i][0], pos[i][1], pos[i][2]);
        }
        return p;
    }
    //?} else {
    /*private static final int STRIDE = 8;

    private static TextureAtlasSprite sprite(BakedQuad q) {
        return q.getSprite();
    }

    private static Direction direction(BakedQuad q) {
        return q.getDirection();
    }

    private static float[][] positions(BakedQuad q) {
        int[] data = q.getVertices();
        float[][] p = new float[4][];
        for (int i = 0; i < 4; i++) {
            int o = i * STRIDE;
            p[i] = new float[]{Float.intBitsToFloat(data[o]), Float.intBitsToFloat(data[o + 1]), Float.intBitsToFloat(data[o + 2])};
        }
        return p;
    }

    private static int[] vertices(BakedQuad q, float[][] pos, float[] su, float[] sv) {
        int[] data = q.getVertices().clone();
        for (int i = 0; i < 4; i++) {
            int o = i * STRIDE;
            data[o] = Float.floatToRawIntBits(pos[i][0]);
            data[o + 1] = Float.floatToRawIntBits(pos[i][1]);
            data[o + 2] = Float.floatToRawIntBits(pos[i][2]);
            data[o + 4] = Float.floatToRawIntBits(su[i]);
            data[o + 5] = Float.floatToRawIntBits(sv[i]);
        }
        return data;
    }
    *///?}

    private static BakedQuad make(BakedQuad q, float[][] pos, float[] su, float[] sv, TextureAtlasSprite sprite) {
        //? if >=26.3 && neoforge {
        /*long[] uv = packUv(su, sv);
        Vector3f[] p = vectors(pos);
        BakedQuad.MaterialInfo i = q.materialInfo();
        BakedQuad.MaterialInfo info = new BakedQuad.MaterialInfo(sprite, i.layer(), i.itemRenderType(), i.itemGlintRenderType(), i.itemGlintSpecialRenderType(),
                i.tintIndex(), i.shadeDirectionOverride(), i.lightEmission(), i.ambientOcclusion());
        return new BakedQuad(p[0], p[1], p[2], p[3], uv[0], uv[1], uv[2], uv[3], q.direction(), info, q.bakedNormals(), q.bakedColors());
        *///?} else if >=26.3 {
        /*long[] uv = packUv(su, sv);
        Vector3f[] p = vectors(pos);
        BakedQuad.MaterialInfo i = q.materialInfo();
        BakedQuad.MaterialInfo info = new BakedQuad.MaterialInfo(sprite, i.layer(), i.itemRenderType(), i.itemGlintRenderType(), i.itemGlintSpecialRenderType(),
                i.tintIndex(), i.shadeDirectionOverride(), i.lightEmission());
        return new BakedQuad(p[0], p[1], p[2], p[3], uv[0], uv[1], uv[2], uv[3], q.direction(), info);
        *///?} else if >=26.1 && neoforge {
        long[] uv = packUv(su, sv);
        Vector3f[] p = vectors(pos);
        BakedQuad.MaterialInfo i = q.materialInfo();
        BakedQuad.MaterialInfo info = new BakedQuad.MaterialInfo(sprite, i.layer(), i.itemRenderType(), i.tintIndex(), i.shade(), i.lightEmission(), i.ambientOcclusion());
        return new BakedQuad(p[0], p[1], p[2], p[3], uv[0], uv[1], uv[2], uv[3], q.direction(), info, q.bakedNormals(), q.bakedColors());
        //?} else if >=26.1 {
        /*long[] uv = packUv(su, sv);
        Vector3f[] p = vectors(pos);
        BakedQuad.MaterialInfo i = q.materialInfo();
        BakedQuad.MaterialInfo info = new BakedQuad.MaterialInfo(sprite, i.layer(), i.itemRenderType(), i.tintIndex(), i.shade(), i.lightEmission());
        return new BakedQuad(p[0], p[1], p[2], p[3], uv[0], uv[1], uv[2], uv[3], q.direction(), info);
        *///?} else if fabric {
        /*return new BakedQuad(vertices(q, pos, su, sv), q.getTintIndex(), q.getDirection(), sprite, q.isShade());
        *///?} else {
        /*return new BakedQuad(vertices(q, pos, su, sv), q.getTintIndex(), q.getDirection(), sprite, q.isShade(), q.hasAmbientOcclusion());
        *///?}
    }
}
