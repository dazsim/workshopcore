package com.workshopcraft.workshopcore.neoforge;

import com.workshopcraft.workshopcore.ctm.client.CtmDef;
import com.workshopcraft.workshopcore.ctm.client.CtmDefs;
import com.workshopcraft.workshopcore.ctm.client.CtmMask;
import com.workshopcraft.workshopcore.ctm.client.CtmQuads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;

import java.util.List;
import java.util.Map;

/** Wraps a block state model so its face quads pick connected tiles from the neighbouring blocks. */
public final class CtmNeoModel extends DelegateBlockStateModel {
    private final CtmDef def;

    private CtmNeoModel(BlockStateModel delegate, CtmDef def) {
        super(delegate);
        this.def = def;
    }

    public static void bake(ModelEvent.ModifyBakingResult event) {
        CtmDefs.reload(Minecraft.getInstance().getResourceManager());
        Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();
        for (Map.Entry<Block, CtmDef> e : CtmDefs.all().entrySet()) {
            for (BlockState state : e.getKey().getStateDefinition().getPossibleStates()) {
                BlockStateModel model = models.get(state);
                if (model != null) {
                    models.put(state, new CtmNeoModel(model, e.getValue()));
                }
            }
        }
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        int start = parts.size();
        delegate.collectParts(level, pos, state, random, parts);
        if (def.sprites() == null) {
            return;
        }
        long mask = CtmMask.compute(level, pos, state, def);
        for (int i = start; i < parts.size(); i++) {
            parts.set(i, new Part(parts.get(i), mask, def));
        }
    }

    private static final class Part implements BlockStateModelPart {
        private static final Direction[] DIRECTIONS = Direction.values();
        private final BlockStateModelPart base;
        private final List<BakedQuad>[] faces;
        private final List<BakedQuad> unculled;

        @SuppressWarnings("unchecked")
        Part(BlockStateModelPart base, long mask, CtmDef def) {
            this.base = base;
            this.faces = new List[6];
            for (Direction d : DIRECTIONS) {
                faces[d.ordinal()] = CtmQuads.transform(base.getQuads(d), d, mask, def);
            }
            this.unculled = CtmQuads.transform(base.getQuads(null), null, mask, def);
        }

        @Override
        public List<BakedQuad> getQuads(Direction direction) {
            return direction == null ? unculled : faces[direction.ordinal()];
        }

        @Override
        public boolean useAmbientOcclusion() {
            return base.useAmbientOcclusion();
        }

        @Override
        public TriState ambientOcclusion() {
            return base.ambientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return base.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return base.materialFlags();
        }
    }
}
