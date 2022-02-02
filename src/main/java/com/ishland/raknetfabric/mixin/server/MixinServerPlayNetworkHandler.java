package com.ishland.raknetfabric.mixin.server;

import com.ishland.raknetfabric.mixin.access.IClientConnection;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import network.ycc.raknet.RakNet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {

    @Shadow
    @Final
    public ClientConnection connection;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;disconnect(Lnet/minecraft/text/Text;)V"))
    private void stopTimeoutPlayers(ServerPlayNetworkHandler instance, Text reason) {
        if (reason instanceof TranslatableText translatableText &&
                ((IClientConnection) this.connection).getChannel().config() instanceof RakNet.Config &&
                translatableText.getKey().hashCode() == "disconnect.timeout".hashCode()) {
            return;
        }
        instance.disconnect(reason);
    }

    @Redirect(method = "onKeepAlive", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;disconnect(Lnet/minecraft/text/Text;)V"))
    private void stopTimeoutPlayersOnKeepAlive(ServerPlayNetworkHandler instance, Text reason) {
        if (reason instanceof TranslatableText translatableText &&
                ((IClientConnection) this.connection).getChannel().config() instanceof RakNet.Config &&
                translatableText.getKey().hashCode() == "disconnect.timeout".hashCode()) {
            return;
        }
        instance.disconnect(reason);
    }


}
