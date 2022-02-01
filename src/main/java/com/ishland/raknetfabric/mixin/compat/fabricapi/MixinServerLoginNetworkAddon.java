package com.ishland.raknetfabric.mixin.compat.fabricapi;

import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.networking.server.ServerLoginNetworkAddon")
public class MixinServerLoginNetworkAddon {

    @Shadow
    @Final
    private ClientConnection connection;

    @SuppressWarnings("DefaultAnnotationParam")
    @Dynamic("Pseudo")
    @Inject(method = "sendCompressionPacket()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V", ordinal = 0, shift = At.Shift.AFTER, remap = true), remap = false)
    private void setupDummyCompressionImmediately(CallbackInfo info) {
        this.connection.setCompressionThreshold(Integer.MAX_VALUE, false);
    }

}
