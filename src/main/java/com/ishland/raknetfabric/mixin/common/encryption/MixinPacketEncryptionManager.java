package com.ishland.raknetfabric.mixin.common.encryption;

import net.minecraft.network.encryption.PacketEncryptionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

@Mixin(PacketEncryptionManager.class)
public class MixinPacketEncryptionManager {

    @Redirect(method = "decrypt", at = @At(value = "INVOKE", target = "Ljavax/crypto/Cipher;update([BII[BI)I"))
    private int redirectDecrypt(Cipher instance, byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        return instance.doFinal(input, inputOffset, inputLen, output, outputOffset);
    }

    @Redirect(method = "encrypt", at = @At(value = "INVOKE", target = "Ljavax/crypto/Cipher;update([BII[B)I"))
    private int redirectEncrypt(Cipher instance, byte[] input, int inputOffset, int inputLen, byte[] output) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        return instance.doFinal(input, inputOffset, inputLen, output);
    }

}
