package com.ishland.raknetfabric.common.connection;

import com.google.common.collect.Iterables;
import com.ishland.raknetfabric.Constants;
import com.ishland.raknetfabric.common.util.ThreadLocalUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.util.math.MathHelper;
import network.ycc.raknet.RakNet;
import network.ycc.raknet.client.channel.RakNetClientThreadedChannel;
import network.ycc.raknet.pipeline.UserDataCodec;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class RaknetClientConnectionUtil {

    private RaknetClientConnectionUtil() {
    }

    public static ClientConnection connect(InetSocketAddress address, boolean useEpoll, boolean largeMTU) {
//        // [VanillaCopy] from Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetSocketAddress;Z)Lnet/minecraft/network/ClientConnection;
//        final ClientConnection clientConnection = new ClientConnection(NetworkSide.CLIENTBOUND);
//        ChannelFactory<RakNetClientChannel> channelFactory; // RaknetFabric
//        @SuppressWarnings("deprecation") Lazy<? extends EventLoopGroup> lazy;
//        if (Epoll.isAvailable() && useEpoll) {
//            channelFactory = () -> new RakNetClientChannel(EpollDatagramChannel.class); // RaknetFabric
//            lazy = EPOLL_CLIENT_IO_GROUP;
//        } else {
//            channelFactory = () -> new RakNetClientChannel(NioDatagramChannel.class); // RaknetFabric
//            lazy = CLIENT_IO_GROUP;
//        }
//
//        (new Bootstrap())
//                .group(lazy.get())
//                .handler(
//                        new ChannelInitializer<>() {
//                            @Override
//                            protected void initChannel(Channel channel) {
//                                // RaknetFabric
////                                try {
////                                    channel.config().setOption(ChannelOption.TCP_NODELAY, true);
////                                } catch (ChannelException var3) {
////                                }
//
//                                RakNet.config(channel).setMaxQueuedBytes(Constants.MAX_QUEUED_SIZE); // RaknetFabric
//
//                                channel.pipeline()
//                                        .addLast("raknet_backend", new UserDataCodec(Constants.RAKNET_GAME_PACKET_ID)) // RaknetFabric
//                                        .addLast("timeout", new ReadTimeoutHandler(30))
//                                        .addLast("splitter", new SplitterHandler())
//                                        .addLast("decoder", new DecoderHandler(NetworkSide.CLIENTBOUND))
//                                        .addLast("prepender", new SizePrepender())
//                                        .addLast("encoder", new PacketEncoder(NetworkSide.SERVERBOUND))
//                                        .addLast("packet_handler", clientConnection);
//                            }
//                        }
//                )
//                .channelFactory(channelFactory) // RaknetFabric
//                .connect(address.getAddress(), address.getPort())
//                .syncUninterruptibly();
//        return clientConnection;

        try {
            ThreadLocalUtil.setInitializingRaknet(true);
            ThreadLocalUtil.setInitializingRaknetLargeMTU(largeMTU);
            return ClientConnection.connect(address, useEpoll);
        } finally {
            ThreadLocalUtil.setInitializingRaknet(false);
            ThreadLocalUtil.setInitializingRaknetLargeMTU(false);
        }
    }

}
