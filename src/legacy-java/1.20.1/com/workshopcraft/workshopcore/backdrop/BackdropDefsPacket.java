package com.workshopcraft.workshopcore.backdrop;

import com.workshopcraft.workshopcore.WorkshopCore;
import com.workshopcraft.workshopcore.backdrop.client.BackdropClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Server to client: all loaded backdrop definitions. */
public record BackdropDefsPacket(Map<ResourceLocation, Backdrop> defs) implements CustomPacketPayload {
    public static final Type<BackdropDefsPacket> TYPE = new Type<>(new ResourceLocation(WorkshopCore.MOD_ID, "backdrop_defs"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static BackdropDefsPacket read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, Backdrop> defs = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            defs.put(buf.readResourceLocation(), Backdrop.read(buf));
        }
        return new BackdropDefsPacket(defs);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(defs.size());
        for (Map.Entry<ResourceLocation, Backdrop> entry : defs.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            Backdrop.write(buf, entry.getValue());
        }
    }

    public void handle() {
        BackdropClient.acceptDefinitions(defs);
    }
}
