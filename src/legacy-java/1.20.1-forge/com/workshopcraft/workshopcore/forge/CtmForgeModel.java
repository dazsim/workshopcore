package com.workshopcraft.workshopcore.forge;

import com.workshopcraft.workshopcore.ctm.client.CtmDef;
import com.workshopcraft.workshopcore.ctm.client.CtmDefs;
import com.workshopcraft.workshopcore.ctm.client.CtmMask;
import com.workshopcraft.workshopcore.ctm.client.CtmQuads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.List;
import java.util.Map;

/** Wraps a baked model; the connection mask is worked out in getModelData and read back in getQuads. */
public final class CtmForgeModel extends BakedModelWrapper<BakedModel> {
    private static final ModelProperty<Long> MASK = new ModelProperty<>();
    private final CtmDef def;

    private CtmForgeModel(BakedModel original, CtmDef def) {
        super(original);
        this.def = def;
    }

    public static void bake(ModelEvent.ModifyBakingResult event) {
        CtmDefs.reload(Minecraft.getInstance().getResourceManager());
        Map<net.minecraft.resources.ResourceLocation, BakedModel> models = event.getModels();
        for (Map.Entry<Block, CtmDef> e : CtmDefs.all().entrySet()) {
            for (BlockState state : e.getKey().getStateDefinition().getPossibleStates()) {
                ModelResourceLocation id = BlockModelShaper.stateToModelLocation(state);
                BakedModel model = models.get(id);
                if (model != null) {
                    models.put(id, new CtmForgeModel(model, e.getValue()));
                }
            }
        }
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        ModelData data = super.getModelData(level, pos, state, modelData);
        return data.derive().with(MASK, CtmMask.compute(level, pos, state, def)).build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData, RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, extraData, renderType);
        Long mask = extraData.get(MASK);
        return mask == null ? quads : CtmQuads.transform(quads, side, mask, def);
    }
}
