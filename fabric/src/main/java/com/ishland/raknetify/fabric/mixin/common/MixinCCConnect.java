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

package com.ishland.raknetify.fabric.mixin.common;

import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.RakNetConnectionUtil;
import com.ishland.raknetify.common.connection.RaknetifyEventLoops;
import com.ishland.raknetify.common.util.ThreadLocalUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.minecraft.network.ClientConnection;
import network.ycc.raknet.RakNet;
import network.ycc.raknet.client.channel.RakNetClientThreadedChannel;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.net.InetSocketAddress;

@Mixin(ClientConnection.class)
public class MixinCCConnect {

    @Dynamic("method_10753 for compat")
    @WrapOperation(method = {"connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;", "method_10753(Ljava/net/InetSocketAddress;Z)Lnet/minecraft/network/ClientConnection;", "method_10753"}, at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;group(Lio/netty/channel/EventLoopGroup;)Lio/netty/bootstrap/AbstractBootstrap;", remap = false), require = 1)
    private static AbstractBootstrap<Bootstrap, Channel> redirectGroup(Bootstrap instance, EventLoopGroup eventLoopGroup, Operation<AbstractBootstrap<Bootstrap, Channel>> original, InetSocketAddress address, boolean useEpoll) {
        return ThreadLocalUtil.isInitializingRaknet()
                ? original.call(instance, RaknetifyEventLoops.DEFAULT_CLIENT_EVENT_LOOP_GROUP.get())
                : original.call(instance, eventLoopGroup);
    }

    @Dynamic("method_10753 for compat")
    @WrapOperation(method = {"connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;", "method_10753(Ljava/net/InetSocketAddress;Z)Lnet/minecraft/network/ClientConnection;", "method_10753"}, at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;", remap = false), require = 1)
    private static AbstractBootstrap<Bootstrap, Channel> redirectChannel(Bootstrap instance, Class<? extends SocketChannel> aClass, Operation<AbstractBootstrap<Bootstrap, Channel>> original, InetSocketAddress address, boolean useEpoll) {
        boolean actuallyUseEpoll = Epoll.isAvailable() && useEpoll;
        return ThreadLocalUtil.isInitializingRaknet()
                ? instance.channelFactory(() -> {
                    final boolean initializingRaknetLargeMTU = ThreadLocalUtil.isInitializingRaknetLargeMTU();
                    final RakNetClientThreadedChannel channel = new RakNetClientThreadedChannel(() -> {
                        final DatagramChannel channel1 = actuallyUseEpoll ? new EpollDatagramChannel() : new NioDatagramChannel();
                        channel1.config().setOption(ChannelOption.IP_TOS, RakNetConnectionUtil.DEFAULT_IP_TOS);
                        if (initializingRaknetLargeMTU)
                            channel1.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(Constants.LARGE_MTU + 512).maxMessagesPerRead(128));
                        else
                            channel1.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(Constants.DEFAULT_MTU + 512).maxMessagesPerRead(128));
                        return channel1;
                    });
                    RakNet.config(channel).setMTU(initializingRaknetLargeMTU ? Constants.LARGE_MTU : Constants.DEFAULT_MTU);
                    channel.setProvidedParentEventLoop(actuallyUseEpoll ? RaknetifyEventLoops.EPOLL_CLIENT_EVENT_LOOP_GROUP.get().next() : RaknetifyEventLoops.NIO_CLIENT_EVENT_LOOP_GROUP.get().next());
                    return channel;
                })
                : original.call(instance, aClass);
    }

}
