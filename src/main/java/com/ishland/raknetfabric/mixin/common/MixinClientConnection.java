package com.ishland.raknetfabric.mixin.common;

import com.ishland.raknetfabric.common.util.ThreadLocalUtil;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import network.ycc.raknet.client.channel.RakNetClientChannel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(ClientConnection.class)
public abstract class MixinClientConnection {

    @Shadow
    private Channel channel;

    @Unique
    private volatile boolean isClosing = false;

    @Redirect(method = "disconnect", at = @At(value = "INVOKE", target = "Lio/netty/channel/ChannelFuture;awaitUninterruptibly()Lio/netty/channel/ChannelFuture;", remap = false))
    private ChannelFuture noDisconnectWait(ChannelFuture instance) {
        isClosing = true;
        if (instance.channel().eventLoop().inEventLoop()) {
            return instance; // no-op
        } else {
            return instance.awaitUninterruptibly();
        }
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lio/netty/channel/Channel;isOpen()Z", remap = false))
    private boolean redirectIsOpen(Channel instance) {
        return this.channel != null && (this.channel.isOpen() && !this.isClosing);
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

    @Inject(method = "exceptionCaught", at = @At("HEAD"))
    private void onExceptionCaught(ChannelHandlerContext context, Throwable ex, CallbackInfo ci) {
        System.out.println(ex.toString());
        ex.printStackTrace();
    }

}
