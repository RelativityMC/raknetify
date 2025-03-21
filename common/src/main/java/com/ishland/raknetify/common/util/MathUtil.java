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

package com.ishland.raknetify.common.util;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class MathUtil {

    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.2f %ciB", value / 1024.0, ci.current());
    }

    public static int readVarInt(ByteBuf buf) {
        // TODO [VanillaCopy]
        int i = 0;
        int j = 0;

        byte b;
        do {
            b = buf.readByte();
            i |= (b & 127) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((b & 128) == 128);

        return i;
    }

    public static String readString(ByteBuf buf) {
        return readString(buf, Short.MAX_VALUE);
    }

    public static String readString(ByteBuf buf, int maxLen) {
        // Copied from BungeeCord
        int len = readVarInt(buf);
        if (len > maxLen * 3) {
            throw new IllegalArgumentException("Cannot receive string longer than " + maxLen * 3 + " (got " + len + " bytes)");
        }

        String s = buf.toString(buf.readerIndex(), len, Charsets.UTF_8);
        buf.readerIndex(buf.readerIndex() + len);

        if (s.length() > maxLen) {
            throw new IllegalArgumentException("Cannot receive string longer than " + maxLen + " (got " + s.length() + " characters)");
        }

        return s;
    }

}
