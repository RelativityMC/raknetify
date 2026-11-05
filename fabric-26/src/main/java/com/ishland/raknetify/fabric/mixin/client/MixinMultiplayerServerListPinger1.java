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

package com.ishland.raknetify.fabric.mixin.client;

import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import io.netty.channel.Channel;
import network.ycc.raknet.RakNet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.server.network.EventLoopGroupHolder;

@Mixin(targets = "net/minecraft/client/multiplayer/ServerStatusPinger$1")
public abstract class MixinMultiplayerServerListPinger1 implements ClientStatusPacketListener {

    @Shadow
    @Final
    Connection val$connection; // synthetic

    @Shadow
    @Final
    ServerData val$data; // synthetic

    @Inject(method = "handleStatusResponse(Lnet/minecraft/network/protocol/status/ClientboundStatusResponsePacket;)V", at = @At("RETURN"))
    private void setPingImmediately(CallbackInfo ci) {
        final Channel channel = ((IClientConnection) val$connection).getChannel();
        if (channel.config() instanceof RakNet.Config config) {
            val$data.ping = config.getRTTNanos() / 1_000_000;
        }
    }

    @WrapWithCondition(method = "onDisconnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ServerStatusPinger;pingLegacyServer(Ljava/net/InetSocketAddress;Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;Lnet/minecraft/server/network/EventLoopGroupHolder;)V"), require = 0)
    private boolean noPingRaknet(ServerStatusPinger instance, InetSocketAddress socketAddress, ServerAddress address, ServerData serverInfo, EventLoopGroupHolder backend) {
        final Channel channel = ((IClientConnection) val$connection).getChannel();
        return !(channel.config() instanceof RakNet.Config);
    }

}
