package com.workshopcraft.workshopcore.backdrop;

import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import com.workshopcraft.workshopcore.WorkshopCore;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads data/&lt;ns&gt;/workshopcore/backdrops/*.json and backdrop_defaults.json. */
public final class BackdropReload extends SimplePreparableReloadListener<BackdropReload.Prepared> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(WorkshopCore.MOD_ID, "backdrops");
    private static final Logger LOGGER = LoggerFactory.getLogger("workshopcore");
    private static final FileToIdConverter BACKDROPS = FileToIdConverter.json("workshopcore/backdrops");

    public record Prepared(Map<Identifier, Backdrop> defs, BackdropDefaults defaults) {
    }

    @Override
    protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, Backdrop> defs = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(manager, BACKDROPS, JsonOps.INSTANCE, Backdrop.CODEC, defs);

        BackdropDefaults defaults = BackdropDefaults.empty();
        Map<Identifier, List<Resource>> stacks = manager.listResourceStacks("workshopcore", id -> id.getPath().equals("workshopcore/backdrop_defaults.json"));
        for (Map.Entry<Identifier, List<Resource>> entry : stacks.entrySet()) {
            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    var parsed = BackdropDefaults.CODEC.parse(JsonOps.INSTANCE, StrictJsonParser.parse(reader));
                    BackdropDefaults current = defaults;
                    defaults = parsed.resultOrPartial(message -> LOGGER.error("Couldn't parse {}: {}", entry.getKey(), message))
                            .map(next -> next.mergeOver(current))
                            .orElse(current);
                } catch (IOException | JsonParseException | IllegalArgumentException exception) {
                    LOGGER.error("Couldn't read {}", entry.getKey(), exception);
                }
            }
        }
        return new Prepared(Map.copyOf(defs), defaults);
    }

    @Override
    protected void apply(Prepared preparations, ResourceManager manager, ProfilerFiller profiler) {
        Backdrops.acceptReload(preparations.defs(), preparations.defaults());
    }
}
