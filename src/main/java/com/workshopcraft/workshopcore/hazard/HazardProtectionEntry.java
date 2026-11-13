package com.workshopcraft.workshopcore.hazard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

record HazardProtectionEntry(Identifier id, Identifier hazard, List<ItemFilter> items, Set<EquipmentSlot> slots, int amount, int intervalTicks) {
    private static final Set<EquipmentSlot> ALLOWED_SLOTS = EnumSet.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    );

    private static final Codec<EquipmentSlot> SLOT_CODEC = EquipmentSlot.CODEC.validate(slot ->
            ALLOWED_SLOTS.contains(slot)
                    ? DataResult.success(slot)
                    : DataResult.error(() -> "Unsupported protection slot " + slot.getSerializedName()));

    static final Codec<HazardProtectionEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("hazard").forGetter(HazardProtectionEntry::hazard),
            ItemFilter.CODEC.listOf().fieldOf("items").forGetter(HazardProtectionEntry::items),
            SLOT_CODEC.listOf().comapFlatMap(
                    list -> list.isEmpty()
                            ? DataResult.error(() -> "slots must not be empty")
                            : DataResult.success(Set.copyOf(list)),
                    slots -> List.copyOf(slots)
            ).fieldOf("slots").forGetter(HazardProtectionEntry::slots),
            Wear.CODEC.optionalFieldOf("wear", Wear.DEFAULT).forGetter(entry -> new Wear(entry.amount, entry.intervalTicks))
    ).apply(instance, (hazard, items, slots, wear) -> new HazardProtectionEntry(
            Identifier.parse("workshopcore:pending"),
            hazard,
            List.copyOf(items),
            Set.copyOf(slots),
            Math.max(0, wear.amount()),
            Math.max(1, wear.intervalTicks())
    )));

    HazardProtectionEntry withId(Identifier fileId) {
        return new HazardProtectionEntry(fileId, hazard, items, slots, amount, intervalTicks);
    }

    boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (ItemFilter filter : items) {
            if (filter.test(stack)) {
                return true;
            }
        }
        return false;
    }

    private record Wear(int amount, int intervalTicks) {
        static final Wear DEFAULT = new Wear(1, 20);
        static final Codec<Wear> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("amount", 1).forGetter(Wear::amount),
                Codec.INT.optionalFieldOf("interval_ticks", 20).forGetter(Wear::intervalTicks)
        ).apply(instance, Wear::new));
    }

    sealed interface ItemFilter {
        Codec<ItemFilter> CODEC = Codec.STRING.comapFlatMap(ItemFilter::parse, ItemFilter::serialized);

        boolean test(ItemStack stack);

        String serialized();

        private static DataResult<ItemFilter> parse(String raw) {
            if (raw.startsWith("#")) {
                return Identifier.read(raw.substring(1)).map(id -> new Tag(TagKey.create(Registries.ITEM, id)));
            }
            return Identifier.read(raw).map(Id::new);
        }

        record Id(Identifier id) implements ItemFilter {
            @Override
            public boolean test(ItemStack stack) {
                return stack.is(net.minecraft.resources.ResourceKey.create(Registries.ITEM, id));
            }

            @Override
            public String serialized() {
                return id.toString();
            }
        }

        record Tag(TagKey<Item> tag) implements ItemFilter {
            @Override
            public boolean test(ItemStack stack) {
                return stack.is(tag);
            }

            @Override
            public String serialized() {
                return "#" + tag.location();
            }
        }
    }
}
