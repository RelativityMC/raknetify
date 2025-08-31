/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022-2025 ishland
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
