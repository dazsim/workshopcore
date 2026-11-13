package com.workshopcraft.workshopcore.hazard;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Helpers for applying a datapack damage type to a living entity. */
public final class HazardDamage {
    private HazardDamage() {
    }

    public static ResourceKey<DamageType> key(Identifier id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, id);
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type));
    }

    /**
     * Applies {@code amount} of {@code type} on the server. Returns whether damage was applied.
     * Does nothing and returns false if the entity is not on a {@link ServerLevel}.
     * Throws if the damage type is not registered in that level.
     */
    public static boolean hurt(LivingEntity entity, ResourceKey<DamageType> type, float amount) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return entity.hurtServer(serverLevel, source(serverLevel, type), amount);
    }
}
