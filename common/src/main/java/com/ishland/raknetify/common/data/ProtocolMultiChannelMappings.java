package com.ishland.raknetify.common.data;

import com.google.gson.Gson;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProtocolMultiChannelMappings {

    public static final ProtocolMultiChannelMappings INSTANCE;

    static {
        final InputStream resource = ProtocolMultiChannelMappings.class.getClassLoader().getResourceAsStream("raknetify-channel-mappings.json");
        if (resource == null) {
            System.err.println("Raknetify: Failed to load raknetify channel mappings");
            INSTANCE = new ProtocolMultiChannelMappings();
        } else {
            ProtocolMultiChannelMappings read = new ProtocolMultiChannelMappings();
            try (var in = resource;
                    var reader = new InputStreamReader(resource)) {
                final Gson gson = new Gson();
                read = gson.fromJson(reader, ProtocolMultiChannelMappings.class);
            } catch (IOException e) {
                System.err.println("Raknetify: Failed to load raknetify channel mappings");
                e.printStackTrace();
            }
            INSTANCE = read;
        }
    }

    public static void init() {
    }

    public Int2ObjectOpenHashMap<VersionMapping> mappings = new Int2ObjectOpenHashMap<>();

    public static class VersionMapping {
        public Int2IntOpenHashMap s2c = new Int2IntOpenHashMap();
        public Int2IntOpenHashMap c2s = new Int2IntOpenHashMap();
    }

}
