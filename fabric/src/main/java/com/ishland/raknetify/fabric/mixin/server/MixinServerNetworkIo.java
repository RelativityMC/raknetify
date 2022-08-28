/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022 ishland
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

package com.ishland.raknetify.fabric.mixin.server;

import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.RakNetConnectionUtil;
import com.ishland.raknetify.common.connection.RaknetifyEventLoops;
import com.ishland.raknetify.common.util.ThreadLocalUtil;
import com.ishland.raknetify.common.util.NetworkInterfaceListener;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
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
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

@Mixin(ServerNetworkIo.class)
public abstract class MixinServerNetworkIo {

    @Unique
    private static final int raknetify$portOverride = Integer.getInteger("raknetify.fabric.portOverride", -1);

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    public abstract void bind(@Nullable InetAddress address, int port) throws IOException;

    @Shadow
    public volatile boolean active;

    @Shadow
    @Final
    private List<ChannelFuture> channels;
    @Unique
    private Consumer<NetworkInterfaceListener.InterfaceAddressChangeEvent> raknetify$eventListener = null;

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
                            System.out.println("Starting raknetify server on %s".formatted(inetAddress));
                            bind(inetAddress, hasPortOverride ? raknetify$portOverride : port);
                        }
                    }

                    if (this.raknetify$eventListener == null) {
                        this.raknetify$eventListener = event -> {
                            if (!this.active) {
                                NetworkInterfaceListener.removeListener(this.raknetify$eventListener);
                                return;
                            }
                            try {
                                ThreadLocalUtil.setInitializingRaknet(true);
                                final InetAddress inetAddress = event.address();
                                if (event.added()) {
                                    System.out.println("Starting raknetify server on %s".formatted(inetAddress));
                                    try {
                                        bind(inetAddress, hasPortOverride ? raknetify$portOverride : port);
                                    } catch (IOException t) {
                                        System.out.println("**** FAILED TO BIND TO PORT! %s".formatted(t.getMessage()));
                                    } catch (Throwable t) {
                                        t.printStackTrace();
                                    }
                                } else {
                                    synchronized (this.channels) {
                                        for (Iterator<ChannelFuture> iter = this.channels.iterator(); iter.hasNext(); ) {
                                            ChannelFuture channel = iter.next();
                                            final SocketAddress socketAddress = channel.channel().localAddress();
                                            if (socketAddress instanceof InetSocketAddress channelAddress) {
                                                if (inetAddress.equals(channelAddress.getAddress())) {
                                                    System.out.println("Stopping raknetify server on %s".formatted(inetAddress));
                                                    channel.channel().close();
                                                    iter.remove();
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Throwable e) {
                                e.printStackTrace();
                            } finally {
                                ThreadLocalUtil.setInitializingRaknet(false);
                            }
                        };
                        NetworkInterfaceListener.addListener(event -> this.server.submit(() -> raknetify$eventListener.accept(event)));
                    }
                } else {
                    System.out.println("Starting raknetify server on %s".formatted(address));
                    bind(address, hasPortOverride ? raknetify$portOverride : port);
                }
            } finally {
                ThreadLocalUtil.setInitializingRaknet(false);
            }
        }
    }

    @Redirect(method = "bind", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/ServerBootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/ServerBootstrap;", remap = false))
    private ServerBootstrap redirectGroup(ServerBootstrap instance, EventLoopGroup group) {
        final boolean useEpoll = Epoll.isAvailable() && this.server.isUsingNativeTransport();
        return ThreadLocalUtil.isInitializingRaknet()
                ? instance.group(useEpoll ? RaknetifyEventLoops.EPOLL_EVENT_LOOP_GROUP.get() : RaknetifyEventLoops.NIO_EVENT_LOOP_GROUP.get())
                : instance.group(group);
    }

    @Redirect(method = "bind", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/ServerBootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;", remap = false))
    private AbstractBootstrap<ServerBootstrap, ServerChannel> redirectChannel(ServerBootstrap instance, Class<? extends ServerSocketChannel> aClass) {
        final boolean useEpoll = Epoll.isAvailable() && this.server.isUsingNativeTransport();
        return ThreadLocalUtil.isInitializingRaknet()
                ? instance.channelFactory(() -> new RakNetServerChannel(() -> {
            final DatagramChannel channel = useEpoll ? new EpollDatagramChannel() : new NioDatagramChannel();
            channel.config().setOption(ChannelOption.SO_REUSEADDR, true);
            channel.config().setOption(ChannelOption.IP_TOS, RakNetConnectionUtil.DEFAULT_IP_TOS);
            channel.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(Constants.LARGE_MTU + 512).maxMessagesPerRead(128));
            return channel;
        }))
                : instance.channel(aClass);
    }

}
