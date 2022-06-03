package com.ishland.raknetify.fabric.common.connection;

import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.SynchronizationLayer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToMessageCodec;
import network.ycc.raknet.frame.FrameData;
import network.ycc.raknet.packet.FramedPacket;

import java.util.List;

public class MultiChannellingDataCodec extends MessageToMessageCodec<FrameData, ByteBuf> {

    public static final String NAME = "raknetify-multi-channel-data-codec";

    public static final Object START_MULTICHANNEL = new Object();

    private final int packetId;

    public MultiChannellingDataCodec(int packetId) {
        this.packetId = packetId;
    }

    private boolean isMultichannelEnabled;
    private MultiChannellingPacketCapture capture = null;

    void setCapture(MultiChannellingPacketCapture capture) {
        this.capture = capture;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg == START_MULTICHANNEL) {
            final FrameData frameData = FrameData.create(ctx.alloc(), Constants.RAKNET_PING_PACKET_ID, ctx.alloc().buffer(1).writeByte(0));
            frameData.setOrderChannel(7);
            ctx.write(frameData).addListener(future -> {
                isMultichannelEnabled = true;
                System.out.println("[MultiChannellingDataCodec] Started multichannel");
            });
            return;
        }
        if (msg == SynchronizationLayer.SYNC_REQUEST_OBJECT) {
            System.out.println("[MultiChannellingDataCodec] Stopped multichannel");
            this.isMultichannelEnabled = false;
        }
        super.write(ctx, msg, promise);
    }

    protected void encode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        if (buf.isReadable()) {
            final FrameData frameData = FrameData.create(ctx.alloc(), packetId, buf);
            final int packetChannelOverride = isMultichannelEnabled ? RakNetMultiChannel.getPacketChannelOverride(this.capture.getPacketClass()) : 0;
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