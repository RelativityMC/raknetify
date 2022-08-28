/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022 ishland
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

package com.ishland.raknetify.common.connection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import network.ycc.raknet.frame.FrameData;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.GeneralSecurityException;
import java.util.Objects;

public class MultiChannellingEncryption extends ChannelDuplexHandler {

    public static final String NAME = "raknetify-multichannel-encryption";

    private final PacketEncryptionManager decryption;
    private final PacketEncryptionManager encryption;

    public MultiChannellingEncryption(SecretKey key) throws GeneralSecurityException {
        Cipher decryption = Cipher.getInstance("AES/CFB8/NoPadding");
        decryption.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(key.getEncoded()));
        this.decryption = new PacketEncryptionManager(decryption);
        Cipher encryption = Cipher.getInstance("AES/CFB8/NoPadding");
        encryption.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(key.getEncoded()));
        this.encryption = new PacketEncryptionManager(encryption);
    }

    public MultiChannellingEncryption(Cipher decryption, Cipher encryption) {
        this.decryption = new PacketEncryptionManager(Objects.requireNonNull(decryption));
        this.encryption = new PacketEncryptionManager(Objects.requireNonNull(encryption));
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof FrameData data) {
            data.touch();
            final ByteBuf buf = data.createData().skipBytes(1);
            ByteBuf res = null;
            FrameData resFrame = null;
            try {
                res = ctx.alloc().buffer(data.getDataSize());
                encryption.doWork(buf, res);
                resFrame = FrameData.create(ctx.alloc(), data.getPacketId(), res);
//                res = null;
                resFrame.setOrderChannel(data.getOrderChannel());
                resFrame.setReliability(data.getReliability());
                ctx.write(resFrame, promise);
                resFrame = null;
                return;
            } finally {
                buf.release();
                data.release();
                if (res != null) res.release();
                if (resFrame != null) resFrame.release();
            }
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FrameData data) {
            data.touch();
            final ByteBuf buf = data.createData().skipBytes(1);
            ByteBuf res = null;
            FrameData resFrame = null;
            try {
                res = ctx.alloc().buffer(data.getDataSize());
                decryption.doWork(buf, res);
                resFrame = FrameData.create(ctx.alloc(), data.getPacketId(), res);
                resFrame.setOrderChannel(data.getOrderChannel());
                resFrame.setReliability(data.getReliability());
                ctx.fireChannelRead(resFrame);
                resFrame = null;
                return;
            } finally {
                buf.release();
                data.release();
                if (res != null) res.release();
                if (resFrame != null) resFrame.release();
            }
        }
        super.channelRead(ctx, msg);
    }
}
