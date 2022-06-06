package com.ishland.raknetify.bungee.connection;

import com.ishland.raknetify.common.connection.SynchronizationLayer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.Respawn;

public class RakNetBungeeServerChannelEventListener extends ChannelDuplexHandler {

    public static final String NAME = "raknetify-bungee-downstream-event-listener";

    private final Channel clientChannel;

    public RakNetBungeeServerChannelEventListener(Channel clientChannel) {
        this.clientChannel = clientChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof PacketWrapper wrapper) {
            if (wrapper.packet instanceof Respawn) {
                clientChannel.write(SynchronizationLayer.SYNC_REQUEST_OBJECT);
            }
        }
        super.channelRead(ctx, msg);
    }
}
