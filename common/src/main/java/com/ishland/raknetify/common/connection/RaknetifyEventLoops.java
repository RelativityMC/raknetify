package com.ishland.raknetify.common.connection;

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
                                    .setNameFormat("Raknetify NIO #%d")
                                    .setDaemon(true)
                                    .build()
                    )
            );

    public static final Supplier<EpollEventLoopGroup> EPOLL_EVENT_LOOP_GROUP =
            Suppliers.memoize(() -> new EpollEventLoopGroup(
                            0,
                            new ThreadFactoryBuilder()
                                    .setThreadFactory(FastThreadLocalThread::new)
                                    .setNameFormat("Raknetify Epoll #%d")
                                    .setDaemon(true)
                                    .build()
                    )
            );

}
