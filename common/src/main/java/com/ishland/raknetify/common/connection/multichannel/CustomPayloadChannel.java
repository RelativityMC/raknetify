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

package com.ishland.raknetify.common.connection.multichannel;

import com.google.common.collect.ImmutableMap;
import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.RakNetSimpleMultiChannelCodec;
import com.ishland.raknetify.common.util.MathUtil;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Objects;
import java.util.function.IntPredicate;

public class CustomPayloadChannel {

    public static final Object2IntOpenHashMap<String> identifier2channel;

    static {
        // See .fabric.common.connection.RakNetMultiChannel

        identifier2channel = new Object2IntOpenHashMap<>();
        identifier2channel.defaultReturnValue(0);
        identifier2channel.put("porting_lib:extra_data_entity_spawn", 2);
    }

    public static class OverrideHandler implements RakNetSimpleMultiChannelCodec.OverrideHandler {

        private final IntPredicate isCustomPayload;

        public OverrideHandler(IntPredicate isCustomPayload) {
            this.isCustomPayload = Objects.requireNonNull(isCustomPayload);
        }

        @Override
        public int getChannelOverride(ByteBuf origBuf) {
            ByteBuf buf = origBuf.slice();
            final int packetId = MathUtil.readVarInt(buf);
            if (isCustomPayload.test(packetId)) {
                final String identifier = MathUtil.readString(buf); // we assume modern custom payloads
                if (Constants.DEBUG) System.out.println("Raknetify: Handling custom payload: " + identifier);
                return identifier2channel.getInt(identifier);
            } else {
                return 0;
            }
        }

    }

}
