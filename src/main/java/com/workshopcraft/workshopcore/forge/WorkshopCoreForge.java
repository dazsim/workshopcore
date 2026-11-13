package com.workshopcraft.workshopcore.forge;

import com.workshopcraft.workshopcore.WorkshopCore;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.LoggerFactory;

@Mod(WorkshopCore.MOD_ID)
public class WorkshopCoreForge {
    public WorkshopCoreForge() {
        LoggerFactory.getLogger(WorkshopCore.MOD_ID).info("Workshop Core {}", WorkshopCore.API_VERSION);
    }
}
