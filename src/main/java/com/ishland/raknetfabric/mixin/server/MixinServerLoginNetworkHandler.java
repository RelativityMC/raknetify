package com.ishland.raknetfabric.mixin.server;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(ServerLoginNetworkHandler.class)
public class MixinServerLoginNetworkHandler {

    @Shadow @Final public ClientConnection connection;

    @Inject(method = "acceptPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V", ordinal = 0, shift = At.Shift.AFTER))
    private void setupDummyCompressionImmediately(CallbackInfo ci) throws Throwable {
        try {
            this.connection.setCompressionThreshold(Integer.MAX_VALUE, false);
        } catch (NoSuchMethodError e) {
            System.out.println("An error occurred when starting compression, using alternative method");
            //noinspection JavaReflectionMemberAccess
            final Method method_10760 = ClientConnection.class.getMethod("method_10760", int.class);
            method_10760.invoke(this.connection, Integer.MAX_VALUE);
        }
    }

}
