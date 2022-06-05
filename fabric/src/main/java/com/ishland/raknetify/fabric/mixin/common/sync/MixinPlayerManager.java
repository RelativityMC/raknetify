package com.ishland.raknetify.fabric.mixin.common.sync;

import com.ishland.raknetify.fabric.common.connection.RakNetFabricMultiChannelCodec;
import com.ishland.raknetify.common.connection.SynchronizationLayer;
import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import io.netty.channel.Channel;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import network.ycc.raknet.RakNet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {

    @Inject(method = "respawnPlayer", at = @At(value = "NEW", target = "net/minecraft/network/packet/s2c/play/PlayerRespawnS2CPacket", shift = At.Shift.BEFORE))
    private void beforeMoveToAnotherWorld(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> cir) {
        final Channel channel = ((IClientConnection) player.networkHandler.connection).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("Raknetify: Warning: %s don't have valid channel when teleporting to another dimension, not sending sync packet".formatted(this));
            return;
        }
        if (channel.config() instanceof RakNet.Config) {
            channel.write(SynchronizationLayer.SYNC_REQUEST_OBJECT);
        }
    }

    @Inject(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;onSpawn()V", shift = At.Shift.AFTER))
    private void afterMoveToAnotherWorld(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> cir) {
        final Channel channel = ((IClientConnection) player.networkHandler.connection).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("Raknetify: Warning: %s don't have valid channel when teleporting to another dimension, not starting multichannel".formatted(this));
            return;
        }
        if (channel.config() instanceof RakNet.Config) {
            channel.write(RakNetFabricMultiChannelCodec.SIGNAL_START_MULTICHANNEL);


        }
    }
}
