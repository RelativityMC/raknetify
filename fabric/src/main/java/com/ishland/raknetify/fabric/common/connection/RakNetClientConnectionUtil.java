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

package com.ishland.raknetify.fabric.common.connection;

import com.ishland.raknetify.common.util.ThreadLocalUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.netty.channel.ChannelFuture;
import net.minecraft.network.ClientConnection;

import java.net.InetSocketAddress;

public class RakNetClientConnectionUtil {

    private RakNetClientConnectionUtil() {
    }

    public static ClientConnection connect(InetSocketAddress address, boolean useEpoll, boolean largeMTU, Operation<ClientConnection> original, boolean hasPerformanceLog) {
        System.out.println("aaa");
        try {
            ThreadLocalUtil.setInitializingRaknet(true);
            ThreadLocalUtil.setInitializingRaknetLargeMTU(largeMTU);
            if (hasPerformanceLog) {
                return original.call(address, useEpoll, null);
            } else {
                return original.call(address, useEpoll);
            }
        } finally {
            ThreadLocalUtil.setInitializingRaknet(false);
            ThreadLocalUtil.setInitializingRaknetLargeMTU(false);
        }
    }

    public static ChannelFuture connect(InetSocketAddress address, boolean useEpoll, boolean largeMTU, ClientConnection connection) {
        System.out.println("aaaa");
        try {
            ThreadLocalUtil.setInitializingRaknet(true);
            ThreadLocalUtil.setInitializingRaknetLargeMTU(largeMTU);
            return ClientConnection.connect(address, useEpoll, connection);
        } finally {
            ThreadLocalUtil.setInitializingRaknet(false);
            ThreadLocalUtil.setInitializingRaknetLargeMTU(false);
        }
    }

}
