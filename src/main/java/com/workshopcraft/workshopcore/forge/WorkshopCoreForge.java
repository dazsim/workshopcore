package com.workshopcraft.workshopcore.forge;

import com.workshopcraft.workshopcore.WorkshopCore;
import com.workshopcraft.workshopcore.backdrop.BackdropCommands;
import com.workshopcraft.workshopcore.backdrop.BackdropDefsPacket;
import com.workshopcraft.workshopcore.backdrop.BackdropNetworking;
import com.workshopcraft.workshopcore.backdrop.BackdropReload;
import com.workshopcraft.workshopcore.backdrop.BackdropSyncPacket;
import com.workshopcraft.workshopcore.backdrop.Backdrops;
import com.workshopcraft.workshopcore.hazard.HazardCommands;
import com.workshopcraft.workshopcore.hazard.HazardProtection;
import com.workshopcraft.workshopcore.hazard.HazardProtectionReload;
import com.workshopcraft.workshopcore.hazard.Haze;
import com.workshopcraft.workshopcore.hazard.HazeNetworking;
import com.workshopcraft.workshopcore.hazard.HazeSyncPacket;
import com.workshopcraft.workshopcore.screwdriver.Screwdrivers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.LoggerFactory;

@Mod(WorkshopCore.MOD_ID)
@Mod.EventBusSubscriber(modid = WorkshopCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class WorkshopCoreForge {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(WorkshopCore.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    public WorkshopCoreForge() {
        int id = 0;
        CHANNEL.messageBuilder(BackdropSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BackdropSyncPacket::write)
                .decoder(BackdropSyncPacket::read)
                .consumerMainThread((msg, ctx) -> {
                    msg.handle();
                    ctx.get().setPacketHandled(true);
                })
                .add();
        CHANNEL.messageBuilder(BackdropDefsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BackdropDefsPacket::write)
                .decoder(BackdropDefsPacket::read)
                .consumerMainThread((msg, ctx) -> {
                    msg.handle();
                    ctx.get().setPacketHandled(true);
                })
                .add();
        CHANNEL.messageBuilder(HazeSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(HazeSyncPacket::write)
                .decoder(HazeSyncPacket::read)
                .consumerMainThread((msg, ctx) -> {
                    msg.handle();
                    ctx.get().setPacketHandled(true);
                })
                .add();
        BackdropNetworking.setHooks(
                (player, payload) -> CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload),
                (level, payload) -> CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), payload));
        HazeNetworking.setHook((player, payload) -> CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload));
        MinecraftForge.EVENT_BUS.register(GameEvents.class);
        LoggerFactory.getLogger(WorkshopCore.MOD_ID).info("Workshop Core {}", WorkshopCore.API_VERSION);
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ITEMS, helper ->
                Screwdrivers.registerItem((id, item) -> helper.register(id, item)));
        event.register(ForgeRegistries.Keys.RECIPE_SERIALIZERS, helper ->
                Screwdrivers.registerSerializer((id, serializer) -> helper.register(id, serializer)));
    }

    public static final class GameEvents {
        private GameEvents() {
        }

        @SubscribeEvent
        public static void addReloaders(AddReloadListenerEvent event) {
            event.addListener(new BackdropReload());
            event.addListener(new HazardProtectionReload());
        }

        @SubscribeEvent
        public static void commands(RegisterCommandsEvent event) {
            BackdropCommands.register(event.getDispatcher());
            HazardCommands.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void login(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                BackdropNetworking.sendAllTo(player);
                Haze.resend(player);
            }
        }

        @SubscribeEvent
        public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                Haze.forget(player);
                HazardProtection.clearWear(player);
            }
        }

        @SubscribeEvent
        public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                Haze.resend(player);
            }
        }

        @SubscribeEvent
        public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
                Backdrops.onLevelLoad(level);
                BackdropNetworking.sendDefsTo(player);
                BackdropNetworking.sendAssignment(player, level.dimension(), Backdrops.get(level));
                Haze.resend(player);
            }
        }

        @SubscribeEvent
        public static void levelLoad(LevelEvent.Load event) {
            if (event.getLevel() instanceof ServerLevel level) {
                Backdrops.onLevelLoad(level);
            }
        }

        @SubscribeEvent
        public static void levelUnload(LevelEvent.Unload event) {
            if (event.getLevel() instanceof ServerLevel level) {
                Backdrops.onLevelUnload(level);
            }
        }

        @SubscribeEvent
        public static void datapackSync(OnDatapackSyncEvent event) {
            if (event.getPlayer() != null) {
                BackdropNetworking.sendAllTo(event.getPlayer());
                return;
            }
            for (ServerPlayer player : event.getPlayerList().getPlayers()) {
                BackdropNetworking.sendAllTo(player);
            }
        }

        @SubscribeEvent
        public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
            var result = Screwdrivers.use(event.getLevel(), event.getPos(), event.getEntity(), event.getItemStack(), event.getHand(), event.getHitVec());
            if (result == com.workshopcraft.workshopcore.screwdriver.ScrewdriverResult.PASS) {
                return;
            }
            event.setCanceled(true);
            event.setCancellationResult(result == com.workshopcraft.workshopcore.screwdriver.ScrewdriverResult.SUCCESS
                    ? InteractionResult.SUCCESS
                    : InteractionResult.FAIL);
        }

        @SubscribeEvent
        public static void missing(MissingMappingsEvent event) {
            for (var alias : Screwdrivers.aliases()) {
                for (var mapping : event.getMappings(net.minecraft.core.registries.Registries.ITEM, alias.getNamespace())) {
                    if (alias.equals(mapping.getKey())) {
                        mapping.remap(Screwdrivers.item());
                    }
                }
            }
        }
    }
}
