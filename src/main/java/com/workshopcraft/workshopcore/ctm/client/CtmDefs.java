package com.workshopcraft.workshopcore.ctm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.workshopcraft.workshopcore.ctm.ConnectRule;
import com.workshopcraft.workshopcore.ctm.ConnectedTextures;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Loads assets/&lt;ns&gt;/workshopcore/connected_textures/*.json each time models are baked. */
public final class CtmDefs {
    static final Logger LOGGER = LoggerFactory.getLogger("workshopcore");
    public static final String DIR = "workshopcore/connected_textures";
    private static volatile Map<Block, CtmDef> defs = Map.of();

    private CtmDefs() {
    }

    public static CtmDef get(Block block) {
        return defs.get(block);
    }

    public static Map<Block, CtmDef> all() {
        return defs;
    }

    public static void reload(ResourceManager manager) {
        Map<Block, CtmDef> out = new HashMap<>();
        for (Map.Entry<Identifier, Resource> e : manager.listResources(DIR, p -> p.getPath().endsWith(".json")).entrySet()) {
            Identifier file = e.getKey();
            try (Reader reader = e.getValue().openAsReader()) {
                parse(file, JsonParser.parseReader(reader).getAsJsonObject(), out);
            } catch (Exception ex) {
                LOGGER.error("Couldn't read connected textures file {}", file, ex);
            }
        }
        defs = Map.copyOf(out);
        if (!out.isEmpty()) {
            LOGGER.info("Connected textures on {} blocks", out.size());
        }
    }

    private static void parse(Identifier file, JsonObject json, Map<Block, CtmDef> out) {
        Identifier[] tiles = tiles(json.get("tiles"));
        if (tiles == null) {
            LOGGER.error("Connected textures file {} needs \"tiles\": a texture folder or a list of 5 textures", file);
            return;
        }
        String connect = json.has("connect") ? json.get("connect").getAsString() : "block";
        ConnectRule rule = ConnectedTextures.parse(connect);
        if (rule == null) {
            LOGGER.error("Connected textures file {} has unknown connect rule {}", file, connect);
            return;
        }
        boolean hide = json.has("hide_inner_faces") && json.get("hide_inner_faces").getAsBoolean();
        Set<Identifier> match = null;
        if (json.has("match")) {
            match = new HashSet<>();
            for (JsonElement m : list(json.get("match"))) {
                Identifier id = Identifier.tryParse(m.getAsString());
                if (id != null) {
                    match.add(id);
                }
            }
        }
        CtmDef def = new CtmDef(file, tiles, rule, hide, match == null ? null : Set.copyOf(match));
        for (JsonElement b : list(json.get("blocks"))) {
            Identifier id = Identifier.tryParse(b.getAsString());
            Optional<Block> block = id == null ? Optional.empty() : BuiltInRegistries.BLOCK.getOptional(id);
            if (block.isPresent()) {
                out.put(block.get(), def);
            } else {
                LOGGER.debug("Connected textures file {} names unknown block {}", file, b.getAsString());
            }
        }
    }

    private static Identifier[] tiles(JsonElement e) {
        if (e == null) {
            return null;
        }
        Identifier[] out = new Identifier[5];
        if (e.isJsonPrimitive()) {
            Identifier base = Identifier.tryParse(e.getAsString());
            if (base == null) {
                return null;
            }
            for (int i = 0; i < 5; i++) {
                out[i] = base.withSuffix("/" + i);
            }
            return out;
        }
        JsonArray a = e.getAsJsonArray();
        if (a.size() != 5) {
            return null;
        }
        for (int i = 0; i < 5; i++) {
            out[i] = Identifier.tryParse(a.get(i).getAsString());
            if (out[i] == null) {
                return null;
            }
        }
        return out;
    }

    private static Iterable<JsonElement> list(JsonElement e) {
        if (e == null) {
            return java.util.List.of();
        }
        if (e.isJsonArray()) {
            return e.getAsJsonArray();
        }
        return java.util.List.of(e);
    }
}
