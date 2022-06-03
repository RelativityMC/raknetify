package com.ishland.raknetify.fabric.mixin.common.encryption;

import com.ishland.raknetify.fabric.common.connection.encryption.PacketEncryptionManagerInterface;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.encryption.PacketEncryptionManager;
import network.ycc.raknet.RakNet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

@Mixin(PacketEncryptionManager.class)
public class MixinPacketEncryptionManager implements PacketEncryptionManagerInterface {

    @Unique
    private ChannelHandlerContext ctx;

    @Override
    public void setContext(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Redirect(method = "decrypt", at = @At(value = "INVOKE", target = "Ljavax/crypto/Cipher;update([BII[BI)I"))
    private int redirectDecrypt(Cipher instance, byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        if (ctx.channel().config() instanceof RakNet.Config) {
            return instance.doFinal(input, inputOffset, inputLen, output, outputOffset);
        } else {
            return instance.update(input, inputOffset, inputLen, output, outputOffset);
        }
    }

    @Redirect(method = "encrypt", at = @At(value = "INVOKE", target = "Ljavax/crypto/Cipher;update([BII[B)I"))
    private int redirectEncrypt(Cipher instance, byte[] input, int inputOffset, int inputLen, byte[] output) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        if (ctx.channel().config() instanceof RakNet.Config) {
            return instance.doFinal(input, inputOffset, inputLen, output);
        } else {
            return instance.update(input, inputOffset, inputLen, output);
        }
    }

}
