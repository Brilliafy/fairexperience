package com.brilliafy.fairexperience.mixin;

import com.brilliafy.fairexperience.FairExperienceConfig;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.RepairContainer;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.IntReferenceHolder;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.ForgeHooks;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Mixin(RepairContainer.class)
public abstract class MixinRepairContainer {

    @Shadow
    protected IInventory inputSlots;

    @Shadow
    protected CraftResultInventory resultSlots;

    @Shadow
    protected PlayerEntity player;

    @Shadow
    public int repairItemCountCost;

    @Shadow
    private String itemName;

    @Shadow
    @Final
    private IntReferenceHolder cost;

    @Shadow
    public abstract void broadcastChanges();

    @Unique
    private static int fairExperience$getRarityMultiplier(Enchantment enchantment, boolean isBook) {
        if (enchantment == null || enchantment.getRarity() == null) {
            return 1;
        }
        int multiplier;
        switch (enchantment.getRarity()) {
            case UNCOMMON:
                multiplier = 2;
                break;
            case RARE:
                multiplier = 4;
                break;
            case VERY_RARE:
                multiplier = 8;
                break;
            case COMMON:
            default:
                multiplier = 1;
                break;
        }
        if (isBook) {
            multiplier = Math.max(1, multiplier / 2);
        }
        return multiplier;
    }

    /**
     * Calculates the potential function Phi(I) for a set of enchantments on an item:
     * <pre>Phi(I) = sum(multiplier * level) + (count * (count - 1)) / 2</pre>
     * The cost of transitioning an item from state A to state B is {@code Phi(B) - Phi(A)},
     * which guarantees path independence while still making each subsequent enchantment
     * more expensive as existing enchantments accumulate.
     */
    @Unique
    private static int fairExperience$calculateEnchantmentPotential(Map<Enchantment, Integer> enchants, boolean isBook) {
        if (enchants == null || enchants.isEmpty()) {
            return 0;
        }
        int baseSum = 0;
        int count = 0;
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment ench = entry.getKey();
            Integer lvlObj = entry.getValue();
            if (ench != null && lvlObj != null && lvlObj > 0) {
                int mult = fairExperience$getRarityMultiplier(ench, isBook);
                baseSum += mult * lvlObj;
                count++;
            }
        }
        int surcharge = count * (count - 1) / 2;
        return baseSum + surcharge;
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void fairExperience$createResult(CallbackInfo ci) {
        if (!FairExperienceConfig.enableFairAnvilCombining) {
            return;
        }

        ci.cancel();

        ItemStack itemstack = this.inputSlots.getItem(0);
        this.cost.set(1);
        int i = 0;
        int j = 0; // Prior work penalty is zeroed out in fair mode
        int k = 0;

        if (itemstack.isEmpty()) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.cost.set(0);
            return;
        }

        ItemStack itemstack1 = itemstack.copy();
        ItemStack itemstack2 = this.inputSlots.getItem(1);
        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(itemstack1);
        this.repairItemCountCost = 0;
        boolean flag = false;

