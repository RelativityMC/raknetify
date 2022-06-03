package com.ishland.raknetify.fabric.mixin.server;

import com.ishland.raknetify.fabric.Constants;
import com.ishland.raknetify.fabric.common.util.ThreadLocalUtil;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerNetworkIo;
import network.ycc.raknet.server.channel.RakNetServerChannel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.net.InetAddress;

@Mixin(ServerNetworkIo.class)
public abstract class MixinServerNetworkIo {

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow public abstract void bind(@Nullable InetAddress address, int port) throws IOException;

    @Inject(method = "bind", at = @At("HEAD"))
    private void bindUdp(InetAddress address, int port, CallbackInfo ci) throws IOException {
        if (!ThreadLocalUtil.isInitializingRaknet()) {
            try {
                ThreadLocalUtil.setInitializingRaknet(true);
                bind(address, port);
            } finally {
                ThreadLocalUtil.setInitializingRaknet(false);
            }
        }
    }

    @Redirect(method = "bind", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/ServerBootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;", remap = false))
    private AbstractBootstrap<ServerBootstrap, ServerChannel> redirectChannel(ServerBootstrap instance, Class<? extends ServerSocketChannel> aClass) {
        final boolean useEpoll = Epoll.isAvailable() && this.server.isUsingNativeTransport();
        return ThreadLocalUtil.isInitializingRaknet()
                ? instance.channelFactory(() -> new RakNetServerChannel(() -> {
                    final DatagramChannel channel = useEpoll ? new EpollDatagramChannel() : new NioDatagramChannel();
                    channel.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(Constants.LARGE_MTU + 512));
                    return channel;
                }))
                : instance.channel(aClass);
    }

}
