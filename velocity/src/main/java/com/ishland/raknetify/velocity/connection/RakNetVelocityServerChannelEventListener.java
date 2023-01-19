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
import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.velocity.RaknetifyVelocityPlugin;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import network.ycc.raknet.RakNet;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RakNetVelocityServerChannelEventListener extends ChannelDuplexHandler {

    public static final String NAME = "raknetify-bungee-downstream-event-listener";

    private final Channel clientChannel;

    public RakNetVelocityServerChannelEventListener(Channel clientChannel) {
        Preconditions.checkArgument(clientChannel.config() instanceof RakNet.Config);
        this.clientChannel = clientChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof KeepAlive) {
            if (Constants.DEBUG) RaknetifyVelocityPlugin.LOGGER.info("Received downstream keepalive, swallowing it");
            final long rttNanos = RakNet.config(clientChannel).getRTTNanos();
//            RaknetifyVelocityPlugin.PROXY.getScheduler().buildTask(RaknetifyVelocityPlugin.INSTANCE, () -> ctx.write(msg))
//                    .delay(Math.max(rttNanos - 4_000_000, 0), TimeUnit.NANOSECONDS) // reduce delay to aid scheduling overhead
//                    .clearRepeat()
//                    .schedule();
            ctx.channel().eventLoop().schedule(() -> ctx.writeAndFlush(msg), Math.max(rttNanos - 4_000_000, 0), TimeUnit.NANOSECONDS); // reduce delay to aid scheduling overhead
            return; // prevent keepalive from being sent to clients
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        for (Map.Entry<String, ChannelHandler> entry : ctx.channel().pipeline().toMap().entrySet()) {
            System.out.println("%s: %s".formatted(entry.getKey(), entry.getValue().getClass().getName()));
        }
    }
}
