package com.brilliafy.fairexperience;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Pure maths for converting flat level costs into fair raw XP costs.
 * The level-up curve is identical for all Minecraft versions up to 1.20.1.
 */
public final class XpHelper {

    private XpHelper() {
    }

    /**
     * Returns the experience points needed to level up from {@code level} to {@code level + 1}.
     */
    public static int getXpBarCap(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        } else if (level >= 15) {
            return 37 + (level - 15) * 5;
        } else {
            return 7 + level * 2;
        }
    }

    /**
     * Returns total accumulated raw experience points required to reach {@code level} starting from level 0.
     */
    public static int getRawXpForLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    /**
     * Calculates the player's exact current total raw XP (level + progress).
     */
    public static int getPlayerTotalXp(PlayerEntity player) {
        int levelXp = getRawXpForLevel(player.experienceLevel);
        int cap = getXpBarCap(player.experienceLevel);
        int progressXp = Math.round(player.experienceProgress * cap);
        return levelXp + progressXp;
    }

    /**
     * Sets the player's total raw XP, correctly updating experienceLevel, experience progress
     * and totalExperience. Server-side syncing to the client is handled by the vanilla
     * {@code ServerPlayerEntity} tick, which re-sends the experience packet whenever
     * {@code totalExperience} differs from the last value it sent.
     */
    public static void setPlayerTotalXp(PlayerEntity player, int newTotalXp) {
        if (newTotalXp <= 0) {
            player.experienceLevel = 0;
            player.experienceProgress = 0.0F;
            player.totalExperience = 0;
            return;
        }

        int level = getLevelForRawXp(newTotalXp);
        int baseLevelXp = getRawXpForLevel(level);
        int remainder = newTotalXp - baseLevelXp;

        player.experienceLevel = level;
        int cap = getXpBarCap(level);
        float progress = cap > 0 ? (float) remainder / (float) cap : 0.0F;
        if (progress >= 1.0F) {
            progress = 0.9999F;
        }
        player.experienceProgress = progress;
        player.totalExperience = newTotalXp;
    }

    /**
     * Finds the highest level L such that {@code getRawXpForLevel(L) <= totalXp}.
     */
    public static int getLevelForRawXp(int totalXp) {
        if (totalXp <= 0) {
            return 0;
        }

        int low = 0;
        int high = 20000;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (getRawXpForLevel(mid) <= totalXp) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    /**
     * Calculates the fair raw XP cost for an Enchanting operation.
     *
     * @param reqLevel  required level (e.g. 30 in vanilla, or higher in Apotheosis)
     * @param levelCost level cost (e.g. 3 in vanilla)
     */
    public static int getEnchantFairXpCost(int reqLevel, int levelCost) {
        if (levelCost <= 0) {
            return 0;
        }
        if (reqLevel < levelCost) {
            reqLevel = levelCost;
        }
        int startXp = getRawXpForLevel(reqLevel);
        int targetLevel = Math.max(0, reqLevel - levelCost);
        int endXp = getRawXpForLevel(targetLevel);
        return Math.max(1, startXp - endXp);
    }

    /**
     * Calculates the fair raw XP cost for an Anvil or general level deduction.
     *
     * @param levelCost flat level cost (e.g. 12 levels)
     */
    public static int getAnvilFairXpCost(int levelCost) {
        if (levelCost <= 0) {
            return 0;
        }
        int startXp = getRawXpForLevel(levelCost);
        return Math.max(1, startXp);
    }
}
