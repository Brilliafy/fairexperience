package com.brilliafy.fairexperience.mixin;

import com.brilliafy.fairexperience.FairExperienceConfig;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Mixin(ContainerRepair.class)
public abstract class MixinContainerRepair extends Container {

    @Shadow private IInventory outputSlot;
    @Shadow private IInventory inputSlots;
    @Shadow public int maximumCost;
    @Shadow public int materialCost;
    @Shadow private String repairedItemName;
    @Shadow private EntityPlayer player;

    @Unique
    private static int fairExperience$getRarityMultiplier(Enchantment enchantment, boolean isBook) {
        if (enchantment == null || enchantment.getRarity() == null) {
            return 1;
        }
        int multiplier = 1;
        switch (enchantment.getRarity()) {
            case COMMON:
                multiplier = 1;
                break;
            case UNCOMMON:
                multiplier = 2;
                break;
            case RARE:
                multiplier = 4;
                break;
            case VERY_RARE:
                multiplier = 8;
                break;
        }
        if (isBook) {
            multiplier = Math.max(1, multiplier / 2);
        }
        return multiplier;
    }

    @Inject(method = "updateRepairOutput", at = @At("HEAD"), cancellable = true)
    private void fairExperience$onUpdateRepairOutput(CallbackInfo ci) {
        if (!FairExperienceConfig.enableFairAnvilCombining) {
            return;
        }

        ci.cancel();

        ItemStack itemstack = this.inputSlots.getStackInSlot(0);
        this.maximumCost = 1;
        int i = 0;
        int j = 0; // Prior work penalty is completely zeroed out in fair mode
        int k = 0;

        if (itemstack.isEmpty()) {
            this.outputSlot.setInventorySlotContents(0, ItemStack.EMPTY);
            this.maximumCost = 0;
            return;
        }

        ItemStack itemstack1 = itemstack.copy();
        ItemStack itemstack2 = this.inputSlots.getStackInSlot(1);
        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(itemstack1);
        this.materialCost = 0;
        boolean flag = false;

        if (!itemstack2.isEmpty()) {
            if (!ForgeHooks.onAnvilChange((ContainerRepair) (Object) this, itemstack, itemstack2, outputSlot, repairedItemName, j)) {
                return;
            }

            flag = itemstack2.getItem() == Items.ENCHANTED_BOOK && !ItemEnchantedBook.getEnchantments(itemstack2).isEmpty();

            if (itemstack1.isItemStackDamageable() && itemstack1.getItem().getIsRepairable(itemstack, itemstack2)) {
                int l2 = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);
                if (l2 <= 0) {
                    this.outputSlot.setInventorySlotContents(0, ItemStack.EMPTY);
                    this.maximumCost = 0;
                    return;
                }

                int i3;
                for (i3 = 0; l2 > 0 && i3 < itemstack2.getCount(); ++i3) {
                    int j3 = itemstack1.getItemDamage() - l2;
                    itemstack1.setItemDamage(j3);
                    ++i;
                    l2 = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);
                }
                this.materialCost = i3;
            } else {
                if (!flag && (itemstack1.getItem() != itemstack2.getItem() || !itemstack1.isItemStackDamageable())) {
                    this.outputSlot.setInventorySlotContents(0, ItemStack.EMPTY);
                    this.maximumCost = 0;
                    return;
                }

                if (itemstack1.isItemStackDamageable() && !flag) {
                    int l = itemstack.getMaxDamage() - itemstack.getItemDamage();
                    int i1 = itemstack2.getMaxDamage() - itemstack2.getItemDamage();
                    int j1 = i1 + itemstack1.getMaxDamage() * 12 / 100;
                    int k1 = l + j1;
                    int l1 = itemstack1.getMaxDamage() - k1;
                    if (l1 < 0) {
                        l1 = 0;
                    }
                    if (l1 < itemstack1.getItemDamage()) {
                        itemstack1.setItemDamage(l1);
                        i += 2;
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
                        if (ench == null) continue;

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
                        this.outputSlot.setInventorySlotContents(0, ItemStack.EMPTY);
                        this.maximumCost = 0;
                        return;
                    }

                    if (upgradeCost > 0) {
                        i += upgradeCost;
                    } else if (anyCompatible) {
                        i += 1;
                    }

                    map = resultMap;

                    if (itemstack.getCount() > 1) {
                        this.outputSlot.setInventorySlotContents(0, ItemStack.EMPTY);
                        this.maximumCost = 0;
                        return;
                    }
                } else {
                    // Applying a book or another item to a tool/weapon/armor
                    boolean flag2 = false;
                    boolean flag3 = false;

                    for (Enchantment enchantment1 : map1.keySet()) {
                        if (enchantment1 == null) continue;

                        int i2 = map.getOrDefault(enchantment1, 0);
                        int j2 = map1.get(enchantment1);
                        j2 = (i2 == j2) ? j2 + 1 : Math.max(j2, i2);

                        boolean flag1 = enchantment1.canApply(itemstack);
                        if (this.player.capabilities.isCreativeMode || itemstack.getItem() == Items.ENCHANTED_BOOK) {
                            flag1 = true;
                        }

                        for (Enchantment enchantment : map.keySet()) {
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
                            map.put(enchantment1, j2);

                            int k3 = fairExperience$getRarityMultiplier(enchantment1, isBook2);
                            int levelDiff = j2 - i2;
                            if (levelDiff > 0) {
                                i += k3 * levelDiff;
                            }

                            if (itemstack.getCount() > 1) {
                                this.outputSlot.setInventorySlotContents(0, ItemStack.EMPTY);
                                this.maximumCost = 0;
                                return;
                            }
                        }
                    }

                    if (flag3 && !flag2) {
                        this.outputSlot.setInventorySlotContents(0, ItemStack.EMPTY);
                        this.maximumCost = 0;
                        return;
                    }
                }
            }
        }

        if (StringUtils.isBlank(this.repairedItemName)) {
            if (itemstack.hasDisplayName()) {
                k = 1;
                i += k;
                itemstack1.clearCustomName();
            }
        } else if (!this.repairedItemName.equals(itemstack.getDisplayName())) {
            k = 1;
            i += k;
            itemstack1.setStackDisplayName(this.repairedItemName);
        }

        if (flag && !itemstack1.getItem().isBookEnchantable(itemstack1, itemstack2)) {
            itemstack1 = ItemStack.EMPTY;
        }

        this.maximumCost = i;

        if (i <= 0) {
            itemstack1 = ItemStack.EMPTY;
        }

        if (k == i && k > 0 && this.maximumCost >= 40) {
            this.maximumCost = 39;
        }

        if (!itemstack1.isEmpty()) {
            itemstack1.setRepairCost(0);
            if (itemstack1.hasTagCompound()) {
                itemstack1.getTagCompound().removeTag("RepairCost");
            }

            if (itemstack1.getItem() == Items.ENCHANTED_BOOK && itemstack1.hasTagCompound()) {
                itemstack1.getTagCompound().removeTag("StoredEnchantments");
            }

            EnchantmentHelper.setEnchantments(map, itemstack1);
        }

        this.outputSlot.setInventorySlotContents(0, itemstack1);
        this.detectAndSendChanges();
    }
}
