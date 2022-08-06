package com.ishland.raknetify.fabric.mixin.client;

import com.ishland.raknetify.fabric.common.connection.RakNetClientConnectionUtil;
import com.ishland.raknetify.common.util.PrefixUtil;
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

    @Unique
    private boolean raknetLargeMTU = false;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        final PrefixUtil.Info info = PrefixUtil.getInfo(this.field_33737.getAddress());
        if (info.useRakNet()) {
            this.isRaknet = true;
            this.raknetLargeMTU = info.largeMTU();
            this.field_33737 = new ServerAddress(info.stripped(), this.field_33737.getPort());
        }
    }

    @Redirect(method = "run()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetSocketAddress;Z)Lnet/minecraft/network/ClientConnection;"))
    private ClientConnection connectRaknet(InetSocketAddress address, boolean useEpoll) {
        return this.isRaknet ? RakNetClientConnectionUtil.connect(address, useEpoll, this.raknetLargeMTU) : ClientConnection.connect(address, useEpoll);
    }

}
