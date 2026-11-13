package com.workshopcraft.workshopcore.screwdriver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.workshopcore.WorkshopCore;
import com.workshopcraft.workshopcore.compat.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.List;

/** Shapeless crafting that hands back the ingredient matching "keep" instead of using it up. */
public class KeepToolRecipe implements CraftingRecipe {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(WorkshopCore.MOD_ID, "crafting_shapeless_keep_tool");
    public static final MapCodec<KeepToolRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(o -> o.group),
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(o -> o.category),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(o -> o.result),
            Ingredient.CODEC_NONEMPTY.listOf(1, 9).fieldOf("ingredients").forGetter(o -> o.ingredients),
            Ingredient.CODEC_NONEMPTY.fieldOf("keep").forGetter(o -> o.keep)
    ).apply(i, KeepToolRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, KeepToolRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, o -> o.group,
            CraftingBookCategory.STREAM_CODEC, o -> o.category,
            ItemStack.STREAM_CODEC, o -> o.result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), o -> o.ingredients,
            Ingredient.CONTENTS_STREAM_CODEC, o -> o.keep,
            KeepToolRecipe::new);
    public static final RecipeSerializer<KeepToolRecipe> SERIALIZER = Registration.serializer(MAP_CODEC, STREAM_CODEC);

    private final String group;
    private final CraftingBookCategory category;
    private final ItemStack result;
    private final List<Ingredient> ingredients;
    private final Ingredient keep;

    public KeepToolRecipe(String group, CraftingBookCategory category, ItemStack result, List<Ingredient> ingredients, Ingredient keep) {
        this.group = group;
        this.category = category;
        this.result = result;
        this.ingredients = ingredients;
        this.keep = keep;
    }

    @Override
    public RecipeSerializer<KeepToolRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(ingredients);
        return list;
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
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= ingredients.size();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> left = CraftingRecipe.super.getRemainingItems(input);
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
}
