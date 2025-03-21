/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022-2025 ishland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
