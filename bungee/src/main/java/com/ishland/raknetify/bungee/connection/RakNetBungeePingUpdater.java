package com.ishland.raknetify.bungee.connection;

import com.google.common.base.Preconditions;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import network.ycc.raknet.RakNet;
import network.ycc.raknet.packet.Ping;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RakNetBungeePingUpdater extends ChannelDuplexHandler {

    public static final String NAME = "raknetify-bungee-ping-updater";

    private final UserConnection player;

    ScheduledFuture<?> updateTask = null;

    public RakNetBungeePingUpdater(UserConnection player) {
        this.player = Objects.requireNonNull(player);
    }

    public void handlerAdded(ChannelHandlerContext ctx) {
        if (ctx.channel().config() instanceof RakNet.Config config) {
            updateTask = ctx.channel().eventLoop().scheduleAtFixedRate(
                    () -> player.setPing((int) ((config.getRTTNanos() + config.getRTTStdDevNanos()) / 1_000_000)),
                    0, 1000, TimeUnit.MILLISECONDS
            );
        }
    }

    public void handlerRemoved(ChannelHandlerContext ctx) {
        if (updateTask != null) {
            updateTask.cancel(false);
            updateTask = null;
        }
    }

}
