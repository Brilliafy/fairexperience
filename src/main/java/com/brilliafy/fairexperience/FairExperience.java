package com.brilliafy.fairexperience;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = FairExperience.MODID,
    name = FairExperience.NAME,
    version = FairExperience.VERSION,
    dependencies = "required-after:mixinbooter",
    acceptableRemoteVersions = "*"
)
public class FairExperience {

    public static final String MODID = "fairexperience";
    public static final String NAME = "Fair Experience";
    public static final String VERSION = "1.0.1";

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("Fair Experience initialized! Converting flat level costs into fair raw XP costs.");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
    }
}
