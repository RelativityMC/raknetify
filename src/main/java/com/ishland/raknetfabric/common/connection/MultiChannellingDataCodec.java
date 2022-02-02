package com.ishland.raknetfabric.common.connection;

import com.ishland.raknetfabric.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToMessageCodec;
import network.ycc.raknet.frame.FrameData;
import network.ycc.raknet.packet.FramedPacket;

import java.util.List;

@ChannelHandler.Sharable
public class MultiChannellingDataCodec extends MessageToMessageCodec<FrameData, ByteBuf> {

    public static final String NAME = "raknetfabric-multi-channel-data-codec";

    public static final Object START_MULTICHANNEL = new Object();

    private final int packetId;

    public MultiChannellingDataCodec(int packetId) {
        this.packetId = packetId;
    }

    private boolean isMultichannelEnabled;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg == START_MULTICHANNEL) {
            ctx.write(FrameData.create(ctx.alloc(), Constants.RAKNET_PING_PACKET_ID, ctx.alloc().buffer(1).writeByte(0))).addListener(future -> {
                isMultichannelEnabled = true;
                System.out.println("[MultiChannellingDataCodec] Started multichannel");
            });
            return;
        }
        super.write(ctx, msg, promise);
    }

    protected void encode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        if (buf.isReadable()) {
            final FrameData frameData = FrameData.create(ctx.alloc(), packetId, buf);
            final int packetChannelOverride = isMultichannelEnabled ? RaknetMultiChannel.getPacketChannelOverride() : 0;
            if (packetChannelOverride >= 0)
                frameData.setOrderChannel(packetChannelOverride);
            else if (packetChannelOverride == -1)
                frameData.setReliability(FramedPacket.Reliability.RELIABLE);
            else if (packetChannelOverride == -2)
                frameData.setReliability(FramedPacket.Reliability.UNRELIABLE);
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
