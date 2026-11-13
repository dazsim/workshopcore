package com.workshopcraft.workshopcore.hazard;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protective gear loaded from {@code data/<ns>/workshopcore/hazard_protection/*.json}.
 *
 * <p>Several files may cover the same hazard. The player is protected when every slot
 * listed by those files has an equipped item accepted by at least one file that lists
 * that slot. Adding a file that lists a new slot tightens the requirement. Alternative
 * items for the same slot can be listed in one file, split across files, or given as
 * item tags. A hazard with no loaded files is never protected by gear.
 *
 * <p>{@link #isProtected} only inspects equipment. {@link #tick} is meant to be called
 * once per server tick while the player is exposed. Creative and spectator players
 * skip wear and count as protected. Unprotected players return {@code false} and do
 * not take wear. Wear uses one server-side counter per player and hazard: the interval
 * is the minimum {@code interval_ticks} among files for that hazard, and when it
 * elapses each protected slot is damaged by the maximum {@code amount} among files
 * that both list that slot and accept the equipped item. Items break the vanilla way.
 * Counters reset when the player is unprotected, logs out, or datapacks reload.
 */
public final class HazardProtection {
    private static volatile Map<Identifier, List<HazardProtectionEntry>> byHazard = Map.of();
    private static final Map<UUID, Map<Identifier, Integer>> WEAR_TICKS = new ConcurrentHashMap<>();

    private HazardProtection() {
    }

    static void acceptReload(List<HazardProtectionEntry> entries) {
        Map<Identifier, List<HazardProtectionEntry>> grouped = new HashMap<>();
        for (HazardProtectionEntry entry : entries) {
            grouped.computeIfAbsent(entry.hazard(), key -> new java.util.ArrayList<>()).add(entry);
        }
        Map<Identifier, List<HazardProtectionEntry>> frozen = HashMap.newHashMap(grouped.size());
        for (var e : grouped.entrySet()) {
            frozen.put(e.getKey(), List.copyOf(e.getValue()));
        }
        byHazard = Map.copyOf(frozen);
        WEAR_TICKS.clear();
    }

    static Set<Identifier> hazards() {
        return byHazard.keySet();
    }

    public static boolean isProtected(Player player, Identifier hazard) {
        List<HazardProtectionEntry> entries = byHazard.getOrDefault(hazard, List.of());
        if (entries.isEmpty()) {
            return false;
        }
        return matches(player, entries);
    }

    /**
     * Call once per tick while the player is exposed. Returns {@code false} when the
     * player is not protected. Creative and spectator players return {@code true}
     * and take no wear.
     */
    public static boolean tick(Player player, Identifier hazard) {
        if (player.isCreative() || player.isSpectator()) {
            clearWear(player, hazard);
            return true;
        }
        List<HazardProtectionEntry> entries = byHazard.getOrDefault(hazard, List.of());
        if (entries.isEmpty() || !matches(player, entries)) {
            clearWear(player, hazard);
            return false;
        }
        if (player.level() instanceof ServerLevel) {
            applyWear(player, hazard, entries);
        }
        return true;
    }

    public static void clearWear(Player player) {
        WEAR_TICKS.remove(player.getUUID());
    }

    private static void clearWear(Player player, Identifier hazard) {
        Map<Identifier, Integer> counters = WEAR_TICKS.get(player.getUUID());
        if (counters != null) {
            counters.remove(hazard);
            if (counters.isEmpty()) {
                WEAR_TICKS.remove(player.getUUID());
            }
        }
    }

    private static boolean matches(Player player, List<HazardProtectionEntry> entries) {
        EnumMap<EquipmentSlot, Boolean> slots = new EnumMap<>(EquipmentSlot.class);
        for (HazardProtectionEntry entry : entries) {
            for (EquipmentSlot slot : entry.slots()) {
                slots.putIfAbsent(slot, false);
            }
        }
        if (slots.isEmpty()) {
            return false;
        }
        for (EquipmentSlot slot : slots.keySet()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                return false;
            }
            boolean ok = false;
            for (HazardProtectionEntry entry : entries) {
                if (entry.slots().contains(slot) && entry.accepts(stack)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static void applyWear(Player player, Identifier hazard, List<HazardProtectionEntry> entries) {
        int interval = Integer.MAX_VALUE;
        for (HazardProtectionEntry entry : entries) {
            interval = Math.min(interval, entry.intervalTicks());
        }
        if (interval == Integer.MAX_VALUE) {
            return;
        }
        Map<Identifier, Integer> counters = WEAR_TICKS.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
        int ticks = counters.getOrDefault(hazard, 0) + 1;
        if (ticks < interval) {
            counters.put(hazard, ticks);
            return;
        }
        counters.put(hazard, 0);
        EnumMap<EquipmentSlot, Integer> amounts = new EnumMap<>(EquipmentSlot.class);
        for (HazardProtectionEntry entry : entries) {
            for (EquipmentSlot slot : entry.slots()) {
                ItemStack stack = player.getItemBySlot(slot);
                if (entry.accepts(stack)) {
                    amounts.merge(slot, entry.amount(), Math::max);
                }
            }
        }
        for (var e : amounts.entrySet()) {
            ItemStack stack = player.getItemBySlot(e.getKey());
            if (!stack.isEmpty()) {
                stack.hurtAndBreak(e.getValue(), player, e.getKey());
            }
        }
    }
}
