package com.ishland.raknetfabric.mixin.server;

import com.ishland.raknetfabric.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetfabric.mixin.access.IClientConnection;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(value = ServerLoginNetworkHandler.class, priority = 900)
public class MixinServerLoginNetworkHandler {

    @Shadow @Final public ClientConnection connection;

    @Shadow @Final private MinecraftServer server;

    @Redirect(method = "acceptPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getNetworkCompressionThreshold()I"), require = 0)
    private int stopCompressionIfStreamingCompressionExists(MinecraftServer server) {
        final MultiChannelingStreamingCompression compression = ((IClientConnection) this.connection).getChannel().pipeline().get(MultiChannelingStreamingCompression.class);
        if (compression != null && compression.isActive()) {
            System.out.println("Preventing vanilla compression as streaming compression is enabled");
            return -1;
        }
        return server.getNetworkCompressionThreshold();
    }

    @Inject(method = "acceptPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V", ordinal = 0, shift = At.Shift.AFTER))
    private void setupDummyCompressionImmediately(CallbackInfo ci) throws Throwable {
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
