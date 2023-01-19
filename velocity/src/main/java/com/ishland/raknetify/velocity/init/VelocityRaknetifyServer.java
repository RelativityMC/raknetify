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

package com.ishland.raknetify.velocity.init;

import com.google.common.base.Preconditions;
import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.RakNetConnectionUtil;
import com.ishland.raknetify.common.util.NetworkInterfaceListener;
import com.ishland.raknetify.velocity.RaknetifyVelocityPlugin;
import com.ishland.raknetify.velocity.connection.RakNetVelocityConnectionUtil;
import com.velocitypowered.api.event.proxy.ListenerBoundEvent;
import com.velocitypowered.api.event.proxy.ListenerCloseEvent;
import com.velocitypowered.api.network.ListenerType;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.ConnectionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.DatagramChannel;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import network.ycc.raknet.server.channel.RakNetServerChannel;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.concurrent.TimeUnit;

import static com.ishland.raknetify.common.util.ReflectionUtil.accessible;

public class VelocityRaknetifyServer {

    private static final Method INIT_CHANNEL;

    static {
        try {
            INIT_CHANNEL = accessible(ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static final ReferenceOpenHashSet<ChannelFuture> channels = new ReferenceOpenHashSet<>();
    private static volatile InetSocketAddress currentServerAddress = null;
    private static volatile boolean active = false;

    static {
        NetworkInterfaceListener.addListener(event -> {
            RaknetifyVelocityPlugin.PROXY.getScheduler()
                    .buildTask(RaknetifyVelocityPlugin.INSTANCE, () -> {
                        if (!active) return;
                        final InetSocketAddress address = currentServerAddress;
                        if (address != null && address.getAddress().isAnyLocalAddress()) {
                            if (event.added()) {
                                startServer(new InetSocketAddress(event.address(), address.getPort()));
                            } else {
                                synchronized (channels) {
                                    final ObjectIterator<ChannelFuture> iterator = channels.iterator();
                                    while (iterator.hasNext()) {
                                        final ChannelFuture future = iterator.next();
                                        if (((InetSocketAddress) future.channel().localAddress()).getAddress().equals(event.address())) {
                                            closeServer(future);
                                            iterator.remove();
                                        }
                                    }
                                }
                            }
                        }
                    })
                    .delay(100, TimeUnit.MILLISECONDS)
                    .schedule();
        });
    }

    public static void start(ListenerBoundEvent evt) {
        if (evt.getListenerType() != ListenerType.MINECRAFT) return;
        Preconditions.checkArgument(RaknetifyVelocityPlugin.PROXY instanceof VelocityServer);
        Preconditions.checkState(channels.isEmpty(), "Raknetify Server is already running");
        final InetSocketAddress address = evt.getAddress();
        currentServerAddress = address;
        active = true;

        if (address.getAddress().isAnyLocalAddress()) {
            try {
                for (NetworkInterface networkInterface : NetworkInterface.networkInterfaces().toList()) {
                    for (InetAddress inetAddress : networkInterface.inetAddresses().toList()) {
                        try {
                            startServer(new InetSocketAddress(inetAddress, address.getPort()));
                        } catch (Throwable t) {
                            RaknetifyVelocityPlugin.LOGGER.error("Failed to start raknetify server", t);
                        }
                    }
                }
            } catch (Throwable t) {
                throw new RuntimeException("Failed to start raknetify server", t);
            }
        } else {
            startServer(address);
        }
    }

    private static void startServer(InetSocketAddress address) {
        try {
            final ConnectionManager cm = (ConnectionManager) accessible(VelocityServer.class.getDeclaredField("cm")).get(RaknetifyVelocityPlugin.PROXY);
            final Object transportType = accessible(ConnectionManager.class.getDeclaredField("transportType")).get(cm);
            final ChannelFactory<? extends DatagramChannel> datagramChannelFactory =
                    (ChannelFactory<? extends DatagramChannel>) accessible(Class.forName("com.velocitypowered.proxy.network.TransportType").getDeclaredField("datagramChannelFactory")).get(transportType);
            final EventLoopGroup bossGroup = (EventLoopGroup) accessible(ConnectionManager.class.getDeclaredField("bossGroup")).get(cm);
            final EventLoopGroup workerGroup = (EventLoopGroup) accessible(ConnectionManager.class.getDeclaredField("workerGroup")).get(cm);

            synchronized (channels) {
                for (ChannelFuture future : channels) {
                    if (future.channel().localAddress().equals(address)) return;
                }

                ChannelFuture future = new ServerBootstrap()
                        .channelFactory(() -> new RakNetServerChannel(() -> {
                            final DatagramChannel channel = datagramChannelFactory.newChannel();
                            channel.config().setOption(ChannelOption.IP_TOS, RakNetConnectionUtil.DEFAULT_IP_TOS);
                            channel.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(Constants.LARGE_MTU + 512).maxMessagesPerRead(128));
                            return channel;
                        }))
                        .group(bossGroup, workerGroup)
                        .childHandler(new ChannelInitializer<>() {
                            @Override
                            protected void initChannel(Channel channel) throws Exception {
                                RakNetVelocityConnectionUtil.initChannel(channel);
                                INIT_CHANNEL.invoke(cm.serverChannelInitializer.get(), channel);
                                RakNetVelocityConnectionUtil.postInitChannel(channel, false);
                            }
                        })
                        .localAddress(address)
                        .bind()
                        .syncUninterruptibly();

                RaknetifyVelocityPlugin.LOGGER.info("Raknetify server started on {}", future.channel().localAddress());
                channels.add(future);
            }

        } catch (Throwable t) {
            if (t instanceof IOException) {
                RaknetifyVelocityPlugin.LOGGER.error("Raknetify server failed to start on {}: {}", address, t.getMessage());
                return;
            }
            throw new RuntimeException("Failed to start Raknetify server", t);
        }
    }

    public static void stop(ListenerCloseEvent evt) {
        if (evt.getListenerType() != ListenerType.MINECRAFT) return;
        Preconditions.checkArgument(RaknetifyVelocityPlugin.PROXY instanceof VelocityServer);
        Preconditions.checkState(!channels.isEmpty(), "Raknetify Server is not running");
        synchronized (channels) {
            active = false;
            for (ChannelFuture channel : channels) {
                closeServer(channel);
            }

            channels.clear();
        }
    }

    private static void closeServer(ChannelFuture channel) {
        RaknetifyVelocityPlugin.LOGGER.info("Closing Raknetify server {}", channel.channel().localAddress());
        try {
            channel.channel().close().sync();
        } catch (InterruptedException e) {
            RaknetifyVelocityPlugin.LOGGER.error("Interrupted whilst closing raknetify server");
        }
    }

}
