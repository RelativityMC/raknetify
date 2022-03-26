package com.ishland.raknetfabric.mixin.common;

import com.ishland.raknetfabric.Constants;
import com.ishland.raknetfabric.common.util.ThreadLocalUtil;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.minecraft.network.ClientConnection;
import net.minecraft.util.Lazy;
import network.ycc.raknet.RakNet;
import network.ycc.raknet.client.channel.RakNetClientChannel;
import network.ycc.raknet.client.channel.RakNetClientThreadedChannel;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetSocketAddress;

@Mixin(ClientConnection.class)
public class MixinCCConnect {

    @Shadow
    private Channel channel;

    @Shadow
    @Final
    public static Lazy<EpollEventLoopGroup> EPOLL_CLIENT_IO_GROUP;

    @Shadow
    @Final
    public static Lazy<NioEventLoopGroup> CLIENT_IO_GROUP;

    @Dynamic("method_10753 for compat")
    @Redirect(method = {"connect", "method_10753"}, at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;", remap = false), require = 0)
    private static AbstractBootstrap<Bootstrap, Channel> redirectChannel(Bootstrap instance, Class<? extends SocketChannel> aClass, InetSocketAddress address, boolean useEpoll) {
        boolean actuallyUseEpoll = Epoll.isAvailable() && useEpoll;
        return ThreadLocalUtil.isInitializingRaknet()
                ? instance.channelFactory(() -> {
                    final boolean initializingRaknetLargeMTU = ThreadLocalUtil.isInitializingRaknetLargeMTU();
                    final RakNetClientThreadedChannel channel = new RakNetClientThreadedChannel(() -> {
                        final DatagramChannel channel1 = actuallyUseEpoll ? new EpollDatagramChannel() : new NioDatagramChannel();
                        if (initializingRaknetLargeMTU)
                            channel1.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(Constants.LARGE_MTU + 512));
                        return channel1;
                    });
                    RakNet.config(channel).setMTU(initializingRaknetLargeMTU ? Constants.LARGE_MTU : Constants.DEFAULT_MTU);
                    channel.setProvidedEventLoop(actuallyUseEpoll ? EPOLL_CLIENT_IO_GROUP.get().next() : CLIENT_IO_GROUP.get().next());
                    return channel;
                })
                : instance.channel(aClass);
    }

}
