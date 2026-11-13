package com.workshopcraft.workshopcore.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** 1.20.1 has no GUI sprite atlas; draws the same png from textures/gui/sprites. */
public final class Gui {
    private Gui() {
    }

    private static ResourceLocation tex(ResourceLocation sprite) {
        return new ResourceLocation(sprite.getNamespace(), "textures/gui/sprites/" + sprite.getPath() + ".png");
    }

    public static void blitSprite(GuiGraphics g, ResourceLocation sprite, int x, int y, int w, int h) {
        g.blit(tex(sprite), x, y, 0, 0, w, h, w, h);
    }

    public static void blitSprite(GuiGraphics g, ResourceLocation sprite, int texW, int texH, int u, int v, int x, int y, int w, int h) {
        g.blit(tex(sprite), x, y, u, v, w, h, texW, texH);
    }
}
