/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022-2025 ishland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ishland.raknetify.fabric.mixin.client.hud;

import com.ishland.raknetify.common.connection.MetricsSynchronizationHandler;
import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.common.connection.SimpleMetricsLogger;
import com.ishland.raknetify.fabric.common.util.MultiVersionUtil;
import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import com.ishland.raknetify.fabric.mixin.access.IClientPlayNetworkHandler;
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
            final ClientConnection connection = (ClientConnection) MultiVersionUtil.ClientPlayNetworkHandler$connection.get(networkHandler);
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
