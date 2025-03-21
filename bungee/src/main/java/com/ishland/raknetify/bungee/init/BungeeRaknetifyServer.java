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

package com.ishland.raknetify.bungee.init;

import com.ishland.raknetify.bungee.RaknetifyBungeePlugin;
import com.ishland.raknetify.bungee.connection.RakNetBungeeConnectionUtil;
import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.RakNetConnectionUtil;
import com.ishland.raknetify.common.util.NetworkInterfaceListener;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.AttributeKey;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.query.QueryHandler;
import network.ycc.raknet.server.channel.RakNetServerChannel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Stream;

import static com.ishland.raknetify.common.util.ReflectionUtil.accessible;

public class BungeeRaknetifyServer {

    private static final int portOverride = Integer.getInteger("raknetify.bungee.portOverride", -1);

    private static final Method INIT_CHANNEL;
    private static final Field BUNGEE_LISTENERS_FIELD;

    private static final Class<?> ACCEPTOR_CLASS;

    static {
        try {
            INIT_CHANNEL = accessible(ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class));
            BUNGEE_LISTENERS_FIELD = accessible(BungeeCord.class.getDeclaredField("listeners"));
            ACCEPTOR_CLASS = Class.forName("io.netty.bootstrap.ServerBootstrap$ServerBootstrapAcceptor");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static final Reference2ReferenceOpenHashMap<Channel, ReferenceOpenHashSet<ChannelFuture>> channels = new Reference2ReferenceOpenHashMap<>();
    private static final ReferenceOpenHashSet<ChannelFuture> nonWildcardChannels = new ReferenceOpenHashSet<>();

    private static volatile boolean active = false;
    private static volatile int activeIndex = 0;
    private static boolean injected = false;

    private static volatile Consumer<NetworkInterfaceListener.InterfaceAddressChangeEvent> listener = null;

    public static void inject() {
        if (active) return;
        try {
            active = true;
            final BungeeCord instance = (BungeeCord) ProxyServer.getInstance();

//            stopAllQueryPorts();

            final Set<Channel> listeners = (Set<Channel>) BUNGEE_LISTENERS_FIELD.get(instance);

            if (!injected) {
                BUNGEE_LISTENERS_FIELD.set(instance, new InjectedSet(listeners));
                injected = true;
            }

            List<Throwable> errors = new ArrayList<>();

            for (Channel listener : listeners) {
                try {
                    injectChannel(instance, listener, true);
                } catch (Throwable t) {
                    errors.add(t);
                }
            }

            int currentActiveIndex = ++activeIndex;
            listener = event -> {
                if (!active) {
                    NetworkInterfaceListener.removeListener(listener);
                }

                if (currentActiveIndex != activeIndex) return; // we can't remove ourselves now, is plugin reloaded?

                if (event.added()) {
                    for (Channel channel : channels.keySet()) {
                        injectChannel(instance, channel, false);
                    }
                } else {
                    for (ReferenceOpenHashSet<ChannelFuture> futures : channels.values()) {
                        for (ObjectIterator<ChannelFuture> iterator = futures.iterator(); iterator.hasNext(); ) {
                            ChannelFuture future = iterator.next();
                            if (((InetSocketAddress) future.channel().localAddress()).getAddress().equals(event.address())) {
                                RaknetifyBungeePlugin.LOGGER.info("Closing Raknetify server %s".formatted(future.channel().localAddress()));
                                future.channel().close();
                                iterator.remove();
                            }
                        }
                    }
                }
            };
            NetworkInterfaceListener.addListener(event ->
                    instance.getScheduler().schedule(RaknetifyBungeePlugin.INSTANCE, () -> listener.accept(event), 100, TimeUnit.MILLISECONDS));

            if (!errors.isEmpty()) {
                final RuntimeException exception = new RuntimeException("Failed to start Raknetify server");
                errors.forEach(exception::addSuppressed);
                throw exception;
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to start Raknetify server", t);
        }
    }

    public static void injectChannel(BungeeCord instance, Channel listener, boolean throwErrors) {
        try {
            if (!active) return;

            final boolean hasPortOverride = portOverride > 0 && portOverride < 65535;
            if (hasPortOverride && !channels.isEmpty()) return; // avoid duplicate listeners

            if (!(listener.localAddress() instanceof InetSocketAddress)) return;

            ChannelInitializer<Channel> initializer = null;
            ListenerInfo info = null;

            for (String name : listener.pipeline().names()) {
                final ChannelHandler handler = listener.pipeline().get(name);
                if (handler instanceof QueryHandler) {
                    return;
                }
                if (handler != null && ACCEPTOR_CLASS.isAssignableFrom(handler.getClass())) {
                    try {
                        initializer = (ChannelInitializer<Channel>) accessible(ACCEPTOR_CLASS.getDeclaredField("childHandler")).get(handler);
                        final Map.Entry<AttributeKey<?>, ?>[] attrs = (Map.Entry<AttributeKey<?>, ?>[])
                                accessible(ACCEPTOR_CLASS.getDeclaredField("childAttrs")).get(handler);
                        for (var attr : attrs) {
                            if (attr.getKey() == PipelineUtils.LISTENER) {
                                info = (ListenerInfo) attr.getValue();
                            }
                        }
                    } catch (IllegalAccessException | NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (initializer == null) {
                RaknetifyBungeePlugin.LOGGER.severe("Unable to find channel initializer for listener %s".formatted(listener));
                return;
            }

            if (info == null) {
                RaknetifyBungeePlugin.LOGGER.severe("Unable to find listener info for listener %s".formatted(listener));
                return;
            }

            if (listener.getClass().getName().startsWith("org.geysermc.geyser.network")) { // filter out geyser
                return;
            }

            if (((InetSocketAddress) info.getSocketAddress()).getAddress().isAnyLocalAddress()) {
                for (NetworkInterface networkInterface : NetworkInterface.networkInterfaces().toList()) {
                    final Iterator<InetAddress> iterator = networkInterface.getInetAddresses().asIterator();
                    while (iterator.hasNext()) {
                        final InetAddress address = iterator.next();
                        try {
                            startServer(instance, listener, initializer, info, address);
                        } catch (Throwable t) {
                            RaknetifyBungeePlugin.LOGGER.log(Level.SEVERE, "Failed to start Raknetify server", t);
                        }
                    }
                }
            } else {
                startServer(instance, listener, initializer, info, null);
            }

        } catch (Throwable t) {
            if (throwErrors) throw new RuntimeException("Failed to start Raknetify server", t);
            else RaknetifyBungeePlugin.LOGGER.log(Level.SEVERE, "Failed to start Raknetify server", t);
        }
    }

    private static void startServer(BungeeCord instance, Channel listener, ChannelInitializer<Channel> initializer, ListenerInfo info, InetAddress address) throws NoSuchFieldException, IllegalAccessException {
        if (address != null) {
            final ReferenceOpenHashSet<ChannelFuture> futures = channels.get(listener);
            if (futures != null) {
                for (ChannelFuture future : futures) {
                    if (((InetSocketAddress) future.channel().localAddress()).getAddress().equals(address))
                        return; // avoid duplicate
                }
            }
        }

        final boolean hasPortOverride = portOverride > 0 && portOverride < 65535;
        final ReflectiveChannelFactory<? extends DatagramChannel> factory = new ReflectiveChannelFactory<>(PipelineUtils.getDatagramChannel());
        final InetAddress actualAddress = address == null ? ((InetSocketAddress) info.getSocketAddress()).getAddress() : address;
        final ChannelFuture future = new ServerBootstrap()
                .channelFactory(() -> new RakNetServerChannel(() -> {
                    final DatagramChannel channel = factory.newChannel();
                    channel.config().setOption(ChannelOption.IP_TOS, RakNetConnectionUtil.DEFAULT_IP_TOS);
                    channel.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(Constants.LARGE_MTU + 512).maxMessagesPerRead(128));
                    return channel;
                }))
                .option(ChannelOption.SO_REUSEADDR, true)
                .childAttr(PipelineUtils.LISTENER, info)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        RakNetBungeeConnectionUtil.initChannel(channel);
                        INIT_CHANNEL.invoke(initializer, channel);
                        RakNetBungeeConnectionUtil.postInitChannel(channel, false);
                    }
                })
                .group(getBossEventLoopGroup(instance), getWorkerEventLoopGroup(instance))
                .localAddress(hasPortOverride ? new InetSocketAddress(actualAddress, portOverride) : new InetSocketAddress(actualAddress, ((InetSocketAddress) info.getSocketAddress()).getPort()))
                .bind()
                .syncUninterruptibly();
        if (address == null) {
            nonWildcardChannels.add(future);
        } else {
            channels.computeIfAbsent(listener, unused -> new ReferenceOpenHashSet<>()).add(future);
        }

        RaknetifyBungeePlugin.LOGGER.info("Raknetify server started on %s".formatted(future.channel().localAddress()));
    }

    public static void stopAll() {
        if (!active) return;
        for (ChannelFuture future : Stream.concat(channels.values().stream().flatMap(Collection::stream), nonWildcardChannels.stream()).toList()) {
            RaknetifyBungeePlugin.LOGGER.info("Closing Raknetify server %s".formatted(future.channel().localAddress()));
            try {
                future.channel().close().sync();
            } catch (InterruptedException e) {
                RaknetifyBungeePlugin.LOGGER.severe("Interrupted whilst closing raknetify server");
            }
        }

        channels.clear();
        nonWildcardChannels.clear();
    }

    public static void disable() {
        if (!active) return;
        stopAll();
        active = false;
    }

    private static EventLoopGroup getBossEventLoopGroup(BungeeCord instance) throws NoSuchFieldException, IllegalAccessException {
        try {
            return (EventLoopGroup) accessible(BungeeCord.class.getDeclaredField("eventLoops")).get(instance);
        } catch (NoSuchFieldException e) { // waterfall: use split boss and worker group
            //noinspection JavaReflectionMemberAccess
            return (EventLoopGroup) accessible(BungeeCord.class.getDeclaredField("bossEventLoopGroup")).get(instance);
        }
    }

    private static EventLoopGroup getWorkerEventLoopGroup(BungeeCord instance) throws NoSuchFieldException, IllegalAccessException {
        try {
            return (EventLoopGroup) accessible(BungeeCord.class.getDeclaredField("eventLoops")).get(instance);
        } catch (NoSuchFieldException e) { // waterfall: use split boss and worker group
            //noinspection JavaReflectionMemberAccess
            return (EventLoopGroup) accessible(BungeeCord.class.getDeclaredField("workerEventLoopGroup")).get(instance);
        }
    }

}
