package com.workshopcraft.workshopcore.backdrop;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Server API for per-level backdrops. A set backdrop replaces the vanilla sky.
 * {@link #clear(ServerLevel)} and an unset level fall back to vanilla.
 */
public final class Backdrops {
    private static final Logger LOGGER = LoggerFactory.getLogger("workshopcore");
    private static Map<Identifier, Backdrop> DEFS = Map.of();
    private static BackdropDefaults DEFAULTS = BackdropDefaults.empty();

    private Backdrops() {
    }

    static void acceptReload(Map<Identifier, Backdrop> defs, BackdropDefaults defaults) {
        DEFS = Map.copyOf(defs);
        DEFAULTS = defaults;
        LOGGER.info("Workshop Core loaded {} backdrops", DEFS.size());
    }

    public static boolean exists(Identifier id) {
        return DEFS.containsKey(id);
    }

    public static Optional<Backdrop> definition(Identifier id) {
        return Optional.ofNullable(DEFS.get(id));
    }

    public static Set<Identifier> ids() {
        return Collections.unmodifiableSet(DEFS.keySet());
    }

    public static Map<Identifier, Backdrop> definitions() {
        return DEFS;
    }

    public static Optional<Identifier> get(ServerLevel level) {
        ensureInitialized(level);
        return data(level).id();
    }

    public static void set(ServerLevel level, Identifier id) {
        if (!exists(id)) {
            throw new IllegalArgumentException("Unknown backdrop " + id);
        }
        apply(level, Optional.of(id));
    }

    public static void clear(ServerLevel level) {
        apply(level, Optional.empty());
    }

    public static void onLevelLoad(ServerLevel level) {
        ensureInitialized(level);
    }

    static void ensureInitialized(ServerLevel level) {
        BackdropData data = data(level);
        if (data.initialized()) {
            return;
        }
        Optional<Identifier> fallback = defaultFor(level);
        data.assign(fallback);
        if (fallback.isPresent()) {
            if (!exists(fallback.get())) {
                LOGGER.warn("Default backdrop {} for {} is not loaded; sky will be vanilla until a valid id is set", fallback.get(), level.dimension().identifier());
            }
            BackdropEvents.fire(level, Optional.empty(), fallback);
            BackdropNetworking.syncLevel(level);
        }
    }

    private static void apply(ServerLevel level, Optional<Identifier> id) {
        BackdropData data = data(level);
        Optional<Identifier> old = data.initialized() ? data.id() : Optional.empty();
        if (data.initialized() && old.equals(id)) {
            return;
        }
        data.assign(id);
        BackdropEvents.fire(level, old, id);
        BackdropNetworking.syncLevel(level);
    }

    private static BackdropData data(ServerLevel level) {
        return BackdropData.TYPE.get(level);
    }

    static Optional<Identifier> defaultFor(ServerLevel level) {
        Identifier dimension = level.dimension().identifier();
        Identifier fromDimension = DEFAULTS.dimensions().get(dimension);
        if (fromDimension != null) {
            return Optional.of(fromDimension);
        }
        ResourceKey<DimensionType> typeKey = level.dimensionTypeRegistration().unwrapKey().orElse(null);
        if (typeKey != null) {
            Identifier fromType = DEFAULTS.dimensionTypes().get(typeKey.identifier());
            if (fromType != null) {
                return Optional.of(fromType);
            }
        }
        return Optional.empty();
    }

    static void sendAssignments(net.minecraft.server.level.ServerPlayer player) {
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            ensureInitialized(level);
            BackdropNetworking.sendAssignment(player, level.dimension(), data(level).id());
        }
    }

    static ResourceKey<Level> key(ServerLevel level) {
        return level.dimension();
    }

    static Map<Identifier, Backdrop> copyDefs() {
        return new HashMap<>(DEFS);
    }
}
