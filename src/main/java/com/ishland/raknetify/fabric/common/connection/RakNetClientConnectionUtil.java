package com.ishland.raknetify.fabric.common.connection;

import com.ishland.raknetify.fabric.common.util.ThreadLocalUtil;
import net.minecraft.network.ClientConnection;

import java.net.InetSocketAddress;

public class RakNetClientConnectionUtil {

    private RakNetClientConnectionUtil() {
    }

    public static ClientConnection connect(InetSocketAddress address, boolean useEpoll, boolean largeMTU) {
        try {
            ThreadLocalUtil.setInitializingRaknet(true);
            ThreadLocalUtil.setInitializingRaknetLargeMTU(largeMTU);
            return ClientConnection.connect(address, useEpoll);
        } finally {
            ThreadLocalUtil.setInitializingRaknet(false);
            ThreadLocalUtil.setInitializingRaknetLargeMTU(false);
        }
    }

}
