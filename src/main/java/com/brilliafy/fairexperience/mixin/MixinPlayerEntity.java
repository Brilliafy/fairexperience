package com.brilliafy.fairexperience.mixin;

import com.brilliafy.fairexperience.FairExperienceConfig;
import com.brilliafy.fairexperience.FairExperienceContext;
import com.brilliafy.fairexperience.IFairExperiencePlayer;
import com.brilliafy.fairexperience.XpHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.EnchantmentContainer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity implements IFairExperiencePlayer {

    @Shadow
    protected int enchantmentSeed;

    @Unique
    private int fairExperience$lastLevel = -1;
    @Unique
    private int fairExperience$lastTotalXp = -1;

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
     * Intercepts the experience deduction performed by the enchanting table
     * ({@code EnchantmentContainer.clickMenuButton} calls this with the flat level cost).
     * Only the server side modifies XP state to avoid client packet desyncs.
     */
    @Inject(method = "onEnchantmentPerformed", at = @At("HEAD"), cancellable = true)
    private void fairExperience$onEnchantmentPerformed(ItemStack enchantedItem, int cost, CallbackInfo ci) {
        if (!FairExperienceConfig.enableFairEnchanting) {
            return;
        }

        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self.level.isClientSide || self.abilities.instabuild) {
            return;
        }

        ci.cancel();

        int reqLevel = FairExperienceContext.getEnchantReqLevel();
        if (reqLevel <= 0 && self.containerMenu instanceof EnchantmentContainer) {
            EnchantmentContainer container = (EnchantmentContainer) self.containerMenu;
            int slotId = cost - 1;
            if (container.costs != null && slotId >= 0 && slotId < container.costs.length) {
                reqLevel = container.costs[slotId];
            }
        }
        if (reqLevel <= 0) {
            if (cost == 1) {
                reqLevel = 8;
            } else if (cost == 2) {
                reqLevel = 15;
            } else if (cost == 3) {
                reqLevel = 30;
            } else {
                reqLevel = Math.max(30, cost * 10);
            }
        }

        int rawXpCost = XpHelper.getEnchantFairXpCost(reqLevel, cost);
        int currentTotalXp = XpHelper.getPlayerTotalXp(self);
        int newTotalXp = Math.max(0, currentTotalXp - rawXpCost);

        XpHelper.setPlayerTotalXp(self, newTotalXp);
        this.enchantmentSeed = self.getRandom().nextInt();

        this.fairExperience$lastLevel = self.experienceLevel;
        this.fairExperience$lastTotalXp = XpHelper.getPlayerTotalXp(self);
    }

    /**
     * Intercepts {@code giveExperienceLevels(levels)}. For negative values (level deductions
     * such as the anvil cost) the flat level cost is converted to a fair raw XP cost on the
     * server side.
     */
    @Inject(method = "giveExperienceLevels", at = @At("HEAD"), cancellable = true)
    private void fairExperience$giveExperienceLevels(int levels, CallbackInfo ci) {
        if (levels >= 0) {
            return; // Gains are handled normally by vanilla
        }

        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self.level.isClientSide || self.abilities.instabuild) {
            return;
        }

        int costLevels = -levels;
        int reqLevel = FairExperienceContext.getEnchantReqLevel();

        if (reqLevel > 0) {
            if (!FairExperienceConfig.enableFairEnchanting) {
                return;
            }
        } else if (!FairExperienceConfig.enableFairAnvils) {
            return;
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
     * Detects direct {@code experienceLevel} field modifications (e.g. {@code experienceLevel -= 3})
     * performed by other mods on the server side and converts them to fair raw XP costs.
     * Raw XP modifications that properly update {@code totalExperience} are respected and never
     * overwritten.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void fairExperience$onTick(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        if (self.level.isClientSide) {
            this.fairExperience$lastLevel = self.experienceLevel;
            this.fairExperience$lastTotalXp = XpHelper.getPlayerTotalXp(self);
            return;
        }

        if (!FairExperienceConfig.enableFairDirectModifications || self.isDeadOrDying()) {
            this.fairExperience$lastLevel = self.experienceLevel;
            this.fairExperience$lastTotalXp = XpHelper.getPlayerTotalXp(self);
            return;
        }

        if (FairExperienceContext.isProcessing()) {
            return;
        }

        int currentLevel = self.experienceLevel;
        int currentTotalXp = XpHelper.getPlayerTotalXp(self);

        if (this.fairExperience$lastLevel < 0) {
            this.fairExperience$lastLevel = currentLevel;
            this.fairExperience$lastTotalXp = currentTotalXp;
            return;
        }

        if (currentLevel < this.fairExperience$lastLevel) {
            // A raw-XP draining mod recalculates and stores totalExperience, so it stays in sync
            // with the recomputed total. A bare level decrease leaves totalExperience stale.
            boolean isRawXpModification = Math.abs(self.totalExperience - currentTotalXp) <= 2
                && Math.abs(self.totalExperience - this.fairExperience$lastTotalXp) > 2;

            if (isRawXpModification) {
                this.fairExperience$lastLevel = currentLevel;
                this.fairExperience$lastTotalXp = currentTotalXp;
                return;
            }

            FairExperienceContext.setProcessing(true);
            try {
                int levelDrop = this.fairExperience$lastLevel - currentLevel;
                int reqLevel = FairExperienceContext.getEnchantReqLevel();
                int rawXpCost;

                if (reqLevel > 0) {
                    rawXpCost = XpHelper.getEnchantFairXpCost(reqLevel, levelDrop);
                } else if (self.containerMenu instanceof EnchantmentContainer) {
                    EnchantmentContainer container = (EnchantmentContainer) self.containerMenu;
                    int slotId = levelDrop - 1;
                    if (container.costs != null && slotId >= 0 && slotId < container.costs.length && container.costs[slotId] > 0) {
                        rawXpCost = XpHelper.getEnchantFairXpCost(container.costs[slotId], levelDrop);
                    } else {
                        rawXpCost = XpHelper.getAnvilFairXpCost(levelDrop);
                    }
                } else {
                    rawXpCost = XpHelper.getAnvilFairXpCost(levelDrop);
                }

                int baseTotalXp = this.fairExperience$lastTotalXp > 0
                    ? this.fairExperience$lastTotalXp
                    : XpHelper.getRawXpForLevel(this.fairExperience$lastLevel);
                int newTotalXp = Math.max(0, baseTotalXp - rawXpCost);

                XpHelper.setPlayerTotalXp(self, newTotalXp);

                this.fairExperience$lastLevel = self.experienceLevel;
                this.fairExperience$lastTotalXp = XpHelper.getPlayerTotalXp(self);
            } finally {
                FairExperienceContext.setProcessing(false);
            }
        } else {
            this.fairExperience$lastLevel = currentLevel;
            this.fairExperience$lastTotalXp = currentTotalXp;
        }
    }
}
