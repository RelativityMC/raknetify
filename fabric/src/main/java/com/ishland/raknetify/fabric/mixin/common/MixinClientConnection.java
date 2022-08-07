package com.ishland.raknetify.fabric.mixin.common;

import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.util.DebugUtil;
import com.ishland.raknetify.fabric.common.util.NetworkStates;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
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

    @Shadow protected abstract NetworkState getState();

    @Shadow private SocketAddress address;
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
            System.err.println("Exception caught for connection %s".formatted(this.getAddress()));
            System.err.println(DebugUtil.printChannelDetails(this.channel));
            ex.printStackTrace();
        } else if (this.getState() != null && this.getState() != NetworkState.HANDSHAKING) {
            System.err.println(String.format("%s %s %s", this.address, NetworkStates.getName(this.getState()), ex.toString()));
        }
    }

}
