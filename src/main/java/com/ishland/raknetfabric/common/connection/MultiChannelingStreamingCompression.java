package com.ishland.raknetfabric.common.connection;

import com.ishland.raknetfabric.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import network.ycc.raknet.frame.FrameData;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class MultiChannelingStreamingCompression extends ChannelDuplexHandler {

    public static final long IDENTIFIER = 0x40000000;
    private final Inflater[] inflaters = new Inflater[8];
    private final Deflater[] deflaters = new Deflater[8];

    private final byte[] inflateBuffer = new byte[8192];
    private final byte[] deflateBuffer = new byte[8192];

    {
        for (int i = 0; i < 8; i ++) {
            inflaters[i] = new Inflater();
            deflaters[i] = new Deflater();
        }
    }

    private final int rawPacketId;
    private final int compressedPacketId;

    private volatile long outBytesRaw = 0L;
    private volatile long outBytesCompressed = 0L;
    private volatile long inBytesCompressed = 0L;
    private volatile long inBytesRaw = 0L;

    private boolean active;

    public MultiChannelingStreamingCompression(int rawPacketId, int compressedPacketId) {
        this.rawPacketId = rawPacketId;
        this.compressedPacketId = compressedPacketId;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        ctx.write(FrameData.create(ctx.alloc(), Constants.RAKNET_STREAMING_COMPRESSION_PACKET_ID,
                ctx.alloc().buffer().writeLong(IDENTIFIER)));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FrameData compressedFrameData) {
            if (compressedFrameData.getPacketId() == compressedPacketId && compressedFrameData.getReliability().isReliable && compressedFrameData.getReliability().isOrdered && !compressedFrameData.getReliability().isSequenced) {
                if (!active) {
                    ByteBuf payload = null;
                    try {
                        payload = compressedFrameData.createData().skipBytes(1);
                        if (payload.readableBytes() == 8 && payload.readLong() == IDENTIFIER) {
//                            System.out.println("[MultiChannellingStreamingCompression] starting streaming compression");
                            active = true;
                            return;
                        }
                    } finally {
                        if (payload != null) payload.release();
                    }

                }

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
                    out = null;
                    rawFrameData.setReliability(compressedFrameData.getReliability());
                    rawFrameData.setOrderChannel(orderChannel);
                    ctx.fireChannelRead(rawFrameData);
                    rawFrameData = null;
                    return;
                } finally {
                    data.release();
                    if (out != null) out.release();
                    if (rawFrameData != null) rawFrameData.release();
                }
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (active && msg instanceof FrameData rawFrameData) {
            if (rawFrameData.getPacketId() == rawPacketId && rawFrameData.getReliability().isReliable && rawFrameData.getReliability().isOrdered && !rawFrameData.getReliability().isSequenced) {

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
                    out = null;
                    compressedFrameData.setReliability(rawFrameData.getReliability());
                    compressedFrameData.setOrderChannel(orderChannel);
                    ctx.write(compressedFrameData, promise);
                    compressedFrameData = null;
                    return;
                } finally {
                    data.release();
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
