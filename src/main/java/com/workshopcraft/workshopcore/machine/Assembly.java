package com.workshopcraft.workshopcore.machine;

import com.workshopcraft.workshopcore.Ids;
import com.workshopcraft.workshopcore.compat.Players;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Part assembly on a {@link ChassisBlock}: core, control board, upgrades, panels, worktop.
 * Each part is fitted loose by hand and fixed with a screwdriver; sneaking with a screwdriver removes the outermost part.
 */
public final class Assembly {
    public static final TagKey<Item> CONTROL_BOARDS = TagKey.create(Registries.ITEM, Ids.of("workshopcore", "control_boards"));
    public static final TagKey<Item> PANELS = TagKey.create(Registries.ITEM, Ids.of("workshopcore", "machine_panels"));
    public static final TagKey<Item> WORKTOPS = TagKey.create(Registries.ITEM, Ids.of("c", "worktops"));

    /** Chassis STEP values. Odd steps hold a loose part. */
    public static final int EMPTY = 0, CORE = 2, BOARD = 4, PANELS_FIXED = 6, WORKTOP_LOOSE = 7;

    private static final Map<Identifier, MachineCore> CORES = new ConcurrentHashMap<>();
    private static final Map<Identifier, MachineUpgrade> UPGRADES = new ConcurrentHashMap<>();

    private Assembly() {
    }

    /** Makes the item with this id a core part. Safe to call before the item is registered. */
    public static void registerCore(Identifier item, MachineCore core) {
        CORES.put(item, core);
    }

    public static void registerUpgrade(Identifier item, MachineUpgrade upgrade) {
        UPGRADES.put(item, upgrade);
    }

    public static MachineCore core(ItemStack stack) {
        return stack.isEmpty() ? null : CORES.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static MachineUpgrade upgrade(ItemStack stack) {
        return stack.isEmpty() ? null : UPGRADES.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean isPart(ItemStack stack) {
        return core(stack) != null || upgrade(stack) != null || stack.is(CONTROL_BOARDS) || stack.is(PANELS) || stack.is(WORKTOPS);
    }

    /** Why this upgrade cannot go on, as a translation key, or null if it fits. */
    public static String refusal(MachineCore core, List<ItemStack> fitted, MachineUpgrade upgrade) {
        if (core != null && !core.accepts(upgrade)) return "message.workshopcore.assembly.upgrade_refused";
        if (core != null && fitted.size() >= core.upgradeSlots()) return "message.workshopcore.assembly.upgrades_full";
        for (ItemStack s : fitted) {
            final MachineUpgrade u = upgrade(s);
            if (u == null) continue;
            if (u.drive() != null && upgrade.drive() != null) return "message.workshopcore.assembly.one_drive";
            if (upgrade.unique() && u.id().equals(upgrade.id())) return "message.workshopcore.assembly.duplicate";
        }
        return null;
    }

    /** Hand fits a part. Returns true if the click was used. */
    public static boolean fit(BlockState state, Level level, BlockPos pos, Player player, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof ChassisBlockEntity be)) return false;
        final int step = state.getValue(ChassisBlock.STEP);
        String msg = null;
        boolean fitted = false;
        if (step == EMPTY && core(stack) != null) {
            be.core = one(stack);
            fitted = true;
        } else if (step == CORE && stack.is(CONTROL_BOARDS)) {
            be.board = one(stack);
            fitted = true;
        } else if (step == BOARD && upgrade(stack) != null) {
            if (be.loose) msg = "message.workshopcore.assembly.fix_loose";
            else msg = refusal(core(be.core), be.upgrades, upgrade(stack));
            if (msg == null) {
                be.upgrades.add(one(stack));
                be.loose = true;
                take(player, stack);
                be.changed();
                Players.overlay(player, Component.translatable("message.workshopcore.assembly.screw"));
                click(level, pos);
                return true;
            }
        } else if (step == BOARD && stack.is(PANELS)) {
            if (be.loose) msg = "message.workshopcore.assembly.fix_loose";
            else {
                be.panels = one(stack);
                fitted = true;
            }
        } else if (step == PANELS_FIXED && stack.is(WORKTOPS)) {
            be.worktop = one(stack);
            fitted = true;
        } else if (isPart(stack)) {
            msg = step % 2 == 1 ? "message.workshopcore.assembly.screw" : "message.workshopcore.assembly.wrong_part";
        }
        if (fitted) {
            take(player, stack);
            be.changed();
            level.setBlock(pos, state.setValue(ChassisBlock.STEP, step + 1), Block.UPDATE_ALL);
            Players.overlay(player, Component.translatable("message.workshopcore.assembly.screw"));
            click(level, pos);
            return true;
        }
        if (msg != null) {
            Players.overlay(player, Component.translatable(msg));
            return true;
        }
        return false;
    }

