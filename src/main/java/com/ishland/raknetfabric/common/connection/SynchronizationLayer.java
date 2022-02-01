package com.ishland.raknetfabric.common.connection;

import com.ishland.raknetfabric.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import network.ycc.raknet.frame.Frame;
import network.ycc.raknet.frame.FrameData;
import network.ycc.raknet.packet.FrameSet;
import network.ycc.raknet.packet.FramedPacket;
import network.ycc.raknet.pipeline.FrameOrderIn;
import network.ycc.raknet.pipeline.FrameOrderOut;
import network.ycc.raknet.pipeline.ReliabilityHandler;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public class SynchronizationLayer extends ChannelDuplexHandler {

    // Request structure:
    // integer: syncId
    // byte: total channel count `n`
    // next `n` groups:
    // byte: channel index
    // integer: current orderIndex (or lastOrderIndex, (nextOrderIndex - 1))
    //
    // Response callback is handled using reliable transport

    public static final Object SYNC_REQUEST_OBJECT = new Object();

    private static final Class<?> CLASS_QUEUE;
    private static final Field FIELD_QUEUE_LAST_ORDER_INDEX;

    static {
        try {
            CLASS_QUEUE = Class.forName("network.ycc.raknet.pipeline.FrameOrderIn$OrderedChannelPacketQueue");

            FIELD_QUEUE_LAST_ORDER_INDEX = accessible(CLASS_QUEUE.getDeclaredField("lastOrderIndex"));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static Field accessible(Field field) {
        field.setAccessible(true);
        return field;
    }

    private static Method accessible(Method method) {
        method.setAccessible(true);
        return method;
    }

    private final IntSet channelToIgnore = new IntOpenHashSet();

    private FrameOrderIn frameOrderIn;
    private Object[] frameOrderInQueues;
    private FrameOrderOut frameOrderOut;
    private int[] frameOrderOutNextOrderIndex;
    private ReliabilityHandler reliabilityHandler;
    private ObjectSortedSet<Frame> reliabilityHandlerFrameQueue;
    private Int2ObjectMap<FrameSet> reliabilityHandlerPendingFrameSets;
    private boolean initialized = false;

    public SynchronizationLayer(int... channelsToIgnore) {
        for (int ch : channelsToIgnore) {
            channelToIgnore.add(ch);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        try {
            this.frameOrderIn = ctx.pipeline().get(FrameOrderIn.class);
            Object frameOrderInQueueArray = accessible(FrameOrderIn.class.getDeclaredField("channels")).get(this.frameOrderIn);
            this.frameOrderInQueues = new Object[Array.getLength(frameOrderInQueueArray)];
            for (int i = 0; i < this.frameOrderInQueues.length; i ++) {
                this.frameOrderInQueues[i] = Array.get(frameOrderInQueueArray, i);
            }

            this.frameOrderOut = ctx.pipeline().get(FrameOrderOut.class);
            this.frameOrderOutNextOrderIndex = (int[]) accessible(FrameOrderOut.class.getDeclaredField("nextOrderIndex")).get(this.frameOrderOut);

            this.reliabilityHandler = ctx.pipeline().get(ReliabilityHandler.class);
            this.reliabilityHandlerFrameQueue = (ObjectSortedSet<Frame>) accessible(ReliabilityHandler.class.getDeclaredField("frameQueue")).get(this.reliabilityHandler);
            this.reliabilityHandlerPendingFrameSets = (Int2ObjectMap<FrameSet>) accessible(ReliabilityHandler.class.getDeclaredField("pendingFrameSets")).get(this.reliabilityHandler);

            initialized = true;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!initialized) {
            super.channelRead(ctx, msg);
            return;
        }
        if (msg instanceof FrameData packet) {
            if (packet.getPacketId() == Constants.RAKNET_SYNC_PACKET_ID) {
                // read
                {
                    System.out.println("Received sync packet");
                    final ByteBuf byteBuf = packet.createData().skipBytes(1);
                    try {
                        final byte count = byteBuf.readByte();
                        for (int i = 0; i < count; i++) {
                            final int channel = byteBuf.readByte();
                            final int orderIndex = byteBuf.readInt();
                            System.out.println("Channel %d: %d -> %d"
                                    .formatted(channel, (int) FIELD_QUEUE_LAST_ORDER_INDEX.get(frameOrderInQueues[i]), orderIndex));
                            FIELD_QUEUE_LAST_ORDER_INDEX.set(frameOrderInQueues[channel], orderIndex);
                        }
                    } finally {
                        byteBuf.release();
                    }
                    return;
                }
            }
        }
        super.channelRead(ctx, msg);
    }

    private final Reference2ReferenceLinkedOpenHashMap<ChannelPromise, Object> queue = new Reference2ReferenceLinkedOpenHashMap<>();
    private boolean isWaitingForResponse = false;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg == SYNC_REQUEST_OBJECT) {
            if (isWaitingForResponse) return;

            System.out.println("Dropping %d previously queued frames".formatted(this.reliabilityHandlerFrameQueue.size()));
            this.reliabilityHandlerFrameQueue.forEach(frame -> {
                final ChannelPromise promise1 = frame.getPromise();
                if (promise1 != null) promise1.trySuccess();
                frame.release();
            });
            this.reliabilityHandlerFrameQueue.clear();

            System.out.println("Dropping %d previously pending udp packets".formatted(this.reliabilityHandlerPendingFrameSets.size()));
            this.reliabilityHandlerPendingFrameSets.values().forEach(frameSet -> {
                frameSet.succeed();
                frameSet.release();
            });
            this.reliabilityHandlerPendingFrameSets.clear();

            final int channelsLength = frameOrderOutNextOrderIndex.length;
            final ByteBuf byteBuf = ctx.alloc().buffer(1 + channelsLength * 4);
            byteBuf.writeByte(channelsLength);
            for (int channel = 0, frameOrderOutNextOrderIndexLength = frameOrderOutNextOrderIndex.length; channel < frameOrderOutNextOrderIndexLength; channel++) {
                if (channelToIgnore.contains(channel)) continue;
                int orderOutNextOrderIndex = frameOrderOutNextOrderIndex[channel];
                System.out.println("Writing sync packet: Channel %d: %d".formatted(channel, orderOutNextOrderIndex - 1));
                byteBuf.writeByte(channel);
                byteBuf.writeInt(orderOutNextOrderIndex - 1);
            }
            final FrameData frameData = FrameData.create(ctx.alloc(), Constants.RAKNET_SYNC_PACKET_ID, byteBuf);
            frameData.setReliability(FramedPacket.Reliability.RELIABLE);
            this.isWaitingForResponse = true;
            ctx.write(frameData, promise).addListener(future -> this.flushQueue(ctx));
            return;
        }
        if (isWaitingForResponse) {
            this.queue.put(promise, msg);
            return;
        }
        super.write(ctx, msg, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        flush(ctx);
        super.exceptionCaught(ctx, cause);
    }

    private void flushQueue(ChannelHandlerContext ctx) {
        if (!isWaitingForResponse) {
            System.out.println("Ignoring duplicate call to flushQueue()");
            return;
        }
        if (!ctx.channel().eventLoop().inEventLoop()) {
            ctx.channel().eventLoop().execute(() -> flushQueue(ctx));
            return;
        }
        System.out.println("Flushing %d queued packets as synchronization finished".formatted(this.queue.size()));
        this.isWaitingForResponse = false;
        while (!this.queue.isEmpty()) {
            final ChannelPromise promise = this.queue.firstKey();
            final Object msg = this.queue.removeFirst();
            ctx.write(msg, promise);
        }
    }

}
