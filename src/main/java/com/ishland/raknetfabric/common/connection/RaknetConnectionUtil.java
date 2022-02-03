package com.ishland.raknetfabric.common.connection;

import com.ishland.raknetfabric.Constants;
import com.ishland.raknetfabric.common.compat.viafabric.ViaFabricCompatInjector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import network.ycc.raknet.RakNet;

import java.util.concurrent.TimeUnit;

public class RaknetConnectionUtil {

    private RaknetConnectionUtil() {
    }

    public static void initChannel(Channel channel) {
        if (channel.config() instanceof RakNet.Config config) {
            config.setMTU(Constants.DEFAULT_MTU);
            config.setMaxQueuedBytes(Constants.MAX_QUEUED_SIZE);
            config.setMaxPendingFrameSets(Constants.MAX_PENDING_FRAME_SETS);
            config.setRetryDelayNanos(TimeUnit.NANOSECONDS.convert(200, TimeUnit.MILLISECONDS));
            config.setDefaultPendingFrameSets(Constants.DEFAULT_PENDING_FRAME_SETS);
            final SimpleMetricsLogger simpleMetricsLogger = new SimpleMetricsLogger();
            config.setMetrics(simpleMetricsLogger);
            final MetricsSynchronizationHandler metricsSynchronizationHandler = new MetricsSynchronizationHandler();
            simpleMetricsLogger.setMetricsSynchronizationHandler(metricsSynchronizationHandler);
//            channel.pipeline().addLast("raknetfabric-flush-enforcer", new FlushEnforcer());
            channel.pipeline().addLast("raknetfabric-metrics-sync", metricsSynchronizationHandler);
            channel.pipeline().addLast("raknetfabric-flush-consolidation", new FlushConsolidationHandler(Integer.MAX_VALUE, true));
            channel.pipeline().addLast("raknetfabric-synchronization-layer", new SynchronizationLayer(1));
            channel.pipeline().addLast("raknetfabric-multi-channel-data-codec", new MultiChannellingDataCodec(Constants.RAKNET_GAME_PACKET_ID));
        }
    }

    public static void postInitChannel(Channel channel, boolean isClientSide) {
        if (channel.config() instanceof RakNet.Config) {
            ViaFabricCompatInjector.inject(channel, isClientSide);
            channel.pipeline().replace("timeout", "timeout", new ChannelDuplexHandler()); // no-op
            channel.pipeline().addFirst("raknetfabric-timeout", new ReadTimeoutHandler(15));
            channel.pipeline().replace("splitter", "splitter", new ChannelDuplexHandler()); // no-op
            channel.pipeline().replace("prepender", "prepender", new ChannelDuplexHandler()); // no-op
            channel.pipeline().addLast("raknetfabric-multi-channel-packet-cature", new MultiChannellingPacketCapture());
        }
    }

}
