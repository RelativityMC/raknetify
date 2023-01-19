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

package com.ishland.raknetify.fabric.common.compat.viafabric;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Constructor;

public class ViaFabricCompatInjector {

    public static void inject(Channel channel, boolean isClientSide) {
        Preconditions.checkNotNull(channel);
        if (FabricLoader.getInstance().isModLoaded("viafabric")) {
            try {
                final Class<?> userConnectionImplClass = Class.forName("com.viaversion.viaversion.connection.UserConnectionImpl");
                final Constructor<?> userConnectionImplConstructor = userConnectionImplClass.getConstructor(Channel.class, boolean.class);
                final Object user = userConnectionImplConstructor.newInstance(channel, isClientSide);

                final Class<?> userConnectionClass = Class.forName("com.viaversion.viaversion.api.connection.UserConnection");

                final Class<?> protocolPipelineImplClass = Class.forName("com.viaversion.viaversion.protocol.ProtocolPipelineImpl");
                protocolPipelineImplClass.getConstructor(userConnectionClass).newInstance(user);

                final Class<?> commonTransformerClass = Class.forName("com.viaversion.fabric.common.handler.CommonTransformer");
                final String handlerEncoderName = (String) commonTransformerClass.getField("HANDLER_ENCODER_NAME").get(null);
                final String handlerDecoderName = (String) commonTransformerClass.getField("HANDLER_DECODER_NAME").get(null);

                @SuppressWarnings("unchecked") final Class<? extends MessageToMessageEncoder<ByteBuf>> fabricEncodeHandlerClass = (Class<? extends MessageToMessageEncoder<ByteBuf>>) Class.forName("com.viaversion.fabric.common.handler.FabricEncodeHandler");
                final MessageToMessageEncoder<ByteBuf> fabricEncodeHandler = fabricEncodeHandlerClass.getConstructor(userConnectionClass).newInstance(user);

                @SuppressWarnings("unchecked") final Class<? extends MessageToMessageDecoder<ByteBuf>> fabricDecodeHandlerClass = (Class<? extends MessageToMessageDecoder<ByteBuf>>) Class.forName("com.viaversion.fabric.common.handler.FabricDecodeHandler");
                final MessageToMessageDecoder<ByteBuf> fabricDecodeHandler = fabricDecodeHandlerClass.getConstructor(userConnectionClass).newInstance(user);

                channel.pipeline().addBefore("encoder", handlerEncoderName, fabricEncodeHandler);
                channel.pipeline().addBefore("decoder", handlerDecoderName, fabricDecodeHandler);
            } catch (Throwable t) {
                //noinspection RedundantStringFormatCall
                System.err.println(String.format("Raknetify: Could not inject ViaVersion compatibility into RakNet channel %s: %s", channel, t));
                t.printStackTrace();
            }
        }
    }

}
