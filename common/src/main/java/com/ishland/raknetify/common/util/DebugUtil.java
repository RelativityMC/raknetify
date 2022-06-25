package com.ishland.raknetify.common.util;

import com.ishland.raknetify.common.connection.MetricsSynchronizationHandler;
import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.common.connection.SimpleMetricsLogger;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import network.ycc.raknet.RakNet;

public class DebugUtil {

    public static String printChannelDetails(Channel channel) {
        final StringBuilder b = new StringBuilder();
        b.append("Channel details for ").append(channel.toString()).append('\n');
        b.append("Connection: ").append(channel.localAddress()).append(" <--> ").append(channel.remoteAddress()).append('\n');
        b.append("Open: ").append(channel.isOpen()).append('\n');
        b.append("Active: ").append(channel.isActive()).append('\n');
        b.append("Auto Read: ").append(channel.config().isAutoRead());
        if (channel.config() instanceof RakNet.Config config) {
            b.append("MTU: ").append(config.getMTU()).append('\n');
            b.append("RTT: %.2f/%.2fms"
                    .formatted(
                            config.getRTTNanos() / 1_000_000.0,
                            config.getRTTStdDevNanos() / 1_000_000.0
                    )).append('\n');
            if (config.getMetrics() instanceof SimpleMetricsLogger logger) {
                final MetricsSynchronizationHandler sync = logger.getMetricsSynchronizationHandler();
                if (sync != null && sync.isRemoteSupported()) {
                    b.append("Local Buffer: %.2fMB; Remote buffer: %.2fMB"
                            .formatted(
                                    logger.getCurrentQueuedBytes() / 1024.0 / 1024.0,
                                    sync.getQueuedBytes() / 1024.0 / 1024.0
                            )).append('\n');
                } else {
                    b.append("Local buffer: %.2fMB"
                            .formatted(
                                    logger.getCurrentQueuedBytes() / 1024.0 / 1024.0
                            )).append('\n');
                }

                b.append("Local traffic: I: %s, O: %s"
                        .formatted(
                                logger.getMeasureTrafficInFormatted(),
                                logger.getMeasureTrafficOutFormatted()
                        )).append('\n');

                b.append("Local Statistics: ERR: %.4f%%, %d tx, %d rx, Burst: %d"
                        .formatted(
                                logger.getMeasureErrorRate() * 100.0,
                                logger.getMeasureTX(), logger.getMeasureRX(),
                                logger.getMeasureBurstTokens() + config.getDefaultPendingFrameSets()
                        )).append('\n');

                if (sync != null && sync.isRemoteSupported()) {
                    b.append("Remote Statistics: ERR: %.4f%%, %d tx, %d rx, Burst: %d"
                            .formatted(
                                    sync.getErrorRate() * 100.0,
                                    sync.getTX(), sync.getRX(),
                                    sync.getBurst()
                            )).append('\n');
                }

                final MultiChannelingStreamingCompression compression = channel.pipeline().get(MultiChannelingStreamingCompression.class);
                if (compression != null && compression.isActive()) {
                    b.append("Local Streaming Compression Ratio: I: %.2f%%, O: %.2f%%"
                            .formatted(compression.getInCompressionRatio() * 100, compression.getOutCompressionRatio() * 100)).append('\n');
                }

            }
        }

        b.append('\n');
        b.append("Pipeline: ").append('\n');
        for (String name : channel.pipeline().names()) {
            final ChannelHandler channelHandler = channel.pipeline().get(name);
            b.append("\t").append(name).append(": \t").append(channelHandler.toString()).append("(").append(channelHandler.getClass().getName()).append(")").append('\n');
        }

        if (channel.parent() != null) {
            b.append('\n');
            b.append("Parent: ").append('\n');

            b.append(printChannelDetails(channel.parent()).replace("\n", "\n\t"));
        }


        return b.toString();
    }

}
