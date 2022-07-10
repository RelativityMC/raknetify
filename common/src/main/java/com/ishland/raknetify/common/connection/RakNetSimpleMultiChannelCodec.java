package com.ishland.raknetify.common.connection;

import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.util.MathUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToMessageCodec;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import network.ycc.raknet.frame.FrameData;
import network.ycc.raknet.packet.FramedPacket;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class RakNetSimpleMultiChannelCodec extends MessageToMessageCodec<FrameData, ByteBuf> {

    public static final String NAME = "raknetify-simple-multi-channel-data-codec";

    public static final Object SIGNAL_START_MULTICHANNEL = new Object();

    private final int packetId;

    private final IntOpenHashSet unknownPacketIds = new IntOpenHashSet();
    private Int2IntOpenHashMap channelMapping = null;
    private String descriptiveProtocolStatus = "unknown protocol";

    public RakNetSimpleMultiChannelCodec(int packetId) {
        this.packetId = packetId;
    }

    public void setSimpleChannelMapping(Int2IntMap channelMapping) {
        if (channelMapping == null) return;
        this.channelMapping = new Int2IntOpenHashMap(channelMapping);
        this.channelMapping.defaultReturnValue(Integer.MAX_VALUE);
    }

    public void setDescriptiveProtocolStatus(String descriptiveProtocolStatus) {
        this.descriptiveProtocolStatus = descriptiveProtocolStatus;
    }

    private boolean isMultichannelEnabled;

    private boolean queuePendingWrites = false;
    private Queue<PendingWrite> pendingWrites = new LinkedList<>();

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (this.queuePendingWrites) {
            pendingWrites.add(new PendingWrite(msg, promise, getUserData(msg)));
            return;
        }

        if (msg == SIGNAL_START_MULTICHANNEL && !this.isMultichannelEnabled) {
            if (!this.isMultichannelAvailable()) {
                System.out.println("Raknetify: [MultiChannellingDataCodec] Failed to start multichannel: not available for %s".formatted(this.descriptiveProtocolStatus));
                return;
            }
            final ByteBuf buf = ctx.alloc().buffer(1).writeByte(0);
            try {
                final FrameData frameData = FrameData.create(ctx.alloc(), Constants.RAKNET_PING_PACKET_ID, buf);
                frameData.setOrderChannel(7);
                this.queuePendingWrites = true;
                ctx.write(frameData).addListener(future -> {
                    isMultichannelEnabled = true;
                    if (Constants.DEBUG) System.out.println("Raknetify: [MultiChannellingDataCodec] Started multichannel");
                    flushPendingWrites(ctx);
                });
            } finally {
                buf.release();
            }
            promise.trySuccess();
            return;
        }
        if (msg == SynchronizationLayer.SYNC_REQUEST_OBJECT && this.isMultichannelEnabled) {
            if (Constants.DEBUG) System.out.println("Raknetify: [MultiChannellingDataCodec] Stopped multichannel");
            this.isMultichannelEnabled = false;
            super.write(ctx, msg, promise);
            return;
        }
        super.write(ctx, msg, promise);
    }

    protected void encode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        if (buf.isReadable()) {
            final FrameData frameData = FrameData.create(ctx.alloc(), packetId, buf);
            final int packetChannelOverride = isMultichannelEnabled ? getChannelOverride(buf) : 0;
            if (packetChannelOverride >= 0)
                frameData.setOrderChannel(packetChannelOverride);
            else if (packetChannelOverride == -1)
                frameData.setReliability(FramedPacket.Reliability.RELIABLE);
            else if (packetChannelOverride == -2)
                frameData.setReliability(FramedPacket.Reliability.UNRELIABLE);
            out.add(frameData);
        }
    }

    private void flushPendingWrites(ChannelHandlerContext ctx) {
        this.queuePendingWrites = false;
        PendingWrite pendingWrite;
        while ((pendingWrite = this.pendingWrites.poll()) != null) {
            try {
                this.setUserData(pendingWrite.msg, pendingWrite.userData);
                this.write(ctx, pendingWrite.msg, pendingWrite.promise);
            } catch (Throwable t) {
                ctx.fireExceptionCaught(t);
            }
        }
    }

    protected boolean isMultichannelAvailable() {
        return this.channelMapping != null;
    }

    protected int getChannelOverride(ByteBuf buf) {
        final ByteBuf slice = buf.slice();
        final int packetId = MathUtil.readVarInt(slice);
        final int override = this.channelMapping.get(packetId);
        if (override == Integer.MAX_VALUE) {
            if (this.unknownPacketIds.add(packetId)) {
                System.err.println("Raknetify: Unknown packet id %d for %s".formatted(packetId, descriptiveProtocolStatus));
            }
            return 7;
        }
        return override;
    }

    protected Object getUserData(Object msg) {
        return null;
    }

    protected void setUserData(Object msg, Object userData) {
    }

    protected void decode(ChannelHandlerContext ctx, FrameData packet, List<Object> out) {
        assert !packet.isFragment();
        if (packet.getDataSize() > 0) {
            if (packetId == packet.getPacketId()) {
                out.add(packet.createData().skipBytes(1));
            } else if (packet.getPacketId() == Constants.RAKNET_PING_PACKET_ID) {
                return;
            } else {
                out.add(packet.retain());
            }
        }
    }

    private record PendingWrite(Object msg, ChannelPromise promise, Object userData){
    }

}
