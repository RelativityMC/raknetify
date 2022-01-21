package com.ishland.raknetfabric.mixin.common;

import io.netty.channel.ChannelFuture;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Shadow @Final private NetworkSide side;

    @Redirect(method = "disconnect", at = @At(value = "INVOKE", target = "Lio/netty/channel/ChannelFuture;awaitUninterruptibly()Lio/netty/channel/ChannelFuture;"))
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

}
