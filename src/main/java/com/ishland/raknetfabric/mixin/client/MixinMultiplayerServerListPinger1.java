package com.ishland.raknetfabric.mixin.client;

import com.ishland.raknetfabric.mixin.access.IClientConnection;
import io.netty.channel.Channel;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ClientQueryPacketListener;
import network.ycc.raknet.RakNet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(targets = "net/minecraft/client/network/MultiplayerServerListPinger$1")
public abstract class MixinMultiplayerServerListPinger1 implements ClientQueryPacketListener {

    @Shadow
    @Final
    ClientConnection field_3774; // synthetic

    @Shadow
    @Final
    ServerInfo field_3776; // synthetic

    @Inject(method = "onResponse(Lnet/minecraft/network/packet/s2c/query/QueryResponseS2CPacket;)V", at = @At("RETURN"))
    private void setPingImmediately(CallbackInfo ci) {
        final Channel channel = ((IClientConnection) field_3774).getChannel();
        if (channel.config() instanceof RakNet.Config config) {
            field_3776.ping = config.getRTTNanos() / 1_000_000;
        }
    }

    @Redirect(method = "onDisconnected(Lnet/minecraft/text/Text;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/MultiplayerServerListPinger;ping(Ljava/net/InetSocketAddress;Lnet/minecraft/client/network/ServerInfo;)V"))
    private void noPingRaknet(MultiplayerServerListPinger instance, InetSocketAddress address, ServerInfo info) {
        // no-op
    }

}
