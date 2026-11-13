package com.workshopcraft.workshopcore.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** GUI sprite blit that works on every version. */
public final class Gui {
    private Gui() {
    }

    public static void blitSprite(GuiGraphicsExtractor g, Identifier sprite, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h);
    }

    public static void blitSprite(GuiGraphicsExtractor g, Identifier sprite, int texW, int texH, int u, int v, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, texW, texH, u, v, x, y, w, h);
    }
}
