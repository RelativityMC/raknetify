package com.ishland.raknetify.velocity.connection;

import com.google.common.base.Preconditions;
import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.velocity.RaknetifyVelocityPlugin;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.VelocityConnectionEvent;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponse;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import com.velocitypowered.proxy.util.EncryptionUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class RakNetVelocityChannelEventListener extends ChannelDuplexHandler {

    public static final String NAME = "raknetify-velocity-event-listener";

    private SecretKey encryptionKey = null;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof SetCompression) {
            final MultiChannelingStreamingCompression compression = ctx.channel().pipeline().get(MultiChannelingStreamingCompression.class);
            if (compression != null && compression.isActive()) {
                RaknetifyVelocityPlugin.LOGGER.info("Preventing vanilla compression as streaming compression is enabled");
                promise.setSuccess(); // swallow SetCompression packet
                return;
            }
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof EncryptionResponse packet) {
            try {
                byte[] secret = EncryptionUtils.decryptRsa(((VelocityServer) RaknetifyVelocityPlugin.PROXY).getServerKeyPair(), packet.getSharedSecret());
                this.encryptionKey = new SecretKeySpec(secret, "AES");
            } catch (Throwable t) {
                RaknetifyVelocityPlugin.LOGGER.warn("Failed to decrypt captured encryption secret, the raknetify connection is broken", t);
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt == VelocityConnectionEvent.COMPRESSION_ENABLED) {
            final MultiChannelingStreamingCompression compression = ctx.channel().pipeline().get(MultiChannelingStreamingCompression.class);
            if (compression != null && compression.isActive()) {
                RaknetifyVelocityPlugin.LOGGER.info("Preventing vanilla compression as streaming compression is enabled");
                ctx.channel().pipeline().replace(Connections.COMPRESSION_ENCODER, Connections.COMPRESSION_ENCODER, new ChannelDuplexHandler()); // no-op
                ctx.channel().pipeline().replace(Connections.COMPRESSION_DECODER, Connections.COMPRESSION_DECODER, new ChannelDuplexHandler()); // no-op
            }
        } else if (evt == VelocityConnectionEvent.COMPRESSION_DISABLED) {
            ctx.channel().pipeline().replace(Connections.FRAME_DECODER, Connections.FRAME_DECODER, new ChannelDuplexHandler()); // no-op
            ctx.channel().pipeline().replace(Connections.FRAME_ENCODER, Connections.FRAME_ENCODER, new ChannelDuplexHandler()); // no-op
        } else if (evt == VelocityConnectionEvent.ENCRYPTION_ENABLED) {
            Preconditions.checkState(this.encryptionKey != null, "EncryptionResponse not received yet or already consumed");
            ctx.channel().pipeline().replace(Connections.CIPHER_ENCODER, Connections.CIPHER_ENCODER, new ChannelDuplexHandler());
            ctx.channel().pipeline().replace(Connections.CIPHER_DECODER, Connections.CIPHER_DECODER, new ChannelDuplexHandler());
            ctx.channel().pipeline().addAfter(MultiChannelingStreamingCompression.NAME, MultiChannellingEncryption.NAME, new MultiChannellingEncryption(encryptionKey));
        }
    }
}
