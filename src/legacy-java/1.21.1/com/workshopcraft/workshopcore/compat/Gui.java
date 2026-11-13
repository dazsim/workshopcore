package com.workshopcraft.workshopcore.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** GUI sprite blit on 1.21.1 (no render pipeline argument). */
public final class Gui {
    private Gui() {
    }

    public static void blitSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h) {
        g.blitSprite(sprite, x, y, w, h);
    }

    public static void blitSprite(GuiGraphics g, ResourceLocation sprite, int texW, int texH, int u, int v, int x, int y, int w, int h) {
        g.blitSprite(sprite, texW, texH, u, v, x, y, w, h);
    }
}
