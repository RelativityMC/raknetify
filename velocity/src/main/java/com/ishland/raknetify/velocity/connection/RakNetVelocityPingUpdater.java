/*
 * This file is a part of the Velocity implementation of the Raknetify
 * project, licensed under GPLv3.
 *
 * Copyright (c) 2022-2025 ishland
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

import com.ishland.raknetify.common.util.ReflectionUtil;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import network.ycc.raknet.RakNet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RakNetVelocityPingUpdater extends ChannelDuplexHandler {

    public static final String NAME = "raknetify-velocity-ping-updater";

    private static final Method PLAYER_SET_PING;

    static {
        try {
            PLAYER_SET_PING = ReflectionUtil.accessible(ConnectedPlayer.class.getDeclaredMethod("setPing", long.class));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private final ConnectedPlayer player;

    ScheduledFuture<?> updateTask = null;

    public RakNetVelocityPingUpdater(ConnectedPlayer player) {
        this.player = Objects.requireNonNull(player);
    }

    public void handlerAdded(ChannelHandlerContext ctx) {
        if (ctx.channel().config() instanceof RakNet.Config config) {
            updateTask = ctx.channel().eventLoop().scheduleAtFixedRate(
                    () -> {
                        try {
                            PLAYER_SET_PING.invoke(player, ((config.getRTTNanos() + config.getRTTStdDevNanos()) / 1_000_000));
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    0, 1000, TimeUnit.MILLISECONDS
            );
        }
    }

    public void handlerRemoved(ChannelHandlerContext ctx) {
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }
    }

}
