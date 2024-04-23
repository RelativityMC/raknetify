/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022-2023 ishland
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
import com.ishland.raknetify.common.util.DebugUtil;
import com.ishland.raknetify.fabric.common.util.NetworkStates;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.listener.PacketListener;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;

@Mixin(ClientConnection.class)
public abstract class MixinClientConnection {

    @Shadow
    private Channel channel;

    @Shadow public abstract SocketAddress getAddress();

    @Shadow private SocketAddress address;

    @Shadow public abstract NetworkSide getSide();

    @Shadow private volatile @Nullable PacketListener packetListener;
    @Unique
    private volatile boolean isClosing = false;

    @Redirect(method = "disconnect", at = @At(value = "INVOKE", target = "Lio/netty/channel/ChannelFuture;awaitUninterruptibly()Lio/netty/channel/ChannelFuture;", remap = false))
    private ChannelFuture noDisconnectWait(ChannelFuture instance) {
        isClosing = true;
//        if (instance.channel().eventLoop().inEventLoop()) {
//            return instance; // no-op
//        } else {
//            return instance.awaitUninterruptibly();
//        }
        return instance;
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

    @Inject(method = "exceptionCaught", at = @At("HEAD"))
    private void onExceptionCaught(ChannelHandlerContext context, Throwable ex, CallbackInfo ci) {
        if (ex instanceof ClosedChannelException) return;
        if (Constants.DEBUG) {
            System.err.println("Exception caught for connection %s".formatted(this.channel));
            for (String s : DebugUtil.printChannelDetails(this.channel).split("\n")) {
                System.err.println("  " + s);
            }
            ex.printStackTrace();
        } else {
            final NetworkPhase state;
            Object handler = this.channel.attr(AttributeKey.valueOf("protocol")).get(); // pre-1.20.2
            if (handler != null) {
                if (handler instanceof NetworkPhase state1) {
                    state = state1;
                } else {
                    System.err.println("Unknown handler type: " + handler.getClass().getName());
                    state = null;
                }
            } else {
                final PacketListener packetListener1 = this.packetListener;
                state = packetListener1 != null ? packetListener1.getPhase() : null;
            }
            if (state != null && state != NetworkPhase.HANDSHAKING) {
                System.err.println(String.format("%s %s %s", this.address, NetworkStates.getName(state), ex.toString()));
            }
        }
    }

}
