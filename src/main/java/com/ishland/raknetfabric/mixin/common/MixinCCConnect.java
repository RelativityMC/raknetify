package com.ishland.raknetfabric.mixin.common;

import com.ishland.raknetfabric.common.util.ThreadLocalUtil;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.minecraft.network.ClientConnection;
import network.ycc.raknet.client.channel.RakNetClientChannel;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetSocketAddress;

@Mixin(ClientConnection.class)
public class MixinCCConnect {

    @Dynamic("method_10753 for compat")
    @Redirect(method = {"connect", "method_10753"}, at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;", remap = false), require = 0)
    private static AbstractBootstrap<Bootstrap, Channel> redirectChannel(Bootstrap instance, Class<? extends SocketChannel> aClass, InetSocketAddress address, boolean useEpoll) {
        boolean actuallyUseEpoll = Epoll.isAvailable() && useEpoll;
        return ThreadLocalUtil.isInitializingRaknet()
                ? instance.channelFactory(() -> new RakNetClientChannel(actuallyUseEpoll ? EpollDatagramChannel.class : NioDatagramChannel.class))
                : instance.channel(aClass);
    }

}
