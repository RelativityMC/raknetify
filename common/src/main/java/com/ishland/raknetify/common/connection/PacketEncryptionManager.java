package com.ishland.raknetify.common.connection;

import io.netty.buffer.ByteBuf;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;

// TODO [VanillaCopy] from fabric
public class PacketEncryptionManager {
    private final Cipher cipher;
    private byte[] conversionBuffer = new byte[0];
    private byte[] encryptionBuffer = new byte[0];

    protected PacketEncryptionManager(Cipher cipher) {
        this.cipher = cipher;
    }

    private byte[] toByteArray(ByteBuf buf) {
        int i = buf.readableBytes();
        if (this.conversionBuffer.length < i) {
            this.conversionBuffer = new byte[i];
        }

        buf.readBytes(this.conversionBuffer, 0, i);
        return this.conversionBuffer;
    }

    public void doWork(ByteBuf buf, ByteBuf result) throws GeneralSecurityException {
        int i = buf.readableBytes();
        byte[] bs = this.toByteArray(buf);
        int outputSize = this.cipher.getOutputSize(i);
        if (this.encryptionBuffer.length < outputSize) {
            this.encryptionBuffer = new byte[outputSize];
        }

        result.writeBytes(this.encryptionBuffer, 0, this.cipher.doFinal(bs, 0, i, this.encryptionBuffer));
    }
}

