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

package com.ishland.raknetify.common.connection;

import com.ishland.raknetify.common.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import network.ycc.raknet.frame.FrameData;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class MultiChannelingStreamingCompression extends ChannelDuplexHandler {

    public static final String NAME = "raknetify-multichannel-streaming-compression";

    public static final long SERVER_HANDSHAKE = 0x40000010;
    public static final long CHANNEL_START = 0x40000012;
    private final Inflater[] inflaters = new Inflater[8];
    private final Deflater[] deflaters = new Deflater[8];

    private final IntOpenHashSet channelsToIgnoreWhenReinit = new IntOpenHashSet();

    private final byte[] inflateBuffer = new byte[256 * 1024];
    private final byte[] deflateBuffer = new byte[256 * 1024];

    private final int rawPacketId;
    private final int compressedPacketId;

    private volatile long outBytesRaw = 0L;
    private volatile long outBytesCompressed = 0L;
    private volatile long inBytesCompressed = 0L;
    private volatile long inBytesRaw = 0L;

    private boolean active = false;

    public MultiChannelingStreamingCompression(int rawPacketId, int compressedPacketId) {
        this.rawPacketId = rawPacketId;
        this.compressedPacketId = compressedPacketId;
    }

    private void doServerHandshake(ChannelHandlerContext ctx) {
        final ByteBuf buf = ctx.alloc().buffer().writeLong(SERVER_HANDSHAKE);
        try {
            final FrameData data = FrameData.create(ctx.alloc(), Constants.RAKNET_STREAMING_COMPRESSION_HANDSHAKE_PACKET_ID,
                    buf);
            ctx.write(data);
        } finally {
            buf.release();
        }
    }

    private void doChannelStart(ChannelHandlerContext ctx) {
        if (!active) return;
        ByteBuf buf = ctx.alloc().buffer().writeLong(CHANNEL_START);
        try {
            for (int i = 0; i < 8; i++) {
                final FrameData data = FrameData.create(ctx.alloc(), Constants.RAKNET_STREAMING_COMPRESSION_HANDSHAKE_PACKET_ID,
                        buf);
                data.setOrderChannel(i);
                ctx.write(data);
                initDeflater(i);
            }
        } finally {
            buf.release();
        }
    }

    private void initDeflater(int channel) {
        if (!active) return;
        if (deflaters[channel] != null) deflaters[channel].end();
        deflaters[channel] = new Deflater();
        if (Constants.DEBUG) System.out.println("Raknetify: Streaming compression deflater for ch%d is ready".formatted(channel));
    }

    private void initInflater(int channel) {
        if (!active) return;
        if (inflaters[channel] != null) inflaters[channel].end();
        inflaters[channel] = new Inflater();
        if (Constants.DEBUG) System.out.println("Raknetify: Streaming compression inflater for ch%d is ready".formatted(channel));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        doServerHandshake(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FrameData compressedFrameData) {
            compressedFrameData.touch();
            if (compressedFrameData.getPacketId() == Constants.RAKNET_STREAMING_COMPRESSION_HANDSHAKE_PACKET_ID) {
                final int orderChannel = compressedFrameData.getOrderChannel();
                ByteBuf payload = null;
                try {
                    payload = compressedFrameData.createData().skipBytes(1);
                    if (payload.readableBytes() == 8) {
                        final long l = payload.readLong();
                        if (l == CHANNEL_START) {
                            initInflater(orderChannel);
                            return;
                        } else if (l == SERVER_HANDSHAKE) {
                            active = true;
                            doChannelStart(ctx);
                            return;
                        }
                    }
                } finally {
                    compressedFrameData.release();
                    if (payload != null) payload.release();
                }
            } else if (compressedFrameData.getPacketId() == compressedPacketId && compressedFrameData.getReliability().isReliable && compressedFrameData.getReliability().isOrdered && !compressedFrameData.getReliability().isSequenced && inflaters[compressedFrameData.getOrderChannel()] != null) {
                final int orderChannel = compressedFrameData.getOrderChannel();
                final Inflater inflater = inflaters[orderChannel];
                final ByteBuf data = compressedFrameData.createData().skipBytes(1);

                ByteBuf out = null;
                FrameData rawFrameData = null;
                try {
                    inflater.setInput(data.nioBuffer());

                    inBytesCompressed += data.readableBytes();

                    out = ctx.alloc().buffer();
                    {
                        int inflatedBytes;
                        while ((inflatedBytes = inflater.inflate(inflateBuffer)) != 0) {
                            out.writeBytes(inflateBuffer, 0, inflatedBytes);
                        }
                    }

                    inBytesRaw += out.writerIndex();

                    rawFrameData = FrameData.create(ctx.alloc(), rawPacketId, out);
                    rawFrameData.setReliability(compressedFrameData.getReliability());
                    rawFrameData.setOrderChannel(orderChannel);
                    ctx.fireChannelRead(rawFrameData);
                    rawFrameData = null;
                    return;
                } finally {
                    data.release();
                    compressedFrameData.release();
                    if (out != null) out.release();
                    if (rawFrameData != null) rawFrameData.release();
                }
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg == SynchronizationLayer.SYNC_REQUEST_OBJECT) {
            super.write(ctx, msg, promise);
            doChannelStart(ctx);
            return;
        } else if (msg instanceof FrameData rawFrameData) {
            rawFrameData.touch();
            if (rawFrameData.getPacketId() == rawPacketId && rawFrameData.getReliability().isReliable && rawFrameData.getReliability().isOrdered && !rawFrameData.getReliability().isSequenced && deflaters[rawFrameData.getOrderChannel()] != null) {
                if (rawFrameData.getDataSize() < 16 + 1) {
                    outBytesRaw += rawFrameData.getDataSize() - 1;
                    outBytesCompressed += rawFrameData.getDataSize() - 1;
                    ctx.write(rawFrameData, promise);
                    return;
                }

                final int orderChannel = rawFrameData.getOrderChannel();
                final Deflater deflater = deflaters[orderChannel];
                final ByteBuf data = rawFrameData.createData().skipBytes(1);

                ByteBuf out = null;
                FrameData compressedFrameData = null;
                try {
                    deflater.setInput(data.nioBuffer());

                    outBytesRaw += data.readableBytes();

                    out = ctx.alloc().buffer();
                    {
                        int deflatedBytes;
                        while ((deflatedBytes = deflater.deflate(deflateBuffer, 0, deflateBuffer.length, Deflater.SYNC_FLUSH)) != 0) {
                            out.writeBytes(deflateBuffer, 0, deflatedBytes);
                        }
                    }

                    outBytesCompressed += out.writerIndex();

                    compressedFrameData = FrameData.create(ctx.alloc(), compressedPacketId, out);
                    compressedFrameData.setReliability(rawFrameData.getReliability());
                    compressedFrameData.setOrderChannel(orderChannel);
                    ctx.write(compressedFrameData, promise);
                    compressedFrameData = null;
                    return;
                } finally {
                    data.release();
                    rawFrameData.release();
                    if (out != null) out.release();
                    if (compressedFrameData != null) compressedFrameData.release();
                }
            }
        }
        super.write(ctx, msg, promise);
    }

    public long getInBytesCompressed() {
        return inBytesCompressed;
    }

    public long getInBytesRaw() {
        return inBytesRaw;
    }

    public long getOutBytesCompressed() {
        return outBytesCompressed;
    }

    public long getOutBytesRaw() {
        return outBytesRaw;
    }

    public boolean isActive() {
        return active;
    }

    private ScheduledFuture<?> future;

    public void handlerAdded(ChannelHandlerContext ctx) {
        this.future = ctx.channel().eventLoop().scheduleAtFixedRate(this::tickMetrics, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        if (future != null) future.cancel(false);
        for (int i = 0; i < 8; i++) {
            if (inflaters[i] != null) inflaters[i].end();
            if (deflaters[i] != null) deflaters[i].end();
        }
    }

    private long lastInBytesCompressed;
    private long lastInBytesRaw;
    private long lastOutBytesRaw;
    private long lastOutBytesCompressed;
    private final DescriptiveStatistics inCompressionRatioStats = new DescriptiveStatistics(16);
    private final DescriptiveStatistics outCompressionRatioStats = new DescriptiveStatistics(16);
    private volatile double inCompressionRatio;
    private volatile double outCompressionRatio;

    private void tickMetrics() {
        long deltaInBytesCompressed = this.inBytesCompressed - this.lastInBytesCompressed;
        long deltaInBytesRaw = this.inBytesRaw - this.lastInBytesRaw;
        long deltaOutBytesCompressed = this.outBytesCompressed - this.lastOutBytesCompressed;
        long deltaOutBytesRaw = this.outBytesRaw - this.lastOutBytesRaw;

        if (deltaInBytesRaw != 0) {
            double currentInCompressionRatio = deltaInBytesCompressed / (double) deltaInBytesRaw;
            inCompressionRatioStats.addValue(currentInCompressionRatio);
        }

        if (deltaOutBytesRaw != 0) {
            double currentOutCompressionRatio = deltaOutBytesCompressed / (double) deltaOutBytesRaw;
            outCompressionRatioStats.addValue(currentOutCompressionRatio);
        }

        this.inCompressionRatio = inCompressionRatioStats.getMean();
        this.outCompressionRatio = outCompressionRatioStats.getMean();

        this.lastInBytesCompressed = this.inBytesCompressed;
        this.lastInBytesRaw = this.inBytesRaw;
        this.lastOutBytesCompressed = this.outBytesCompressed;
        this.lastOutBytesRaw = this.outBytesRaw;


    }

    public double getInCompressionRatio() {
        return inCompressionRatio;
    }

    public double getOutCompressionRatio() {
        return outCompressionRatio;
    }
}
