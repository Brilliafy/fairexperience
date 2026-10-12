package com.brilliafy.fairexperience.mixin;

import com.brilliafy.fairexperience.FairExperienceConfig;
import net.minecraft.client.gui.GuiRepair;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@SideOnly(Side.CLIENT)
@Mixin(GuiRepair.class)
public abstract class MixinGuiRepair {

    @ModifyConstant(
        method = "drawGuiContainerForegroundLayer",
        constant = @Constant(intValue = 40)
    )
    private int fairExperience$modifyMaxAnvilCost(int constant) {
        if (FairExperienceConfig.enableFairAnvilCombining) {
            return Integer.MAX_VALUE;
        }
        return constant;
    }
}
