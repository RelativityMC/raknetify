package com.ishland.raknetify.fabric.mixin.client.hud;

import com.ishland.raknetify.common.connection.MetricsSynchronizationHandler;
import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.common.connection.SimpleMetricsLogger;
import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import io.netty.channel.Channel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import network.ycc.raknet.RakNet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DebugHud.class)
public class MixinDebugHud {

    @Inject(method = "getLeftText", at = @At("RETURN"))
    private void getLeftText(CallbackInfoReturnable<List<String>> cir) {
        final ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (networkHandler != null) {
            final ClientConnection connection = networkHandler.getConnection();
            final Channel channel = ((IClientConnection) connection).getChannel();
            if (channel != null) {
                if (channel.config() instanceof RakNet.Config config) {
                    if (config.getMetrics() instanceof SimpleMetricsLogger logger) {
                        cir.getReturnValue().add(
                                "[Raknetify] A: true, MTU: %d, RTT: %.2f/%.2fms"
                                        .formatted(config.getMTU(),
                                                logger.getMeasureRTTns() / 1_000_000.0,
                                                logger.getMeasureRTTnsStdDev() / 1_000_000.0
                                        ));
                        final MetricsSynchronizationHandler serverSync = logger.getMetricsSynchronizationHandler();
                        if (serverSync != null && serverSync.isRemoteSupported()) {
                            cir.getReturnValue().add(
                                    "[Raknetify] C: BUF: %.2fMB; S: BUF: %.2fMB"
                                            .formatted(
                                                    logger.getCurrentQueuedBytes() / 1024.0 / 1024.0,
                                                    serverSync.getQueuedBytes() / 1024.0 / 1024.0
                                            ));
                        } else {
                            cir.getReturnValue().add(
                                    "[Raknetify] C: BUF: %.2fMB"
                                            .formatted(
                                                    logger.getCurrentQueuedBytes() / 1024.0 / 1024.0
                                            ));
                        }

                        cir.getReturnValue().add(
                                "[Raknetify] C: I: %s, O: %s"
                                        .formatted(
                                                logger.getMeasureTrafficInFormatted(),
                                                logger.getMeasureTrafficOutFormatted()
                                        ));

                        cir.getReturnValue().add(
                                "[Raknetify] C: ERR: %.4f%%, %d tx, %d rx, BST: %d"
                                        .formatted(
                                                logger.getMeasureErrorRate() * 100.0,
                                                logger.getMeasureTX(), logger.getMeasureRX(),
                                                logger.getMeasureBurstTokens() + config.getDefaultPendingFrameSets()
                                        ));
                        if (serverSync != null && serverSync.isRemoteSupported()) {
                            cir.getReturnValue().add(
                                    "[Raknetify] S: ERR: %.4f%%, %d tx, %d rx, BST: %d"
                                            .formatted(
                                                    serverSync.getErrorRate() * 100.0,
                                                    serverSync.getTX(), serverSync.getRX(),
                                                    serverSync.getBurst()
                                            ));
                        }
                    } else {
                        cir.getReturnValue().add(
                                "[Raknetify] A: true, MTU: %d"
                                        .formatted(config.getMTU())
                        );
                    }

                    final MultiChannelingStreamingCompression compression = channel.pipeline().get(MultiChannelingStreamingCompression.class);
                    if (compression != null && compression.isActive()) {
                        cir.getReturnValue().add(
                                "[Raknetify] CRatio: I: %.2f%%, O: %.2f%%"
                                        .formatted(compression.getInCompressionRatio() * 100, compression.getOutCompressionRatio() * 100)
                        );
                    }

                    return;
                }
            }
        }
        cir.getReturnValue().add("[Raknetify] A: false");
    }

}
