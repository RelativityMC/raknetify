package com.ishland.raknetify.fabric.mixin.common.sync;

import com.ishland.raknetify.fabric.common.connection.MultiChannellingDataCodec;
import com.ishland.raknetify.fabric.common.connection.SynchronizationLayer;
import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import io.netty.channel.Channel;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import network.ycc.raknet.RakNet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class MixinServerPlayerEntity {

    @Shadow public ServerPlayNetworkHandler networkHandler;

    @Inject(method = "teleport", at = @At(value = "NEW", target = "net/minecraft/network/packet/s2c/play/PlayerRespawnS2CPacket", shift = At.Shift.BEFORE))
    private void beforeTeleportToAnotherDimension(CallbackInfo ci) {
        final Channel channel = ((IClientConnection) this.networkHandler.connection).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("Warning: %s don't have valid channel when teleporting to another dimension, not sending sync packet".formatted(this));
            return;
        }
        if (channel.config() instanceof RakNet.Config) {
            channel.write(SynchronizationLayer.SYNC_REQUEST_OBJECT);
        }
    }

    @Inject(method = "teleport", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendPlayerStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V", shift = At.Shift.AFTER))
    private void afterTeleportToAnotherDimension(CallbackInfo ci) {
        final Channel channel = ((IClientConnection) this.networkHandler.connection).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("Warning: %s don't have valid channel when teleporting to another dimension, not starting multichannel".formatted(this));
            return;
        }
        if (channel.config() instanceof RakNet.Config) {
            channel.write(MultiChannellingDataCodec.START_MULTICHANNEL);
        }
    }

    @Inject(method = "moveToWorld", at = @At(value = "NEW", target = "net/minecraft/network/packet/s2c/play/PlayerRespawnS2CPacket", shift = At.Shift.BEFORE))
    private void beforeMoveToAnotherWorld(CallbackInfoReturnable<Entity> cir) {
        final Channel channel = ((IClientConnection) this.networkHandler.connection).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("Warning: %s don't have valid channel when teleporting to another dimension, not sending sync packet".formatted(this));
            return;
        }
        if (channel.config() instanceof RakNet.Config) {
            channel.write(SynchronizationLayer.SYNC_REQUEST_OBJECT);
        }
    }

    @Inject(method = "moveToWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendPlayerStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V", shift = At.Shift.AFTER))
    private void afterMoveToAnotherWorld(CallbackInfoReturnable<Entity> cir) {
        final Channel channel = ((IClientConnection) this.networkHandler.connection).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("Warning: %s don't have valid channel when teleporting to another dimension, not starting multichannel".formatted(this));
            return;
        }
        if (channel.config() instanceof RakNet.Config) {
            channel.write(MultiChannellingDataCodec.START_MULTICHANNEL);
        }
    }

}
