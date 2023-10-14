/*
 * This file is a part of the Velocity implementation of the Raknetify
 * project, licensed under GPLv3.
 *
 * Copyright (c) 2022-2023 ishland
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

import com.google.common.base.Preconditions;
import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.common.connection.MultiChannellingEncryption;
import com.ishland.raknetify.common.connection.RakNetSimpleMultiChannelCodec;
import com.ishland.raknetify.common.connection.SynchronizationLayer;
import com.ishland.raknetify.velocity.RaknetifyVelocityPlugin;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.crypto.EncryptionUtils;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.VelocityConnectionEvent;
import com.velocitypowered.proxy.protocol.packet.AvailableCommands;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponse;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.Respawn;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdate;
import com.velocitypowered.proxy.protocol.packet.config.StartUpdate;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class RakNetVelocityChannelEventListener extends ChannelDuplexHandler {

    public static final String NAME = "raknetify-velocity-event-listener";

    private SecretKey encryptionKey = null;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof SetCompression) {
            final MultiChannelingStreamingCompression compression = ctx.channel().pipeline().get(MultiChannelingStreamingCompression.class);
            if (compression != null && compression.isActive()) {
                RaknetifyVelocityPlugin.LOGGER.info("Preventing vanilla compression as streaming compression is enabled");
                promise.setSuccess(); // swallow SetCompression packet
                return;
            }
        } else if (msg instanceof Respawn || msg instanceof JoinGame || msg instanceof StartUpdate || msg instanceof FinishedUpdate) {
            ctx.write(SynchronizationLayer.SYNC_REQUEST_OBJECT); // sync
            super.write(ctx, msg, promise);
            return;
        } else if (msg instanceof AvailableCommands) {
            ctx.write(RakNetSimpleMultiChannelCodec.SIGNAL_START_MULTICHANNEL);
            super.write(ctx, msg, promise);
            return;
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof EncryptionResponse packet) {
            try {
                byte[] secret = EncryptionUtils.decryptRsa(((VelocityServer) RaknetifyVelocityPlugin.PROXY).getServerKeyPair(), packet.getSharedSecret());
                this.encryptionKey = new SecretKeySpec(secret, "AES");
            } catch (Throwable t) {
                RaknetifyVelocityPlugin.LOGGER.warn("Failed to decrypt captured encryption secret, the raknetify connection is broken", t);
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt == VelocityConnectionEvent.COMPRESSION_ENABLED) {
            final MultiChannelingStreamingCompression compression = ctx.channel().pipeline().get(MultiChannelingStreamingCompression.class);
            if (compression != null && compression.isActive()) {
                RaknetifyVelocityPlugin.LOGGER.info("Preventing vanilla compression as streaming compression is enabled");
                ctx.channel().pipeline().replace(Connections.COMPRESSION_ENCODER, Connections.COMPRESSION_ENCODER, new ChannelDuplexHandler()); // no-op
                ctx.channel().pipeline().replace(Connections.COMPRESSION_DECODER, Connections.COMPRESSION_DECODER, new ChannelDuplexHandler()); // no-op

                // sync velocity compression state
                final MinecraftConnection minecraftConnection = ctx.channel().pipeline().get(MinecraftConnection.class);
                if (minecraftConnection != null) {
                    minecraftConnection.setCompressionThreshold(-1);
                } else {
                    RaknetifyVelocityPlugin.LOGGER.warn("Unable to sync compression state with velocity");
                }
            }
        } else if (evt == VelocityConnectionEvent.COMPRESSION_DISABLED) {
            ctx.channel().pipeline().replace(Connections.FRAME_DECODER, Connections.FRAME_DECODER, new ChannelDuplexHandler()); // no-op
            ctx.channel().pipeline().replace(Connections.FRAME_ENCODER, Connections.FRAME_ENCODER, new ChannelDuplexHandler()); // no-op
        } else if (evt == VelocityConnectionEvent.ENCRYPTION_ENABLED) {
            Preconditions.checkState(this.encryptionKey != null, "EncryptionResponse not received yet or already consumed");
            ctx.channel().pipeline().replace(Connections.CIPHER_ENCODER, Connections.CIPHER_ENCODER, new ChannelDuplexHandler());
            ctx.channel().pipeline().replace(Connections.CIPHER_DECODER, Connections.CIPHER_DECODER, new ChannelDuplexHandler());
            ctx.channel().pipeline().addBefore(MultiChannelingStreamingCompression.NAME, MultiChannellingEncryption.NAME, new MultiChannellingEncryption(encryptionKey));
        }
    }
}
