package com.ishland.raknetify.common.connection.multichannel;

import com.google.common.collect.ImmutableMap;
import com.ishland.raknetify.common.Constants;
import com.ishland.raknetify.common.connection.RakNetSimpleMultiChannelCodec;
import com.ishland.raknetify.common.util.MathUtil;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Objects;
import java.util.function.IntPredicate;

public class CustomPayloadChannel {

    public static final Object2IntOpenHashMap<String> identifier2channel;

    static {
        // See .fabric.common.connection.RakNetMultiChannel

        identifier2channel = new Object2IntOpenHashMap<>();
        identifier2channel.defaultReturnValue(0);
        identifier2channel.put("porting_lib:extra_data_entity_spawn", 2);
    }

    public static class OverrideHandler implements RakNetSimpleMultiChannelCodec.OverrideHandler {

        private final IntPredicate isCustomPayload;

        public OverrideHandler(IntPredicate isCustomPayload) {
            this.isCustomPayload = Objects.requireNonNull(isCustomPayload);
        }

        @Override
        public int getChannelOverride(ByteBuf origBuf) {
            ByteBuf buf = origBuf.slice();
            final int packetId = MathUtil.readVarInt(buf);
            if (isCustomPayload.test(packetId)) {
                final String identifier = MathUtil.readString(buf); // we assume modern custom payloads
                if (Constants.DEBUG) System.out.println("Raknetify: Handling custom payload: " + identifier);
                return identifier2channel.getInt(identifier);
            } else {
                return 0;
            }
        }

    }

}
