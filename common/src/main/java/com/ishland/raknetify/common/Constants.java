package com.ishland.raknetify.common;

public class Constants {

    public static final boolean DEBUG = Boolean.getBoolean("raknetify.debug");

    public static final String RAKNET_PREFIX = "raknet;";
    public static final String RAKNET_LARGE_MTU_PREFIX = "raknetl;";
    public static final int RAKNET_PING_PACKET_ID = 0xFA;
    public static final int RAKNET_GAME_PACKET_ID = 0xFD;
    public static final int RAKNET_STREAMING_COMPRESSION_PACKET_ID = 0xED;
    public static final int RAKNET_STREAMING_COMPRESSION_HANDSHAKE_PACKET_ID = 0xEC;
    public static final int RAKNET_SYNC_PACKET_ID = 0xFC;
    public static final int RAKNET_METRICS_SYNC_PACKET_ID = 0xFB;
    public static final int MAX_QUEUED_SIZE = 256 * 1024 * 1024;
    public static final int DEFAULT_MTU = 1400;
    public static final int LARGE_MTU = 8192;
    public static final int MAX_PENDING_FRAME_SETS = 128;
    public static final int DEFAULT_PENDING_FRAME_SETS = 4;
    public static final int[] SYNC_IGNORE_CHANNELS = new int[] {1};

    private Constants() {
    }

}
