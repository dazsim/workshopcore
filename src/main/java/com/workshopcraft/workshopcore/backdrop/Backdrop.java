package com.workshopcraft.workshopcore.backdrop;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/** A named backdrop: an ordered list of layers, back to front. */
public record Backdrop(List<BackdropLayer> layers) {
    public static final Codec<Backdrop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BackdropLayer.CODEC.listOf().fieldOf("layers").forGetter(Backdrop::layers)
    ).apply(instance, Backdrop::new));

    public static final StreamCodec<ByteBuf, Backdrop> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            json -> CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow(),
            backdrop -> CODEC.encodeStart(JsonOps.INSTANCE, backdrop).getOrThrow().toString()
    );
}
