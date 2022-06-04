package com.ishland.raknetify.fabric.mixin.common;

import com.ishland.raknetify.fabric.common.connection.RakNetFabricConnectionUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/network/ClientConnection$1")
public abstract class MixinClientConnection1 extends ChannelInitializer<Channel> {

    @Inject(method = "initChannel(Lio/netty/channel/Channel;)V", at = @At("HEAD"))
    private void onChannelInit(Channel channel, CallbackInfo ci) {
        RakNetFabricConnectionUtil.initChannel(channel);
    }

    @Inject(method = "initChannel(Lio/netty/channel/Channel;)V", at = @At("RETURN"))
    private void postChannelInit(Channel channel, CallbackInfo ci) {
        RakNetFabricConnectionUtil.postInitChannel(channel, true);
    }

}
