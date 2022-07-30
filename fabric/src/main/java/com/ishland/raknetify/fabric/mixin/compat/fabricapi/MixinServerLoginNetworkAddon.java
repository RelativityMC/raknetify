package com.ishland.raknetify.fabric.mixin.compat.fabricapi;

import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Dynamic("Pseudo")
    @Redirect(method = "sendCompressionPacket()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getNetworkCompressionThreshold()I"))
    private int stopCompressionIfStreamingCompressionExists(MinecraftServer server) {
        final MultiChannelingStreamingCompression compression = ((IClientConnection) this.connection).getChannel().pipeline().get(MultiChannelingStreamingCompression.class);
        if (compression != null && compression.isActive()) {
            System.out.println("Raknetify: Preventing vanilla compression as streaming compression is enabled");
            return -1;
        }
        return server.getNetworkCompressionThreshold();
    }

    @SuppressWarnings("DefaultAnnotationParam")
    @Dynamic("Pseudo")
    @Inject(
            method = "sendCompressionPacket()V",
            at = {
                    @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/Packet;Lnet/minecraft/class_7648;)V", ordinal = 0, shift = At.Shift.AFTER),
                    @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;method_10752(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V", ordinal = 0, shift = At.Shift.AFTER, remap = true)
            },
            remap = false
    )
    private void setupDummyCompressionImmediately(CallbackInfo info) {
        ((IClientConnection) this.connection).getChannel().eventLoop().execute(() -> {
            try {
                try {
                    this.connection.setCompressionThreshold(this.server.getNetworkCompressionThreshold(), false);
                } catch (NoSuchMethodError e) {
                    System.out.println("Raknetify: An error occurred when starting compression, using alternative method: " + e.toString());
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
