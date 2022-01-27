package com.ishland.raknetfabric.common.connection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Packet;

public class MultiChannellingPacketCapture extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        final Class<?> msgClass = msg.getClass();
        RaknetMultiChannel.setCurrentPacketClass(msgClass);
        try {
            super.write(ctx, msg, promise);
        } finally {
            RaknetMultiChannel.clearCurrentPacketClass(msgClass);
        }
    }
}
