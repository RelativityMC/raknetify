package com.ishland.raknetfabric;

public class Constants {

    public static final String RAKNET_PREFIX = "raknet;";
    public static final String RAKNET_LARGE_MTU_PREFIX = "raknetl;";
    public static final int RAKNET_PING_PACKET_ID = 0xFE;
    public static final int RAKNET_GAME_PACKET_ID = 0xFD;
    public static final int RAKNET_STREAMING_COMPRESSION_PACKET_ID = 0xED;
    public static final int RAKNET_SYNC_PACKET_ID = 0xFC;
    public static final int RAKNET_METRICS_SYNC_PACKET_ID = 0xFB;
    public static final int MAX_QUEUED_SIZE = 256 * 1024 * 1024;
    public static final int DEFAULT_MTU = 1400;
    public static final int LARGE_MTU = 8192;
    public static final int MAX_PENDING_FRAME_SETS = 2 * 1024;
    public static final int DEFAULT_PENDING_FRAME_SETS = 4;

    private Constants() {
    }

}
