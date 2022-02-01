package com.ishland.raknetfabric.common.connection;

import com.ishland.raknetfabric.Constants;
import com.ishland.raknetfabric.common.compat.viafabric.ViaFabricCompatInjector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import network.ycc.raknet.RakNet;

public class RaknetConnectionUtil {

    private RaknetConnectionUtil() {
    }

    public static void initChannel(Channel channel) {
        if (channel.config() instanceof RakNet.Config config) {
            config.setMTU(Constants.DEFAULT_MTU);
            config.setMaxQueuedBytes(Constants.MAX_QUEUED_SIZE);
            config.setMetrics(new SimpleMetricsLogger());
            channel.pipeline().addLast("raknetfabric-synchronization-layer", new SynchronizationLayer(1));
            channel.pipeline().addLast("raknetfabric-multi-channel-data-codec", new MultiChannellingDataCodec(Constants.RAKNET_GAME_PACKET_ID));
        }
    }

    public static void postInitChannel(Channel channel, boolean isClientSide) {
        if (channel.config() instanceof RakNet.Config) {
            ViaFabricCompatInjector.inject(channel, isClientSide);
            channel.pipeline().replace("splitter", "splitter", new ChannelDuplexHandler());
            channel.pipeline().replace("prepender", "prepender", new ChannelDuplexHandler());
            channel.pipeline().addLast("raknetfabric-multi-channel-packet-cature", new MultiChannellingPacketCapture());
        }
    }

}
