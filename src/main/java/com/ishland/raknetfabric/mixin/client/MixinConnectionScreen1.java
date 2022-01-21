package com.ishland.raknetfabric.mixin.client;

import com.ishland.raknetfabric.Constants;
import com.ishland.raknetfabric.common.connection.RaknetClientConnectionUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(targets = "net/minecraft/client/gui/screen/ConnectScreen$1")
public class MixinConnectionScreen1 extends Thread {

    @Mutable
    @Shadow
    @Final
    ServerAddress field_33737;

    @Unique
    private boolean isRaknet = false;

    @Inject(method = "<init>(Lnet/minecraft/client/gui/screen/ConnectScreen;Ljava/lang/String;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/MinecraftClient;)V", at = @At("RETURN"))
    private void onInit(ConnectScreen connectScreen, String string, ServerAddress serverAddress, MinecraftClient minecraftClient, CallbackInfo ci) {
        if (serverAddress.getAddress().startsWith(Constants.RAKNET_PREFIX)) {
            this.isRaknet = true;
            this.field_33737 = new ServerAddress(serverAddress.getAddress().substring(Constants.RAKNET_PREFIX.length()), serverAddress.getPort());
        }
    }

    @Redirect(method = "run()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetSocketAddress;Z)Lnet/minecraft/network/ClientConnection;"))
    private ClientConnection connectRaknet(InetSocketAddress address, boolean useEpoll) {
        return this.isRaknet ? RaknetClientConnectionUtil.connect(address, useEpoll) : ClientConnection.connect(address, useEpoll);
    }

}
