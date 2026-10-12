package com.brilliafy.fairexperience;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(FairExperience.MOD_ID)
public class FairExperience {

    public static final String MOD_ID = "fairexperience";
    public static final String NAME = "Fair Experience";
    public static final String VERSION = "1.1.0";

    public static final Logger LOGGER = LogManager.getLogger();

    public FairExperience() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FairExperienceConfig.SPEC);
        LOGGER.info("Fair Experience initialized! Converting flat level costs into fair raw XP costs.");
    }
}
