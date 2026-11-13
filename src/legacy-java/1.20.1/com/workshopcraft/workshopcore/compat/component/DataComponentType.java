package com.workshopcraft.workshopcore.compat.component;

import com.mojang.serialization.Codec;
import com.workshopcraft.workshopcore.compat.codec.StreamCodec;

/** 1.20.1 stand-in for a data component type. {@link #storage()} is the item NBT compound key, e.g. "SimpleMachines". */
public final class DataComponentType<T> {
    private final Codec<T> codec;
    private final String storage;
    private String name;

    public DataComponentType(Codec<T> codec, String storage, String name) {
        this.codec = codec;
        this.storage = storage;
        this.name = name;
    }

    public DataComponentType(Codec<T> codec, String name) {
        this(codec, null, name);
    }

    public void bind(String name) {
        if (this.name == null) this.name = name;
    }

    public String name() {
        return name;
    }

    public String storage() {
        return storage;
    }

    public Codec<T> codec() {
        return codec;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private Codec<T> codec;
        private String storage;
        private String name;

        public Builder<T> persistent(Codec<T> codec) {
            this.codec = codec;
            return this;
        }

        public Builder<T> storage(String storage) {
            this.storage = storage;
            return this;
        }

        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> networkSynchronized(StreamCodec<?, T> codec) {
            return this;
        }

        public Builder<T> cacheEncoding() {
            return this;
        }

        public DataComponentType<T> build() {
            return new DataComponentType<>(codec, storage, name);
        }
    }
}
