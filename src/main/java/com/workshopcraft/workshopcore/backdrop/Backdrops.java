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

    /**
     * Set the backdrop, using the target definition's {@code transition.duration_ticks}
     * or {@link BackdropTransition#DEFAULT_DURATION_TICKS} if that field is omitted.
     * The assignment is applied immediately; the client animates over that many ticks.
     */
    public static void set(ServerLevel level, Identifier id) {
        if (!exists(id)) {
            throw new IllegalArgumentException("Unknown backdrop " + id);
        }
        apply(level, Optional.of(id), durationOf(id));
    }

    /**
     * Set the backdrop and tell the client to transition over {@code transitionTicks}.
     * Zero is instant (the 0.3.0 behaviour). Values are clamped to 0..1200.
     */
    public static void set(ServerLevel level, Identifier id, int transitionTicks) {
        if (!exists(id)) {
            throw new IllegalArgumentException("Unknown backdrop " + id);
        }
        apply(level, Optional.of(id), transitionTicks);
    }

    /**
     * Clear to vanilla, using the outgoing backdrop's transition duration when present.
     */
    public static void clear(ServerLevel level) {
        apply(level, Optional.empty(), durationOf(get(level)));
    }

    /**
     * Clear to vanilla and tell the client to transition over {@code transitionTicks}.
     * Zero is instant.
     */
    public static void clear(ServerLevel level, int transitionTicks) {
        apply(level, Optional.empty(), transitionTicks);
    }

    public static void onLevelLoad(ServerLevel level) {
        ensureInitialized(level);
    }

    /**
     * Drop the client cache for this dimension key without changing saved data.
     * A later load of a new level with the same key will not inherit a stale client assignment.
     */
    public static void onLevelUnload(ServerLevel level) {
        BackdropNetworking.forgetDimension(level.getServer(), level.dimension());
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

    private static void apply(ServerLevel level, Optional<Identifier> id, int transitionTicks) {
        BackdropData data = data(level);
        Optional<Identifier> old = data.initialized() ? data.id() : Optional.empty();
        if (data.initialized() && old.equals(id)) {
            return;
        }
        data.assign(id);
        BackdropEvents.fire(level, old, id);
        BackdropNetworking.syncLevel(level, clampTicks(transitionTicks));
    }

    static int durationOf(Identifier id) {
        return durationOf(Optional.of(id));
    }

    static int durationOf(Optional<Identifier> id) {
        return id.flatMap(Backdrops::definition)
                .map(backdrop -> backdrop.transition().durationTicks())
                .orElse(BackdropTransition.DEFAULT_DURATION_TICKS);
    }

    static int clampTicks(int ticks) {
        return Math.max(0, Math.min(ticks, BackdropTransition.MAX_DURATION_TICKS));
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
