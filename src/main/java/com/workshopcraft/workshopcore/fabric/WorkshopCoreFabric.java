package com.workshopcraft.workshopcore.fabric;

import com.workshopcraft.workshopcore.WorkshopCore;
import net.fabricmc.api.ModInitializer;
import org.slf4j.LoggerFactory;

public class WorkshopCoreFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        LoggerFactory.getLogger(WorkshopCore.MOD_ID).info("Workshop Core {}", WorkshopCore.API_VERSION);
    }
}
