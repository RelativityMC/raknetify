package com.ishland.raknetify.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ishland.raknetify.common.data.ProtocolMultiChannelMappings;
import com.ishland.raknetify.fabric.common.connection.RakNetMultiChannel;
import com.ishland.raknetify.common.util.NetworkInterfaceListener;
import com.ishland.raknetify.fabric.mixin.access.INetworkState;
import com.ishland.raknetify.fabric.mixin.access.INetworkStatePacketHandler;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.internal.SystemPropertyUtil;
import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.api.ModInitializer;
import net.minecraft.SharedConstants;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class RaknetifyFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        RakNetMultiChannel.init();
        RakNetMultiChannel.iterateKnownPackets();
        NetworkInterfaceListener.init();

        // If new property name is present, use it
        String levelStr = SystemPropertyUtil.get("io.netty.leakDetection.level", ResourceLeakDetector.Level.SIMPLE.name());
        ResourceLeakDetector.Level level = ResourceLeakDetector.Level.SIMPLE;
        try {
            level = ResourceLeakDetector.Level.valueOf(levelStr.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ignored) {
        }
        System.out.println("Raknetify: Using leak detector level %s".formatted(level));
        ResourceLeakDetector.setLevel(level);

        Int2IntArrayMap s2c = new Int2IntArrayMap();
        Int2IntArrayMap c2s = new Int2IntArrayMap();
        for (Map.Entry<NetworkSide, ? extends NetworkState.PacketHandler<?>> entry : ((INetworkState) (Object) NetworkState.PLAY).getPacketHandlers().entrySet()) {
            for (Object2IntMap.Entry<Class<? extends Packet<?>>> type : ((INetworkStatePacketHandler) entry.getValue()).getPacketIds().object2IntEntrySet()) {
                if (entry.getKey() == NetworkSide.CLIENTBOUND)
                    s2c.put(type.getIntValue(), RakNetMultiChannel.getPacketChannelOverride(type.getKey()));
                else if (entry.getKey() == NetworkSide.SERVERBOUND)
                    c2s.put(type.getIntValue(), RakNetMultiChannel.getPacketChannelOverride(type.getKey()));
            }
        }
        if (Boolean.getBoolean("raknetify.saveChannelMappings")) {
            final Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            ProtocolMultiChannelMappings mappings = new ProtocolMultiChannelMappings();
            Path path = Path.of("channelMappings.json");
            try {
                mappings = gson.fromJson(Files.readString(path), ProtocolMultiChannelMappings.class);
            } catch (IOException e) {
                System.out.println("Error reading previously generated mappings: " + e.toString());
            }
            final ProtocolMultiChannelMappings.VersionMapping versionMapping = new ProtocolMultiChannelMappings.VersionMapping();
            versionMapping.c2s = c2s;
            versionMapping.s2c = s2c;
            mappings.mappings.put(SharedConstants.getProtocolVersion(), versionMapping);

            // reproducible mappings
            mappings.mappings = mappings.mappings.int2ObjectEntrySet()
                    .stream()
                    .map(entry -> {
                        final ProtocolMultiChannelMappings.VersionMapping value = entry.getValue();
                        value.s2c = value.s2c.int2IntEntrySet().stream()
                                .sorted(Comparator.comparingInt(Int2IntMap.Entry::getIntKey))
                                .collect(Collectors.toMap(Int2IntMap.Entry::getIntKey, Int2IntMap.Entry::getIntValue, (o, o2) -> {throw new RuntimeException("Unresolvable conflicts");}, Int2IntArrayMap::new));
                        value.c2s = value.c2s.int2IntEntrySet().stream()
                                .sorted(Comparator.comparingInt(Int2IntMap.Entry::getIntKey))
                                .collect(Collectors.toMap(Int2IntMap.Entry::getIntKey, Int2IntMap.Entry::getIntValue, (o, o2) -> {throw new RuntimeException("Unresolvable conflicts");}, Int2IntArrayMap::new));
                        return new AbstractInt2ObjectMap.BasicEntry<>(
                                entry.getIntKey(),
                                value
                        );
                    })
                    .sorted(Comparator.comparingInt(Int2ObjectMap.Entry::getIntKey))
                    .collect(Collectors.toMap(Int2ObjectMap.Entry::getIntKey, Int2ObjectMap.Entry::getValue, (o, o2) -> {throw new RuntimeException("Unresolvable conflicts");}, Int2ObjectArrayMap::new));

            try {
                Files.writeString(path, gson.toJson(mappings), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                System.out.println("Error writing generated mappings: " + e.toString());
            }
            if (Boolean.getBoolean("raknetify.saveChannelMappings.exit")) System.exit(0);
        }
    }
}
