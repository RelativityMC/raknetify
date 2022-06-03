package com.ishland.raknetify.fabric.mixin.common.encryption;

import com.ishland.raknetify.fabric.common.connection.encryption.PacketEncryptionManagerInterface;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.encryption.PacketEncryptionManager;
import net.minecraft.network.encryption.PacketEncryptor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketEncryptor.class)
public class MixinPacketEncryptor {

    @Shadow @Final private PacketEncryptionManager manager;

    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Lio/netty/buffer/ByteBuf;)V", at = @At("HEAD"))
    private void preEncrypt(ChannelHandlerContext ctx, ByteBuf byteBuf, ByteBuf byteBuf2, CallbackInfo ci) {
        ((PacketEncryptionManagerInterface) this.manager).setContext(ctx);
    }

}
