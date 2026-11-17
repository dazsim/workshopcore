package com.workshopcraft.workshopcore.ctm.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Per face connection bits, 9 per face: up, down, left, right, the four diagonals, then whether the block
 * across the face connects. Up and left are as seen in the texture, see {@link #UP} and {@link #LEFT}.
 */
public final class CtmMask {
    public static final int U = 1, D = 2, L = 4, R = 8, UL = 16, UR = 32, DL = 64, DR = 128, ACROSS = 256;
    static final Direction[] UP = new Direction[6];
    static final Direction[] LEFT = new Direction[6];

    static {
        axes(Direction.DOWN, Direction.SOUTH, Direction.WEST);
        axes(Direction.UP, Direction.NORTH, Direction.WEST);
        axes(Direction.NORTH, Direction.UP, Direction.EAST);
        axes(Direction.SOUTH, Direction.UP, Direction.WEST);
        axes(Direction.WEST, Direction.UP, Direction.NORTH);
        axes(Direction.EAST, Direction.UP, Direction.SOUTH);
    }

    private CtmMask() {
    }

    private static void axes(Direction face, Direction up, Direction left) {
        UP[face.ordinal()] = up;
        LEFT[face.ordinal()] = left;
    }

    public static long compute(BlockGetter level, BlockPos pos, BlockState state, CtmDef def) {
        long mask = 0;
        for (Direction face : Direction.values()) {
            Direction up = UP[face.ordinal()], left = LEFT[face.ordinal()];
            Direction down = up.getOpposite(), right = left.getOpposite();
            int bits = 0;
            if (joins(level, pos, state, def, face, pos.relative(up))) bits |= U;
            if (joins(level, pos, state, def, face, pos.relative(down))) bits |= D;
            if (joins(level, pos, state, def, face, pos.relative(left))) bits |= L;
            if (joins(level, pos, state, def, face, pos.relative(right))) bits |= R;
            if (joins(level, pos, state, def, face, pos.relative(up).relative(left))) bits |= UL;
            if (joins(level, pos, state, def, face, pos.relative(up).relative(right))) bits |= UR;
            if (joins(level, pos, state, def, face, pos.relative(down).relative(left))) bits |= DL;
            if (joins(level, pos, state, def, face, pos.relative(down).relative(right))) bits |= DR;
            BlockPos across = pos.relative(face);
            if (def.rule.connects(level, pos, state, across, level.getBlockState(across), face)) bits |= ACROSS;
            mask |= (long) bits << (face.ordinal() * 9);
        }
        return mask;
    }

    // A neighbour whose own face is covered by a connecting block does not join, so outer corners keep their border
    private static boolean joins(BlockGetter level, BlockPos pos, BlockState state, CtmDef def, Direction face, BlockPos other) {
        if (!def.rule.connects(level, pos, state, other, level.getBlockState(other), face)) {
            return false;
        }
        BlockPos front = other.relative(face);
        return !def.rule.connects(level, pos, state, front, level.getBlockState(front), face);
    }

    public static int face(long mask, Direction face) {
        return (int) (mask >>> (face.ordinal() * 9)) & 511;
    }

    /** Tile for one quarter of a face: 0 border, 1 no border, 2 horizontal run, 3 vertical run, 4 inner corner. */
    public static int tile(int bits, boolean right, boolean bottom) {
        boolean v = (bits & (bottom ? D : U)) != 0;
        boolean h = (bits & (right ? R : L)) != 0;
        if (v && h) {
            int diag = bottom ? (right ? DR : DL) : (right ? UR : UL);
            return (bits & diag) != 0 ? 1 : 4;
        }
        return h ? 2 : v ? 3 : 0;
    }
}
