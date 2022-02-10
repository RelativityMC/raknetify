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
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.MathHelper;
import network.ycc.raknet.RakNet;
import network.ycc.raknet.client.channel.RakNetClientChannel;
import network.ycc.raknet.client.channel.RakNetClientThreadedChannel;
import network.ycc.raknet.pipeline.UserDataCodec;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class RaknetClientConnectionUtil {

    private RaknetClientConnectionUtil() {
    }

    public static ClientConnection connect(InetSocketAddress address, boolean useEpoll) {
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
            return ClientConnection.connect(address, useEpoll);
        } finally {
            ThreadLocalUtil.setInitializingRaknet(false);
        }
    }

    public static void ping(InetSocketAddress address, ServerInfo info) {
        // TODO [VanillaCopy] from Lnet/minecraft/client/network/MultiplayerServerListPinger;ping(Ljava/net/InetSocketAddress;Lnet/minecraft/client/network/ServerInfo;)V
        (new Bootstrap()).group(ClientConnection.CLIENT_IO_GROUP.get())
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        // RaknetFabric
//                        try {
//                            channel.config().setOption(ChannelOption.TCP_NODELAY, true);
//                        } catch (ChannelException var3) {
//                        }

                        RakNet.config(channel).setMaxQueuedBytes(Constants.MAX_QUEUED_SIZE); // RaknetFabric

                        channel.pipeline()
                                .addLast("raknet_backend", new UserDataCodec(Constants.RAKNET_GAME_PACKET_ID)) // RaknetFabric
                                .addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                    @Override
                                    public void channelActive(ChannelHandlerContext context) throws Exception {
                                        super.channelActive(context);
                                        ByteBuf byteBuf = Unpooled.buffer();

                                        try {
                                            byteBuf.writeByte(254);
                                            byteBuf.writeByte(1);
                                            byteBuf.writeByte(250);
                                            char[] cs = "MC|PingHost".toCharArray();
                                            byteBuf.writeShort(cs.length);

                                            for (char c : cs) {
                                                byteBuf.writeChar(c);
                                            }

                                            byteBuf.writeShort(7 + 2 * address.getHostName().length());
                                            byteBuf.writeByte(127);
                                            cs = address.getHostName().toCharArray();
                                            byteBuf.writeShort(cs.length);

                                            for (char c : cs) {
                                                byteBuf.writeChar(c);
                                            }

                                            byteBuf.writeInt(address.getPort());
                                            context.channel().writeAndFlush(byteBuf).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                                        } finally {
                                            byteBuf.release();
                                        }

                                    }

                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) {
                                        short s = byteBuf.readUnsignedByte();
                                        if (s == 255) {
                                            String string = new String(byteBuf.readBytes(byteBuf.readShort() * 2).array(), StandardCharsets.UTF_16BE);
                                            String[] strings = Iterables.toArray(MultiplayerServerListPinger.ZERO_SPLITTER.split(string), String.class);
                                            if ("§1".equals(strings[0])) {
                                                int i = MathHelper.parseInt(strings[1], 0);
                                                String string2 = strings[2];
                                                String string3 = strings[3];
                                                int j = MathHelper.parseInt(strings[4], -1);
                                                int k = MathHelper.parseInt(strings[5], -1);
                                                info.protocolVersion = -1;
                                                info.version = new LiteralText(string2);
                                                info.label = new LiteralText(string3);
                                                info.playerCountLabel = MultiplayerServerListPinger.createPlayerCountText(j, k);
                                            }
                                        }

                                        channelHandlerContext.close();
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) {
                                        channelHandlerContext.close();
                                    }
                                });
                    }
                })
                .channelFactory(() -> { // RaknetFabric
                    final RakNetClientThreadedChannel channel = new RakNetClientThreadedChannel(NioDatagramChannel.class);
                    channel.setProvidedEventLoop(ClientConnection.CLIENT_IO_GROUP.get().next());
                    return channel;
                })
                .connect(address.getAddress(), address.getPort());
    }

}
