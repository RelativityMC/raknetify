package com.ishland.raknetfabric.mixin.client.hud;

import com.ishland.raknetfabric.common.connection.MetricsSynchronizationHandler;
import com.ishland.raknetfabric.common.connection.SimpleMetricsLogger;
import com.ishland.raknetfabric.mixin.access.IClientConnection;
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
                                "[RaknetFabric] A: true, MTU: %d, RTT: %.2f/%.2fms"
                                        .formatted(config.getMTU(),
                                                logger.getMeasureRTTns() / 1_000_000.0,
                                                logger.getMeasureRTTnsStdDev() / 1_000_000.0
                                        ));
                        final MetricsSynchronizationHandler serverSync = logger.getMetricsSynchronizationHandler();
                        if (serverSync != null && serverSync.isServerSupported()) {
                            cir.getReturnValue().add(
                                    "[RaknetFabric] C: BUF: %.2fMB; S: BUF: %.2fMB"
                                            .formatted(
                                                    logger.getCurrentQueuedBytes() / 1024.0 / 1024.0,
                                                    serverSync.getQueuedBytes() / 1024.0 / 1024.0
                                            ));
                        } else {
                            cir.getReturnValue().add(
                                    "[RaknetFabric] C: BUF: %.2fMB"
                                            .formatted(
                                                    logger.getCurrentQueuedBytes() / 1024.0 / 1024.0
                                            ));
                        }
                        cir.getReturnValue().add(
                                "[RaknetFabric] C: ERR: %.4f%%, %d tx, %d rx, BST: %d"
                                        .formatted(
                                                logger.getMeasureErrorRate() * 100.0,
                                                logger.getMeasureTX(), logger.getMeasureRX(),
                                                logger.getMeasureBurstTokens() + config.getDefaultPendingFrameSets()
                                        ));
                        if (serverSync != null && serverSync.isServerSupported()) {
                            cir.getReturnValue().add(
                                    "[RaknetFabric] S: ERR: %.4f%%, %d tx, %d rx, BST: %d"
                                            .formatted(
                                                    serverSync.getErrorRate() * 100.0,
                                                    serverSync.getTX(), serverSync.getRX(),
                                                    serverSync.getBurst()
                                            ));
                        }
                    } else {
                        cir.getReturnValue().add(
                                "[RaknetFabric] A: true, MTU: %d"
                                        .formatted(config.getMTU())
                        );
                    }
                    return;
                }
            }
        }
        cir.getReturnValue().add("[RaknetFabric] A: false");
    }

}
