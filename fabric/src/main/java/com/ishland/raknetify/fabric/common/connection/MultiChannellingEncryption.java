package com.ishland.raknetify.fabric.common.connection;

import com.ishland.raknetify.fabric.common.connection.encryption.PacketEncryptionManagerInterface;
import com.ishland.raknetify.fabric.mixin.access.IPacketEncryptionManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.encryption.PacketEncryptionManager;
import network.ycc.raknet.frame.FrameData;
import org.jetbrains.annotations.NotNull;

public class MultiChannellingEncryption extends ChannelDuplexHandler {

    public static final String NAME = "raknetify-multichannel-encryption";

    private final PacketEncryptionManager decryption;
    private final PacketEncryptionManager encryption;

    public MultiChannellingEncryption(PacketEncryptionManager decryption, PacketEncryptionManager encryption) {
        this.decryption = decryption;
        this.encryption = encryption;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ((PacketEncryptionManagerInterface) decryption).setContext(ctx);
        ((PacketEncryptionManagerInterface) encryption).setContext(ctx);
        super.handlerAdded(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof FrameData data) {
            final ByteBuf buf = data.createData().skipBytes(1);
            ByteBuf res = null;
            FrameData resFrame = null;
            try {
                res = ctx.alloc().buffer(data.getDataSize());
                ((IPacketEncryptionManager) encryption).invokeEncrypt(buf, res);
                resFrame = FrameData.create(ctx.alloc(), data.getPacketId(), res);
                res = null;
                resFrame.setOrderChannel(data.getOrderChannel());
                resFrame.setReliability(data.getReliability());
                ctx.write(resFrame, promise);
                resFrame = null;
                return;
            } finally {
                buf.release();
                if (res != null) res.release();
                if (resFrame != null) resFrame.release();
            }
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        if (msg instanceof FrameData data) {
            final ByteBuf buf = data.createData().skipBytes(1);
            ByteBuf res = null;
            FrameData resFrame = null;
            try {
                res = ((IPacketEncryptionManager) decryption).invokeDecrypt(ctx, buf);
                resFrame = FrameData.create(ctx.alloc(), data.getPacketId(), res);
                res = null;
                resFrame.setOrderChannel(data.getOrderChannel());
                resFrame.setReliability(data.getReliability());
                ctx.fireChannelRead(resFrame);
                resFrame = null;
                return;
            } finally {
                buf.release();
                if (res != null) res.release();
                if (resFrame != null) resFrame.release();
            }
        }
        super.channelRead(ctx, msg);
    }
}
