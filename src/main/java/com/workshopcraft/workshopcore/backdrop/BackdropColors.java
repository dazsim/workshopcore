package com.workshopcraft.workshopcore.backdrop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/** Hex colour strings (#RRGGBB or #AARRGGBB) as ARGB ints. */
public final class BackdropColors {
    public static final Codec<Integer> CODEC = Codec.STRING.comapFlatMap(BackdropColors::parse, BackdropColors::toHex);

    private BackdropColors() {
    }

    public static DataResult<Integer> parse(String text) {
        if (text == null) {
            return DataResult.error(() -> "Missing colour");
        }
        String s = text.startsWith("#") ? text.substring(1) : text;
        try {
            if (s.length() == 6) {
                return DataResult.success(0xFF000000 | Integer.parseInt(s, 16));
            }
            if (s.length() == 8) {
                return DataResult.success((int) Long.parseLong(s, 16));
            }
        } catch (NumberFormatException ignored) {
        }
        return DataResult.error(() -> "Invalid colour '" + text + "'");
    }

    public static int parseOrThrow(String text) {
        return parse(text).getOrThrow();
    }

    public static String toHex(int argb) {
        if ((argb & 0xFF000000) == 0xFF000000) {
            return String.format("#%06X", argb & 0xFFFFFF);
        }
        return String.format("#%08X", argb);
    }
}
