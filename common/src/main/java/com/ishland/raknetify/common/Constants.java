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

package com.ishland.raknetify.common;

public class Constants {

    public static final boolean DEBUG = Boolean.getBoolean("raknetify.debug");

    public static final String RAKNET_PREFIX = "raknet;";
    public static final String RAKNET_LARGE_MTU_PREFIX = "raknetl;";
    public static final int RAKNET_PING_PACKET_ID = 0xFA;
    public static final int RAKNET_GAME_PACKET_ID = 0xFD;
    public static final int RAKNET_STREAMING_COMPRESSION_PACKET_ID = 0xED;
    public static final int RAKNET_STREAMING_COMPRESSION_HANDSHAKE_PACKET_ID = 0xEC;
    public static final int RAKNET_SYNC_PACKET_ID = 0xFC;
    public static final int RAKNET_METRICS_SYNC_PACKET_ID = 0xFB;
    public static final int MAX_QUEUED_SIZE = 256 * 1024 * 1024;
    public static final int DEFAULT_MTU = 1400;
    public static final int LARGE_MTU = 8192;
    public static final int MAX_PENDING_FRAME_SETS = 512;
    public static final int DEFAULT_PENDING_FRAME_SETS = 4;
    public static final int[] SYNC_IGNORE_CHANNELS = new int[] {1};

    private Constants() {
    }

}
