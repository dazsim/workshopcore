package com.workshopcraft.workshopcore.machine;

import net.minecraft.resources.Identifier;

/** A part fitted after the control board. Register with {@link Assembly#registerUpgrade}. */
public final class MachineUpgrade {
    private final Identifier id;
    private final int speed;
    private final int storage;
    private final Drive drive;
    private final boolean unique;

    private MachineUpgrade(Builder b) {
        this.id = b.id;
        this.speed = b.speed;
        this.storage = b.storage;
        this.drive = b.drive;
        this.unique = b.unique;
    }

    public static Builder builder(Identifier id) {
        return new Builder(id);
    }

    public Identifier id() {
        return id;
    }

    /** Extra work speed, in percent. */
    public int speed() {
        return speed;
    }

    /** Extra energy capacity, in percent. */
    public int storage() {
        return storage;
    }

    /** The drive this upgrade fits, or null. A machine takes one drive upgrade. */
    public Drive drive() {
        return drive;
    }

    /** Only one of this upgrade per machine. Drive upgrades are always unique. */
    public boolean unique() {
        return unique || drive != null;
    }

    public static final class Builder {
        private final Identifier id;
        private int speed;
        private int storage;
        private Drive drive;
        private boolean unique;

        private Builder(Identifier id) {
            this.id = id;
        }

        public Builder speed(int percent) {
            this.speed = percent;
            return this;
        }

        public Builder storage(int percent) {
            this.storage = percent;
            return this;
        }

        public Builder drive(Drive drive) {
            this.drive = drive;
            return this;
        }

        public Builder unique() {
            this.unique = true;
            return this;
        }

        public MachineUpgrade build() {
            return new MachineUpgrade(this);
        }
    }
}
