package com.ishland.raknetify.common.connection;

import com.ishland.raknetify.common.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import network.ycc.raknet.RakNet;
import network.ycc.raknet.frame.FrameData;
import network.ycc.raknet.packet.FramedPacket;

import java.util.concurrent.TimeUnit;

public class MetricsSynchronizationHandler extends ChannelDuplexHandler {

    // Packet format:
    // byte: version (currently 0x00)
    // long: packet sent time (used to filter out old packets)
    // int: buffer size in bytes
    // int: burst size
    // double: error rate
    // int: tx
    // int: rx

    private static final byte VERSION = 0x00;

    private ScheduledFuture<?> future;
    private ChannelHandlerContext ctx;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.future = ctx.channel().eventLoop().scheduleAtFixedRate(this::sendSyncPacket, 200, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        if (future != null) future.cancel(false);
    }

    private void sendSyncPacket() {
        if (this.ctx.channel().config() instanceof RakNet.Config config && config.getMetrics() instanceof SimpleMetricsLogger logger){
            ByteBuf buffer = null;
           try {
               buffer = this.ctx.alloc().buffer(1 + 8 + 4 + 4 + 8 + 4 + 4);
               buffer.writeByte(VERSION);
               buffer.writeLong(System.currentTimeMillis());
               buffer.writeInt(logger.getCurrentQueuedBytes());
               buffer.writeInt((int) (logger.getMeasureBurstTokens() + config.getDefaultPendingFrameSets()));
               buffer.writeDouble(logger.getMeasureErrorRate());
               buffer.writeInt(logger.getMeasureTX());
               buffer.writeInt(logger.getMeasureRX());
               final FrameData frameData = FrameData.create(this.ctx.alloc(), Constants.RAKNET_METRICS_SYNC_PACKET_ID, buffer);
               frameData.setReliability(FramedPacket.Reliability.UNRELIABLE);
               this.ctx.write(frameData);
           } finally {
               if (buffer != null) buffer.release();
           }
        }
    }

    private boolean isServerSupported = false;
    private long lastRecv = 0L;
    private int queuedBytes;
    private int burst;
    private double errorRate;
    private int tx;
    private int rx;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FrameData frameData && frameData.getDataSize() > 0 && frameData.getPacketId() == Constants.RAKNET_METRICS_SYNC_PACKET_ID) {
            ByteBuf byteBuf = null;
            try {
                byteBuf = frameData.createData().skipBytes(1);
                final byte version = byteBuf.readByte();
                if (version != VERSION) return;

                final long time = byteBuf.readLong();
                if (time < this.lastRecv) return;
                this.lastRecv = time;

                this.isServerSupported = true;

                this.queuedBytes = byteBuf.readInt();
                this.burst = byteBuf.readInt();
                this.errorRate = byteBuf.readDouble();
                this.tx = byteBuf.readInt();
                this.rx = byteBuf.readInt();
            } finally {
                if (byteBuf != null) byteBuf.release();
                frameData.release();
            }
            return;
        }
        super.channelRead(ctx, msg);
    }

    public boolean isServerSupported() {
        return isServerSupported;
    }

    public int getQueuedBytes() {
        return queuedBytes;
    }

    public int getBurst() {
        return burst;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public int getTX() {
        return tx;
    }

    public int getRX() {
        return rx;
    }
}
