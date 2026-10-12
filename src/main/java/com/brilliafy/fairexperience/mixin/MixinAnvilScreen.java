package com.brilliafy.fairexperience.mixin;

import com.brilliafy.fairexperience.FairExperienceConfig;
import net.minecraft.client.gui.screen.inventory.AnvilScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@OnlyIn(Dist.CLIENT)
@Mixin(AnvilScreen.class)
public abstract class MixinAnvilScreen {

    /**
     * Hides the client-side "Too Expensive!" message by raising the 40-level display cap.
     */
    @ModifyConstant(
        method = "renderLabels",
        constant = @Constant(intValue = 40)
    )
    private int fairExperience$modifyMaxAnvilCost(int constant) {
        if (FairExperienceConfig.removeTooExpensiveLimit) {
            return Integer.MAX_VALUE;
        }
        return constant;
    }
}
