package com.workshopcraft.workshopcore.compat.codec;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/** Common 1.20.1 stream codecs used in place of net.minecraft.network.codec.ByteBufCodecs. */
public final class ByteBufCodecs {
    public static final StreamCodec<FriendlyByteBuf, UUID> UUID = StreamCodec.of(FriendlyByteBuf::writeUUID, FriendlyByteBuf::readUUID);
    public static final StreamCodec<FriendlyByteBuf, Integer> VAR_INT = StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt);
    public static final StreamCodec<FriendlyByteBuf, Boolean> BOOL = StreamCodec.of(FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean);
    public static final StreamCodec<FriendlyByteBuf, String> STRING_UTF8 = StreamCodec.of(FriendlyByteBuf::writeUtf, FriendlyByteBuf::readUtf);

    private ByteBufCodecs() {
    }
}
