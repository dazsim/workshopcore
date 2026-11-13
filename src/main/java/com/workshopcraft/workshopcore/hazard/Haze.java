package com.workshopcraft.workshopcore.hazard;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player coloured haze (fog plus a light screen tint). State is kept on the
 * server and synced only to that player. {@link #set} and {@link #clear} send a
 * packet immediately. The current state is resent with a 0-tick fade on respawn
 * and dimension change, and dropped on logout.
 */
public final class Haze {
    public static final int MAX_FADE_TICKS = 1200;

    private static final Map<UUID, State> STATE = new ConcurrentHashMap<>();

    private Haze() {
    }

    public static void set(ServerPlayer player, int argbColor, float density, int fadeTicks) {
        float clamped = Mth.clamp(density, 0.0F, 1.0F);
        int fade = Math.max(0, fadeTicks);
        if (clamped <= 0.0F) {
            clear(player, fade);
            return;
        }
        STATE.put(player.getUUID(), new State(argbColor, clamped));
        HazeNetworking.send(player, argbColor, clamped, fade);
    }

    public static void clear(ServerPlayer player, int fadeTicks) {
        State previous = STATE.remove(player.getUUID());
        int color = previous != null ? previous.color : 0;
        HazeNetworking.send(player, color, 0.0F, Math.max(0, fadeTicks));
    }

    public static void resend(ServerPlayer player) {
        State state = STATE.get(player.getUUID());
        if (state != null) {
            HazeNetworking.send(player, state.color, state.density, 0);
        } else {
            HazeNetworking.send(player, 0, 0.0F, 0);
        }
    }

    public static void forget(ServerPlayer player) {
        STATE.remove(player.getUUID());
    }

    private record State(int color, float density) {
    }
}
