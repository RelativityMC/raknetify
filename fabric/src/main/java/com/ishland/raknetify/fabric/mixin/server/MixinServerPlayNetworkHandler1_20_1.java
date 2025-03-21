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

package com.ishland.raknetify.fabric.mixin.server;

import com.ishland.raknetify.fabric.common.util.MultiVersionUtil;
import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import io.netty.channel.Channel;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import network.ycc.raknet.RakNet;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler1_20_1 {

    @Shadow public ServerPlayerEntity player;

    @Dynamic
    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;field_14136:J", opcode = Opcodes.GETFIELD))
    private long disableKeepAlive(long original) {
        if (!(((IClientConnection) MultiVersionUtil.ServerPlayNetworkHandler$connection.get((ServerPlayNetworkHandler) (Object) this)).getChannel().config() instanceof RakNet.Config)) {
            return original;
        }
        return Util.getMeasuringTimeMs();
    }

    @Dynamic
    @WrapWithCondition(method = "method_12082", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;method_14367(Lnet/minecraft/text/Text;)V"))
    private boolean stopTimeoutPlayersOnKeepAlive(ServerPlayNetworkHandler instance, Text reason) {
        return !(((IClientConnection) MultiVersionUtil.ServerPlayNetworkHandler$connection.get((ServerPlayNetworkHandler) (Object) this)).getChannel().config() instanceof RakNet.Config);
    }

    @Dynamic
    @WrapWithCondition(method = "method_12082", at = @At(value = "FIELD", target = "Lnet/minecraft/server/network/ServerPlayerEntity;field_13967:I", opcode = Opcodes.PUTFIELD))
    private boolean redirectPingStoring(ServerPlayerEntity instance, int value) {
        return !(((IClientConnection) MultiVersionUtil.ServerPlayNetworkHandler$connection.get((ServerPlayNetworkHandler) (Object) this)).getChannel().config() instanceof RakNet.Config);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        final Channel channel = ((IClientConnection) MultiVersionUtil.ServerPlayNetworkHandler$connection.get((ServerPlayNetworkHandler) (Object) this)).getChannel();
        if (channel != null && channel.config() instanceof RakNet.Config config) {
            assert MultiVersionUtil.ServerPlayerEntity$pingMillis1_20_1 != null;
            MultiVersionUtil.ServerPlayerEntity$pingMillis1_20_1.set(this.player, (int) ((config.getRTTNanos() + config.getRTTStdDevNanos()) / 1_000_000));
        }
    }

}
