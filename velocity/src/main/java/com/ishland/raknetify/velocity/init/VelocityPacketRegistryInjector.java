/*
 * This file is a part of the Velocity implementation of the Raknetify
 * project, licensed under GPLv3.
 *
 * Copyright (c) 2022 ishland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
