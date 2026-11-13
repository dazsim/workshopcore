package com.workshopcraft.workshopcore.backdrop;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/** Server-side listeners for backdrop changes on a level. */
public final class BackdropEvents {
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private BackdropEvents() {
    }

    @FunctionalInterface
    public interface Listener {
        void onChange(ServerLevel level, Optional<Identifier> oldId, Optional<Identifier> newId);
    }

    public static void listen(Listener listener) {
        LISTENERS.add(listener);
    }

    static void fire(ServerLevel level, Optional<Identifier> oldId, Optional<Identifier> newId) {
        for (Listener listener : LISTENERS) {
            listener.onChange(level, oldId, newId);
        }
    }
}
