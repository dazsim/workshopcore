package com.workshopcraft.workshopcore.compat.codec;

import java.util.function.BiConsumer;
import java.util.function.Function;

/** 1.20.1 stand-in for net.minecraft.network.codec.StreamCodec. */
public interface StreamCodec<B, V> {
    V decode(B buf);

    void encode(B buf, V value);

    static <B, V> StreamCodec<B, V> of(BiConsumer<B, V> encoder, Function<B, V> decoder) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buf) {
                return decoder.apply(buf);
            }

            @Override
            public void encode(B buf, V value) {
                encoder.accept(buf, value);
            }
        };
    }

    static <B, V> StreamCodec<B, V> unit(V value) {
        return of((buf, v) -> {}, buf -> value);
    }

    default <O> StreamCodec<B, O> map(Function<V, O> to, Function<O, V> from) {
        return of((buf, o) -> encode(buf, from.apply(o)), buf -> to.apply(decode(buf)));
    }
}
