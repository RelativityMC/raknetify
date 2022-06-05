package com.ishland.raknetify.velocity.init;

import com.google.common.base.Preconditions;
import com.ishland.raknetify.common.Constants;
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
import network.ycc.raknet.server.channel.RakNetServerChannel;

import java.lang.reflect.Method;

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

    private static ChannelFuture channel = null;

    public static void start(ListenerBoundEvent evt) {
        if (evt.getListenerType() != ListenerType.MINECRAFT) return;
        Preconditions.checkArgument(RaknetifyVelocityPlugin.PROXY instanceof VelocityServer);
        Preconditions.checkState(channel == null, "Raknetify Server is already running");
        try {
            final ConnectionManager cm = (ConnectionManager) accessible(VelocityServer.class.getDeclaredField("cm")).get(RaknetifyVelocityPlugin.PROXY);
            final Object transportType = accessible(ConnectionManager.class.getDeclaredField("transportType")).get(cm);
            final ChannelFactory<? extends DatagramChannel> datagramChannelFactory =
                    (ChannelFactory<? extends DatagramChannel>) accessible(Class.forName("com.velocitypowered.proxy.network.TransportType").getDeclaredField("datagramChannelFactory")).get(transportType);
            final EventLoopGroup bossGroup = (EventLoopGroup) accessible(ConnectionManager.class.getDeclaredField("bossGroup")).get(cm);
            final EventLoopGroup workerGroup = (EventLoopGroup) accessible(ConnectionManager.class.getDeclaredField("workerGroup")).get(cm);

            channel = new ServerBootstrap()
                    .channelFactory(() -> new RakNetServerChannel(() -> {
                        final DatagramChannel channel = datagramChannelFactory.newChannel();
                        channel.config().setOption(ChannelOption.IP_TOS, 0x18);
                        channel.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(Constants.LARGE_MTU + 512));
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
                    .localAddress(evt.getAddress())
                    .bind()
                    .syncUninterruptibly();

            RaknetifyVelocityPlugin.LOGGER.info("Raknetify server started on {}", channel.channel().localAddress());
        } catch (Throwable t) {
            throw new RuntimeException("Failed to start Raknetify server", t);
        }
    }

    public static void stop(ListenerCloseEvent evt) {
        if (evt.getListenerType() != ListenerType.MINECRAFT) return;
        Preconditions.checkArgument(RaknetifyVelocityPlugin.PROXY instanceof VelocityServer);
        Preconditions.checkState(channel != null, "Raknetify Server is not running");
        RaknetifyVelocityPlugin.LOGGER.info("Closing Raknetify server {}", channel.channel().localAddress());
        try {
            channel.channel().close().sync();
        } catch (InterruptedException e) {
            RaknetifyVelocityPlugin.LOGGER.error("Interrupted whilst closing raknetify server");
        }
        channel = null;
    }

}
