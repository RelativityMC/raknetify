package com.ishland.raknetify.fabric.mixin.client;

import com.ishland.raknetify.fabric.common.connection.RakNetFabricMultiChannelCodec;
import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import io.netty.channel.Channel;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Shadow @Final private ClientConnection connection;

    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void postGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        final Channel channel = ((IClientConnection) this.connection).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("Raknetify: Warning: %s don't have valid channel when logged in, not sending sync packet".formatted(this));
            return;
        }
        channel.eventLoop().execute(() -> channel.write(RakNetFabricMultiChannelCodec.SIGNAL_START_MULTICHANNEL));
    }

}
