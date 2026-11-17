package com.workshopcraft.workshopcore.ctm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

final class CtmSprites {
    private CtmSprites() {
    }

    static TextureAtlasSprite get(Identifier id) {
        //? if >=26.1 {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS);
        TextureAtlasSprite sprite = atlas.getSprite(id);
        //?} else {
        /*TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(id);
        *///?}
        return MissingTextureAtlasSprite.getLocation().equals(name(sprite)) ? null : sprite;
    }

    static Identifier name(TextureAtlasSprite sprite) {
        return sprite.contents().name();
    }
}
