package com.ishland.raknetfabric.mixin.server;

import com.ishland.raknetfabric.common.util.ThreadLocalUtil;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerNetworkIo;
import network.ycc.raknet.server.channel.RakNetServerChannel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.net.InetAddress;

@Mixin(ServerNetworkIo.class)
public abstract class MixinServerNetworkIo {

    @Shadow
    @Final
    private MinecraftServer server;

//    @Inject(method = "bind", at = @At(value = "HEAD"))
//    private void bindUdp(InetAddress address, int port, CallbackInfo ci) {
//        // [VanillaCopy] modified
//        synchronized (this.channels) {
//            ChannelFactory<RakNetServerChannel> channelFactory; // RaknetFabric
//            @SuppressWarnings("deprecation") Lazy<? extends EventLoopGroup> lazy;
//            if (Epoll.isAvailable() && this.server.isUsingNativeTransport()) {
//                channelFactory = () -> new RakNetServerChannel(EpollDatagramChannel.class); // RaknetFabric
//                lazy = EPOLL_CHANNEL;
//                System.out.println("Using epoll channel type for raknet"); // RaknetFabric
//            } else {
//                channelFactory = () -> new RakNetServerChannel(NioDatagramChannel.class); // RaknetFabric
//                lazy = DEFAULT_CHANNEL;
//                System.out.println("Using default channel type for raknet"); // RaknetFabric
//            }
//
//            this.channels
//                    .add(
//                            (new ServerBootstrap())
//                                    .channelFactory(channelFactory) // RaknetFabric
//                                    .childHandler(
//                                            new ChannelInitializer<>() {
//                                                @Override
//                                                protected void initChannel(Channel channel) {
//                                                    // RaknetFabric
////                                                    try {
////                                                        channel.config().setOption(ChannelOption.TCP_NODELAY, true);
////                                                    } catch (ChannelException var4) {
////                                                    }
//
//                                                    RakNet.config(channel).setMaxQueuedBytes(Constants.MAX_QUEUED_SIZE); // RaknetFabric
//
//                                                    channel.pipeline()
//                                                            .addLast("raknet_backend", new UserDataCodec(Constants.RAKNET_PACKET_ID)) // RaknetFabric
//                                                            .addLast("timeout", new ReadTimeoutHandler(30))
//                                                            .addLast("legacy_query", new LegacyQueryHandler((ServerNetworkIo) (Object) MixinServerNetworkIo.this))
//                                                            .addLast("splitter", new SplitterHandler())
//                                                            .addLast("decoder", new DecoderHandler(NetworkSide.SERVERBOUND))
//                                                            .addLast("prepender", new SizePrepender())
//                                                            .addLast("encoder", new PacketEncoder(NetworkSide.CLIENTBOUND));
//                                                    int i = MixinServerNetworkIo.this.server.getRateLimit();
//                                                    ClientConnection clientConnection = i > 0
//                                                            ? new RateLimitedConnection(i)
//                                                            : new ClientConnection(NetworkSide.SERVERBOUND);
//                                                    MixinServerNetworkIo.this.connections.add(clientConnection);
//                                                    channel.pipeline().addLast("packet_handler", clientConnection);
//                                                    clientConnection.setPacketListener(new ServerHandshakeNetworkHandler(MixinServerNetworkIo.this.server, clientConnection));
//                                                }
//                                            }
//                                    )
//                                    .group(lazy.get())
//                                    .localAddress(address, port)
//                                    .bind()
//                                    .syncUninterruptibly()
//                    );
//        }
//    }

    @Shadow public abstract void bind(@Nullable InetAddress address, int port) throws IOException;

    @Inject(method = "bind", at = @At("HEAD"))
    private void bindUdp(InetAddress address, int port, CallbackInfo ci) throws IOException {
        if (!ThreadLocalUtil.isInitializingRaknet()) {
            try {
                ThreadLocalUtil.setInitializingRaknet(true);
                bind(address, port);
            } finally {
                ThreadLocalUtil.setInitializingRaknet(false);
            }
        }
    }

    @Redirect(method = "bind", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/ServerBootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;", remap = false))
    private AbstractBootstrap<ServerBootstrap, ServerChannel> redirectChannel(ServerBootstrap instance, Class<? extends ServerSocketChannel> aClass) {
        final boolean useEpoll = Epoll.isAvailable() && this.server.isUsingNativeTransport();
        return ThreadLocalUtil.isInitializingRaknet()
                ? instance.channelFactory(() -> new RakNetServerChannel(useEpoll ? EpollDatagramChannel.class : NioDatagramChannel.class))
                : instance.channel(aClass);
    }

}
