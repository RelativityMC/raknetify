package com.ishland.raknetify.fabric.mixin.client;

import com.ishland.raknetify.fabric.common.connection.RakNetClientConnectionUtil;
import com.ishland.raknetify.fabric.common.util.PrefixUtil;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetSocketAddress;

@Mixin(MultiplayerServerListPinger.class)
public abstract class MixinMultiplayerServerListPinger {

    @Redirect(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ServerAddress;parse(Ljava/lang/String;)Lnet/minecraft/client/network/ServerAddress;"))
    private ServerAddress modifyRaknetAddress(String address) {
        final PrefixUtil.Info info = PrefixUtil.getInfo(address);
        return ServerAddress.parse(info.stripped());
    }

    @Redirect(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetSocketAddress;Z)Lnet/minecraft/network/ClientConnection;"))
    private ClientConnection redirectConnect(InetSocketAddress address, boolean useEpoll, ServerInfo entry, Runnable runnable) {
        final PrefixUtil.Info info = PrefixUtil.getInfo(entry.address);
        return info.useRakNet() ? RakNetClientConnectionUtil.connect(address, useEpoll, info.largeMTU()) : ClientConnection.connect(address, useEpoll);
    }

}
