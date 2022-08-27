package com.ishland.raknetify.bungee.connection;

import com.google.common.base.Preconditions;
import com.ishland.raknetify.bungee.RaknetifyBungeePlugin;
import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.common.connection.MultiChannellingEncryption;
import com.ishland.raknetify.common.connection.RakNetSimpleMultiChannelCodec;
import com.ishland.raknetify.common.connection.SynchronizationLayer;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.md_5.bungee.EncryptionUtil;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.packet.Commands;
import net.md_5.bungee.protocol.packet.EncryptionResponse;
import net.md_5.bungee.protocol.packet.Login;
import net.md_5.bungee.protocol.packet.Respawn;
import net.md_5.bungee.protocol.packet.SetCompression;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.util.logging.Level;

public class RakNetBungeeClientChannelEventListener extends ChannelDuplexHandler {

    public static final String NAME = "raknetify-bungee-event-listener";

    private SecretKey encryptionKey = null;
    private boolean needResetCompression = false;

    private Protocol protocol = null;
    private int protocolVersion = -1;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (this.needResetCompression) {
            RaknetifyBungeePlugin.LOGGER.info("Preventing vanilla compression as streaming compression is enabled");

//            // note: this may corrupt bungeecords compression state, find a better way to do this
//            ctx.channel().pipeline().replace("compress", "compress", new ChannelDuplexHandler()); // no-op
//            ctx.channel().pipeline().replace("decompress", "decompress", new ChannelDuplexHandler()); // no-op

            final HandlerBoss handlerBoss = ctx.channel().pipeline().get(HandlerBoss.class);

            final Field channelField = HandlerBoss.class.getDeclaredField("channel");
            channelField.setAccessible(true);
            final ChannelWrapper channelWrapper = (ChannelWrapper) channelField.get(handlerBoss);
            Preconditions.checkState(channelWrapper != null, "channelWrapper is null");
            channelWrapper.setCompressionThreshold(-1);

            this.needResetCompression = false;
        }
        if (this.encryptionKey != null) {
            ctx.channel().pipeline().replace(PipelineUtils.DECRYPT_HANDLER, PipelineUtils.DECRYPT_HANDLER, new ChannelDuplexHandler());
            ctx.channel().pipeline().replace(PipelineUtils.ENCRYPT_HANDLER, PipelineUtils.ENCRYPT_HANDLER, new ChannelDuplexHandler());
            ctx.channel().pipeline().addBefore(MultiChannelingStreamingCompression.NAME, MultiChannellingEncryption.NAME, new MultiChannellingEncryption(encryptionKey));
            this.encryptionKey = null;
        }
        if (msg instanceof SetCompression) {
            final MultiChannelingStreamingCompression compression = ctx.channel().pipeline().get(MultiChannelingStreamingCompression.class);
            if (compression != null && compression.isActive()) {
                RaknetifyBungeePlugin.LOGGER.info("Preventing vanilla compression as streaming compression is enabled");
                promise.setSuccess(); // swallow SetCompression packet
                needResetCompression = true;
                return;
            }
        } else if (msg instanceof Respawn || msg instanceof Login) {
            ctx.write(SynchronizationLayer.SYNC_REQUEST_OBJECT); // sync
            super.write(ctx, msg, promise);
            return;
        } else if (msg instanceof Commands) {
            ctx.write(RakNetSimpleMultiChannelCodec.SIGNAL_START_MULTICHANNEL);
            super.write(ctx, msg, promise);
            return;
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof PacketWrapper wrapper) {
            if (wrapper.packet instanceof EncryptionResponse packet) {
                try {
                    this.encryptionKey = getSecretUnchecked(packet);
                } catch (Throwable t) {
                    RaknetifyBungeePlugin.LOGGER.log(Level.WARNING, "Failed to decrypt captured encryption secret, the raknetify connection is broken", t);
                }
            }
        }
        super.channelRead(ctx, msg);
    }

    private static SecretKey getSecretUnchecked(EncryptionResponse resp) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, EncryptionUtil.keys.getPrivate());
        return new SecretKeySpec(cipher.doFinal(resp.getSharedSecret()), "AES");
    }

    public void setProtocol(Protocol protocol, int protocolVersion) {
        this.protocol = protocol;
        this.protocolVersion = protocolVersion;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
    }

    //    @Override
//    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//        super.userEventTriggered(ctx, evt);
//        if (evt == VelocityConnectionEvent.COMPRESSION_ENABLED) {
//            final MultiChannelingStreamingCompression compression = ctx.channel().pipeline().get(MultiChannelingStreamingCompression.class);
//            if (compression != null && compression.isActive()) {
//                RaknetifyVelocityPlugin.LOGGER.info("Preventing vanilla compression as streaming compression is enabled");
//                ctx.channel().pipeline().replace(Connections.COMPRESSION_ENCODER, Connections.COMPRESSION_ENCODER, new ChannelDuplexHandler()); // no-op
//                ctx.channel().pipeline().replace(Connections.COMPRESSION_DECODER, Connections.COMPRESSION_DECODER, new ChannelDuplexHandler()); // no-op
//            }
//        } else if (evt == VelocityConnectionEvent.COMPRESSION_DISABLED) {
//            ctx.channel().pipeline().replace(Connections.FRAME_DECODER, Connections.FRAME_DECODER, new ChannelDuplexHandler()); // no-op
//            ctx.channel().pipeline().replace(Connections.FRAME_ENCODER, Connections.FRAME_ENCODER, new ChannelDuplexHandler()); // no-op
//        } else if (evt == VelocityConnectionEvent.ENCRYPTION_ENABLED) {
//            Preconditions.checkState(this.encryptionKey != null, "EncryptionResponse not received yet or already consumed");
//            ctx.channel().pipeline().replace(Connections.CIPHER_ENCODER, Connections.CIPHER_ENCODER, new ChannelDuplexHandler());
//            ctx.channel().pipeline().replace(Connections.CIPHER_DECODER, Connections.CIPHER_DECODER, new ChannelDuplexHandler());
//            ctx.channel().pipeline().addAfter(MultiChannelingStreamingCompression.NAME, MultiChannellingEncryption.NAME, new MultiChannellingEncryption(encryptionKey));
//        }
//    }
}
