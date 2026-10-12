package com.brilliafy.fairexperience;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Config for Fair Experience on 1.16.5 Forge.
 *
 * <p>The boolean fields are plain statics so the mixins can read them cheaply;
 * they are re-baked from the {@link ForgeConfigSpec} whenever the config is
 * loaded or reloaded.</p>
 */
@Mod.EventBusSubscriber(modid = FairExperience.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FairExperienceConfig {

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLE_FAIR_ENCHANTING;
    private static final ForgeConfigSpec.BooleanValue ENABLE_FAIR_ANVILS;
    private static final ForgeConfigSpec.BooleanValue ENABLE_FAIR_ANVIL_COMBINING;
    private static final ForgeConfigSpec.BooleanValue REMOVE_TOO_EXPENSIVE_LIMIT;
    private static final ForgeConfigSpec.BooleanValue ENABLE_FAIR_DIRECT_MODIFICATIONS;
    private static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;

    public static boolean enableFairEnchanting = true;
    public static boolean enableFairAnvils = true;
    public static boolean enableFairAnvilCombining = true;
    public static boolean removeTooExpensiveLimit = true;
    public static boolean enableFairDirectModifications = true;
    public static boolean debugLogging = false;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Configuration for Fair Experience").push("general");

        ENABLE_FAIR_ENCHANTING = builder
            .comment("If true, enchanting table level costs will be converted to fair raw XP costs.")
            .define("enableFairEnchanting", true);

        ENABLE_FAIR_ANVILS = builder
            .comment("If true, anvil level costs will be converted to fair raw XP costs.")
            .define("enableFairAnvils", true);

        ENABLE_FAIR_ANVIL_COMBINING = builder
            .comment("If true, anvil enchanting calculations are fair and order-independent: combining books or applying them in any order costs the same, prior work penalties do not penalize combines, and combining books to reach higher levels will not yield higher costs to combine with items.")
            .define("enableFairAnvilCombining", true);

        REMOVE_TOO_EXPENSIVE_LIMIT = builder
            .comment("If true, removes the vanilla 40-level 'Too Expensive!' anvil limit, allowing operations that cost 40 or more levels. If false, operations costing 40+ levels are blocked like in vanilla.")
            .define("removeTooExpensiveLimit", true);

        ENABLE_FAIR_DIRECT_MODIFICATIONS = builder
            .comment("If true, direct experienceLevel subtractions (e.g. by other mods) will be converted to fair raw XP costs.")
            .define("enableFairDirectModifications", true);

        DEBUG_LOGGING = builder
            .comment("Enable detailed logging for experience calculations.")
            .define("debugLogging", false);

        builder.pop();
        SPEC = builder.build();
    }

    private FairExperienceConfig() {
    }

    public static void bake() {
        enableFairEnchanting = ENABLE_FAIR_ENCHANTING.get();
        enableFairAnvils = ENABLE_FAIR_ANVILS.get();
        enableFairAnvilCombining = ENABLE_FAIR_ANVIL_COMBINING.get();
        removeTooExpensiveLimit = REMOVE_TOO_EXPENSIVE_LIMIT.get();
        enableFairDirectModifications = ENABLE_FAIR_DIRECT_MODIFICATIONS.get();
        debugLogging = DEBUG_LOGGING.get();
    }

    @SubscribeEvent
    public static void onLoad(ModConfig.Loading event) {
        if (FairExperience.MOD_ID.equals(event.getConfig().getModId())) {
            bake();
        }
    }

    @SubscribeEvent
    public static void onReload(ModConfig.Reloading event) {
        if (FairExperience.MOD_ID.equals(event.getConfig().getModId())) {
            bake();
        }
    }
}
