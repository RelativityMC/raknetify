package com.ishland.raknetify.bungee.connection;

import com.google.common.base.Preconditions;
import com.ishland.raknetify.bungee.RaknetifyBungeePlugin;
import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.SynchronizationLayer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.KeepAlive;
import net.md_5.bungee.protocol.packet.Respawn;
import network.ycc.raknet.RakNet;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RakNetBungeeServerChannelEventListener extends ChannelDuplexHandler {

    public static final String NAME = "raknetify-bungee-downstream-event-listener";

    private final Channel clientChannel;

    public RakNetBungeeServerChannelEventListener(Channel clientChannel) {
        Preconditions.checkArgument(clientChannel.config() instanceof RakNet.Config);
        this.clientChannel = clientChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof PacketWrapper wrapper) {
            final DefinedPacket packet = wrapper.packet;
            if (packet instanceof Respawn) {
                clientChannel.write(SynchronizationLayer.SYNC_REQUEST_OBJECT);
            } else if (packet instanceof KeepAlive) {
                if (Constants.DEBUG) RaknetifyBungeePlugin.LOGGER.info("Received downstream keepalive, swallowing it");
                final long rttNanos = RakNet.config(clientChannel).getRTTNanos();
//                ProxyServer.getInstance().getScheduler()
//                        .schedule(RaknetifyBungeePlugin.INSTANCE, () -> ctx.write(packet), Math.max(rttNanos - 4_000_000, 0), TimeUnit.NANOSECONDS); // reduce delay to aid scheduling overhead
                ctx.channel().eventLoop().schedule(() -> ctx.writeAndFlush(packet), Math.max(rttNanos - 4_000_000, 0), TimeUnit.NANOSECONDS); // reduce delay to aid scheduling overhead
                return; // prevent keepalive from being sent to clients
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        for (Map.Entry<String, ChannelHandler> entry : ctx.channel().pipeline().toMap().entrySet()) {
            System.out.println("%s: %s".formatted(entry.getKey(), entry.getValue().getClass().getName()));
        }
    }
}
