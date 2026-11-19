package com.workshopcraft.workshopcore.machine;

import net.minecraft.resources.Identifier;

/** A chassis grade. speed and storage are percentages applied to work speed and energy capacity. */
public record ChassisTier(Identifier id, int speed, int storage) {
    public static final ChassisTier BASIC = new ChassisTier(com.workshopcraft.workshopcore.Ids.of("workshopcore", "basic"), 100, 100);
}
