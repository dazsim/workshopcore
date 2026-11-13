package com.workshopcraft.workshopcore;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
//? if >=26.1 {
import net.minecraft.util.StrictJsonParser;
//?}
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/** Datapack JSON scan used on loaders that no longer expose SimpleJsonResourceReloadListener.scanDirectory. */
public final class ResourceScan {
    private static final Logger LOGGER = LoggerFactory.getLogger("workshopcore");

    private ResourceScan() {
    }

    public static <T> void scanDirectory(
            ResourceManager manager,
            FileToIdConverter lister,
            DynamicOps<JsonElement> ops,
            Codec<T> codec,
            Map<Identifier, T> result
    ) {
        for (Map.Entry<Identifier, Resource> entry : lister.listMatchingResources(manager).entrySet()) {
            Identifier location = entry.getKey();
            Identifier id = lister.fileToId(location);
            try (Reader reader = entry.getValue().openAsReader()) {
                codec.parse(ops,
                        //? if >=26.1 {
                        StrictJsonParser.parse(reader)
                        //?} else {
                        /*com.google.gson.JsonParser.parseReader(reader)*/
                        //?}
                )
                        .ifSuccess(value -> {
                            if (result.putIfAbsent(id, value) != null) {
                                throw new IllegalStateException("Duplicate data file ignored with ID " + id);
                            }
                        })
                        .ifError(error -> LOGGER.error("Couldn't parse data file '{}' from '{}': {}", id, location, error));
            } catch (JsonParseException | IllegalArgumentException | IOException exception) {
                LOGGER.error("Couldn't parse data file '{}' from '{}'", id, location, exception);
            }
        }
    }
}
