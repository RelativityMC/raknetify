package com.ishland.raknetify.fabric.common.connection;

import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.common.connection.RakNetConnectionUtil;
import com.ishland.raknetify.fabric.common.compat.viafabric.ViaFabricCompatInjector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import network.ycc.raknet.RakNet;

import static com.ishland.raknetify.common.util.ReflectionUtil.accessible;

public class RakNetFabricConnectionUtil {

    private RakNetFabricConnectionUtil() {
    }

    public static void initChannel(Channel channel) {
        if (channel.config() instanceof RakNet.Config) {
            RakNetConnectionUtil.initChannel(channel);
            channel.pipeline().addAfter(MultiChannelingStreamingCompression.NAME, RakNetFabricMultiChannelCodec.NAME, new RakNetFabricMultiChannelCodec(Constants.RAKNET_GAME_PACKET_ID));
        }
    }

    public static void postInitChannel(Channel channel, boolean isClientSide) {
        if (channel.config() instanceof RakNet.Config) {
            ViaFabricCompatInjector.inject(channel, isClientSide);
            channel.pipeline().replace("timeout", "timeout", new ChannelDuplexHandler()); // no-op
            channel.pipeline().replace("splitter", "splitter", new ChannelDuplexHandler()); // no-op
            channel.pipeline().replace("prepender", "prepender", new ChannelDuplexHandler()); // no-op
            final MultiChannellingPacketCapture handler = new MultiChannellingPacketCapture();
            channel.pipeline().addLast("raknetify-multi-channel-packet-cature", handler);
            channel.pipeline().get(RakNetFabricMultiChannelCodec.class).setCapture(handler);
            channel.pipeline().addLast("raknetify-handle-compression-compatibility", new RakNetCompressionCompatibilityHandler());
        }
    }

}
