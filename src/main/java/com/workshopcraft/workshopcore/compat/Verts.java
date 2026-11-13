package com.workshopcraft.workshopcore.compat;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Packed-colour vertex helper. */
public final class Verts {
    private Verts() {
    }

    public static void add(VertexConsumer vc, float x, float y, float z, int argb, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        vc.addVertex(x, y, z, argb, u, v, overlay, light, nx, ny, nz);
    }
}
