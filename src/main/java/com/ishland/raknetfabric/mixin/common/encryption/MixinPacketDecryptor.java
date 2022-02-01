package com.ishland.raknetfabric.mixin.common.encryption;

import com.ishland.raknetfabric.common.connection.encryption.PacketEncryptionManagerInterface;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.encryption.PacketDecryptor;
import net.minecraft.network.encryption.PacketEncryptionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PacketDecryptor.class)
public class MixinPacketDecryptor {

    @Shadow @Final private PacketEncryptionManager manager;

    @Inject(method = "decode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V", at = @At("HEAD"))
    private void preDecrypt(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list, CallbackInfo ci) {
        ((PacketEncryptionManagerInterface) this.manager).setContext(ctx);
    }

}
