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

package com.ishland.raknetify.fabric.common.connection;

import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.RakNetSimpleMultiChannelCodec;
import com.ishland.raknetify.common.connection.SynchronizationLayer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToMessageCodec;
import network.ycc.raknet.frame.FrameData;
import network.ycc.raknet.packet.FramedPacket;

import java.util.List;

public class RakNetFabricMultiChannelCodec extends RakNetSimpleMultiChannelCodec {

    public static final String NAME = "raknetify-fabric-multi-channel-data-codec";

    public RakNetFabricMultiChannelCodec(int packetId) {
        super(packetId);
    }

    private boolean isMultichannelEnabled;
    private MultiChannellingPacketCapture capture = null;

    void setCapture(MultiChannellingPacketCapture capture) {
        this.capture = capture;
    }

    @Override
    protected boolean isMultichannelAvailable() {
        return true;
    }

    @Override
    protected int getChannelOverride(ByteBuf buf) {
        return RakNetMultiChannel.getPacketChannelOverride(this.capture.getPacketClass());
    }

    @Override
    protected Object getUserData(Object msg) {
        return this.capture.getPacketClass();
    }

    @Override
    protected void setUserData(Object msg, Object userData) {
        if (userData instanceof Class<?> clazz) {
            this.capture.setPacketClass(clazz);
        }
    }
}
