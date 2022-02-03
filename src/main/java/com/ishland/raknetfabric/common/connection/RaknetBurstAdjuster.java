package com.ishland.raknetfabric.common.connection;

import com.ishland.raknetfabric.Constants;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import network.ycc.raknet.RakNet;

public class RaknetBurstAdjuster extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        final RakNet.Config config = RakNet.config(ctx);
        if (config.getMetrics() instanceof SimpleMetricsLogger logger) {
            config.setDefaultPendingFrameSets(Constants.DEFAULT_PENDING_FRAME_SETS + (int) (logger.getCurrentQueuedBytes() / 1024.0 / 1024.0 * 32));
        }
    }
}
