package com.workshopcraft.workshopcore.fabric;

import com.workshopcraft.workshopcore.ctm.client.CtmDef;
import com.workshopcraft.workshopcore.ctm.client.CtmDefs;
import com.workshopcraft.workshopcore.ctm.client.CtmMask;
import com.workshopcraft.workshopcore.ctm.client.CtmQuads;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Fabric renderer API wrapper that swaps face quads for connected tiles. */
public final class CtmFabricModel extends ForwardingBakedModel {
    private static final Direction[] DIRECTIONS = Direction.values();
    private final CtmDef def;

    private CtmFabricModel(BakedModel model, CtmDef def) {
        this.wrapped = model;
        this.def = def;
    }

    public static void register() {
        ModelLoadingPlugin.register(ctx -> {
            CtmDefs.reload(Minecraft.getInstance().getResourceManager());
            Map<String, CtmDef> byModel = new HashMap<>();
            for (Map.Entry<Block, CtmDef> e : CtmDefs.all().entrySet()) {
                for (BlockState state : e.getKey().getStateDefinition().getPossibleStates()) {
                    byModel.put(BlockModelShaper.stateToModelLocation(state).toString(), e.getValue());
                }
            }
            if (byModel.isEmpty()) {
                return;
            }
            ctx.modifyModelAfterBake().register((model, c) -> {
                Object id = modelId(c);
                CtmDef def = model == null || id == null ? null : byModel.get(id.toString());
                return def == null ? model : new CtmFabricModel(model, def);
            });
        });
    }

    private static Object modelId(net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier.AfterBake.Context c) {
        return c.topLevelId();
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> random, RenderContext context) {
        if (def.sprites() == null) {
            super.emitBlockQuads(level, state, pos, random, context);
            return;
        }
        long mask = CtmMask.compute(level, pos, state, def);
        RenderMaterial material = RendererAccess.INSTANCE.getRenderer().materialFinder()
                .blendMode(BlendMode.fromRenderLayer(ItemBlockRenderTypes.getChunkRenderType(state))).find();
        QuadEmitter emitter = context.getEmitter();
        for (Direction d : DIRECTIONS) {
            for (BakedQuad q : CtmQuads.transform(wrapped.getQuads(state, d, random.get()), d, mask, def)) {
                emitter.fromVanilla(q, material, d);
                emitter.emit();
            }
        }
        for (BakedQuad q : CtmQuads.transform(wrapped.getQuads(state, null, random.get()), null, mask, def)) {
            emitter.fromVanilla(q, material, null);
            emitter.emit();
        }
    }
}
