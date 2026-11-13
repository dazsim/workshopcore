package net.minecraft.network.protocol.common.custom;

import net.minecraft.resources.ResourceLocation;

/** Stand-in for the 1.20.5+ payload type used by Workshop Core's public packet hooks. */
public interface CustomPacketPayload {
    Type<? extends CustomPacketPayload> type();

    record Type<T extends CustomPacketPayload>(ResourceLocation id) {
    }
}
