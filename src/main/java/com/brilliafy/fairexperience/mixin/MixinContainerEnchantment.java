package com.brilliafy.fairexperience.mixin;

import com.brilliafy.fairexperience.FairExperienceContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ContainerEnchantment.class)
public abstract class MixinContainerEnchantment {

    @Shadow
    public int[] enchantLevels;

    @Inject(method = "enchantItem", at = @At("HEAD"))
    private void fairExperience$beforeEnchantItem(EntityPlayer playerIn, int id, CallbackInfoReturnable<Boolean> cir) {
        if (this.enchantLevels != null && id >= 0 && id < this.enchantLevels.length) {
            FairExperienceContext.setEnchantReqLevel(this.enchantLevels[id]);
        }
    }

    @Inject(method = "enchantItem", at = @At("RETURN"))
    private void fairExperience$afterEnchantItem(EntityPlayer playerIn, int id, CallbackInfoReturnable<Boolean> cir) {
        FairExperienceContext.clearEnchantReqLevel();
    }
}
