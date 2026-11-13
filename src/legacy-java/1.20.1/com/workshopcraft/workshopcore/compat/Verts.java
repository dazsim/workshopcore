package com.workshopcraft.workshopcore.compat;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Packed-colour addVertex on the 1.20.1 vertex call. */
public final class Verts {
    private Verts() {
    }

    public static void add(VertexConsumer vc, float x, float y, float z, int argb, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        vc.vertex(x, y, z, (argb >> 16 & 255) / 255F, (argb >> 8 & 255) / 255F, (argb & 255) / 255F, (argb >>> 24) / 255F, u, v, overlay, light, nx, ny, nz);
    }
}
