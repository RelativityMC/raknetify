package com.ishland.raknetify.fabric.mixin.server;

import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.util.ThreadLocalUtil;
import com.ishland.raknetify.fabric.common.netif.NetworkInterfaceListener;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Iterator;
import java.util.function.Consumer;

@Mixin(ServerNetworkIo.class)
public abstract class MixinServerNetworkIo {

    @Unique
    private static final int raknetify$portOverride = Integer.getInteger("raknetify.fabric.portOverride", -1);

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow public abstract void bind(@Nullable InetAddress address, int port) throws IOException;

    @Shadow public volatile boolean active;

    @Unique
    private Consumer<NetworkInterfaceListener.InterfaceChangeEvent> raknetify$eventListener = null;

    @Inject(method = "bind", at = @At("HEAD"))
    private void bindUdp(InetAddress address, int port, CallbackInfo ci) throws IOException {
        if (!ThreadLocalUtil.isInitializingRaknet()) {
            try {
                ThreadLocalUtil.setInitializingRaknet(true);
                final boolean hasPortOverride = raknetify$portOverride > 0 && raknetify$portOverride < 65535;
                if (address == null) {
                    for (NetworkInterface networkInterface : NetworkInterface.networkInterfaces().toList()) {
                        final Iterator<InetAddress> iterator = networkInterface.getInetAddresses().asIterator();
                        while (iterator.hasNext()) {
                            final InetAddress inetAddress = iterator.next();
                            System.out.println("Starting raknetify server on interface %s address %s".formatted(networkInterface.getName(), inetAddress));
                            bind(inetAddress, hasPortOverride ? raknetify$portOverride : port);
                        }
                    }

                    if (this.raknetify$eventListener == null) {
                        this.raknetify$eventListener = event -> {
                            if (!this.active) {
                                NetworkInterfaceListener.removeListener(this.raknetify$eventListener);
                                return;
                            }
                            if (event.added()) {
                                try {
                                    ThreadLocalUtil.setInitializingRaknet(true);
                                    final Iterator<InetAddress> iterator = event.networkInterface().getInetAddresses().asIterator();
                                    while (iterator.hasNext()) {
                                        final InetAddress inetAddress = iterator.next();
                                        System.out.println("Starting raknetify server on interface %s address %s".formatted(event.networkInterface().getName(), inetAddress));
                                        bind(inetAddress, hasPortOverride ? raknetify$portOverride : port);
                                    }
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                } finally {
                                    ThreadLocalUtil.setInitializingRaknet(false);
                                }
                            }
                        };
                        NetworkInterfaceListener.addListener(this.raknetify$eventListener);
                    }
                } else {
                    bind(address, hasPortOverride ? raknetify$portOverride : port);
                }
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
                    channel.config().setOption(ChannelOption.SO_REUSEADDR, true);
                    channel.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(Constants.LARGE_MTU + 512));
                    return channel;
                }))
                : instance.channel(aClass);
    }

}
