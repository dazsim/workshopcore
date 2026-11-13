package com.workshopcraft.workshopcore.backdrop;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * A named backdrop: an ordered list of layers, back to front, plus an optional
 * transition used when the client switches to this backdrop.
 */
public record Backdrop(List<BackdropLayer> layers, BackdropTransition transition) {
    public static final Codec<Backdrop> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BackdropLayer.CODEC.listOf().fieldOf("layers").forGetter(Backdrop::layers),
            BackdropTransition.CODEC.optionalFieldOf("transition", BackdropTransition.DEFAULT).forGetter(Backdrop::transition)
    ).apply(instance, Backdrop::new));

    public static final StreamCodec<ByteBuf, Backdrop> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            json -> CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow(),
            backdrop -> CODEC.encodeStart(JsonOps.INSTANCE, backdrop).getOrThrow().toString()
    );

    /** Layers only; uses the default 60-tick auto transition. */
    public Backdrop(List<BackdropLayer> layers) {
        this(layers, BackdropTransition.DEFAULT);
    }

    public Backdrop {
        if (transition == null) {
            transition = BackdropTransition.DEFAULT;
        }
    }
}
