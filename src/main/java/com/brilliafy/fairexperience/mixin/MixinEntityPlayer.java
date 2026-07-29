package com.brilliafy.fairexperience.mixin;

import com.brilliafy.fairexperience.FairExperienceConfig;
import com.brilliafy.fairexperience.FairExperienceContext;
import com.brilliafy.fairexperience.IFairExperiencePlayer;
import com.brilliafy.fairexperience.XpHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer implements IFairExperiencePlayer {

    @Shadow
    protected int xpSeed;

    @Unique private int fairExperience$lastLevel = -1;
    @Unique private int fairExperience$lastTotalXp = -1;

    @Override
    public int fairExperience$getLastLevel() {
        return this.fairExperience$lastLevel;
    }

    @Override
    public void fairExperience$setLastLevel(int level) {
        this.fairExperience$lastLevel = level;
    }

    @Override
    public int fairExperience$getLastTotalXp() {
        return this.fairExperience$lastTotalXp;
    }

    @Override
    public void fairExperience$setLastTotalXp(int totalXp) {
        this.fairExperience$lastTotalXp = totalXp;
    }

    /**
     * Intercepts player.onEnchant(stack, cost) called by Enchanting Table container or mods.
     * Must only modify XP state on the server side to avoid client packet desync/double-dipping.
     */
    @Inject(method = "onEnchant", at = @At("HEAD"), cancellable = true)
    private void fairExperience$onEnchant(ItemStack enchantedItem, int cost, CallbackInfo ci) {
        if (!FairExperienceConfig.enableFairEnchanting) {
            return;
        }

        EntityPlayer self = (EntityPlayer) (Object) this;
        if (self.world.isRemote || self.capabilities.isCreativeMode) {
            return;
        }

        ci.cancel();

        int reqLevel = FairExperienceContext.getEnchantReqLevel();
        if (reqLevel <= 0) {
            if (self.openContainer instanceof ContainerEnchantment) {
                ContainerEnchantment container = (ContainerEnchantment) self.openContainer;
                int slotId = cost - 1;
                if (container.enchantLevels != null && slotId >= 0 && slotId < container.enchantLevels.length) {
                    reqLevel = container.enchantLevels[slotId];
                }
            }
        }
        if (reqLevel <= 0) {
            reqLevel = Math.max(cost, self.experienceLevel);
        }

        int rawXpCost = XpHelper.getEnchantFairXpCost(reqLevel, cost);
        int currentTotalXp = XpHelper.getPlayerTotalXp(self);
        int newTotalXp = Math.max(0, currentTotalXp - rawXpCost);

        XpHelper.setPlayerTotalXp(self, newTotalXp);
        this.xpSeed = self.getRNG().nextInt();

        this.fairExperience$lastLevel = self.experienceLevel;
        this.fairExperience$lastTotalXp = XpHelper.getPlayerTotalXp(self);
    }

    /**
     * Intercepts player.addExperienceLevel(levels).
     * For negative values (level deductions), applies fair raw XP cost on server.
     */
    @Inject(method = "addExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void fairExperience$addExperienceLevel(int levels, CallbackInfo ci) {
        if (levels >= 0) {
            return; // Gains are handled normally by vanilla
        }

        EntityPlayer self = (EntityPlayer) (Object) this;
        if (self.world.isRemote || self.capabilities.isCreativeMode) {
            return;
        }

        int costLevels = -levels;
        int reqLevel = FairExperienceContext.getEnchantReqLevel();

        if (reqLevel > 0) {
            if (!FairExperienceConfig.enableFairEnchanting) return;
        } else {
            if (!FairExperienceConfig.enableFairAnvils) return;
        }

        ci.cancel();

        int rawXpCost;
        if (reqLevel > 0) {
            rawXpCost = XpHelper.getEnchantFairXpCost(reqLevel, costLevels);
        } else {
            rawXpCost = XpHelper.getAnvilFairXpCost(costLevels);
        }

        int currentTotalXp = XpHelper.getPlayerTotalXp(self);
        int newTotalXp = Math.max(0, currentTotalXp - rawXpCost);

        XpHelper.setPlayerTotalXp(self, newTotalXp);

        this.fairExperience$lastLevel = self.experienceLevel;
        this.fairExperience$lastTotalXp = XpHelper.getPlayerTotalXp(self);
    }

    /**
     * Intercepts player tick to detect direct experienceLevel field modifications
     * (e.g. experienceLevel -= 3) by mods like LevelUp2 on the server side.
     */
    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void fairExperience$onUpdate(CallbackInfo ci) {
        EntityPlayer self = (EntityPlayer) (Object) this;

        // On client side, simply track current level without modifying experience state to prevent double-dipping/flickering
        if (self.world.isRemote) {
            this.fairExperience$lastLevel = self.experienceLevel;
            this.fairExperience$lastTotalXp = XpHelper.getPlayerTotalXp(self);
            return;
        }

        if (!FairExperienceConfig.enableFairDirectModifications || self.isDead || self.getHealth() <= 0) {
            return;
        }

        if (FairExperienceContext.isProcessing()) {
            return;
        }

        int currentLevel = self.experienceLevel;

        if (this.fairExperience$lastLevel < 0) {
            this.fairExperience$lastLevel = currentLevel;
            this.fairExperience$lastTotalXp = XpHelper.getPlayerTotalXp(self);
            return;
        }

        if (currentLevel < this.fairExperience$lastLevel) {
            FairExperienceContext.setProcessing(true);
            try {
                int levelDrop = this.fairExperience$lastLevel - currentLevel;
                int reqLevel = FairExperienceContext.getEnchantReqLevel();
                int rawXpCost;

                if (reqLevel > 0) {
                    rawXpCost = XpHelper.getEnchantFairXpCost(reqLevel, levelDrop);
                } else if (self.openContainer instanceof ContainerEnchantment) {
                    ContainerEnchantment container = (ContainerEnchantment) self.openContainer;
                    int slotId = levelDrop - 1;
                    if (container.enchantLevels != null && slotId >= 0 && slotId < container.enchantLevels.length && container.enchantLevels[slotId] > 0) {
                        rawXpCost = XpHelper.getEnchantFairXpCost(container.enchantLevels[slotId], levelDrop);
                    } else {
                        rawXpCost = XpHelper.getAnvilFairXpCost(levelDrop);
                    }
                } else {
                    rawXpCost = XpHelper.getAnvilFairXpCost(levelDrop);
                }

                int baseTotalXp = this.fairExperience$lastTotalXp > 0 ? this.fairExperience$lastTotalXp : XpHelper.getRawXpForLevel(this.fairExperience$lastLevel);
                int newTotalXp = Math.max(0, baseTotalXp - rawXpCost);

                XpHelper.setPlayerTotalXp(self, newTotalXp);

                this.fairExperience$lastLevel = self.experienceLevel;
                this.fairExperience$lastTotalXp = XpHelper.getPlayerTotalXp(self);
            } finally {
                FairExperienceContext.setProcessing(false);
            }
        } else {
            this.fairExperience$lastLevel = self.experienceLevel;
            this.fairExperience$lastTotalXp = XpHelper.getPlayerTotalXp(self);
        }
    }
}
