package com.brilliafy.fairexperience.mixin;

import com.brilliafy.fairexperience.FairExperienceContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.EnchantmentContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentContainer.class)
public abstract class MixinEnchantmentContainer {

    @Shadow
    public int[] costs;

    @Inject(method = "clickMenuButton", at = @At("HEAD"))
    private void fairExperience$beforeClickMenuButton(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (this.costs != null && id >= 0 && id < this.costs.length) {
            FairExperienceContext.setEnchantReqLevel(this.costs[id]);
        }
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void fairExperience$afterClickMenuButton(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        FairExperienceContext.clearEnchantReqLevel();
    }
}
