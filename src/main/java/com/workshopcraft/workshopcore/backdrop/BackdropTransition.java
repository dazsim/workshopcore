package com.workshopcraft.workshopcore.backdrop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Locale;

/**
 * How the client should move from the previous sky to this backdrop.
 * {@code duration_ticks} is the length of the transition. {@code mode} is {@code auto}
 * (warp morph when a warp layer is involved, otherwise a short crossfade),
 * {@code crossfade}, or {@code warp} (force the star/warp morph).
 */
public record BackdropTransition(int durationTicks, Mode mode) {
    public static final int DEFAULT_DURATION_TICKS = 60;
    public static final int MAX_DURATION_TICKS = 1200;
    public static final BackdropTransition DEFAULT = new BackdropTransition(DEFAULT_DURATION_TICKS, Mode.AUTO);

    public enum Mode {
        AUTO,
        CROSSFADE,
        WARP;

        public static final Codec<Mode> CODEC = Codec.STRING.xmap(Mode::fromName, mode -> mode.name().toLowerCase(Locale.ROOT));

        public static Mode fromName(String name) {
            if (name == null || name.isEmpty()) {
                return AUTO;
            }
            try {
                return Mode.valueOf(name.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return AUTO;
            }
        }
    }

    public static final Codec<BackdropTransition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("duration_ticks", DEFAULT_DURATION_TICKS).forGetter(BackdropTransition::durationTicks),
            Mode.CODEC.optionalFieldOf("mode", Mode.AUTO).forGetter(BackdropTransition::mode)
    ).apply(instance, BackdropTransition::new));

    public BackdropTransition {
        durationTicks = Math.max(0, Math.min(durationTicks, MAX_DURATION_TICKS));
        if (mode == null) {
            mode = Mode.AUTO;
        }
    }
}
