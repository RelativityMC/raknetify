package com.ishland.raknetfabric.common.connection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class MultiChannellingPacketCapture extends ChannelOutboundHandlerAdapter {

    private Class<?> packetClass = null;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        this.packetClass = msg.getClass();
        try {
            ctx.write(msg, promise);
        } finally {
            this.packetClass = null;
        }
    }

    public Class<?> getPacketClass() {
        return packetClass;
    }
}
