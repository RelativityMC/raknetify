package com.ishland.raknetfabric.mixin.server;

import com.ishland.raknetfabric.mixin.access.IClientConnection;
import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import network.ycc.raknet.RakNet;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {

    @Shadow
    @Final
    public ClientConnection connection;

    @Shadow public ServerPlayerEntity player;

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

    @Redirect(method = "onKeepAlive", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerPlayerEntity;pingMilliseconds:I", opcode = Opcodes.PUTFIELD))
    private void redirectPingStoring(ServerPlayerEntity instance, int value) {
        // no-op
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        final Channel channel = ((IClientConnection) this.connection).getChannel();
        if (channel != null && channel.config() instanceof RakNet.Config config) {
            this.player.pingMilliseconds = (int) ((config.getRTTNanos() + config.getRTTStdDevNanos()) / 1_000_000);
        }
    }

}
