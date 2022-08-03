package com.ishland.raknetify.fabric.common.connection;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.packet.s2c.login.LoginCompressionS2CPacket;

public class RakNetCompressionCompatibilityHandler extends ChannelDuplexHandler {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof LoginCompressionS2CPacket) {
            promise.trySuccess();
            ctx.write(msg);
            ctx.pipeline().remove(this);
            return;
        }
        super.write(ctx, msg, promise);
    }
}
