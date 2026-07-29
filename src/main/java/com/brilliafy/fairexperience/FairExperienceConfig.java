package com.brilliafy.fairexperience;

import net.minecraftforge.common.config.Config;

@Config(modid = FairExperience.MODID, name = FairExperience.MODID)
public class FairExperienceConfig {

    @Config.Comment("Enable fair experience calculation for enchanting tables.")
    public static boolean enableFairEnchanting = true;

    @Config.Comment("Enable fair experience calculation for anvils.")
    public static boolean enableFairAnvils = true;

    @Config.Comment("Enable detection and fair recalculation of direct experience level modifications by other mods.")
    public static boolean enableFairDirectModifications = true;

    @Config.Comment("Enable detailed logging for experience calculations.")
    public static boolean debugLogging = false;
}
