package com.ishland.raknetfabric.mixin.server;

import com.ishland.raknetfabric.Constants;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import network.ycc.raknet.RakNet;
import network.ycc.raknet.pipeline.UserDataCodec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/server/ServerNetworkIo$1")
public abstract class MixinServerNetworkIo1 extends ChannelInitializer<Channel> {

    @Inject(method = "initChannel(Lio/netty/channel/Channel;)V", at = @At("HEAD"))
    private void onChannelInit(Channel channel, CallbackInfo ci) {
        if (channel.config() instanceof RakNet.Config config) {
            config.setMaxQueuedBytes(Constants.MAX_QUEUED_SIZE);
            channel.pipeline().addLast("raknet_backend", new UserDataCodec(Constants.RAKNET_PACKET_ID));
        }
    }

}
