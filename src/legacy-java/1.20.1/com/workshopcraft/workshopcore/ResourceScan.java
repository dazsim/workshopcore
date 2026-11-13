package com.workshopcraft.workshopcore;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/** Datapack JSON scan for 1.20.1, which has neither ifSuccess/ifError nor (on Fabric) FileToIdConverter in vanilla. */
public final class ResourceScan {
    private static final Logger LOGGER = LoggerFactory.getLogger("workshopcore");

    private ResourceScan() {
    }

    public static <T> void scanDirectory(
            ResourceManager manager,
            FileToIdConverter lister,
            DynamicOps<JsonElement> ops,
            Codec<T> codec,
            Map<ResourceLocation, T> result
    ) {
        for (Map.Entry<ResourceLocation, Resource> entry : lister.listMatchingResources(manager).entrySet()) {
            ResourceLocation location = entry.getKey();
            ResourceLocation id = lister.fileToId(location);
            try (Reader reader = entry.getValue().openAsReader()) {
                var parsed = codec.parse(ops, JsonParser.parseReader(reader));
                parsed.resultOrPartial(error -> LOGGER.error("Couldn't parse data file '{}' from '{}': {}", id, location, error))
                        .ifPresent(value -> {
                            if (result.putIfAbsent(id, value) != null) {
                                throw new IllegalStateException("Duplicate data file ignored with ID " + id);
                            }
                        });
            } catch (JsonParseException | IllegalArgumentException | IOException exception) {
                LOGGER.error("Couldn't parse data file '{}' from '{}'", id, location, exception);
            }
        }
    }
}
