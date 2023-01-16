/*
 * This file is a part of the Velocity implementation of the Raknetify
 * project, licensed under GPLv3.
 *
 * Copyright (c) 2022 ishland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ishland.raknetify.velocity.connection;

import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.common.connection.RakNetConnectionUtil;
import com.ishland.raknetify.common.connection.RakNetSimpleMultiChannelCodec;
import com.ishland.raknetify.common.data.ProtocolMultiChannelMappings;
import com.ishland.raknetify.velocity.RaknetifyVelocityPlugin;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import network.ycc.raknet.RakNet;

import java.util.Arrays;
import java.util.Collections;

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
                final ProtocolMultiChannelMappings.VersionMapping versionMapping = ProtocolMultiChannelMappings.INSTANCE.mappings.get(protocolVersion.getProtocol());
                if (versionMapping != null) {
                    multiChannelCodec.addHandler(new RakNetSimpleMultiChannelCodec.PacketIdBasedOverrideHandler(
                            versionMapping.s2c,
                            "%s (%d)".formatted(protocolVersion.getVersionIntroducedIn(), protocolVersion.getProtocol())
                    ));
                } else {
                    RaknetifyVelocityPlugin.LOGGER.warn("No multi-channel mappings for protocol version {} ({})", protocolVersion.getProtocol(), Arrays.toString(protocolVersion.getVersionsSupportedBy().toArray(String[]::new)));
                }
            }
            RaknetifyVelocityPlugin.LOGGER.info(String.format("Raknetify: %s logged in via RakNet, mtu %d", evt.getPlayer().getGameProfile().getName(), config.getMTU()));
        }
    }

    public static void onServerSwitch(ServerPostConnectEvent evt) {
        final ConnectedPlayer player = (ConnectedPlayer) evt.getPlayer();
        final VelocityServerConnection connectedServer = player.getConnectedServer();
        if (connectedServer == null) {
            RaknetifyVelocityPlugin.LOGGER.warn("No connected server for player ({}) after server switch?", player);
            return;
        }
        final MinecraftConnection serverConnection = connectedServer.getConnection();
        if (serverConnection == null) {
            RaknetifyVelocityPlugin.LOGGER.warn("Connected server ({}) have no underlying connection?", connectedServer);
            return;
        }
        final Channel channel = player.getConnection().getChannel();
        if (channel.config() instanceof RakNet.Config) {
            serverConnection.getChannel().pipeline().addBefore(Connections.HANDLER, RakNetVelocityServerChannelEventListener.NAME, new RakNetVelocityServerChannelEventListener(channel));
        }
    }

}
