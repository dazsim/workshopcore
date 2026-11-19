package com.workshopcraft.workshopcore.machine;

import com.workshopcraft.workshopcore.compat.Stacks;
import com.workshopcraft.workshopcore.compat.ValueInput;
import com.workshopcraft.workshopcore.compat.ValueOutput;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Everything a machine was built from, in fitting order. Empty stacks mean the part is absent. */
public record MachineParts(ItemStack chassis, ItemStack core, ItemStack board, List<ItemStack> upgrades, ItemStack panels, ItemStack worktop) {
    public static final MachineParts NONE = new MachineParts(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, List.of(), ItemStack.EMPTY, ItemStack.EMPTY);

    public MachineParts {
        upgrades = List.copyOf(upgrades);
    }

    public boolean isEmpty() {
        return chassis.isEmpty() && core.isEmpty();
    }

    public ChassisTier tier() {
        return chassis.getItem() instanceof BlockItem b && b.getBlock() instanceof ChassisBlock c ? c.tier() : ChassisTier.BASIC;
    }

    public MachineCore machineCore() {
        return Assembly.core(core);
    }

    public List<MachineUpgrade> upgradeList() {
        final List<MachineUpgrade> out = new ArrayList<>();
        for (ItemStack s : upgrades) {
            final MachineUpgrade u = Assembly.upgrade(s);
            if (u != null) out.add(u);
        }
        return out;
    }

    /** The fitted drive upgrade, else the core's drive, else null. */
    public Drive drive() {
        for (MachineUpgrade u : upgradeList()) if (u.drive() != null) return u.drive();
        final MachineCore c = machineCore();
        return c == null ? null : c.drive();
    }

    /** Work speed in percent: tier speed plus upgrade speed. */
    public int speed() {
        int s = tier().speed();
        for (MachineUpgrade u : upgradeList()) s += u.speed();
        return Math.max(1, s);
    }

    /** Energy capacity in percent: tier storage plus upgrade storage. */
    public int storage() {
        int s = tier().storage();
        for (MachineUpgrade u : upgradeList()) s += u.storage();
        return Math.max(0, s);
    }

    public List<ItemStack> drops() {
        final List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : List.of(chassis, core, board)) if (!s.isEmpty()) out.add(s.copy());
        for (ItemStack s : upgrades) if (!s.isEmpty()) out.add(s.copy());
        for (ItemStack s : List.of(panels, worktop)) if (!s.isEmpty()) out.add(s.copy());
        return out;
    }

    public MachineParts withWorktop(ItemStack worktop) {
        return new MachineParts(chassis, core, board, upgrades, panels, worktop);
    }

    public void save(ValueOutput out, String prefix) {
        put(out, prefix + "chassis", chassis);
        put(out, prefix + "core", core);
        put(out, prefix + "board", board);
        put(out, prefix + "panels", panels);
        put(out, prefix + "worktop", worktop);
        if (!upgrades.isEmpty()) out.store(prefix + "upgrades", Stacks.OPTIONAL_CODEC.listOf(), upgrades);
    }

    public static MachineParts load(ValueInput in, String prefix) {
        return new MachineParts(get(in, prefix + "chassis"), get(in, prefix + "core"), get(in, prefix + "board"),
                in.read(prefix + "upgrades", Stacks.OPTIONAL_CODEC.listOf()).orElse(List.of()),
                get(in, prefix + "panels"), get(in, prefix + "worktop"));
    }

    static void put(ValueOutput out, String key, ItemStack stack) {
        if (!stack.isEmpty()) out.store(key, Stacks.OPTIONAL_CODEC, stack);
    }

    static ItemStack get(ValueInput in, String key) {
        return in.read(key, Stacks.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }
}
