package com.ishland.raknetify.common.util;

public class ThreadLocalUtil {

    private ThreadLocalUtil() {
    }

    private static final ThreadLocal<Boolean> initializingRaknet = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> initializingRaknetLargeMTU = ThreadLocal.withInitial(() -> false);

    public static void setInitializingRaknet(boolean value) {
        initializingRaknet.set(value);
    }

    public static void setInitializingRaknetLargeMTU(boolean value) {
        initializingRaknetLargeMTU.set(value);
    }

    public static boolean isInitializingRaknet() {
        return initializingRaknet.get();
    }

    public static boolean isInitializingRaknetLargeMTU() {
        return initializingRaknetLargeMTU.get();
    }

}
