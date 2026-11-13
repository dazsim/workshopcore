package com.workshopcraft.workshopcore.backdrop.client;

import com.workshopcraft.workshopcore.backdrop.Backdrop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.joml.Matrix4fc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Client state for the current backdrop assignments and definitions. */
public final class BackdropClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("workshopcore");
    private static final Set<Identifier> WARNED = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceKey<Level>, Identifier> ASSIGNMENTS = new HashMap<>();
    private static Map<Identifier, Backdrop> DEFS = Map.of();
    private static ResourceKey<Level> lastLevel;

    private BackdropClient() {
    }

    public static void acceptDefinitions(Map<Identifier, Backdrop> defs) {
        DEFS = Map.copyOf(defs);
        WARNED.clear();
        BackdropRenderer.invalidate();
    }

    public static void acceptAssignment(ResourceKey<Level> dimension, Optional<Identifier> id) {
        if (id.isEmpty()) {
            ASSIGNMENTS.remove(dimension);
        } else {
            ASSIGNMENTS.put(dimension, id.get());
        }
    }

    public static void drop(ResourceKey<Level> dimension) {
        ASSIGNMENTS.remove(dimension);
        BackdropRenderer.invalidate();
    }

    public static void clear() {
        ASSIGNMENTS.clear();
        DEFS = Map.of();
        WARNED.clear();
        lastLevel = null;
        BackdropRenderer.invalidate();
    }

    public static void afterClientLevelChange(ClientLevel next) {
        ResourceKey<Level> nextKey = next == null ? null : next.dimension();
        if (lastLevel != null && lastLevel != nextKey) {
            drop(lastLevel);
        }
        lastLevel = nextKey;
        BackdropRenderer.invalidate();
    }

    public static Optional<Backdrop> current() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return Optional.empty();
        }
        Identifier id = ASSIGNMENTS.get(level.dimension());
        if (id == null) {
            return Optional.empty();
        }
        Backdrop backdrop = DEFS.get(id);
        if (backdrop == null) {
            if (WARNED.add(id)) {
                LOGGER.warn("Backdrop {} is not loaded; using vanilla sky", id);
            }
            return Optional.empty();
        }
        return Optional.of(backdrop);
    }

    public static boolean hasActive() {
        return current().isPresent();
    }

    public static boolean render(Matrix4fc modelView, Runnable setupFog) {
        return current().map(backdrop -> {
            setupFog.run();
            BackdropRenderer.render(backdrop, modelView);
            return true;
        }).orElse(false);
    }
}
