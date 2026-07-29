package com.brilliafy.fairexperience;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class XpHelper {

    /**
     * Returns the experience points needed to level up from level to (level + 1).
     * Equivalent to EntityPlayer.xpBarCap().
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
     * Returns total accumulated raw experience points required to reach 'level' starting from Level 0.
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
    public static int getPlayerTotalXp(EntityPlayer player) {
        int levelXp = getRawXpForLevel(player.experienceLevel);
        int cap = getXpBarCap(player.experienceLevel);
        int progressXp = Math.round(player.experience * cap);
        return levelXp + progressXp;
    }

    /**
     * Sets the player's total raw XP, correctly updating experienceLevel, experience progress,
     * and experienceTotal. Also triggers server-to-client packet sync for EntityPlayerMP.
     */
    public static void setPlayerTotalXp(EntityPlayer player, int newTotalXp) {
        if (newTotalXp <= 0) {
            player.experienceLevel = 0;
            player.experience = 0.0F;
            player.experienceTotal = 0;
        } else {
            int level = getLevelForRawXp(newTotalXp);
            int baseLevelXp = getRawXpForLevel(level);
            int remainder = newTotalXp - baseLevelXp;

            player.experienceLevel = level;
            int cap = getXpBarCap(level);
            float progress = cap > 0 ? (float) remainder / (float) cap : 0.0F;
            if (progress >= 1.0F) {
                progress = 0.9999F;
            }
            player.experience = progress;
            player.experienceTotal = newTotalXp;
        }

        // Trigger network sync for server player
        if (player instanceof EntityPlayerMP) {
            try {
                // Setting lastExperience to -1 causes EntityPlayerMP's tick to send SPacketSetExperience
                java.lang.reflect.Field field = EntityPlayerMP.class.getDeclaredField("field_71144_ck"); // lastExperience SRG
                field.setAccessible(true);
                field.setInt(player, -1);
            } catch (Throwable ignored) {
                try {
                    java.lang.reflect.Field field = EntityPlayerMP.class.getDeclaredField("lastExperience");
                    field.setAccessible(true);
                    field.setInt(player, -1);
                } catch (Throwable ignored2) {
                }
            }
        }
    }

    /**
     * Finds the highest level L such that getRawXpForLevel(L) <= totalXp.
     */
    public static int getLevelForRawXp(int totalXp) {
        if (totalXp <= 0) return 0;

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
     * reqLevel: required level (e.g. 30 in vanilla, or higher in Apotheosis).
     * levelCost: level cost (e.g. 3 in vanilla).
     */
    public static int getEnchantFairXpCost(int reqLevel, int levelCost) {
        if (levelCost <= 0) return 0;
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
     * levelCost: flat level cost (e.g. 12 levels).
     */
    public static int getAnvilFairXpCost(int levelCost) {
        if (levelCost <= 0) return 0;
        int startXp = getRawXpForLevel(levelCost);
        return Math.max(1, startXp);
    }
}
