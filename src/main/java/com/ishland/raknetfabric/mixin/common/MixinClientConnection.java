package com.ishland.raknetfabric.mixin.common;

import com.ishland.raknetfabric.common.util.ThreadLocalUtil;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import network.ycc.raknet.client.channel.RakNetClientChannel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetSocketAddress;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Redirect(method = "disconnect", at = @At(value = "INVOKE", target = "Lio/netty/channel/ChannelFuture;awaitUninterruptibly()Lio/netty/channel/ChannelFuture;", remap = false))
    private ChannelFuture noDisconnectWait(ChannelFuture instance) {
        if (instance.channel().eventLoop().inEventLoop()) {
            return instance; // no-op
        } else {
            return instance.awaitUninterruptibly();
        }
    }

//    @Inject(method = "channelActive", at = @At("HEAD"))
//    private void onChannelActive(ChannelHandlerContext ctx, CallbackInfo ci) {
//        final Channel channel = ctx.channel();
//        if (channel.config() instanceof RakNet.Config config) {
//            if (this.side == NetworkSide.SERVERBOUND) {
//                System.out.println(String.format("RakNet connection from %s, mtu %d", channel.remoteAddress(), config.getMTU()));
//            } else if (this.side == NetworkSide.CLIENTBOUND) {
//                System.out.println(String.format("RakNet conncted to %s, mtu %d", channel.remoteAddress(), config.getMTU()));
//            }
//        }
//    }

    @Redirect(method = "connect", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;", remap = false))
    private static AbstractBootstrap<Bootstrap, Channel> redirectChannel(Bootstrap instance, Class<? extends SocketChannel> aClass, InetSocketAddress address, boolean useEpoll) {
        boolean actuallyUseEpoll = Epoll.isAvailable() && useEpoll;
        return ThreadLocalUtil.isInitializingRaknet()
                ? instance.channelFactory(() -> new RakNetClientChannel(actuallyUseEpoll ? EpollDatagramChannel.class : NioDatagramChannel.class))
                : instance.channel(aClass);
    }

}
