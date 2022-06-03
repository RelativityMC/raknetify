package com.ishland.raknetify.fabric.mixin.compat.krypton;

import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.encryption.NetworkEncryptionException;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.PrivateKey;

@Mixin(ServerLoginNetworkHandler.class)
public class MixinServerLoginNetworkHandler {

    @Shadow
    @Final
    public ClientConnection connection;

    @Shadow @Final private MinecraftServer server;

    @Shadow @Final private byte[] nonce;

    @Inject(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;setupEncryption(Ljavax/crypto/Cipher;Ljavax/crypto/Cipher;)V", shift = At.Shift.AFTER))
    private void afterSetupEncryption(LoginKeyC2SPacket packet, CallbackInfo ci) throws NetworkEncryptionException {
        final ChannelPipeline pipeline = ((IClientConnection) this.connection).getChannel().pipeline();
        if (pipeline.get("decrypt").getClass().getName().equals("me.steinborn.krypton.mod.shared.network.pipeline.MinecraftCipherDecoder") &&
                pipeline.get("encrypt").getClass().getName().equals("me.steinborn.krypton.mod.shared.network.pipeline.MinecraftCipherEncoder")) {
            System.out.println("Krypton detected, applying compatibility");

            pipeline.remove("decrypt");
            pipeline.remove("encrypt");

            // TODO [VanillaCopy]
            PrivateKey privateKey = this.server.getKeyPair().getPrivate();

            SecretKey secretKey = packet.decryptSecretKey(privateKey);
            Cipher cipher = NetworkEncryptionUtils.cipherFromKey(2, secretKey);
            Cipher cipher2 = NetworkEncryptionUtils.cipherFromKey(1, secretKey);

            ((IClientConnection) this.connection).setEncrypted(false);
            this.connection.setupEncryption(cipher, cipher2);
        }
    }

}
