package com.ishland.raknetify.fabric.mixin.server;

import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Dynamic;
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
            System.out.println("Raknetify: Preventing vanilla compression as streaming compression is enabled");
            return -1;
        }
        return server.getNetworkCompressionThreshold();
    }

}
