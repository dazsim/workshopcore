package com.workshopcraft.workshopcore.screwdriver;

import com.workshopcraft.workshopcore.WorkshopCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class Screwdrivers {
    public static final Identifier ITEM_ID = Identifier.fromNamespaceAndPath(WorkshopCore.MOD_ID, "screwdriver");
    public static final TagKey<Item> SCREWDRIVERS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(WorkshopCore.MOD_ID, "screwdrivers"));
    public static final TagKey<Block> ROTATABLE = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(WorkshopCore.MOD_ID, "screwdriver_rotatable"));

    private static final List<HandlerEntry> HANDLERS = new ArrayList<>();
    private static Item item;

    private Screwdrivers() {
    }

    public static Item item() {
        if (item == null) {
            throw new IllegalStateException("workshopcore:screwdriver is not registered yet");
        }
        return item;
    }

    public static boolean isScrewdriver(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (item != null && stack.is(item)) {
            return true;
        }
        if (stack.getItem() instanceof AliasScrewdriverItem) {
            return true;
        }
        return stack.is(SCREWDRIVERS);
    }

    public static void register(Block block, ScrewdriverHandler handler) {
        register(state -> state.is(block), handler);
    }

    public static void register(TagKey<Block> tag, ScrewdriverHandler handler) {
        register(state -> state.is(tag), handler);
    }

    public static void register(Predicate<BlockState> test, ScrewdriverHandler handler) {
        HANDLERS.add(new HandlerEntry(test, handler));
    }

    /**
     * Maps an old item id onto {@link #ITEM_ID}. Call this from the consumer that used to own that id, and do not
     * register the old item yourself.
     *
     * <p>On NeoForge this uses a registry alias, so items already in a world or a chest remap when the world loads.
     *
     * <p>On Fabric there is no remap for stored items, so a hidden alias item is registered under the old id. It looks
     * like the shared screwdriver and converts when it is in a player's inventory or used. Items sitting in a closed
     * chest keep the old id until taken out; they still work if the old id is also listed in {@link #SCREWDRIVERS}.
     */
    public static void alias(Identifier oldId) {
        if (oldId.equals(ITEM_ID)) {
            return;
        }
        //? if fabric {
        if (BuiltInRegistries.ITEM.containsKey(oldId)) {
            LoggerFactory.getLogger(WorkshopCore.MOD_ID).warn("Screwdrivers.alias skipped {}, already registered", oldId);
            return;
        }
        Item alias = new AliasScrewdriverItem(properties(oldId)
                .stacksTo(1)
                .overrideDescription("item.workshopcore.screwdriver")
                .component(DataComponents.ITEM_MODEL, ITEM_ID));
        net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, oldId, alias);
        //?} else {
        /*BuiltInRegistries.ITEM.addAlias(oldId, ITEM_ID);*/
        //?}
    }

    public static ScrewdriverResult use(Level level, BlockPos pos, Player player, ItemStack held,
                                        InteractionHand hand, BlockHitResult hit) {
        if (player.isSpectator()) {
            return ScrewdriverResult.PASS;
        }
        ItemStack stack = convertInHand(player, hand, held);
        if (!isScrewdriver(stack) || !player.mayBuild()) {
            return ScrewdriverResult.PASS;
        }
        BlockState state = level.getBlockState(pos);
        ScrewdriverContext ctx = new ScrewdriverContext(level, pos, state, player, stack, hand, hit);
        if (state.getBlock() instanceof ScrewdriverTarget target) {
            return target.useScrewdriver(ctx);
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ScrewdriverTarget target) {
            return target.useScrewdriver(ctx);
        }
        for (HandlerEntry entry : HANDLERS) {
            if (entry.test.test(state)) {
                return entry.handler.useScrewdriver(ctx);
            }
        }
        if (state.is(ROTATABLE)) {
            return rotate(ctx);
        }
        return ScrewdriverResult.PASS;
    }

    static ItemStack convert(ItemStack stack) {
        if (!(stack.getItem() instanceof AliasScrewdriverItem) || item == null) {
            return stack;
        }
        return stack.transmuteCopy(item);
    }

    public static void registerItem(BiConsumer<Identifier, Item> r) {
        item = new Item(properties(ITEM_ID).stacksTo(1));
        r.accept(ITEM_ID, item);
    }

    public static void registerSerializer(BiConsumer<Identifier, RecipeSerializer<?>> r) {
        r.accept(KeepToolRecipe.ID, KeepToolRecipe.SERIALIZER);
    }

    private static Item.Properties properties(Identifier id) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }

    private static ItemStack convertInHand(Player player, InteractionHand hand, ItemStack held) {
        ItemStack converted = convert(held);
        if (converted != held) {
            player.setItemInHand(hand, converted);
        }
        return converted;
    }

    private static ScrewdriverResult rotate(ScrewdriverContext ctx) {
        Rotation rotation = ctx.sneaking() ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
        BlockState next = ctx.state().rotate(rotation);
        if (next == ctx.state()) {
            return ScrewdriverResult.PASS;
        }
        if (!ctx.isClientSide()) {
            ctx.level().setBlock(ctx.pos(), next, 3);
            ctx.click();
        }
        return ScrewdriverResult.SUCCESS;
    }

    private record HandlerEntry(Predicate<BlockState> test, ScrewdriverHandler handler) {
    }
}
