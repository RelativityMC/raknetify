package com.ishland.raknetfabric.common.connection;

public class RaknetChannelUtil {

    private RaknetChannelUtil() {
    }

    private static final ThreadLocal<Integer> scheduledChannel = new ThreadLocal<>();

    public static void pushScheduledChannel(int channel) {
        if (scheduledChannel.get() != null) throw new IllegalStateException("Already scheduled");
        scheduledChannel.set(channel);
    }

    public static int getScheduledChannel() {
        final Integer integer = scheduledChannel.get();
        return integer != null ? integer : 0;
    }

    public static void popScheduledChannel() {
        if (scheduledChannel.get() == null) throw new IllegalStateException("Not scheduled");
        scheduledChannel.set(null);
    }

}
