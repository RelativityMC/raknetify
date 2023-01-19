/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022-2023 ishland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ishland.raknetify.bungee.connection;

import com.ishland.raknetify.bungee.RaknetifyBungeePlugin;
import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.common.connection.RakNetConnectionUtil;
import com.ishland.raknetify.common.connection.RakNetSimpleMultiChannelCodec;
import com.ishland.raknetify.common.connection.SynchronizationLayer;
import com.ishland.raknetify.common.connection.multichannel.CustomPayloadChannel;
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
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.packet.PluginMessage;
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

                // multi-channel setup
                final RakNetSimpleMultiChannelCodec multiChannelCodec = channel.pipeline().get(RakNetSimpleMultiChannelCodec.class);
                if (multiChannelCodec != null) {
                    final int protocolVersion = (int) ENCODER_PROTOCOL_VERSION.get(channel.pipeline().get(MinecraftEncoder.class));
                    final ProtocolMultiChannelMappings.VersionMapping versionMapping = ProtocolMultiChannelMappings.INSTANCE.mappings.get(protocolVersion);
                    if (versionMapping != null) {

                        // handle custom payload separately
                        final Object directionDataToClient = accessible(Protocol.class.getDeclaredField("TO_CLIENT")).get(Protocol.GAME);
                        final int pluginMessageId = (int) accessible(Class.forName("net.md_5.bungee.protocol.Protocol$DirectionData").getDeclaredMethod("getId", Class.class, int.class))
                                .invoke(directionDataToClient, PluginMessage.class, protocolVersion);
                        if (Constants.DEBUG) RaknetifyBungeePlugin.LOGGER.info("PluginMessage packetId=%d at version=%d".formatted(pluginMessageId, protocolVersion));
                        multiChannelCodec.addHandler(new CustomPayloadChannel.OverrideHandler(value -> value == pluginMessageId));

                        // packet id -> channel id
                        multiChannelCodec.addHandler(new RakNetSimpleMultiChannelCodec.PacketIdBasedOverrideHandler(
                                versionMapping.s2c,
                                "protocol version %d".formatted(protocolVersion)
                        ));
                    } else {
                        RaknetifyBungeePlugin.LOGGER.warning("No multi-channel mapping found for protocol version %d, reduced responsiveness is expected"
                                .formatted(protocolVersion));
                    }
                }

                // ping update setup
                channel.pipeline().addBefore(PipelineUtils.BOSS_HANDLER, RakNetBungeePingUpdater.NAME, new RakNetBungeePingUpdater(player));

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
