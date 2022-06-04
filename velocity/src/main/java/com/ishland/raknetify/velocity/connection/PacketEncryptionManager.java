package com.ishland.raknetify.velocity.connection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import javax.crypto.Cipher;
import javax.crypto.ShortBufferException;

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

    public ByteBuf decrypt(ChannelHandlerContext context, ByteBuf buf) throws ShortBufferException {
        int i = buf.readableBytes();
        byte[] bs = this.toByteArray(buf);
        ByteBuf byteBuf = context.alloc().heapBuffer(this.cipher.getOutputSize(i));
        byteBuf.writerIndex(this.cipher.update(bs, 0, i, byteBuf.array(), byteBuf.arrayOffset()));
        return byteBuf;
    }

    public void encrypt(ByteBuf buf, ByteBuf result) throws ShortBufferException {
        int i = buf.readableBytes();
        byte[] bs = this.toByteArray(buf);
        int j = this.cipher.getOutputSize(i);
        if (this.encryptionBuffer.length < j) {
            this.encryptionBuffer = new byte[j];
        }

        result.writeBytes(this.encryptionBuffer, 0, this.cipher.update(bs, 0, i, this.encryptionBuffer));
    }
}

