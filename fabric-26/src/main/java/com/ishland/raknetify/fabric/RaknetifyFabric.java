/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022-2025 ishland
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

package com.ishland.raknetify.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ishland.raknetify.common.data.ProtocolMultiChannelMappings;
import com.ishland.raknetify.fabric.common.client.DebugHudUtil1_21_9;
import com.ishland.raknetify.fabric.common.util.MinecraftVersions;
import com.ishland.raknetify.fabric.common.connection.RakNetMultiChannel;
import com.ishland.raknetify.common.util.NetworkInterfaceListener;
import com.ishland.raknetify.fabric.common.util.FieldSignatureParser;
import com.ishland.raknetify.fabric.mixin.access.IPacketCodecDispatcher;
import io.netty.buffer.ByteBuf;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.internal.SystemPropertyUtil;
import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraft.SharedConstants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.common.CommonPacketTypes;
import net.minecraft.network.protocol.cookie.CookiePacketTypes;
import net.minecraft.network.protocol.game.GamePacketTypes;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.ping.PingPacketTypes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RaknetifyFabric implements ModInitializer, PreLaunchEntrypoint {

    private static final boolean SAVE_CHANNEL_MAPPINGS = Boolean.getBoolean("raknetify.saveChannelMappings");
    private static final boolean EXIT_AFTER_SAVE_CHANNEL_MAPPINGS = Boolean.getBoolean("raknetify.saveChannelMappings.exit");

    @Override
    public void onPreLaunch() {
    }

    @Override
    public void onInitialize() {

        if (FabricLoader.getInstance().isDevelopmentEnvironment())
            auditMixins();

        handleMappings();

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT && !MinecraftVersions.IS_26_4_SNAPSHOT) {
            DebugHudUtil1_21_9.init();
        }

        // If new property name is present, use it
        String levelStr = SystemPropertyUtil.get("io.netty.leakDetection.level", ResourceLeakDetector.Level.SIMPLE.name());
        ResourceLeakDetector.Level level = ResourceLeakDetector.Level.SIMPLE;
        try {
            level = ResourceLeakDetector.Level.valueOf(levelStr.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ignored) {
        }
        System.out.println("Raknetify: Using leak detector level %s".formatted(level));
        ResourceLeakDetector.setLevel(level);
    }

    private static void handleMappings() {
        RakNetMultiChannel.init();
        NetworkInterfaceListener.init();

        Int2IntArrayMap s2c = new Int2IntArrayMap();
        Int2IntArrayMap c2s = new Int2IntArrayMap();
        final var c2sPacketTypes = ((IPacketCodecDispatcher<ByteBuf, Packet<? super ServerGamePacketListener>, PacketType<Packet<? super ServerGamePacketListener>>>)
                    GameProtocols.SERVERBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(null), null).codec()).getById()
                    .stream().map(byteBufPacketPacketTypePacketType -> byteBufPacketPacketTypePacketType.type()).toList();
        final var s2cPacketTypes = ((IPacketCodecDispatcher<ByteBuf, Packet<? super ServerGamePacketListener>, PacketType<Packet<? super ServerGamePacketListener>>>)
                    GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(null)).codec()).getById()
                    .stream().map(byteBufPacketPacketTypePacketType -> byteBufPacketPacketTypePacketType.type()).toList();

        record FieldNodeClazzPair(FieldNode fieldNode, Class<?> clazz) {
        }

        Map<PacketType<?>, Class<?>> vanillaPacketTypes = new Object2ObjectOpenHashMap<>();
        for (FieldNodeClazzPair fieldNodeClazzPair : Stream.of(GamePacketTypes.class, CommonPacketTypes.class, CookiePacketTypes.class, PingPacketTypes.class)
                .flatMap(clazz -> {
                    try {
                        return MixinService.getService().getBytecodeProvider().getClassNode(Type.getInternalName(clazz)).fields.stream()
                                .map(fieldNode -> new FieldNodeClazzPair(fieldNode, clazz));
                    } catch (ClassNotFoundException | IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList()) {
            final FieldNode field = fieldNodeClazzPair.fieldNode();
            final List<Type> parameterGenerics = FieldSignatureParser.parse(field.signature);
            for (Type type : parameterGenerics) {
                try {
                    final Class<?> clazz = Class.forName(type.getClassName());
                    if (Packet.class.isAssignableFrom(clazz)) {
                        final PacketType<?> actualType;
                        try {
                            actualType = (PacketType<?>) fieldNodeClazzPair.clazz().getField(field.name).get(null);
                        } catch (IllegalAccessException | NoSuchFieldException e) {
                            throw new RuntimeException(e);
                        }
                        vanillaPacketTypes.put(actualType, clazz);
                    }
                } catch (ClassNotFoundException ignored) {
                }
            }
        }

        for (int i = 0, s2cPacketTypesSize = s2cPacketTypes.size(); i < s2cPacketTypesSize; i++) {
            PacketType<Packet<? super ServerGamePacketListener>> s2cPacketType = s2cPacketTypes.get(i);
            final Class<?> clazz = vanillaPacketTypes.get(s2cPacketType);
            if (clazz == null) {
                System.out.println("Skipping unmapped packet type: " + s2cPacketType);
                continue;
            }
            s2c.put(i, RakNetMultiChannel.getPacketChannelOverride(clazz, false));
        //                System.out.println("Mapped packet type: " + s2cPacketType + " to channel " + RakNetMultiChannel.getPacketChannelOverride(clazz));
        }

        for (int i = 0, c2sPacketTypesSize = c2sPacketTypes.size(); i < c2sPacketTypesSize; i++) {
            PacketType<Packet<? super ServerGamePacketListener>> c2sPacketType = c2sPacketTypes.get(i);
            final Class<?> clazz = vanillaPacketTypes.get(c2sPacketType);
            if (clazz == null) {
                System.out.println("Skipping unmapped packet type: " + c2sPacketType);
                continue;
            }
            c2s.put(i, RakNetMultiChannel.getPacketChannelOverride(clazz, false));
        //                System.out.println("Mapped packet type: " + c2sPacketType + " to channel " + RakNetMultiChannel.getPacketChannelOverride(clazz));
        }

        if (SAVE_CHANNEL_MAPPINGS) {
            auditMixins();
            final Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            ProtocolMultiChannelMappings mappings = new ProtocolMultiChannelMappings();
            // Preserve proxy mappings for versions no longer supported by Fabric.
            for (int protocol : new int[]{755, 756}) {
                final ProtocolMultiChannelMappings.VersionMapping retained = ProtocolMultiChannelMappings.INSTANCE.mappings.get(protocol);
                if (retained != null) mappings.mappings.put(protocol, retained);
            }
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
                                .collect(Collectors.toMap(Int2IntMap.Entry::getIntKey, Int2IntMap.Entry::getIntValue, (o, o2) -> {
                                    throw new RuntimeException("Unresolvable conflicts");
                                }, Int2IntArrayMap::new));
                        value.c2s = value.c2s.int2IntEntrySet().stream()
                                .sorted(Comparator.comparingInt(Int2IntMap.Entry::getIntKey))
                                .collect(Collectors.toMap(Int2IntMap.Entry::getIntKey, Int2IntMap.Entry::getIntValue, (o, o2) -> {
                                    throw new RuntimeException("Unresolvable conflicts");
                                }, Int2IntArrayMap::new));
                        return new AbstractInt2ObjectMap.BasicEntry<>(
                                entry.getIntKey(),
                                value
                        );
                    })
                    .sorted(Comparator.comparingInt(Int2ObjectMap.Entry::getIntKey))
                    .collect(Collectors.toMap(Int2ObjectMap.Entry::getIntKey, Int2ObjectMap.Entry::getValue, (o, o2) -> {
                        throw new RuntimeException("Unresolvable conflicts");
                    }, Int2ObjectArrayMap::new));

            try {
                Files.writeString(path, gson.toJson(mappings), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                System.out.println("Error writing generated mappings: " + e.toString());
            }
            if (EXIT_AFTER_SAVE_CHANNEL_MAPPINGS) System.exit(0);
        }
    }

    private static void auditMixins() {
        Logger auditLogger = LoggerFactory.getLogger("Raknetify Mixin Audit");
        try {
            final Class<?> transformerClazz = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer");
            if (transformerClazz.isInstance(MixinEnvironment.getCurrentEnvironment().getActiveTransformer())) {
                final Field processorField = transformerClazz.getDeclaredField("processor");
                processorField.setAccessible(true);
                final Object processor = processorField.get(MixinEnvironment.getCurrentEnvironment().getActiveTransformer());
                final Class<?> processorClazz = Class.forName("org.spongepowered.asm.mixin.transformer.MixinProcessor");
                final Field configsField = processorClazz.getDeclaredField("configs");
                configsField.setAccessible(true);
                final List<?> configs = (List<?>) configsField.get(processor);
                final Class<?> configClazz = Class.forName("org.spongepowered.asm.mixin.transformer.MixinConfig");
                final Method getUnhandledTargetsMethod = configClazz.getDeclaredMethod("getUnhandledTargets");
                getUnhandledTargetsMethod.setAccessible(true);
                Set<String> unhandled = new HashSet<>();
                for (Object config : configs) {
                    final Set<String> unhandledTargets = (Set<String>) getUnhandledTargetsMethod.invoke(config);
                    unhandled.addAll(unhandledTargets);
                }
                for (String s : unhandled) {
                    auditLogger.info("Loading class {}", s);
                    MixinService.getService().getClassProvider().findClass(s, false);
                }
                for (Object config : configs) {
                    for (String unhandledTarget : (Set<String>) getUnhandledTargetsMethod.invoke(config)) {
                        auditLogger.error("{} is already classloaded", unhandledTarget);
                    }
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to audit mixins", t);
        }
    }

}
