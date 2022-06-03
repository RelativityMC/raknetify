package com.ishland.raknetify.common.connection;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

public class NoFlush extends ChannelDuplexHandler {

    @Override
    public void flush(ChannelHandlerContext ctx) {
        // no-op
    }
}
