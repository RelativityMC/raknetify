package com.ishland.raknetify.bungee.connection;

import com.ishland.raknetify.bungee.RaknetifyBungeePlugin;
import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.common.connection.RakNetConnectionUtil;
import com.ishland.raknetify.common.connection.RakNetSimpleMultiChannelCodec;
import com.ishland.raknetify.common.connection.SynchronizationLayer;
import com.ishland.raknetify.common.data.ProtocolMultiChannelMappings;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import net.md_5.bungee.ServerConnection;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.MinecraftEncoder;
import network.ycc.raknet.RakNet;

import java.lang.reflect.Field;

import static com.ishland.raknetify.common.util.ReflectionUtil.accessible;

public class RakNetBungeeConnectionUtil {

    private static final Field USER_CONNECTION_CH;
    private static final Field SERVER_CONNECTION_CH;
    private static final Field ENCODER_PROTOCOL_VERSION;

    static {
        try {
            USER_CONNECTION_CH = accessible(UserConnection.class.getDeclaredField("ch"));
            SERVER_CONNECTION_CH = accessible(ServerConnection.class.getDeclaredField("ch"));
            ENCODER_PROTOCOL_VERSION = accessible(MinecraftEncoder.class.getDeclaredField("protocolVersion"));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private RakNetBungeeConnectionUtil() {
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
            channel.pipeline().replace(PipelineUtils.TIMEOUT_HANDLER, PipelineUtils.TIMEOUT_HANDLER, new ChannelDuplexHandler()); // no-op
            channel.pipeline().replace(PipelineUtils.FRAME_DECODER, PipelineUtils.FRAME_DECODER, new ChannelDuplexHandler()); // no-op
            channel.pipeline().replace(PipelineUtils.FRAME_PREPENDER, PipelineUtils.FRAME_PREPENDER, new ChannelDuplexHandler()); // no-op
            if (channel.pipeline().get(HAProxyMessageDecoder.class) != null)
                channel.pipeline().remove(HAProxyMessageDecoder.class);
            channel.pipeline().addBefore(PipelineUtils.BOSS_HANDLER, RakNetBungeeClientChannelEventListener.NAME, new RakNetBungeeClientChannelEventListener());
//            System.out.println(channel.pipeline().names());
//            final MultiChannellingPacketCapture handler = new MultiChannellingPacketCapture();
//            channel.pipeline().addLast("raknetify-multi-channel-packet-cature", handler);
//            channel.pipeline().get(MultiChannellingDataCodec.class).setCapture(handler);
        }
    }

    public static void onPlayerLogin(PostLoginEvent evt) {
        try {
            final UserConnection player = (UserConnection) evt.getPlayer();
            final ChannelWrapper channelWrapper = (ChannelWrapper) USER_CONNECTION_CH.get(player);
            final Channel channel = channelWrapper.getHandle();
            if (channel != null && channel.config() instanceof RakNet.Config config) {
                final RakNetSimpleMultiChannelCodec multiChannelCodec = channel.pipeline().get(RakNetSimpleMultiChannelCodec.class);
                if (multiChannelCodec != null) {
                    final int protocolVersion = (int) ENCODER_PROTOCOL_VERSION.get(channel.pipeline().get(MinecraftEncoder.class));
                    multiChannelCodec.setDescriptiveProtocolStatus("protocol version %d".formatted(protocolVersion));
                    final ProtocolMultiChannelMappings.VersionMapping versionMapping = ProtocolMultiChannelMappings.INSTANCE.mappings.get(protocolVersion);
                    if (versionMapping != null) multiChannelCodec.setSimpleChannelMapping(versionMapping.s2c);
                }
                RaknetifyBungeePlugin.LOGGER.info(String.format("Raknetify: %s logged in via RakNet, mtu %d", player.getName(), config.getMTU()));
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static void handleServerSwitch(ServerConnectedEvent evt) {

        try {
            final UserConnection player = (UserConnection) evt.getPlayer();
            final Channel playerChannel = ((ChannelWrapper) USER_CONNECTION_CH.get(player)).getHandle();

            if (playerChannel != null && playerChannel.config() instanceof RakNet.Config config) {
                // this exists because bungeecord sends several packets to reset state before Respawn packet during server switch
                playerChannel.write(SynchronizationLayer.SYNC_REQUEST_OBJECT);

                // and inject into the server channel
                final ServerConnection server = (ServerConnection) evt.getServer();
                final Channel serverChannel = ((ChannelWrapper) SERVER_CONNECTION_CH.get(server)).getHandle();
                serverChannel.pipeline().addBefore(PipelineUtils.BOSS_HANDLER, RakNetBungeeServerChannelEventListener.NAME, new RakNetBungeeServerChannelEventListener(playerChannel));
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
