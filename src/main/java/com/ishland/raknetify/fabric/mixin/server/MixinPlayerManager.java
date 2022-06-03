package com.ishland.raknetify.fabric.mixin.server;

import com.ishland.raknetify.fabric.common.connection.MultiChannellingDataCodec;
import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import network.ycc.raknet.RakNet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {

    @Inject(method = "onPlayerConnect", at = @At("HEAD"))
    private void onJoin(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        if (((IClientConnection) connection).getChannel().config() instanceof RakNet.Config config) {
            System.out.println(String.format("%s logged in via RakNet, mtu %d", player.getName().getString(), config.getMTU()));
        }
    }

    @Inject(method = "onPlayerConnect", at = @At("RETURN"))
    private void postJoin(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        final Channel channel = ((IClientConnection) connection).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("Warning: %s don't have valid channel when logged in, not sending sync packet".formatted(this));
            return;
        }
        channel.eventLoop().execute(() -> channel.write(MultiChannellingDataCodec.START_MULTICHANNEL));
    }

}