        if (!itemstack2.isEmpty()) {
            if (!ForgeHooks.onAnvilChange((RepairContainer) (Object) this, itemstack, itemstack2, this.resultSlots, this.itemName, j, this.player)) {
                return;
            }

            flag = itemstack2.getItem() == Items.ENCHANTED_BOOK && !EnchantedBookItem.getEnchantments(itemstack2).isEmpty();

            // 1. Material repair (e.g. diamonds on a diamond sword)
            if (itemstack1.isDamageableItem() && itemstack1.getItem().isValidRepairItem(itemstack, itemstack2)) {
                int l2 = Math.min(itemstack1.getDamageValue(), itemstack1.getMaxDamage() / 4);
                if (l2 <= 0) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.cost.set(0);
                    return;
                }

                int i3;
                for (i3 = 0; l2 > 0 && i3 < itemstack2.getCount(); ++i3) {
                    int j3 = itemstack1.getDamageValue() - l2;
                    itemstack1.setDamageValue(j3);
                    ++i;
                    l2 = Math.min(itemstack1.getDamageValue(), itemstack1.getMaxDamage() / 4);
                }
                this.repairItemCountCost = i3;

                // Existing enchantments make repairs fairly more expensive: +1 level each.
                i += map.size();
            } else {
                if (!flag && (itemstack1.getItem() != itemstack2.getItem() || !itemstack1.isDamageableItem())) {
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    this.cost.set(0);
                    return;
                }

                // 2. Durability combine repair for two damageable items of the same type
                if (itemstack1.isDamageableItem() && !flag) {
                    int l = itemstack.getMaxDamage() - itemstack.getDamageValue();
                    int i1 = itemstack2.getMaxDamage() - itemstack2.getDamageValue();
                    int j1 = i1 + itemstack1.getMaxDamage() * 12 / 100;
                    int k1 = l + j1;
                    int l1 = itemstack1.getMaxDamage() - k1;
                    if (l1 < 0) {
                        l1 = 0;
                    }
                    if (l1 < itemstack1.getDamageValue()) {
                        itemstack1.setDamageValue(l1);
                        i += 2;
                        // Surcharge for existing enchantments on the repaired item
                        i += map.size();
                    }
                }

                Map<Enchantment, Integer> map1 = EnchantmentHelper.getEnchantments(itemstack2);
                boolean isBook1 = itemstack1.getItem() == Items.ENCHANTED_BOOK;
                boolean isBook2 = flag;

                if (isBook1 && isBook2) {
                    // Combining two enchanted books symmetrically and order-independently
                    Set<Enchantment> allEnchants = new LinkedHashSet<>(map.keySet());
                    allEnchants.addAll(map1.keySet());

                    boolean anyIncompatible = false;
                    boolean anyCompatible = false;
                    int upgradeCost = 0;
                    Map<Enchantment, Integer> resultMap = new LinkedHashMap<>();

                    for (Enchantment ench : allEnchants) {
                        if (ench == null) {
                            continue;
                        }

                        int lvl1 = map.getOrDefault(ench, 0);
                        int lvl2 = map1.getOrDefault(ench, 0);
                        int resLvl;

                        if (lvl1 == lvl2 && lvl1 > 0) {
                            resLvl = Math.min(lvl1 + 1, ench.getMaxLevel());
                        } else {
                            resLvl = Math.max(lvl1, lvl2);
                        }

                        boolean canApply = true;
                        for (Enchantment other : resultMap.keySet()) {
                            if (other != ench && !ench.isCompatibleWith(other)) {
                                canApply = false;
                                ++i;
                                break;
                            }
                        }

                        if (!canApply) {
                            anyIncompatible = true;
                        } else {
                            anyCompatible = true;
                            resultMap.put(ench, resLvl);

                            if (lvl1 == lvl2 && lvl1 > 0) {
                                int mult = fairExperience$getRarityMultiplier(ench, true);
                                upgradeCost += mult * (resLvl - lvl1);
                            }
                        }
                    }

                    if (anyIncompatible && !anyCompatible) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                        return;
                    }

                    if (upgradeCost > 0) {
                        i += upgradeCost;
                    } else if (anyCompatible) {
                        i += 1;
                    }

                    map = resultMap;

                    if (itemstack.getCount() > 1) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                        return;
                    }
                } else {
                    // 3. Applying a book or another item to a tool/weapon/armor
                    boolean flag2 = false;
                    boolean flag3 = false;
                    int oldPotential = fairExperience$calculateEnchantmentPotential(map, false);
                    Map<Enchantment, Integer> targetMap = new LinkedHashMap<>(map);

                    for (Enchantment enchantment1 : map1.keySet()) {
                        if (enchantment1 == null) {
                            continue;
                        }

                        int i2 = targetMap.getOrDefault(enchantment1, 0);
                        int j2 = map1.get(enchantment1);
                        j2 = (i2 == j2) ? j2 + 1 : Math.max(j2, i2);

                        boolean flag1 = enchantment1.canEnchant(itemstack);
                        if (this.player.abilities.instabuild || itemstack.getItem() == Items.ENCHANTED_BOOK) {
                            flag1 = true;
                        }

                        for (Enchantment enchantment : targetMap.keySet()) {
                            if (enchantment != enchantment1 && !enchantment1.isCompatibleWith(enchantment)) {
                                flag1 = false;
                                ++i;
                            }
                        }

                        if (!flag1) {
                            flag3 = true;
                        } else {
                            flag2 = true;
                            if (j2 > enchantment1.getMaxLevel()) {
                                j2 = enchantment1.getMaxLevel();
                            }
                            targetMap.put(enchantment1, j2);

                            if (itemstack.getCount() > 1) {
                                this.resultSlots.setItem(0, ItemStack.EMPTY);
                                this.cost.set(0);
                                return;
                            }
                        }
                    }

                    if (flag3 && !flag2) {
                        this.resultSlots.setItem(0, ItemStack.EMPTY);
                        this.cost.set(0);
                        return;
                    }

                    if (flag2) {
                        map = targetMap;
                        int newPotential = fairExperience$calculateEnchantmentPotential(map, false);
                        int enchantCost = Math.max(0, newPotential - oldPotential);
                        i += enchantCost;
                    }
                }
            }
        }

        if (StringUtils.isBlank(this.itemName)) {
            if (itemstack.hasCustomHoverName()) {
                k = 1;
                i += k;
                itemstack1.resetHoverName();
            }
        } else if (!this.itemName.equals(itemstack.getHoverName().getString())) {
            k = 1;
            i += k;
            itemstack1.setHoverName(new StringTextComponent(this.itemName));
        }

        if (flag && !itemstack1.isBookEnchantable(itemstack2)) {
            itemstack1 = ItemStack.EMPTY;
        }

        this.cost.set(i);

        if (i <= 0) {
            itemstack1 = ItemStack.EMPTY;
        }

        if (k == i && k > 0 && this.cost.get() >= 40) {
            this.cost.set(39);
        }

        if (!FairExperienceConfig.removeTooExpensiveLimit && this.cost.get() >= 40 && !this.player.abilities.instabuild) {
            itemstack1 = ItemStack.EMPTY;
        }

        if (!itemstack1.isEmpty()) {
            itemstack1.setRepairCost(0);
            if (itemstack1.hasTag()) {
                itemstack1.getTag().remove("RepairCost");

                if (itemstack1.getItem() == Items.ENCHANTED_BOOK) {
                    itemstack1.getTag().remove("StoredEnchantments");
                }
            }

            EnchantmentHelper.setEnchantments(map, itemstack1);
        }

        this.resultSlots.setItem(0, itemstack1);
        this.broadcastChanges();
    }

    /**
     * Patches the vanilla 40-level "Too Expensive!" cap when fair anvil combining is disabled
     * but the limit is still configured to be removed.
     */
    @ModifyConstant(
        method = "createResult",
        constant = @Constant(intValue = 40, ordinal = 2)
    )
    private int fairExperience$modifyMaxCostVanilla(int constant) {
        if (FairExperienceConfig.removeTooExpensiveLimit) {
            return Integer.MAX_VALUE;
        }
        return constant;
    }
}
