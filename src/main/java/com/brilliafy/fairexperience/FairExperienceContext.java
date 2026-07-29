package com.brilliafy.fairexperience;

public class FairExperienceContext {

    private static final ThreadLocal<Integer> ENCHANT_REQ_LEVEL = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IS_PROCESSING = ThreadLocal.withInitial(() -> false);

    public static void setEnchantReqLevel(int level) {
        ENCHANT_REQ_LEVEL.set(level);
    }

    public static int getEnchantReqLevel() {
        Integer val = ENCHANT_REQ_LEVEL.get();
        return val != null ? val : -1;
    }

    public static void clearEnchantReqLevel() {
        ENCHANT_REQ_LEVEL.remove();
    }

    public static boolean isProcessing() {
        return IS_PROCESSING.get();
    }

    public static void setProcessing(boolean processing) {
        IS_PROCESSING.set(processing);
    }
}
