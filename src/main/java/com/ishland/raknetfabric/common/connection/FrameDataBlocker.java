package com.ishland.raknetfabric.common.connection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import network.ycc.raknet.frame.FrameData;

public class FrameDataBlocker extends ChannelInboundHandlerAdapter {

    private static final boolean printBlockedFrames = Boolean.getBoolean("raknetfabric.printBlockedFrames");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FrameData) {
            if (printBlockedFrames) System.out.println("Blocked %s".formatted(msg));
            return;
        }
        ctx.fireChannelRead(msg);
    }
}
