package com.ishland.raknetfabric.mixin.server;

import com.ishland.raknetfabric.mixin.access.IClientConnection;
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

}
