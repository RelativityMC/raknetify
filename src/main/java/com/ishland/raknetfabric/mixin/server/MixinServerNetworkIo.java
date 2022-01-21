package com.ishland.raknetfabric.mixin.server;

import com.ishland.raknetfabric.Constants;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DecoderHandler;
import net.minecraft.network.LegacyQueryHandler;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.RateLimitedConnection;
import net.minecraft.network.SizePrepender;
import net.minecraft.network.SplitterHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerNetworkIo;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import net.minecraft.util.Lazy;
import network.ycc.raknet.RakNet;
import network.ycc.raknet.pipeline.UserDataCodec;
import network.ycc.raknet.server.channel.RakNetServerChannel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;
import java.util.List;

@Mixin(ServerNetworkIo.class)
public class MixinServerNetworkIo {

    @Shadow
    @Final
    private List<ChannelFuture> channels;

    @SuppressWarnings("deprecation")
    @Shadow
    @Final
    public static Lazy<NioEventLoopGroup> DEFAULT_CHANNEL;

    @SuppressWarnings("deprecation")
    @Shadow
    @Final
    public static Lazy<EpollEventLoopGroup> EPOLL_CHANNEL;

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    @Final
    private List<ClientConnection> connections;

    @Inject(method = "bind", at = @At(value = "HEAD"))
    private void bindUdp(InetAddress address, int port, CallbackInfo ci) {
        // TODO [VanillaCopy] modified
        synchronized (this.channels) {
            ChannelFactory<RakNetServerChannel> channelFactory; // RaknetFabric
            @SuppressWarnings("deprecation") Lazy<? extends EventLoopGroup> lazy;
            if (Epoll.isAvailable() && this.server.isUsingNativeTransport()) {
                channelFactory = () -> new RakNetServerChannel(EpollDatagramChannel.class); // RaknetFabric
                lazy = EPOLL_CHANNEL;
                System.out.println("Using epoll channel type for raknet"); // RaknetFabric
            } else {
                channelFactory = () -> new RakNetServerChannel(NioDatagramChannel.class); // RaknetFabric
                lazy = DEFAULT_CHANNEL;
                System.out.println("Using default channel type for raknet"); // RaknetFabric
            }

            this.channels
                    .add(
                            (new ServerBootstrap())
                                    .channelFactory(channelFactory) // RaknetFabric
                                    .childHandler(
                                            new ChannelInitializer<>() {
                                                @Override
                                                protected void initChannel(Channel channel) {
                                                    // RaknetFabric
//                                                    try {
//                                                        channel.config().setOption(ChannelOption.TCP_NODELAY, true);
//                                                    } catch (ChannelException var4) {
//                                                    }

                                                    RakNet.config(channel).setMaxQueuedBytes(Constants.MAX_QUEUED_SIZE); // RaknetFabric

                                                    channel.pipeline()
                                                            .addLast("raknet_backend", new UserDataCodec(Constants.RAKNET_PACKET_ID)) // RaknetFabric
                                                            .addLast("timeout", new ReadTimeoutHandler(30))
                                                            .addLast("legacy_query", new LegacyQueryHandler((ServerNetworkIo) (Object) MixinServerNetworkIo.this))
                                                            .addLast("splitter", new SplitterHandler())
                                                            .addLast("decoder", new DecoderHandler(NetworkSide.SERVERBOUND))
                                                            .addLast("prepender", new SizePrepender())
                                                            .addLast("encoder", new PacketEncoder(NetworkSide.CLIENTBOUND));
                                                    int i = MixinServerNetworkIo.this.server.getRateLimit();
                                                    ClientConnection clientConnection = i > 0
                                                            ? new RateLimitedConnection(i)
                                                            : new ClientConnection(NetworkSide.SERVERBOUND);
                                                    MixinServerNetworkIo.this.connections.add(clientConnection);
                                                    channel.pipeline().addLast("packet_handler", clientConnection);
                                                    clientConnection.setPacketListener(new ServerHandshakeNetworkHandler(MixinServerNetworkIo.this.server, clientConnection));
                                                }
                                            }
                                    )
                                    .group(lazy.get())
                                    .localAddress(address, port)
                                    .bind()
                                    .syncUninterruptibly()
                    );
        }
    }

}
