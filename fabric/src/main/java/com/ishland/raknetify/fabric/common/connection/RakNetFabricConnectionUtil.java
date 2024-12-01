/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022-2023 ishland
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

package com.ishland.raknetify.fabric.common.connection;

import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.MultiChannelingStreamingCompression;
import com.ishland.raknetify.common.connection.RakNetConnectionUtil;
import com.ishland.raknetify.common.connection.RakNetSimpleMultiChannelCodec;
import com.ishland.raknetify.fabric.common.compat.viafabric.ViaFabricCompatInjector;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import network.ycc.raknet.RakNet;

public class RakNetFabricConnectionUtil {

    public static final String NAME_RAKNETIFY_MULTI_CHANNEL_PACKET_CATURE = "raknetify-multi-channel-packet-cature";

    private RakNetFabricConnectionUtil() {
    }

    public static void initChannel(Channel channel) {
        if (channel.config() instanceof RakNet.Config) {
            RakNetConnectionUtil.initChannel(channel);
            channel.pipeline().addAfter(MultiChannelingStreamingCompression.NAME, RakNetSimpleMultiChannelCodec.NAME, new RakNetSimpleMultiChannelCodec(Constants.RAKNET_GAME_PACKET_ID));
        }
    }

    public static void postInitChannel(Channel channel, boolean isClientSide) {
        if (channel.config() instanceof RakNet.Config) {
            ViaFabricCompatInjector.inject(channel, isClientSide);
            channel.pipeline().replace("timeout", "timeout", new ChannelDuplexHandler()); // no-op
            channel.pipeline().replace("splitter", "splitter", new ChannelDuplexHandler()); // no-op
            channel.pipeline().replace("prepender", "prepender", new ChannelDuplexHandler()); // no-op
            final MultiChannellingPacketCapture handler = new MultiChannellingPacketCapture();
            channel.pipeline().addLast(NAME_RAKNETIFY_MULTI_CHANNEL_PACKET_CATURE, handler);
            onPipelineReorder(channel.pipeline());
            channel.pipeline().get(RakNetSimpleMultiChannelCodec.class)
                    .addHandler(handler.getCustomPayloadHandler())
                    .addHandler(handler.getCaptureBasedHandler());
            channel.pipeline().addLast("raknetify-handle-compression-compatibility", new RakNetCompressionCompatibilityHandler());
            channel.pipeline().addBefore("packet_handler", RakNetFabricChannelEventListener.NAME, new RakNetFabricChannelEventListener());
        }
    }

    static void onPipelineReorder(ChannelPipeline pipeline) {
        if (pipeline.get("encoder") == null) {
//            System.out.println("Reordering failed: no encoder");
            return;
        }
//        System.out.println("Reordering");
        ChannelHandler handler = pipeline.remove(RakNetFabricConnectionUtil.NAME_RAKNETIFY_MULTI_CHANNEL_PACKET_CATURE);
        if (handler != null) {
            pipeline.addAfter("encoder", RakNetFabricConnectionUtil.NAME_RAKNETIFY_MULTI_CHANNEL_PACKET_CATURE, handler);
        }
    }

}
