package com.ishland.raknetify.velocity.connection;

import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.common.connection.RakNetConnectionUtil;
import com.velocitypowered.proxy.network.Connections;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import network.ycc.raknet.RakNet;
import network.ycc.raknet.pipeline.UserDataCodec;

public class RakNetVelocityConnectionUtil {

    private RakNetVelocityConnectionUtil() {
    }

    public static void initChannel(Channel channel) {
        if (channel.config() instanceof RakNet.Config) {
            RakNetConnectionUtil.initChannel(channel);
//            channel.pipeline().addAfter(MultiChannelingStreamingCompression.NAME, MultiChannellingDataCodec.NAME, new MultiChannellingDataCodec(Constants.RAKNET_GAME_PACKET_ID));
            channel.pipeline().addAfter(MultiChannelingStreamingCompression.NAME, UserDataCodec.NAME, new UserDataCodec(Constants.RAKNET_GAME_PACKET_ID));
        }
    }

    public static void postInitChannel(Channel channel, boolean isClientSide) {
        if (channel.config() instanceof RakNet.Config) {
            channel.pipeline().replace(Connections.READ_TIMEOUT, Connections.READ_TIMEOUT, new ChannelDuplexHandler()); // no-op
            channel.pipeline().replace(Connections.FRAME_DECODER, Connections.FRAME_DECODER, new ChannelDuplexHandler()); // no-op
            channel.pipeline().replace(Connections.FRAME_ENCODER, Connections.FRAME_ENCODER, new ChannelDuplexHandler()); // no-op
            if (channel.pipeline().get(HAProxyMessageDecoder.class) != null)
                channel.pipeline().remove(HAProxyMessageDecoder.class);
            channel.pipeline().addLast(RakNetVelocityChannelEventListener.NAME, new RakNetVelocityChannelEventListener());
//            final MultiChannellingPacketCapture handler = new MultiChannellingPacketCapture();
//            channel.pipeline().addLast("raknetify-multi-channel-packet-cature", handler);
//            channel.pipeline().get(MultiChannellingDataCodec.class).setCapture(handler);
        }
    }

}
