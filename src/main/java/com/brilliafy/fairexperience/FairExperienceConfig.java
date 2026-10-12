package com.brilliafy.fairexperience;

import net.minecraftforge.common.config.Config;

@Config(modid = FairExperience.MODID, name = FairExperience.MODID)
public class FairExperienceConfig {

    @Config.Comment("Enable fair experience calculation for enchanting tables.")
    public static boolean enableFairEnchanting = true;

    @Config.Comment("Enable fair experience calculation for anvils.")
    public static boolean enableFairAnvils = true;

    @Config.Comment("If enabled, anvil enchanting calculations are fair and order-independent: combining books or applying them in any order costs the same, prior work penalties do not penalize combines, and combining books to reach higher levels will not yield higher costs to combine with items.")
    public static boolean enableFairAnvilCombining = true;

    @Config.Comment("If true, removes the vanilla 40-level 'Too Expensive!' anvil limit, allowing operations that cost 40 or more levels. If false, operations costing 40+ levels are blocked like in vanilla.")
    public static boolean removeTooExpensiveLimit = true;

    @Config.Comment("Enable detection and fair recalculation of direct experience level modifications by other mods.")
    public static boolean enableFairDirectModifications = true;

    @Config.Comment("Enable detailed logging for experience calculations.")
    public static boolean debugLogging = false;
}
