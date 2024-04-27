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

package com.ishland.raknetify.fabric.mixin.client;

import com.ishland.raknetify.fabric.common.connection.RakNetClientConnectionUtil;
import com.ishland.raknetify.common.util.PrefixUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetSocketAddress;

@Mixin(MultiplayerServerListPinger.class)
public abstract class MixinMultiplayerServerListPinger {

    @Dynamic
    @Redirect(method = {"add", "method_3003"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ServerAddress;parse(Ljava/lang/String;)Lnet/minecraft/client/network/ServerAddress;"))
    private ServerAddress modifyRaknetAddress(String address) {
        final PrefixUtil.Info info = PrefixUtil.getInfo(address);
        return ServerAddress.parse(info.stripped());
    }

    @Dynamic
    @WrapOperation(method = {"add", "method_3003"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;method_10753(Ljava/net/InetSocketAddress;Z)Lnet/minecraft/network/ClientConnection;"), require = 0)
    private ClientConnection redirectConnect(InetSocketAddress address, boolean useEpoll, Operation<ClientConnection> original, ServerInfo entry, Runnable runnable) {
        final PrefixUtil.Info info = PrefixUtil.getInfo(entry.address);
        return info.useRakNet() ? RakNetClientConnectionUtil.connect(address, useEpoll, info.largeMTU(), original, false) : original.call(address, useEpoll);
    }

}
