package com.workshopcraft.workshopcore.machine;

import com.workshopcraft.workshopcore.compat.ValueInput;
import com.workshopcraft.workshopcore.compat.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Plain energy store. Loaders expose it as FE (NeoForge, Forge) or Team Reborn Energy (Fabric). No cables. */
public class EnergyBuffer {
    @FunctionalInterface
    public interface Pusher {
        int push(ServerLevel level, BlockPos pos, Direction side, int amount);
    }

    @FunctionalInterface
    public interface Lookup {
        boolean has(Level level, BlockPos pos, Direction side);
    }

    /** Inserts into the energy store at pos, seen from side. Installed by the loader. */
    public static Pusher pusher = (level, pos, side, amount) -> 0;
    /** True if pos has an energy store on side. Installed by the loader. */
    public static Lookup lookup = (level, pos, side) -> false;

    public int capacity;
    public int maxIn;
    public int maxOut;
    public int energy;
    private final Runnable onChange;
    private int next;

    public EnergyBuffer(int capacity, int maxIn, int maxOut, Runnable onChange) {
        this.capacity = capacity;
        this.maxIn = maxIn;
        this.maxOut = maxOut;
        this.onChange = onChange == null ? () -> { } : onChange;
    }

    public void changed() {
        onChange.run();
    }

    public int receive(int amount, boolean simulate) {
        final int n = Math.max(0, Math.min(capacity - energy, Math.min(amount, maxIn)));
        if (n > 0 && !simulate) {
            energy += n;
            changed();
        }
        return n;
    }

    public int extract(int amount, boolean simulate) {
        final int n = Math.max(0, Math.min(energy, Math.min(amount, maxOut)));
        if (n > 0 && !simulate) {
            energy -= n;
            changed();
        }
        return n;
    }

    /** Uses energy internally, ignoring maxOut. Returns false and uses nothing if there is not enough. */
    public boolean use(int amount) {
        if (amount <= 0) return true;
        if (energy < amount) return false;
        energy -= amount;
        changed();
        return true;
    }

    public int pushTo(ServerLevel level, BlockPos pos, Direction side) {
        final int offer = Math.min(energy, maxOut);
        if (offer <= 0) return 0;
        final int n = pusher.push(level, pos.relative(side), side.getOpposite(), offer);
        if (n > 0) {
            energy -= Math.min(n, energy);
            changed();
        }
        return n;
    }

    /** Pushes to neighbours round robin, starting one side further each call. */
    public int pushToNeighbours(ServerLevel level, BlockPos pos) {
        int total = 0;
        final Direction[] dirs = Direction.values();
        for (int i = 0; i < dirs.length && energy > 0; i++) total += pushTo(level, pos, dirs[(next + i) % dirs.length]);
        next = (next + 1) % dirs.length;
        return total;
    }

    public int permille() {
        return capacity <= 0 ? 0 : (int) (1000L * energy / capacity);
    }

    public void load(ValueInput in, String key) {
        energy = Math.min(capacity, Math.max(0, in.getIntOr(key, 0)));
    }

    public void save(ValueOutput out, String key) {
        out.putInt(key, energy);
    }
}
