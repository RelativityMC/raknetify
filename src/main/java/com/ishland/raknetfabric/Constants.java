package com.ishland.raknetfabric;

public class Constants {

    public static final String RAKNET_PREFIX = "raknet;";
    public static final int RAKNET_PING_PACKET_ID = 0xFE;
    public static final int RAKNET_GAME_PACKET_ID = 0xFD;
    public static final int RAKNET_SYNC_PACKET_ID = 0xFC;
    public static final int MAX_QUEUED_SIZE = 64 * 1024 * 1024;
    public static final int DEFAULT_MTU = 1400;

    private Constants() {
    }

}
