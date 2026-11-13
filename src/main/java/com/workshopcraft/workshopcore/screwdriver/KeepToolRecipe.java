package com.workshopcraft.workshopcore.screwdriver;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.workshopcore.WorkshopCore;
import com.workshopcraft.workshopcore.compat.Registration;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.NormalCraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Shapeless crafting that hands back the ingredient matching "keep" instead of using it up.
 * The kept ingredient must also be listed in "ingredients".
 */
public class KeepToolRecipe extends NormalCraftingRecipe {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(WorkshopCore.MOD_ID, "crafting_shapeless_keep_tool");
    public static final MapCodec<KeepToolRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result),
            Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(o -> o.ingredients),
            Ingredient.CODEC.fieldOf("keep").forGetter(o -> o.keep)
    ).apply(i, KeepToolRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, KeepToolRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
            ItemStackTemplate.STREAM_CODEC, o -> o.result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), o -> o.ingredients,
            Ingredient.CONTENTS_STREAM_CODEC, o -> o.keep,
            KeepToolRecipe::new);
    public static final RecipeSerializer<KeepToolRecipe> SERIALIZER = Registration.serializer(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate result;
    private final List<Ingredient> ingredients;
    private final Ingredient keep;

    public KeepToolRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                          ItemStackTemplate result, List<Ingredient> ingredients, Ingredient keep) {
        super(commonInfo, bookInfo);
        this.result = result;
        this.ingredients = ingredients;
        this.keep = keep;
    }

    @Override
    public RecipeSerializer<KeepToolRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(ingredients);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != ingredients.size()) {
            return false;
        }
        return input.size() == 1 && ingredients.size() == 1
                ? ingredients.get(0).test(input.getItem(0))
                : input.stackedContents().canCraft(this, null);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return result.create();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> left = CraftingRecipe.defaultCraftingReminder(input);
        boolean kept = false;
        for (int slot = 0; slot < left.size(); slot++) {
            ItemStack in = input.getItem(slot);
            if (!kept && !in.isEmpty() && keep.test(in)) {
                left.set(slot, in.copyWithCount(1));
                kept = true;
            }
        }
        return left;
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new ShapelessCraftingRecipeDisplay(
                ingredients.stream().map(Ingredient::display).toList(),
                new SlotDisplay.ItemStackSlotDisplay(result),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }
}
