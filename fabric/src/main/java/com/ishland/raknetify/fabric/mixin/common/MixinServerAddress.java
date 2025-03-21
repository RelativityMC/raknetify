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

package com.ishland.raknetify.fabric.mixin.common;

import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.util.PrefixUtil;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.network.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerAddress.class)
public class MixinServerAddress {

    @WrapMethod(method = "parse")
    private static ServerAddress wrapParsing(String address, Operation<ServerAddress> original) {
        PrefixUtil.Info info = PrefixUtil.getInfo(address);
        if (info.useRakNet()) {
            ServerAddress addr = original.call(info.stripped());
            return new ServerAddress(
                    (info.largeMTU() ? Constants.RAKNET_LARGE_MTU_PREFIX : Constants.RAKNET_PREFIX) + addr.getAddress(),
                    addr.getPort()
            );
        } else {
            return original.call(address);
        }
    }

}
