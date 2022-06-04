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
}
