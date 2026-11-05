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

package com.ishland.raknetify.fabric.mixin.compat.krypton;

import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import network.ycc.raknet.RakNet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.PrivateKey;

@Mixin(ServerLoginPacketListenerImpl.class)
public class MixinServerLoginNetworkHandler {

    @Shadow
    @Final
    public Connection connection;

    @Shadow @Final private MinecraftServer server;

    @Shadow @Final private byte[] challenge;

    @Inject(method = "handleKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setEncryptionKey(Ljavax/crypto/Cipher;Ljavax/crypto/Cipher;)V", shift = At.Shift.AFTER))
    private void afterSetupEncryption(ServerboundKeyPacket packet, CallbackInfo ci) throws CryptException {
        final ChannelPipeline pipeline = ((IClientConnection) this.connection).getChannel().pipeline();
        if (((IClientConnection) this.connection).getChannel().config() instanceof RakNet.Config) {
            final ChannelHandler decrypt = pipeline.get("decrypt");
            final ChannelHandler encrypt = pipeline.get("encrypt");
            if (decrypt != null && (decrypt.getClass().getName().equals("me.steinborn.krypton.mod.shared.network.pipeline.MinecraftCipherDecoder")) &&
                    encrypt != null && (encrypt.getClass().getName().equals("me.steinborn.krypton.mod.shared.network.pipeline.MinecraftCipherEncoder"))) {
                System.out.println("Raknetify: Krypton detected, applying compatibility");

                pipeline.remove("decrypt");
                pipeline.remove("encrypt");

                // TODO [VanillaCopy]
                PrivateKey privateKey = this.server.getKeyPair().getPrivate();

                SecretKey secretKey = packet.getSecretKey(privateKey);
                Cipher cipher = Crypt.getCipher(2, secretKey);
                Cipher cipher2 = Crypt.getCipher(1, secretKey);

                this.connection.setEncryptionKey(cipher, cipher2);
            }
        }
    }

}
