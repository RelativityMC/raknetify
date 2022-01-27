package com.ishland.raknetfabric.common.connection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import network.ycc.raknet.frame.FrameData;
import network.ycc.raknet.packet.FramedPacket;

import java.util.List;

@ChannelHandler.Sharable
public class MultiChannellingDataCodec extends MessageToMessageCodec<FrameData, ByteBuf> {

    public static final String NAME = "raknetfabric-multi-channel-data-codec";

    private final int packetId;

    public MultiChannellingDataCodec(int packetId) {
        this.packetId = packetId;
    }

    protected void encode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        if (buf.isReadable()) {
            final FrameData frameData = FrameData.create(ctx.alloc(), packetId, buf);
            final int packetChannelOverride = RaknetMultiChannel.getPacketChannelOverride();
            if (packetChannelOverride >= 0)
                frameData.setOrderChannel(packetChannelOverride);
            else if (packetChannelOverride == -1)
                frameData.setReliability(FramedPacket.Reliability.RELIABLE);
            out.add(frameData);
        }
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
