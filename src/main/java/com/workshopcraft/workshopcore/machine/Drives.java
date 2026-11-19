package com.workshopcraft.workshopcore.machine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Drives {
    /** Ticks of running each crank adds, and the most it stores. */
    public static int CRANK_TICKS = 20;
    public static int CRANK_MAX = 100;

    /** Player cranks the machine with an empty hand. */
    public static final Drive CRANK = new Drive() {
        @Override
        public boolean run(WorkBlockEntity m, ServerLevel level, int cost) {
            if (m.driveTicks <= 0) return false;
            m.driveTicks--;
            return true;
        }

        @Override
        public boolean crank(WorkBlockEntity m, Player player) {
            if (m.driveTicks + CRANK_TICKS > CRANK_MAX) return false;
            m.driveTicks += CRANK_TICKS;
            m.driveTotal = CRANK_MAX;
            player.causeFoodExhaustion(0.05f);
            m.getLevel().playSound(null, m.getBlockPos(), SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, 0.8f + 0.4f * m.getLevel().getRandom().nextFloat());
            m.setChanged();
            return true;
        }

        @Override
        public int level(WorkBlockEntity m) {
            return Math.min(1000, m.driveTicks * 1000 / CRANK_MAX);
        }
    };

    /** Burns furnace fuel from the machine's fuel slot, only while there is work. */
    public static final Drive FUEL = new Drive() {
        @Override
        public boolean run(WorkBlockEntity m, ServerLevel level, int cost) {
            if (m.driveTicks <= 0) {
                final int slot = m.fuelSlot();
                if (slot < 0) return false;
                final ItemStack fuel = m.getItem(slot);
                final int burn = fuel.isEmpty() ? 0 : Fuels.burnTime(level, fuel, m);
                if (burn <= 0) return false;
                m.setItem(slot, Fuels.consume(fuel));
                m.driveTicks = burn;
                m.driveTotal = burn;
            }
            m.driveTicks--;
            return true;
        }

        @Override
        public int level(WorkBlockEntity m) {
            return m.driveTotal <= 0 ? 0 : Math.min(1000, m.driveTicks * 1000 / m.driveTotal);
        }
    };

    /** Uses energyCost from the machine's EnergyBuffer each tick. */
    public static final Drive ENERGY = new Drive() {
        @Override
        public boolean run(WorkBlockEntity m, ServerLevel level, int cost) {
            return m.energy().use(cost);
        }

        @Override
        public int level(WorkBlockEntity m) {
            return m.energy().permille();
        }

        @Override
        public boolean usesEnergy() {
            return true;
        }
    };

    private Drives() {
    }
}
