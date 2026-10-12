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
    public static final String VERSION = "1.0.3";

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        logger.info("Fair Experience initialized! Converting flat level costs into fair raw XP costs.");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
    }

    @Mod.EventBusSubscriber(modid = FairExperience.MODID)
    public static class EventHandler {
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public static void onConfigChanged(net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent event) {
            if (FairExperience.MODID.equals(event.getModID())) {
                net.minecraftforge.common.config.ConfigManager.sync(FairExperience.MODID, net.minecraftforge.common.config.Config.Type.INSTANCE);
            }
        }
    }
}