    /** Screwdriver without sneaking: fixes the loose part, or finishes the machine. */
    public static boolean screw(BlockState state, Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof ChassisBlockEntity be)) return false;
        final int step = state.getValue(ChassisBlock.STEP);
        if (step == BOARD && be.loose) {
            be.loose = false;
            be.changed();
        } else if (step == WORKTOP_LOOSE) {
            final MachineParts parts = be.parts(state);
            final MachineCore core = parts.machineCore();
            if (core == null) return false;
            BlockState next = core.machine().defaultBlockState();
            if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && next.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                next = next.setValue(BlockStateProperties.HORIZONTAL_FACING, state.getValue(BlockStateProperties.HORIZONTAL_FACING));
            }
            be.clear();
            level.setBlock(pos, next, Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof WorkBlockEntity m) m.install(parts);
        } else if (step % 2 == 1) {
            level.setBlock(pos, state.setValue(ChassisBlock.STEP, step + 1), Block.UPDATE_ALL);
        } else {
            Players.overlay(player, Component.translatable(hint(step)));
            return true;
        }
        ratchet(level, pos);
        return true;
    }

    /** Screwdriver while sneaking: takes the outermost part off and drops it. */
    public static boolean unscrew(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof ChassisBlockEntity be)) return false;
        final int step = state.getValue(ChassisBlock.STEP);
        if (step == EMPTY) return false;
        final ItemStack part;
        int next;
        if (step >= PANELS_FIXED + 1) {
            part = be.worktop;
            be.worktop = ItemStack.EMPTY;
            next = PANELS_FIXED;
        } else if (step > BOARD) {
            part = be.panels;
            be.panels = ItemStack.EMPTY;
            next = BOARD;
        } else if (step == BOARD && !be.upgrades.isEmpty()) {
            part = be.upgrades.remove(be.upgrades.size() - 1);
            be.loose = false;
            next = BOARD;
        } else if (step > CORE) {
            part = be.board;
            be.board = ItemStack.EMPTY;
            next = CORE;
        } else {
            part = be.core;
            be.core = ItemStack.EMPTY;
            next = EMPTY;
        }
        be.changed();
        if (next != step) level.setBlock(pos, state.setValue(ChassisBlock.STEP, next), Block.UPDATE_ALL);
        if (!part.isEmpty()) Block.popResource(level, pos.above(), part);
        ratchet(level, pos);
        return true;
    }

    /** Turns a finished machine back into its chassis with the worktop removed. Contents drop. */
    public static boolean disassemble(Level level, BlockPos pos, WorkBlockEntity machine) {
        final MachineParts parts = machine.parts();
        if (parts.isEmpty() || !(parts.chassis().getItem() instanceof net.minecraft.world.item.BlockItem bi)
                || !(bi.getBlock() instanceof ChassisBlock chassis)) return false;
        final BlockState old = level.getBlockState(pos);
        machine.dropContents();
        machine.forgetParts();
        BlockState next = chassis.defaultBlockState().setValue(ChassisBlock.STEP, PANELS_FIXED);
        if (old.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            next = next.setValue(BlockStateProperties.HORIZONTAL_FACING, old.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof ChassisBlockEntity be) be.restore(parts.withWorktop(ItemStack.EMPTY));
        if (!parts.worktop().isEmpty()) Block.popResource(level, pos.above(), parts.worktop().copy());
        ratchet(level, pos);
        return true;
    }

    public static String hint(int step) {
        return switch (step) {
            case EMPTY -> "message.workshopcore.assembly.need_core";
            case CORE -> "message.workshopcore.assembly.need_board";
            case BOARD -> "message.workshopcore.assembly.need_panels";
            case PANELS_FIXED -> "message.workshopcore.assembly.need_worktop";
            default -> "message.workshopcore.assembly.screw";
        };
    }

    static ItemStack one(ItemStack stack) {
        final ItemStack c = stack.copy();
        c.setCount(1);
        return c;
    }

    static void take(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) stack.shrink(1);
    }

    static void click(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 1.0f);
    }

    static void ratchet(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.7f, 1.4f);
    }
}
