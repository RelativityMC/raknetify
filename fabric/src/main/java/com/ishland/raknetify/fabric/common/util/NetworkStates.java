package com.ishland.raknetify.fabric.common.util;

import net.minecraft.network.NetworkState;

public class NetworkStates {

    public static String getName(NetworkState state) {
        return switch (state) {
            case HANDSHAKING -> "HANDSHAKING";
            case PLAY -> "PLAY";
            case STATUS -> "STATUS";
            case LOGIN -> "LOGIN";
        };
    }

}
