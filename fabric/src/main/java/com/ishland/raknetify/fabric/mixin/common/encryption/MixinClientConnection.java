package com.ishland.raknetify.fabric.mixin.common.encryption;

import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.common.connection.MultiChannellingEncryption;
import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.encryption.PacketDecryptor;
import net.minecraft.network.encryption.PacketEncryptionManager;
import net.minecraft.network.encryption.PacketEncryptor;
import network.ycc.raknet.RakNet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.crypto.Cipher;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Shadow
    private boolean encrypted;

    @Shadow
    private Channel channel;

    @Inject(method = "setupEncryption", at = @At("HEAD"), cancellable = true)
    public void beforeSetupEncryption(Cipher decryptionCipher, Cipher encryptionCipher, CallbackInfo ci) {
        if (this.channel.config() instanceof RakNet.Config) {
            ci.cancel();
            this.encrypted = true;
            try {
                this.channel.pipeline().remove("decrypt");
                this.channel.pipeline().remove("encrypt");
            } catch (Throwable ignored) {
            }
            try {
                this.channel.pipeline().remove(MultiChannellingEncryption.NAME);
            } catch (Throwable ignored) {
            }
            this.channel.pipeline().addBefore(MultiChannelingStreamingCompression.NAME, MultiChannellingEncryption.NAME,
                    new MultiChannellingEncryption(decryptionCipher, encryptionCipher));
        }
    }

}
