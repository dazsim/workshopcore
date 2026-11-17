package com.workshopcraft.workshopcore.fabric;

import com.workshopcraft.workshopcore.ctm.client.CtmDef;
import com.workshopcraft.workshopcore.ctm.client.CtmDefs;
import com.workshopcraft.workshopcore.ctm.client.CtmMask;
import com.workshopcraft.workshopcore.ctm.client.CtmQuads;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Fabric renderer API wrapper that swaps face quads for connected tiles. */
public final class CtmFabricModel implements BlockStateModel {
    private static final Direction[] DIRECTIONS = Direction.values();
    private final BlockStateModel delegate;
    private final CtmDef def;

    private CtmFabricModel(BlockStateModel delegate, CtmDef def) {
        this.delegate = delegate;
        this.def = def;
    }

    public static void register() {
        ModelLoadingPlugin.register(ctx -> {
            CtmDefs.reload(Minecraft.getInstance().getResourceManager());
            ctx.modifyBlockModelAfterBake().register((model, c) -> {
                CtmDef def = model == null ? null : CtmDefs.get(c.state().getBlock());
                return def == null ? model : new CtmFabricModel(model, def);
            });
        });
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<Direction> cullTest) {
        if (def.sprites() == null) {
            delegate.emitQuads(emitter, level, pos, state, random, cullTest);
            return;
        }
        long mask = CtmMask.compute(level, pos, state, def);
        List<BlockStateModelPart> parts = new ArrayList<>();
        delegate.collectParts(random, parts);
        for (BlockStateModelPart part : parts) {
            for (Direction d : DIRECTIONS) {
                if (cullTest.test(d)) {
                    continue;
                }
                for (BakedQuad q : CtmQuads.transform(part.getQuads(d), d, mask, def)) {
                    emitter.fromBakedQuad(q);
                    emitter.cullFace(d);
                    emitter.emit();
                }
            }
            for (BakedQuad q : CtmQuads.transform(part.getQuads(null), null, mask, def)) {
                emitter.fromBakedQuad(q);
                emitter.emit();
            }
        }
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return null;
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        delegate.collectParts(random, parts);
    }

    @Override
    public Material.Baked particleMaterial() {
        return delegate.particleMaterial();
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return delegate.particleMaterial(level, pos, state);
    }

    @Override
    public int materialFlags() {
        return delegate.materialFlags();
    }

    @Override
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return delegate.materialFlags(level, pos, state, random);
    }
}
