package com.workshopcraft.workshopcore.ctm;

import com.workshopcraft.workshopcore.Ids;
import com.workshopcraft.workshopcore.WorkshopCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Connected texture rules. Blocks opt in with client resource files under
 * assets/&lt;ns&gt;/workshopcore/connected_textures/; see the README for the format.
 */
public final class ConnectedTextures {
    public static final Identifier SAME_BLOCK = Ids.of(WorkshopCore.MOD_ID, "block");
    public static final Identifier SAME_STATE = Ids.of(WorkshopCore.MOD_ID, "state");

    private static final Map<Identifier, ConnectRule> RULES = new ConcurrentHashMap<>();

    static {
        RULES.put(SAME_BLOCK, (level, pos, self, otherPos, other, face) -> other.getBlock() == self.getBlock());
        RULES.put(SAME_STATE, (level, pos, self, otherPos, other, face) -> other == self);
    }

    private ConnectedTextures() {
    }

    /** Registers a rule that resource files can name in "connect". Safe to call from common init. */
    public static void registerRule(Identifier id, ConnectRule rule) {
        RULES.put(id, rule);
    }

    public static ConnectRule rule(Identifier id) {
        return RULES.get(id);
    }

    public static ConnectRule tag(TagKey<Block> tag) {
        return (level, pos, self, otherPos, other, face) -> other.is(tag);
    }

    /** "block", "state", "#ns:tag", or a registered rule id. Returns null if unknown. */
    public static ConnectRule parse(String spec) {
        if (spec == null || spec.isEmpty() || spec.equals("block")) {
            return RULES.get(SAME_BLOCK);
        }
        if (spec.equals("state")) {
            return RULES.get(SAME_STATE);
        }
        if (spec.startsWith("#")) {
            Identifier id = Identifier.tryParse(spec.substring(1));
            return id == null ? null : tag(TagKey.create(Registries.BLOCK, id));
        }
        Identifier id = Identifier.tryParse(spec);
        return id == null ? null : RULES.get(id);
    }
}
