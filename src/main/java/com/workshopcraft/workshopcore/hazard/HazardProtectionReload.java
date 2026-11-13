package com.workshopcraft.workshopcore.hazard;

import com.mojang.serialization.JsonOps;
import com.workshopcraft.workshopcore.WorkshopCore;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads {@code data/<ns>/workshopcore/hazard_protection/*.json} on datapack reload. */
public final class HazardProtectionReload extends SimplePreparableReloadListener<List<HazardProtectionEntry>> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(WorkshopCore.MOD_ID, "hazard_protection");
    private static final Logger LOGGER = LoggerFactory.getLogger("workshopcore");
    private static final FileToIdConverter FILES = FileToIdConverter.json("workshopcore/hazard_protection");

    @Override
    protected List<HazardProtectionEntry> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, HazardProtectionEntry> parsed = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(manager, FILES, JsonOps.INSTANCE, HazardProtectionEntry.CODEC, parsed);
        List<HazardProtectionEntry> entries = new ArrayList<>(parsed.size());
        for (var e : parsed.entrySet()) {
            HazardProtectionEntry entry = e.getValue().withId(e.getKey());
            if (entry.items().isEmpty()) {
                LOGGER.error("Hazard protection {} has no items, skipping", e.getKey());
                continue;
            }
            entries.add(entry);
        }
        return List.copyOf(entries);
    }

    @Override
    protected void apply(List<HazardProtectionEntry> preparations, ResourceManager manager, ProfilerFiller profiler) {
        HazardProtection.acceptReload(preparations);
        LOGGER.info("Loaded {} hazard protection file(s) covering {} hazard(s)", preparations.size(), HazardProtection.hazards().size());
    }
}
