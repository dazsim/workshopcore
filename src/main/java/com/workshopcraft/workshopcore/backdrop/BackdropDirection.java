package com.workshopcraft.workshopcore.backdrop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * A direction in degrees. Yaw 0 pitch 0 points toward -Z (north).
 * Yaw 90 points toward -X (west). Pitch -90 is straight up.
 */
public record BackdropDirection(float yaw, float pitch) {
    public static final BackdropDirection MINUS_Z = new BackdropDirection(0.0F, 0.0F);
    public static final BackdropDirection UP = new BackdropDirection(0.0F, -90.0F);

    public static final Codec<BackdropDirection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(BackdropDirection::yaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(BackdropDirection::pitch)
    ).apply(instance, BackdropDirection::new));

    public Vector3f vector() {
        float y = yaw * ((float) Math.PI / 180.0F);
        float p = pitch * ((float) Math.PI / 180.0F);
        float cp = Mth.cos(p);
        return new Vector3f(-Mth.sin(y) * cp, -Mth.sin(p), -Mth.cos(y) * cp);
    }
}
