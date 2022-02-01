package com.ishland.raknetfabric.common.connection;

import com.ishland.raknetfabric.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class SynchronizationLayer extends ChannelDuplexHandler {

    // Request structure:
    // integer: syncId
    // byte: total channel count `n`
    // next `n` groups: {
    // byte: channel index
    // integer: current orderIndex (or lastOrderIndex, (nextOrderIndex - 1))
    // }
    // integer: current seqId (or lastReceivedSeqId, (nextSendSeqId - 1))
    //
    // Response callback is handled using reliable transport

    public static final Object SYNC_REQUEST_OBJECT = new Object();

    private static final Class<?> CLASS_QUEUE;
    private static final Field FIELD_QUEUE_LAST_ORDER_INDEX;
    private static final Field FIELD_RELIABILITY_NEXT_SEND_SEQ_ID;
    private static final Field FIELD_RELIABILITY_LAST_RECEIVED_SEQ_ID;

    static {
        try {
            CLASS_QUEUE = Class.forName("network.ycc.raknet.pipeline.FrameOrderIn$OrderedChannelPacketQueue");

            FIELD_QUEUE_LAST_ORDER_INDEX = accessible(CLASS_QUEUE.getDeclaredField("lastOrderIndex"));
            FIELD_RELIABILITY_NEXT_SEND_SEQ_ID = accessible(ReliabilityHandler.class.getDeclaredField("nextSendSeqId"));
            FIELD_RELIABILITY_LAST_RECEIVED_SEQ_ID = accessible(ReliabilityHandler.class.getDeclaredField("lastReceivedSeqId"));

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
    private int channelsLength;
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

            int originalChannelsLength = this.frameOrderOutNextOrderIndex.length;
            this.channelsLength = (int) (originalChannelsLength - this.channelToIgnore.intStream().filter(value -> value < originalChannelsLength).count());

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
                                    .formatted(channel,
                                            (int) FIELD_QUEUE_LAST_ORDER_INDEX.get(frameOrderInQueues[i]),
                                            orderIndex
                                    ));
                            FIELD_QUEUE_LAST_ORDER_INDEX.set(frameOrderInQueues[channel], orderIndex);
                        }
                        final int seqId = byteBuf.readInt();
                        System.out.println("ReliabilityHandler: %d -> %d".formatted(
                                (int) FIELD_RELIABILITY_LAST_RECEIVED_SEQ_ID.get(this.reliabilityHandler),
                                seqId
                        ));
                        FIELD_RELIABILITY_LAST_RECEIVED_SEQ_ID.set(this.reliabilityHandler, seqId);
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
    private final ObjectArrayList<Frame> queuedFrames = new ObjectArrayList<>();
    private boolean isWaitingForResponse = false;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg == SYNC_REQUEST_OBJECT) {
            if (isWaitingForResponse) return;

            dropSenderPackets();

            final ByteBuf byteBuf = ctx.alloc().buffer(1 + channelsLength * 5 + 4);
            byteBuf.writeByte(channelsLength);
            for (int channel = 0, frameOrderOutNextOrderIndexLength = frameOrderOutNextOrderIndex.length; channel < frameOrderOutNextOrderIndexLength; channel++) {
                if (channelToIgnore.contains(channel)) continue;
                int orderOutNextOrderIndex = frameOrderOutNextOrderIndex[channel];
                System.out.println("Writing sync packet: Channel %d: %d".formatted(channel, orderOutNextOrderIndex - 1));
                byteBuf.writeByte(channel);
                byteBuf.writeInt(orderOutNextOrderIndex - 1);
            }
            int seqId = (int) FIELD_RELIABILITY_NEXT_SEND_SEQ_ID.get(this.reliabilityHandler); // TODO implementation details (probable lib bug): nextSendSeqId == lastReceivedSeqId
            System.out.println("Writing sync packet: ReliabilityHandler: %d".formatted(seqId));
            byteBuf.writeInt(seqId);

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

    private void dropSenderPackets() {
        int droppedFrames = 0;

        ArrayList<Frame> retainedFrameList = new ArrayList<>();

        //noinspection CollectionAddAllCanBeReplacedWithConstructor
        retainedFrameList.addAll(this.reliabilityHandlerFrameQueue);
        this.reliabilityHandlerFrameQueue.clear();

        for (FrameSet frameSet : this.reliabilityHandlerPendingFrameSets.values()) {
            frameSet.createFrames(retainedFrameList::add);
        }
        this.reliabilityHandlerPendingFrameSets.clear();
        for (Iterator<Frame> iterator = retainedFrameList.iterator(); iterator.hasNext(); ) {
            Frame frame = iterator.next();
            if (frame.getReliability().isOrdered && !channelToIgnore.contains(frame.getOrderChannel())) {
                final ChannelPromise promise1 = frame.getPromise();
                if (promise1 != null) promise1.trySuccess();
                iterator.remove();
                frame.release();
                droppedFrames++;
            }
        }
        this.queuedFrames.addAll(retainedFrameList);

        System.out.println("Dropping %d frames".formatted(droppedFrames));

        this.reliabilityHandlerPendingFrameSets.clear();
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

        this.isWaitingForResponse = false;

        System.out.println("Picking up %d queued frames".formatted(this.queuedFrames.size()));
        this.reliabilityHandlerFrameQueue.addAll(this.queuedFrames);
        this.queuedFrames.clear();

        System.out.println("Flushing %d queued packets as synchronization finished".formatted(this.queue.size()));
        while (!this.queue.isEmpty()) {
            final ChannelPromise promise = this.queue.firstKey();
            final Object msg = this.queue.removeFirst();
            ctx.write(msg, promise);
        }
    }

}
