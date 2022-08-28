/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022 ishland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ishland.raknetify.common.connection;

import com.ishland.raknetify.common.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import network.ycc.raknet.frame.Frame;
import network.ycc.raknet.frame.FrameData;
import network.ycc.raknet.packet.FrameSet;
import network.ycc.raknet.packet.FramedPacket;
import network.ycc.raknet.pipeline.FrameJoiner;
import network.ycc.raknet.pipeline.FrameOrderIn;
import network.ycc.raknet.pipeline.FrameOrderOut;
import network.ycc.raknet.pipeline.ReliabilityHandler;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.PriorityQueue;

import static com.ishland.raknetify.common.util.ReflectionUtil.accessible;

public class SynchronizationLayer extends ChannelDuplexHandler {

    // Request structure:
    // byte: total channel count `n`
    // next `n` groups: {
    // byte: channel index
    // integer: current orderIndex (or lastOrderIndex, (nextOrderIndex - 1))
    // }
    // integer: current seqId (or lastReceivedSeqId, (nextSendSeqId - 1))
    //
    // Response callback is handled using reliable transport

    public static final Object SYNC_REQUEST_OBJECT = new Object();

    static final Class<?> CLASS_QUEUE;
    static final Class<?> CLASS_FRAME_JOINER_BUILDER;
    static final Field FIELD_QUEUE_LAST_ORDER_INDEX;
    static final Method METHOD_QUEUE_BUILDER_RELEASE;
    static final Method METHOD_QUEUE_CLEAR;
    static final Field FIELD_RELIABILITY_NEXT_SEND_SEQ_ID;
    static final Field FIELD_RELIABILITY_LAST_RECEIVED_SEQ_ID;
    static final Field FIELD_RELIABILITY_QUEUED_BYTES;
    static final Field FIELD_FRAME_JOINER_BUILDER_SAMPLE_PACKET;

