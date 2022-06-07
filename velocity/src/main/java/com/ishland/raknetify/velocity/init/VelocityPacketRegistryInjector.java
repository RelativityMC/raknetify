package com.ishland.raknetify.velocity.init;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Respawn;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.Map;
import java.util.function.Supplier;

import static com.ishland.raknetify.common.util.ReflectionUtil.accessible;

public class VelocityPacketRegistryInjector {

    public static void inject() {
        try {
            final StateRegistry.PacketRegistry registry = StateRegistry.PLAY.clientbound;
            final var versions = (Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>)
                    accessible(StateRegistry.PacketRegistry.class.getDeclaredField("versions")).get(registry);
            for (StateRegistry.PacketRegistry.ProtocolRegistry value : versions.values()) {
                final var packetClassToId = (Object2IntMap<Class<? extends MinecraftPacket>>)
                        accessible(StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("packetClassToId")).get(value);
                final var packetIdToSupplier = (IntObjectMap<Supplier<? extends MinecraftPacket>>)
                        accessible(StateRegistry.PacketRegistry.ProtocolRegistry.class.getDeclaredField("packetIdToSupplier")).get(value);
                if (packetClassToId.containsKey(Respawn.class)) {
                    final int packetId = packetClassToId.getInt(Respawn.class);
                    if (!packetIdToSupplier.containsKey(packetId)) {
                        packetIdToSupplier.put(packetId, Respawn::new); // make respawn packet no longer encodeOnly
                    }
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to inject velocity packet registry", t);
        }
    }

}
