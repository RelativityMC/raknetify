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

package com.ishland.raknetify.common.connection;

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.FastThreadLocalThread;

import java.util.function.Supplier;

public class RaknetifyEventLoops {

    public static final Supplier<NioEventLoopGroup> NIO_EVENT_LOOP_GROUP =
            Suppliers.memoize(() -> new NioEventLoopGroup(
                            0,
                            new ThreadFactoryBuilder()
                                    .setThreadFactory(FastThreadLocalThread::new)
                                    .setNameFormat("Netty Server NIO Raknetify #%d")
                                    .setDaemon(true)
                                    .build()
                    )
            );

    public static final Supplier<NioEventLoopGroup> NIO_CLIENT_EVENT_LOOP_GROUP =
            Suppliers.memoize(() -> new NioEventLoopGroup(
                            0,
                            new ThreadFactoryBuilder()
                                    .setThreadFactory(FastThreadLocalThread::new)
                                    .setNameFormat("Netty Client NIO Raknetify #%d")
                                    .setDaemon(true)
                                    .build()
                    )
            );

    public static final Supplier<EpollEventLoopGroup> EPOLL_EVENT_LOOP_GROUP =
            Suppliers.memoize(() -> new EpollEventLoopGroup(
                            0,
                            new ThreadFactoryBuilder()
                                    .setThreadFactory(FastThreadLocalThread::new)
                                    .setNameFormat("Netty Server Epoll Raknetify  #%d")
                                    .setDaemon(true)
                                    .build()
                    )
            );

    public static final Supplier<EpollEventLoopGroup> EPOLL_CLIENT_EVENT_LOOP_GROUP =
            Suppliers.memoize(() -> new EpollEventLoopGroup(
                            0,
                            new ThreadFactoryBuilder()
                                    .setThreadFactory(FastThreadLocalThread::new)
                                    .setNameFormat("Netty Client Epoll Raknetify  #%d")
                                    .setDaemon(true)
                                    .build()
                    )
            );

    public static final Supplier<DefaultEventLoopGroup> DEFAULT_EVENT_LOOP_GROUP =
            Suppliers.memoize(() -> new DefaultEventLoopGroup(
                            0,
                            new ThreadFactoryBuilder()
                                    .setThreadFactory(FastThreadLocalThread::new)
                                    .setNameFormat("Netty Server App Raknetify #%d")
                                    .setDaemon(true)
                                    .build()
                    )
            );

    public static final Supplier<DefaultEventLoopGroup> DEFAULT_CLIENT_EVENT_LOOP_GROUP =
            Suppliers.memoize(() -> new DefaultEventLoopGroup(
                            0,
                            new ThreadFactoryBuilder()
                                    .setThreadFactory(FastThreadLocalThread::new)
                                    .setNameFormat("Netty Client App Raknetify #%d")
                                    .setDaemon(true)
                                    .build()
                    )
            );

}
