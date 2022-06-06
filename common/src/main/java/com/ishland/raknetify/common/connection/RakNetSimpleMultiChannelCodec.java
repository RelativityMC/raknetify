package com.ishland.raknetify.common.connection;

import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.util.MathUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToMessageCodec;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import network.ycc.raknet.frame.FrameData;
import network.ycc.raknet.packet.FramedPacket;

import java.util.List;

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

    public void setSimpleChannelMapping(Int2IntOpenHashMap channelMapping) {
        this.channelMapping = channelMapping;
        if (this.channelMapping != null) this.channelMapping.defaultReturnValue(Integer.MAX_VALUE);
    }

    public void setDescriptiveProtocolStatus(String descriptiveProtocolStatus) {
        this.descriptiveProtocolStatus = descriptiveProtocolStatus;
    }

    private boolean isMultichannelEnabled;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg == SIGNAL_START_MULTICHANNEL && !this.isMultichannelEnabled) {
            if (!this.isMultichannelAvailable()) {
                System.out.println("Raknetify: [MultiChannellingDataCodec] Failed to start multichannel: not available for %s".formatted(this.descriptiveProtocolStatus));
                return;
            }
            final FrameData frameData = FrameData.create(ctx.alloc(), Constants.RAKNET_PING_PACKET_ID, ctx.alloc().buffer(1).writeByte(0));
            frameData.setOrderChannel(7);
            ctx.write(frameData).addListener(future -> {
                isMultichannelEnabled = true;
                if (Constants.DEBUG) System.out.println("Raknetify: [MultiChannellingDataCodec] Started multichannel");
            });
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

    protected void decode(ChannelHandlerContext ctx, FrameData packet, List<Object> out) {
        assert !packet.isFragment();
        if (packet.getDataSize() > 0) {
            if (packetId == packet.getPacketId()) {
                out.add(packet.createData().skipBytes(1));
            } else {
                out.add(packet.retain());
            }
        }
    }

}
