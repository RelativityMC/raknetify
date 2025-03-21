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

package com.ishland.raknetify.fabric.mixin.compat.fabricapi;

import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.fabric.mixin.access.IClientConnection;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.networking.server.ServerLoginNetworkAddon")
public class MixinServerLoginNetworkAddon {

    @Shadow
    @Final
    private ClientConnection connection;

    @Dynamic("Pseudo")
    @Redirect(method = "sendCompressionPacket()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getNetworkCompressionThreshold()I"))
    private int stopCompressionIfStreamingCompressionExists(MinecraftServer server) {
        final MultiChannelingStreamingCompression compression = ((IClientConnection) this.connection).getChannel().pipeline().get(MultiChannelingStreamingCompression.class);
        if (compression != null && compression.isActive()) {
            System.out.println("Raknetify: Preventing vanilla compression as streaming compression is enabled");
            return -1;
        }
        return server.getNetworkCompressionThreshold();
    }

}
