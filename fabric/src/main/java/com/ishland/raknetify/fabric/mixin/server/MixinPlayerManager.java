/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022-2023 ishland
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

package com.ishland.raknetify.fabric.mixin.server;

import com.ishland.raknetify.common.connection.RakNetSimpleMultiChannelCodec;
import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import network.ycc.raknet.RakNet;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class MixinPlayerManager {

    @Dynamic
    @Inject(method = {"onPlayerConnect", "method_12975"}, at = @At("HEAD"))
    private void onJoin(ClientConnection connection, ServerPlayerEntity player, int latency, CallbackInfo ci) {
        if (((IClientConnection) connection).getChannel().config() instanceof RakNet.Config config) {
            System.out.println(String.format("Raknetify: %s logged in via RakNet, mtu %d", player.getName().getString(), config.getMTU()));
        }
    }

    @Surrogate
    private void onJoin(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        if (((IClientConnection) connection).getChannel().config() instanceof RakNet.Config config) {
            System.out.println(String.format("Raknetify: %s logged in via RakNet, mtu %d", player.getName().getString(), config.getMTU()));
        }
    }


    @Dynamic
    @Inject(method = {"onPlayerConnect", "method_12975"}, at = @At("RETURN"))
    private void postJoin(ClientConnection connection, ServerPlayerEntity player, int latency, CallbackInfo ci) {
        final Channel channel = ((IClientConnection) connection).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("Raknetify: Warning: %s don't have valid channel when logged in, not sending sync packet".formatted(this));
            return;
        }
        channel.write(RakNetSimpleMultiChannelCodec.SIGNAL_START_MULTICHANNEL);
    }

    @Surrogate
    private void postJoin(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        final Channel channel = ((IClientConnection) connection).getChannel();
        if (channel == null) {
            //noinspection RedundantStringFormatCall
            System.err.println("Raknetify: Warning: %s don't have valid channel when logged in, not sending sync packet".formatted(this));
            return;
        }
        channel.write(RakNetSimpleMultiChannelCodec.SIGNAL_START_MULTICHANNEL);
    }

}
