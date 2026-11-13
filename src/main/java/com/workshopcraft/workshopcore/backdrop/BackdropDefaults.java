package com.workshopcraft.workshopcore.backdrop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/** One data file at data/<ns>/workshopcore/backdrop_defaults.json. */
public record BackdropDefaults(Map<Identifier, Identifier> dimensions, Map<Identifier, Identifier> dimensionTypes) {
    public static final Codec<BackdropDefaults> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, Identifier.CODEC).optionalFieldOf("dimensions", Map.of()).forGetter(BackdropDefaults::dimensions),
            Codec.unboundedMap(Identifier.CODEC, Identifier.CODEC).optionalFieldOf("dimension_types", Map.of()).forGetter(BackdropDefaults::dimensionTypes)
    ).apply(instance, BackdropDefaults::new));

    static BackdropDefaults empty() {
        return new BackdropDefaults(Map.of(), Map.of());
    }

    BackdropDefaults mergeOver(BackdropDefaults earlier) {
        Map<Identifier, Identifier> dimensions = new HashMap<>(earlier.dimensions);
        dimensions.putAll(this.dimensions);
        Map<Identifier, Identifier> types = new HashMap<>(earlier.dimensionTypes);
        types.putAll(this.dimensionTypes);
        return new BackdropDefaults(Map.copyOf(dimensions), Map.copyOf(types));
    }
}
