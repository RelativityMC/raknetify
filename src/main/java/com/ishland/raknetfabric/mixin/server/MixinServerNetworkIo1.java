package com.ishland.raknetfabric.mixin.server;

import com.ishland.raknetfabric.common.connection.RaknetConnectionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/server/ServerNetworkIo$1")
public abstract class MixinServerNetworkIo1 extends ChannelInitializer<Channel> {

    @Inject(method = "initChannel(Lio/netty/channel/Channel;)V", at = @At("HEAD"))
    private void onChannelInit(Channel channel, CallbackInfo ci) {
        RaknetConnectionUtil.initChannel(channel);
    }

    @Inject(method = "initChannel(Lio/netty/channel/Channel;)V", at = @At("RETURN"))
    private void postChannelInit(Channel channel, CallbackInfo ci) {
        RaknetConnectionUtil.postInitChannel(channel, false);
    }

}
