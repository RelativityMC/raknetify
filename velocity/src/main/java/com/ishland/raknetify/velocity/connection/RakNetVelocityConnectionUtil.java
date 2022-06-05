package com.ishland.raknetify.velocity.connection;

import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.common.connection.RakNetConnectionUtil;
import com.ishland.raknetify.common.connection.RakNetSimpleMultiChannelCodec;
import com.ishland.raknetify.common.data.ProtocolMultiChannelMappings;
import com.ishland.raknetify.velocity.RaknetifyVelocityPlugin;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
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
            channel.pipeline().addAfter(MultiChannelingStreamingCompression.NAME, RakNetSimpleMultiChannelCodec.NAME, new RakNetSimpleMultiChannelCodec(Constants.RAKNET_GAME_PACKET_ID));
        }
    }

    public static void postInitChannel(Channel channel, boolean isClientSide) {
        if (channel.config() instanceof RakNet.Config) {
            channel.pipeline().replace(Connections.READ_TIMEOUT, Connections.READ_TIMEOUT, new ChannelDuplexHandler()); // no-op
            channel.pipeline().replace(Connections.FRAME_DECODER, Connections.FRAME_DECODER, new ChannelDuplexHandler()); // no-op
            channel.pipeline().replace(Connections.FRAME_ENCODER, Connections.FRAME_ENCODER, new ChannelDuplexHandler()); // no-op
            if (channel.pipeline().get(HAProxyMessageDecoder.class) != null)
                channel.pipeline().remove(HAProxyMessageDecoder.class);
            channel.pipeline().addBefore(Connections.HANDLER, RakNetVelocityChannelEventListener.NAME, new RakNetVelocityChannelEventListener());
//            System.out.println(channel.pipeline().names());
//            final MultiChannellingPacketCapture handler = new MultiChannellingPacketCapture();
//            channel.pipeline().addLast("raknetify-multi-channel-packet-cature", handler);
//            channel.pipeline().get(MultiChannellingDataCodec.class).setCapture(handler);
        }
    }

    public static void onPlayerLogin(LoginEvent evt) {
        final ConnectedPlayer player = (ConnectedPlayer) evt.getPlayer();
        final Channel channel = player.getConnection().getChannel();
        if (channel != null && channel.config() instanceof RakNet.Config config) {
            final RakNetSimpleMultiChannelCodec multiChannelCodec = channel.pipeline().get(RakNetSimpleMultiChannelCodec.class);
            if (multiChannelCodec != null) {
                final ProtocolVersion protocolVersion = player.getProtocolVersion();
                multiChannelCodec.setDescriptiveProtocolStatus("%s (%d)".formatted(protocolVersion.getVersionIntroducedIn(), protocolVersion.getProtocol()));
                final ProtocolMultiChannelMappings.VersionMapping versionMapping = ProtocolMultiChannelMappings.INSTANCE.mappings.get(protocolVersion.getProtocol());
                if (versionMapping != null) multiChannelCodec.setSimpleChannelMapping(versionMapping.s2c);
            }
            RaknetifyVelocityPlugin.LOGGER.info(String.format("Raknetify: %s logged in via RakNet, mtu %d", evt.getPlayer().getGameProfile().getName(), config.getMTU()));
        }
    }

}
