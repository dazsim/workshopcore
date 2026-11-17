package com.workshopcraft.workshopcore.ctm.client;

import com.workshopcraft.workshopcore.ctm.ConnectRule;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

import java.util.Set;

/** One parsed connected texture file. Tiles are resolved from the block atlas the first time they are drawn. */
public final class CtmDef {
    public final Identifier id;
    public final Identifier[] tiles;
    public final ConnectRule rule;
    public final boolean hideInnerFaces;
    public final Set<Identifier> match;
    private volatile TextureAtlasSprite[] sprites;
    private volatile boolean broken;

    CtmDef(Identifier id, Identifier[] tiles, ConnectRule rule, boolean hideInnerFaces, Set<Identifier> match) {
        this.id = id;
        this.tiles = tiles;
        this.rule = rule;
        this.hideInnerFaces = hideInnerFaces;
        this.match = match;
    }

    /** The five tile sprites, or null if any is missing from the block atlas. */
    public TextureAtlasSprite[] sprites() {
        TextureAtlasSprite[] s = sprites;
        if (s != null || broken) {
            return s;
        }
        s = new TextureAtlasSprite[tiles.length];
        for (int i = 0; i < tiles.length; i++) {
            s[i] = CtmSprites.get(tiles[i]);
            if (s[i] == null) {
                broken = true;
                CtmDefs.LOGGER.warn("Connected textures {}: tile {} is not in the block atlas (put it under textures/block/)", id, tiles[i]);
                return null;
            }
        }
        sprites = s;
        return s;
    }

    public boolean matches(TextureAtlasSprite sprite) {
        return match == null || match.contains(CtmSprites.name(sprite));
    }
}
