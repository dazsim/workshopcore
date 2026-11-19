package com.workshopcraft.workshopcore.machine;

import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/** The part that decides which machine a chassis becomes. Register with {@link Assembly#registerCore}. */
public final class MachineCore {
    private final Identifier id;
    private final Supplier<? extends Block> machine;
    private final int upgradeSlots;
    private final Drive drive;
    private final Predicate<MachineUpgrade> accepts;

    private MachineCore(Builder b) {
        this.id = b.id;
        this.machine = b.machine;
        this.upgradeSlots = b.upgradeSlots;
        this.drive = b.drive;
        this.accepts = b.accepts;
    }

    public static Builder builder(Identifier id, Supplier<? extends Block> machine) {
        return new Builder(id, machine);
    }

    public Identifier id() {
        return id;
    }

    public Block machine() {
        return machine.get();
    }

    public int upgradeSlots() {
        return upgradeSlots;
    }

    /** Drive used when no fitted upgrade supplies one. */
    public Drive drive() {
        return drive;
    }

    public boolean accepts(MachineUpgrade upgrade) {
        return accepts.test(upgrade);
    }

    public static final class Builder {
        private final Identifier id;
        private final Supplier<? extends Block> machine;
        private int upgradeSlots = 2;
        private Drive drive = Drives.CRANK;
        private Predicate<MachineUpgrade> accepts = u -> true;

        private Builder(Identifier id, Supplier<? extends Block> machine) {
            this.id = id;
            this.machine = machine;
        }

        public Builder upgradeSlots(int n) {
            this.upgradeSlots = n;
            return this;
        }

        public Builder drive(Drive drive) {
            this.drive = drive;
            return this;
        }

        public Builder accepts(Predicate<MachineUpgrade> accepts) {
            this.accepts = accepts;
            return this;
        }

        public MachineCore build() {
            return new MachineCore(this);
        }
    }
}
