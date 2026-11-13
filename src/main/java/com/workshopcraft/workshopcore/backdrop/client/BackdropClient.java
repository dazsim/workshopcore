package com.workshopcraft.workshopcore.backdrop.client;

import com.workshopcraft.workshopcore.backdrop.Backdrop;
import com.workshopcraft.workshopcore.backdrop.BackdropTransition;
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
import java.util.Objects;
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
    private static BackdropSkyTransition playing;

    private BackdropClient() {
    }

    public static void acceptDefinitions(Map<Identifier, Backdrop> defs) {
        DEFS = Map.copyOf(defs);
        WARNED.clear();
        BackdropRenderer.invalidate();
    }

    public static void acceptAssignment(ResourceKey<Level> dimension, Optional<Identifier> id) {
        acceptAssignment(dimension, id, 0);
    }

    public static void acceptAssignment(ResourceKey<Level> dimension, Optional<Identifier> id, int transitionTicks) {
        Identifier previousId = ASSIGNMENTS.get(dimension);
        Identifier nextId = id.orElse(null);
        if (id.isEmpty()) {
            ASSIGNMENTS.remove(dimension);
        } else {
            ASSIGNMENTS.put(dimension, id.get());
        }
        ClientLevel level = Minecraft.getInstance().level;
        boolean visible = level != null && level.dimension().equals(dimension);
        if (!visible || transitionTicks <= 0 || Objects.equals(previousId, nextId)) {
            if (playing != null && playing.appliesTo(dimension)) {
                playing = null;
            }
            return;
        }
        Optional<Backdrop> from = definitionOf(previousId);
        Optional<Backdrop> to = definitionOf(nextId);
        BackdropTransition.Mode mode = to.map(backdrop -> backdrop.transition().mode())
                .or(() -> from.map(backdrop -> backdrop.transition().mode()))
                .orElse(BackdropTransition.Mode.AUTO);
        BackdropSkyTransition previous = playing != null && playing.appliesTo(dimension) ? playing : null;
        playing = BackdropSkyTransition.start(dimension, from, to, transitionTicks, mode, previous);
    }

    public static void drop(ResourceKey<Level> dimension) {
        ASSIGNMENTS.remove(dimension);
        if (playing != null && playing.appliesTo(dimension)) {
            playing = null;
        }
        BackdropRenderer.invalidate();
    }

    public static void clear() {
        ASSIGNMENTS.clear();
        DEFS = Map.of();
        WARNED.clear();
        lastLevel = null;
        playing = null;
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
        return definitionOf(currentId());
    }

    public static boolean hasActive() {
        if (current().isPresent()) {
            return true;
        }
        return playingForCurrent().isPresent();
    }

    public static boolean render(Matrix4fc modelView, Runnable setupFog) {
        Optional<BackdropSkyTransition> active = playingForCurrent();
        if (active.isPresent()) {
            setupFog.run();
            if (active.get().render(modelView)) {
                return true;
            }
            playing = null;
        }
        return current().map(backdrop -> {
            setupFog.run();
            BackdropRenderer.render(backdrop, modelView);
            return true;
        }).orElse(false);
    }

    private static Optional<BackdropSkyTransition> playingForCurrent() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || playing == null || !playing.appliesTo(level.dimension())) {
            return Optional.empty();
        }
        return Optional.of(playing);
    }

    private static Identifier currentId() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        return ASSIGNMENTS.get(level.dimension());
    }

    private static Optional<Backdrop> definitionOf(Identifier id) {
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
}
