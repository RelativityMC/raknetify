/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022-2023 ishland
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

package com.ishland.raknetify.fabric.common.connection;

import com.ishland.raknetify.common.connection.RakNetSimpleMultiChannelCodec;
import com.ishland.raknetify.common.connection.multichannel.CustomPayloadChannel;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;

public class MultiChannellingPacketCapture extends ChannelOutboundHandlerAdapter {

    private Class<?> packetClass = null;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        this.packetClass = msg.getClass();
        try {
            ctx.write(msg, promise);
        } finally {
            this.packetClass = null;
        }
    }

    public Class<?> getPacketClass() {
        return this.packetClass;
    }

    public void setPacketClass(Class<?> packetClass) {
        this.packetClass = packetClass;
    }

    public RakNetSimpleMultiChannelCodec.OverrideHandler getCaptureBasedHandler() {
        return new CaptureBasedHandler();
    }

    public RakNetSimpleMultiChannelCodec.OverrideHandler getCustomPayloadHandler() {
        return new CustomPayloadChannel.OverrideHandler(value -> packetClass == CustomPayloadS2CPacket.class || packetClass == CustomPayloadC2SPacket.class);
    }

    private class CaptureBasedHandler implements RakNetSimpleMultiChannelCodec.OverrideHandler {

        @Override
        public int getChannelOverride(ByteBuf buf) {
            return RakNetMultiChannel.getPacketChannelOverride(packetClass);
        }

    }

}
