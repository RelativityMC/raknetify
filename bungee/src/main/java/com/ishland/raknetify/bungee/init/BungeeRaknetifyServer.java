package com.ishland.raknetify.bungee.init;

import com.ishland.raknetify.bungee.RaknetifyBungeePlugin;
import com.ishland.raknetify.bungee.connection.RakNetBungeeConnectionUtil;
import com.ishland.raknetify.common.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.AttributeKey;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.query.QueryHandler;
import network.ycc.raknet.server.channel.RakNetServerChannel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.ishland.raknetify.common.util.ReflectionUtil.accessible;

public class BungeeRaknetifyServer {

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

    private static final HashSet<ChannelFuture> channels = new HashSet<>();

    private static boolean active = false;
    private static boolean injected = false;

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
                    injectChannel(instance, listener);
                } catch (Throwable t) {
                    errors.add(t);
                }
            }

            if (!errors.isEmpty()) {
                final RuntimeException exception = new RuntimeException("Failed to start Raknetify server");
                errors.forEach(exception::addSuppressed);
                throw exception;
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to start Raknetify server", t);
        }
    }

    public static void injectChannel(BungeeCord instance, Channel listener) {
        if (!active) return;

        final ReflectiveChannelFactory<? extends DatagramChannel> factory = new ReflectiveChannelFactory<>(PipelineUtils.getDatagramChannel());

        if (listener.localAddress() instanceof DomainSocketAddress) return;

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
        }

        final ChannelInitializer<Channel> finalInitializer = initializer;
        final ChannelFuture future = new ServerBootstrap()
                .channelFactory(() -> new RakNetServerChannel(() -> {
                    final DatagramChannel channel = factory.newChannel();
                    channel.config().setOption(ChannelOption.IP_TOS, 0x18);
                    channel.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(Constants.LARGE_MTU + 512));
                    return channel;
                }))
                .option(ChannelOption.SO_REUSEADDR, true)
                .childAttr(PipelineUtils.LISTENER, info)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        RakNetBungeeConnectionUtil.initChannel(channel);
                        INIT_CHANNEL.invoke(finalInitializer, channel);
                        RakNetBungeeConnectionUtil.postInitChannel(channel, false);
                    }
                })
                .group(instance.eventLoops)
                .localAddress(info.getSocketAddress())
                .bind()
                .syncUninterruptibly();
        channels.add(future);

        RaknetifyBungeePlugin.LOGGER.info("Raknetify server started on %s".formatted(future.channel().localAddress()));
    }

    public static void stopAll() {
        if (!active) return;
        for (ChannelFuture future : channels) {
            RaknetifyBungeePlugin.LOGGER.info("Closing Raknetify server %s".formatted(future.channel().localAddress()));
            try {
                future.channel().close().sync();
            } catch (InterruptedException e) {
                RaknetifyBungeePlugin.LOGGER.severe("Interrupted whilst closing raknetify server");
            }
        }

        channels.clear();
    }

    public static void disable() {
        if (!active) return;
        stopAll();
        active = false;
    }

}
