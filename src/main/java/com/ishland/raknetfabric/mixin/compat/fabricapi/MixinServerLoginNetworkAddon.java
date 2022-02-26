package com.ishland.raknetfabric.mixin.compat.fabricapi;

import com.ishland.raknetfabric.mixin.access.IClientConnection;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.networking.server.ServerLoginNetworkAddon")
public class MixinServerLoginNetworkAddon {

    @Shadow
    @Final
    private ClientConnection connection;

    @Shadow
    @Final
    private MinecraftServer server;

    @SuppressWarnings("DefaultAnnotationParam")
    @Dynamic("Pseudo")
    @Inject(method = "sendCompressionPacket()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V", ordinal = 0, shift = At.Shift.AFTER, remap = true), remap = false)
    private void setupDummyCompressionImmediately(CallbackInfo info) {
        ((IClientConnection) this.connection).getChannel().eventLoop().execute(() -> {
            try {
                try {
                    this.connection.setCompressionThreshold(this.server.getNetworkCompressionThreshold(), false);
                } catch (NoSuchMethodError e) {
                    System.out.println("An error occurred when starting compression, using alternative method: " + e.toString());
                    //noinspection JavaReflectionMemberAccess
                    final Method method_10760 = ClientConnection.class.getMethod("method_10760", int.class);
                    method_10760.invoke(this.connection, this.server.getNetworkCompressionThreshold());
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

}
