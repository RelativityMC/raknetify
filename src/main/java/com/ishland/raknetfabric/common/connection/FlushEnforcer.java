package com.ishland.raknetfabric.common.connection;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.concurrent.TimeUnit;

public class FlushEnforcer extends ChannelDuplexHandler {


    private ScheduledFuture<?> scheduledFuture;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        scheduledFuture = ctx.channel().eventLoop().scheduleAtFixedRate(ctx::flush, 0, 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        if (scheduledFuture != null)
            scheduledFuture.cancel(false);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
        ctx.flush();
    }

}
