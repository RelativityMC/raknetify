/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022 ishland
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

package com.ishland.raknetify.fabric.mixin.common.sync;

import com.ishland.raknetify.common.connection.RakNetSimpleMultiChannelCodec;
import com.ishland.raknetify.common.connection.SynchronizationLayer;
import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import com.ishland.raknetify.fabric.mixin.access.IServerPlayNetworkHandler;
import io.netty.channel.Channel;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import network.ycc.raknet.RakNet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {

    @Inject(method = "respawnPlayer", at = @At(value = "NEW", target = "net/minecraft/network/packet/s2c/play/PlayerRespawnS2CPacket", shift = At.Shift.BEFORE))
    private void beforeMoveToAnotherWorld(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> cir) {
        final Channel channel = ((IClientConnection) ((IServerPlayNetworkHandler) player.networkHandler).getConnection()).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("Raknetify: Warning: %s don't have valid channel when teleporting to another dimension, not sending sync packet".formatted(this));
            return;
        }
        if (channel.config() instanceof RakNet.Config) {
            channel.write(SynchronizationLayer.SYNC_REQUEST_OBJECT);
        }
    }

    @Inject(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;onSpawn()V", shift = At.Shift.AFTER))
    private void afterMoveToAnotherWorld(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> cir) {
        final Channel channel = ((IClientConnection) ((IServerPlayNetworkHandler) player.networkHandler).getConnection()).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("Raknetify: Warning: %s don't have valid channel when teleporting to another dimension, not starting multichannel".formatted(this));
            return;
        }
        if (channel.config() instanceof RakNet.Config) {
            channel.write(RakNetSimpleMultiChannelCodec.SIGNAL_START_MULTICHANNEL);
        }
    }
}
