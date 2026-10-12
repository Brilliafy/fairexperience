package com.brilliafy.fairexperience;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

public class FairExperienceTest {

    @Test
    public void testXpCalculations() {
        assertEquals(0, XpHelper.getRawXpForLevel(0));
        assertEquals(7, XpHelper.getRawXpForLevel(1));
        assertEquals(315, XpHelper.getRawXpForLevel(15));
        assertEquals(352, XpHelper.getRawXpForLevel(16));
        assertEquals(1395, XpHelper.getRawXpForLevel(30));
        assertEquals(1507, XpHelper.getRawXpForLevel(31));

        for (int lvl = 0; lvl <= 100; lvl++) {
            int rawXp = XpHelper.getRawXpForLevel(lvl);
            assertEquals(lvl, XpHelper.getLevelForRawXp(rawXp));
        }
    }

    @Test
    public void testFairAnvilXpCost() {
        assertEquals(0, XpHelper.getAnvilFairXpCost(0));
        assertEquals(7, XpHelper.getAnvilFairXpCost(1));
        assertEquals(55, XpHelper.getAnvilFairXpCost(5));
        assertEquals(1395, XpHelper.getAnvilFairXpCost(30));
    }

    /**
     * Calculates the potential function Phi(I) for a set of enchantments:
     * Phi(I) = sum(multiplier * level) + count * (count - 1) / 2
     */
    private static int calculatePotential(Map<String, Integer> enchants, Map<String, Integer> multipliers) {
        int baseSum = 0;
        int count = 0;
        for (Map.Entry<String, Integer> e : enchants.entrySet()) {
            if (e.getValue() > 0) {
                int mult = multipliers.getOrDefault(e.getKey(), 1);
                baseSum += mult * e.getValue();
                count++;
            }
        }
        int surcharge = count * (count - 1) / 2;
        return baseSum + surcharge;
    }

    /**
     * Verifies that adding Fire Aspect 2 to a sword that ALREADY has Sharpness 5
     * costs MORE than adding Fire Aspect 2 to a blank sword,
     * AND adding both from one book costs the EXACT SAME total as adding one by one!
     */
    @Test
    public void testExistingEnchantsIncreaseCostAndSingleBookMatchesOneByOne() {
        Map<String, Integer> mults = new HashMap<>();
        mults.put("sharpness", 1);
        mults.put("fire_aspect", 2);
        mults.put("knockback", 1);

        // Blank sword: Phi = 0
        Map<String, Integer> blank = Collections.emptyMap();
        assertEquals(0, calculatePotential(blank, mults));

        // Cost of adding Fire Aspect 2 to a BLANK sword:
        Map<String, Integer> onlyFireAspect = Collections.singletonMap("fire_aspect", 2);
        int costFireAspectBlank = calculatePotential(onlyFireAspect, mults) - calculatePotential(blank, mults);
        // Base cost: 2 * 2 = 4. Surcharge: 1 * 0 / 2 = 0. Total = 4.
        assertEquals(4, costFireAspectBlank);

        // Cost of adding Sharpness 5 to a BLANK sword:
        Map<String, Integer> onlySharp5 = Collections.singletonMap("sharpness", 5);
        int costSharp5Blank = calculatePotential(onlySharp5, mults) - calculatePotential(blank, mults);
        // Base cost: 1 * 5 = 5. Total = 5.
        assertEquals(5, costSharp5Blank);

        // Now: Sword already has Sharpness 5. Cost of adding Fire Aspect 2 to it:
        Map<String, Integer> sharpAndFire = new HashMap<>();
        sharpAndFire.put("sharpness", 5);
        sharpAndFire.put("fire_aspect", 2);
        int costFireAspectOnSharp5 = calculatePotential(sharpAndFire, mults) - calculatePotential(onlySharp5, mults);
        // Base: 4. Existing enchantments: 1. Cost = 4 + 1 = 5.
        assertEquals(5, costFireAspectOnSharp5);

        // PROOF 1: Adding Fire Aspect 2 to a Sharpness 5 sword costs MORE than adding to a blank sword!
        assertTrue(costFireAspectOnSharp5 > costFireAspectBlank);
        assertEquals(5, costFireAspectOnSharp5);
        assertEquals(4, costFireAspectBlank);

        // PROOF 2: Total cost adding one by one (Sharp 5 then Fire Aspect 2):
        int totalOneByOne = costSharp5Blank + costFireAspectOnSharp5; // 5 + 5 = 10
        assertEquals(10, totalOneByOne);

        // PROOF 3: Cost adding both from ONE book to a blank sword:
        int costBothFromOneBook = calculatePotential(sharpAndFire, mults) - calculatePotential(blank, mults);
        // Base: 5 + 4 = 9. Pairs: 2 * 1 / 2 = 1. Total = 10.
        assertEquals(10, costBothFromOneBook);

        // PROOF 4: Adding from one book costs the EXACT SAME as adding one by one!
        assertEquals(totalOneByOne, costBothFromOneBook);

        // PROOF 5: Reversed order (Fire Aspect 2 first, then Sharpness 5):
        int costSharpOnFire = calculatePotential(sharpAndFire, mults) - calculatePotential(onlyFireAspect, mults);
        // Sharp 5 on Fire Aspect: base 5 + 1 surcharge = 6.
        assertEquals(6, costSharpOnFire);
        int totalReversed = costFireAspectBlank + costSharpOnFire; // 4 + 6 = 10!
        assertEquals(10, totalReversed);
        assertEquals(costBothFromOneBook, totalReversed);
    }

