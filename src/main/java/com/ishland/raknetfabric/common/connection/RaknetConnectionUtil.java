package com.ishland.raknetfabric.common.connection;

import com.ishland.raknetfabric.Constants;
import com.ishland.raknetfabric.common.compat.viafabric.ViaFabricCompatInjector;
import io.netty.channel.Channel;
import network.ycc.raknet.RakNet;
import network.ycc.raknet.pipeline.UserDataCodec;

public class RaknetConnectionUtil {

    private RaknetConnectionUtil() {
    }

    public static void initChannel(Channel channel) {
        if (channel.config() instanceof RakNet.Config config) {
            config.setMaxQueuedBytes(Constants.MAX_QUEUED_SIZE);
            config.setMetrics(new SimpleMetricsLogger());
            channel.pipeline().addLast("raknet_backend", new UserDataCodec(Constants.RAKNET_PACKET_ID));
        }
    }

    public static void postInitChannel(Channel channel, boolean isClientSide) {
        if (channel.config() instanceof RakNet.Config) {
            ViaFabricCompatInjector.inject(channel, isClientSide);
        }
    }

}
