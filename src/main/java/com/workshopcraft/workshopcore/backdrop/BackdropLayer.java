package com.workshopcraft.workshopcore.backdrop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/** One layer of a backdrop. Closed set of types. */
public sealed interface BackdropLayer permits
        BackdropLayer.Color,
        BackdropLayer.Gradient,
        BackdropLayer.Stars,
        BackdropLayer.Warp,
        BackdropLayer.Body {

    String type();

    Codec<BackdropLayer> CODEC = Codec.STRING.dispatch("type", BackdropLayer::type, BackdropLayer::codecFor);

    private static MapCodec<? extends BackdropLayer> codecFor(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "color", "sky_color" -> Color.CODEC;
            case "gradient" -> Gradient.CODEC;
            case "stars" -> Stars.CODEC;
            case "warp" -> Warp.CODEC;
            case "body" -> Body.CODEC;
            default -> throw new IllegalArgumentException("Unknown backdrop layer type: " + type);
        };
    }

    record Color(int color) implements BackdropLayer {
        static final MapCodec<Color> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                BackdropColors.CODEC.fieldOf("color").forGetter(Color::color)
        ).apply(i, Color::new));

        @Override
        public String type() {
            return "color";
        }

    }

    record Gradient(int top, int horizon, int bottom) implements BackdropLayer {
        static final MapCodec<Gradient> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                BackdropColors.CODEC.fieldOf("top").forGetter(Gradient::top),
                BackdropColors.CODEC.optionalFieldOf("horizon").forGetter(g -> java.util.Optional.of(g.horizon)),
                BackdropColors.CODEC.fieldOf("bottom").forGetter(Gradient::bottom)
        ).apply(i, (top, horizon, bottom) -> new Gradient(top, horizon.orElse(lerp(top, bottom)), bottom)));

        private static int lerp(int a, int b) {
            int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255, aa = (a >>> 24);
            int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255, ba = (b >>> 24);
            return (aa + ba) / 2 << 24 | (ar + br) / 2 << 16 | (ag + bg) / 2 << 8 | (ab + bb) / 2;
        }

        @Override
        public String type() {
            return "gradient";
        }

    }

    record Stars(int count, float brightness, int color, long seed, boolean rotateWithTime, float drift) implements BackdropLayer {
        static final MapCodec<Stars> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("count", 2000).forGetter(Stars::count),
                Codec.FLOAT.optionalFieldOf("brightness", 1.0F).forGetter(Stars::brightness),
                BackdropColors.CODEC.optionalFieldOf("color", 0xFFFFFFFF).forGetter(Stars::color),
                Codec.LONG.optionalFieldOf("seed", 1L).forGetter(Stars::seed),
                Codec.BOOL.optionalFieldOf("rotate_with_time", false).forGetter(Stars::rotateWithTime),
                Codec.FLOAT.optionalFieldOf("drift", 0.0F).forGetter(Stars::drift)
        ).apply(i, Stars::new));

        @Override
        public String type() {
            return "stars";
        }

    }

    record Warp(int count, float speed, float length, int color, float spread, BackdropDirection direction) implements BackdropLayer {
        static final MapCodec<Warp> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("count", 80).forGetter(Warp::count),
                Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(Warp::speed),
                Codec.FLOAT.optionalFieldOf("length", 8.0F).forGetter(Warp::length),
                BackdropColors.CODEC.optionalFieldOf("color", 0xFFA0C8FF).forGetter(Warp::color),
                Codec.FLOAT.optionalFieldOf("spread", 25.0F).forGetter(Warp::spread),
                BackdropDirection.CODEC.optionalFieldOf("direction", BackdropDirection.MINUS_Z).forGetter(Warp::direction)
        ).apply(i, Warp::new));

        @Override
        public String type() {
            return "warp";
        }

    }

    record Body(Identifier texture, float size, float azimuth, float altitude, float spin, float orbitSpeed, BackdropDirection orbitAxis) implements BackdropLayer {
        static final MapCodec<Body> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("texture").forGetter(Body::texture),
                Codec.FLOAT.fieldOf("size").forGetter(Body::size),
                Codec.FLOAT.optionalFieldOf("azimuth", 0.0F).forGetter(Body::azimuth),
                Codec.FLOAT.optionalFieldOf("altitude", 20.0F).forGetter(Body::altitude),
                Codec.FLOAT.optionalFieldOf("spin", 0.0F).forGetter(Body::spin),
                Codec.FLOAT.optionalFieldOf("orbit_speed", 0.0F).forGetter(Body::orbitSpeed),
                BackdropDirection.CODEC.optionalFieldOf("orbit_axis", BackdropDirection.UP).forGetter(Body::orbitAxis)
        ).apply(i, Body::new));

        @Override
        public String type() {
            return "body";
        }

    }
}