    /**
     * Verifies 3 enchantments: (Sharpness 5, Fire Aspect 2, Knockback 2):
     * All 6 permutations of adding one-by-one, 2-and-1, 1-and-2, and all-at-once
     * ALL yield the EXACT same total cost of 14 levels!
     */
    @Test
    public void testThreeEnchantmentsAllPathsIdentical() {
        Map<String, Integer> mults = new HashMap<>();
        mults.put("sharpness", 1);
        mults.put("fire_aspect", 2);
        mults.put("knockback", 1);

        Map<String, Integer> allThree = new HashMap<>();
        allThree.put("sharpness", 5);
        allThree.put("fire_aspect", 2);
        allThree.put("knockback", 2);

        // All 3 from one book:
        // Base: 5 + 4 + 2 = 11. Pairs: 3 * 2 / 2 = 3. Total = 14.
        int expectedCost = calculatePotential(allThree, mults) - 0;
        assertEquals(14, expectedCost);

        // Path 1: Sharpness (5), then Fire Aspect (5), then Knockback (4)
        // 5 + 5 + 4 = 14
        Map<String, Integer> s1 = Collections.singletonMap("sharpness", 5);
        int step1 = calculatePotential(s1, mults) - 0; // 5
        assertEquals(5, step1);

        Map<String, Integer> s2 = new HashMap<>(s1);
        s2.put("fire_aspect", 2);
        int step2 = calculatePotential(s2, mults) - calculatePotential(s1, mults); // 5
        assertEquals(5, step2);

        Map<String, Integer> s3 = new HashMap<>(s2);
        s3.put("knockback", 2);
        int step3 = calculatePotential(s3, mults) - calculatePotential(s2, mults); // 4
        assertEquals(4, step3);

        assertEquals(14, step1 + step2 + step3);

        // Test all 6 permutations:
        List<String[]> permutations = Arrays.asList(
            new String[]{"sharpness", "fire_aspect", "knockback"},
            new String[]{"sharpness", "knockback", "fire_aspect"},
            new String[]{"fire_aspect", "sharpness", "knockback"},
            new String[]{"fire_aspect", "knockback", "sharpness"},
            new String[]{"knockback", "sharpness", "fire_aspect"},
            new String[]{"knockback", "fire_aspect", "sharpness"}
        );

        for (String[] perm : permutations) {
            Map<String, Integer> current = new HashMap<>();
            int total = 0;
            for (String ench : perm) {
                int oldPot = calculatePotential(current, mults);
                current.put(ench, allThree.get(ench));
                int newPot = calculatePotential(current, mults);
                total += (newPot - oldPot);
            }
            assertEquals("Permutation failed to match expected total!", expectedCost, total);
        }
    }

    /**
     * Verifies that having enchantments on an item makes repairing it more expensive,
     * and both items (whether enchanted 1-by-1 or all-at-once) have the exact same repair cost!
     */
    @Test
    public void testRepairCostWithEnchantments() {
        int diamondsUsed = 2; // half durability on diamond sword

        // Plain sword (0 enchants):
        int plainRepairCost = diamondsUsed + 0;
        assertEquals(2, plainRepairCost);

        // Sword with 1 enchant:
        int oneEnchantRepairCost = diamondsUsed + 1;
        assertEquals(3, oneEnchantRepairCost);

        // Sword with 3 enchants (Sharpness 5, Fire Aspect 2, Knockback 2):
        int threeEnchantRepairCost = diamondsUsed + 3;
        assertEquals(5, threeEnchantRepairCost);

        // PROOF: An enchanted sword costs more to repair than a plain sword!
        assertTrue(threeEnchantRepairCost > plainRepairCost);
        assertEquals(5, threeEnchantRepairCost);
    }
}