    static {
        try {
            CLASS_QUEUE = Class.forName("network.ycc.raknet.pipeline.FrameOrderIn$OrderedChannelPacketQueue");
            CLASS_FRAME_JOINER_BUILDER = Class.forName("network.ycc.raknet.pipeline.FrameJoiner$Builder");

            FIELD_QUEUE_LAST_ORDER_INDEX = accessible(CLASS_QUEUE.getDeclaredField("lastOrderIndex"));
            FIELD_RELIABILITY_NEXT_SEND_SEQ_ID = accessible(ReliabilityHandler.class.getDeclaredField("nextSendSeqId"));
            FIELD_RELIABILITY_LAST_RECEIVED_SEQ_ID = accessible(ReliabilityHandler.class.getDeclaredField("lastReceivedSeqId"));
            FIELD_RELIABILITY_QUEUED_BYTES = accessible(ReliabilityHandler.class.getDeclaredField("queuedBytes"));
            FIELD_FRAME_JOINER_BUILDER_SAMPLE_PACKET = accessible(CLASS_FRAME_JOINER_BUILDER.getDeclaredField("samplePacket"));

            METHOD_QUEUE_BUILDER_RELEASE = accessible(CLASS_FRAME_JOINER_BUILDER.getDeclaredMethod("release"));
            METHOD_QUEUE_CLEAR = accessible(CLASS_QUEUE.getDeclaredMethod("clear"));

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private final IntSet channelToIgnore = new IntOpenHashSet();

    private FrameOrderIn frameOrderIn;
    private Object[] frameOrderInQueues;
    private FrameOrderOut frameOrderOut;
    private int[] frameOrderOutNextOrderIndex;
    private ReliabilityHandler reliabilityHandler;
    private PriorityQueue<Frame> reliabilityHandlerFrameQueue;
    private Int2ObjectMap<FrameSet> reliabilityHandlerPendingFrameSets;
    private FrameJoiner frameJoiner;
    private Int2ObjectOpenHashMap<?> frameJoinerPendingPackets;
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
        initializeIfNecessary(ctx);
    }

    private void initializeIfNecessary(ChannelHandlerContext ctx) {
        if (initialized) return;
        try {
            this.frameOrderIn = ctx.pipeline().get(FrameOrderIn.class);
            Object frameOrderInQueueArray = accessible(FrameOrderIn.class.getDeclaredField("channels")).get(this.frameOrderIn);
            this.frameOrderInQueues = new Object[Array.getLength(frameOrderInQueueArray)];
            for (int i = 0; i < this.frameOrderInQueues.length; i++) {
                this.frameOrderInQueues[i] = Array.get(frameOrderInQueueArray, i);
            }

            this.frameOrderOut = ctx.pipeline().get(FrameOrderOut.class);
            this.frameOrderOutNextOrderIndex = (int[]) accessible(FrameOrderOut.class.getDeclaredField("nextOrderIndex")).get(this.frameOrderOut);

            this.reliabilityHandler = ctx.pipeline().get(ReliabilityHandler.class);
            this.reliabilityHandlerFrameQueue = (PriorityQueue<Frame>) accessible(ReliabilityHandler.class.getDeclaredField("frameQueue")).get(this.reliabilityHandler);
            this.reliabilityHandlerPendingFrameSets = (Int2ObjectMap<FrameSet>) accessible(ReliabilityHandler.class.getDeclaredField("pendingFrameSets")).get(this.reliabilityHandler);

            int originalChannelsLength = this.frameOrderOutNextOrderIndex.length;
            //noinspection deprecation
            this.channelsLength = (int) (originalChannelsLength - this.channelToIgnore.stream().filter(value -> value < originalChannelsLength).count());

            this.frameJoiner = ctx.pipeline().get(FrameJoiner.class);
            this.frameJoinerPendingPackets = (Int2ObjectOpenHashMap<?>) accessible(FrameJoiner.class.getDeclaredField("pendingPackets")).get(this.frameJoiner);

            initialized = true;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        initializeIfNecessary(ctx);
        if (msg instanceof FrameData packet && packet.getPacketId() == Constants.RAKNET_SYNC_PACKET_ID) {
            // read
            {
                if (Constants.DEBUG) System.out.println("Raknetify: Received sync packet");
                ctx.fireChannelRead(SYNC_REQUEST_OBJECT);
                final ByteBuf byteBuf = packet.createData().skipBytes(1);
                try {
                    final byte count = byteBuf.readByte();
                    for (int i = 0; i < count; i++) {
                        final byte channel = byteBuf.readByte();
                        final int orderIndex = byteBuf.readInt();
                        if (Constants.DEBUG)
                            System.out.println("Raknetify: Channel %d: %d -> %d"
                                    .formatted(channel,
                                            (int) FIELD_QUEUE_LAST_ORDER_INDEX.get(frameOrderInQueues[channel]),
                                            orderIndex
                                    ));
                        FIELD_QUEUE_LAST_ORDER_INDEX.set(frameOrderInQueues[channel], orderIndex);
                        final ObjectIterator<?> iterator = this.frameJoinerPendingPackets.values().iterator();
                        while (iterator.hasNext()) {
                            final Object next = iterator.next();
                            try {
                                final Frame frame = (Frame) FIELD_FRAME_JOINER_BUILDER_SAMPLE_PACKET.get(next);
                                if (frame.getReliability().isOrdered && frame.getOrderChannel() == channel) {
                                    METHOD_QUEUE_BUILDER_RELEASE.invoke(next);
                                    iterator.remove();
                                }
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    final int seqId = byteBuf.readInt();
                    if (Constants.DEBUG)
                        System.out.println("Raknetify: ReliabilityHandler: %d -> %d".formatted(
                                (int) FIELD_RELIABILITY_LAST_RECEIVED_SEQ_ID.get(this.reliabilityHandler),
                                seqId
                        ));
                    FIELD_RELIABILITY_LAST_RECEIVED_SEQ_ID.set(this.reliabilityHandler, seqId);
                } finally {
                    byteBuf.release();
                }
            }
            return;
        }
        ctx.fireChannelRead(msg);
    }

    private final Reference2ReferenceLinkedOpenHashMap<ChannelPromise, Object> queue = new Reference2ReferenceLinkedOpenHashMap<>();
    private final ObjectArrayList<Frame> queuedFrames = new ObjectArrayList<>();
    private boolean isWaitingForResponse = false;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        initializeIfNecessary(ctx);
        if (msg == SYNC_REQUEST_OBJECT) {
            if (isWaitingForResponse) return;

            dropSenderPackets();

            final ByteBuf byteBuf = ctx.alloc().buffer(1 + channelsLength * 5 + 4);
            byteBuf.writeByte(channelsLength);
            for (int channel = 0, frameOrderOutNextOrderIndexLength = frameOrderOutNextOrderIndex.length; channel < frameOrderOutNextOrderIndexLength; channel++) {
                if (channelToIgnore.contains(channel)) continue;
                int orderOutNextOrderIndex = frameOrderOutNextOrderIndex[channel];
                if (Constants.DEBUG)
                    System.out.println("Raknetify: Writing sync packet: Channel %d: %d".formatted(channel, orderOutNextOrderIndex - 1));
                byteBuf.writeByte(channel);
                byteBuf.writeInt(orderOutNextOrderIndex - 1);
            }
            int seqId = (int) FIELD_RELIABILITY_NEXT_SEND_SEQ_ID.get(this.reliabilityHandler); // TODO implementation details (probable lib bug): nextSendSeqId == lastReceivedSeqId
            if (Constants.DEBUG)
                System.out.println("Raknetify: Writing sync packet: ReliabilityHandler: %d".formatted(seqId));
            byteBuf.writeInt(seqId);

            final FrameData frameData = FrameData.create(ctx.alloc(), Constants.RAKNET_SYNC_PACKET_ID, byteBuf);
            frameData.setReliability(FramedPacket.Reliability.RELIABLE);
            this.isWaitingForResponse = true;
            ctx.write(frameData, promise).addListener(future -> this.flushQueue(ctx));
            byteBuf.release();
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
            frameSet.release();
        }
        this.reliabilityHandlerPendingFrameSets.clear();

        int byteSize = 0;
        for (Iterator<Frame> iterator = retainedFrameList.iterator(); iterator.hasNext(); ) {
            Frame frame = iterator.next();
            if (frame.getReliability().isOrdered && !channelToIgnore.contains(frame.getOrderChannel())) {
                final ChannelPromise promise1 = frame.getPromise();
                if (promise1 != null) promise1.trySuccess();
                iterator.remove();
                frame.release();
                droppedFrames++;
            } else {
                byteSize += frame.getRoughPacketSize();
            }
        }
        this.queuedFrames.addAll(retainedFrameList);

        if (Constants.DEBUG) System.out.println("Raknetify: Dropping %d frames".formatted(droppedFrames));
        try {
            FIELD_RELIABILITY_QUEUED_BYTES.set(this.reliabilityHandler, byteSize);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

        this.reliabilityHandlerPendingFrameSets.clear();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        flush(ctx);
        super.exceptionCaught(ctx, cause);
    }

    private void flushQueue(ChannelHandlerContext ctx) {
        if (!isWaitingForResponse) {
            if (Constants.DEBUG) System.out.println("Raknetify: Ignoring duplicate call to flushQueue()");
            return;
        }
        if (!ctx.channel().eventLoop().inEventLoop()) {
            ctx.channel().eventLoop().execute(() -> flushQueue(ctx));
            return;
        }

        this.isWaitingForResponse = false;

        if (Constants.DEBUG) System.out.println("Raknetify: Picking up %d queued frames".formatted(this.queuedFrames.size()));
        this.reliabilityHandlerFrameQueue.addAll(this.queuedFrames);
        this.queuedFrames.clear();

        if (Constants.DEBUG) System.out.println("Raknetify: Flushing %d queued packets as synchronization finished".formatted(this.queue.size()));
        while (!this.queue.isEmpty()) {
            final ChannelPromise promise = this.queue.firstKey();
            final Object msg = this.queue.removeFirst();
            ctx.write(msg, promise);
        }
    }

}
