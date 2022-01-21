package com.ishland.raknetfabric.mixin.common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import network.ycc.raknet.RakNet;
import network.ycc.raknet.channel.DatagramChannelProxy;
import network.ycc.raknet.client.channel.RakNetClientChannel;
import network.ycc.raknet.server.channel.RakNetChildChannel;
import network.ycc.raknet.server.channel.RakNetServerChannel;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private NetworkSide side;

    @Redirect(method = "disconnect", at = @At(value = "INVOKE", target = "Lio/netty/channel/ChannelFuture;awaitUninterruptibly()Lio/netty/channel/ChannelFuture;"))
    private ChannelFuture noDisconnectWait(ChannelFuture instance) {
        return instance; // no-op
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
