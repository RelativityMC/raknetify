package com.ishland.raknetify.fabric.mixin.access;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.encryption.PacketEncryptionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.crypto.ShortBufferException;

@Mixin(PacketEncryptionManager.class)
public interface IPacketEncryptionManager {

    @Invoker
    ByteBuf invokeDecrypt(ChannelHandlerContext context, ByteBuf buf) throws ShortBufferException;

    @Invoker
    void invokeEncrypt(ByteBuf buf, ByteBuf result) throws ShortBufferException;

}
