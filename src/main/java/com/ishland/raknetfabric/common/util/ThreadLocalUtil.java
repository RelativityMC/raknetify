package com.ishland.raknetfabric.common.util;

public class ThreadLocalUtil {

    private ThreadLocalUtil() {
    }

    private static final ThreadLocal<Boolean> initializingRaknet = ThreadLocal.withInitial(() -> false);

    public static void setInitializingRaknet(boolean value) {
        initializingRaknet.set(value);
    }

    public static boolean isInitializingRaknet() {
        return initializingRaknet.get();
    }

}
