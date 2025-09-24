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

package com.ishland.raknetify.fabric.common.util;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.state.NetworkState;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class MultiVersionUtil {

    private static final String INTERMEDIARY = "intermediary";
    private static final String CLASSNAME_ServerPlayNetworkHandler = "net.minecraft.class_3244";
    private static final String CLASSNAME_NetworkStatePacketHandler = "net.minecraft.class_2539$class_8698";
    private static final String CLASSNAME_NetworkState$InternalPacketHandler = "net.minecraft.class_2539$class_4532";
    private static final String CLASSNAME_NetworkState$Factory = "net.minecraft.class_9127$class_9128";
    private static final String CLASSNAME_ContextAwareNetworkStateFactory = "net.minecraft.class_10947";
    private static final String CLASSNAME_PlayStateFactories = "net.minecraft.class_9095";
    private static final String CLASSNAME_HungerManager = "net.minecraft.class_1702";
    private static final String CLASSNAME_Entity = "net.minecraft.class_1297";

    public static final VarHandle ServerPlayNetworkHandler$connection;
    public static final VarHandle ClientPlayNetworkHandler$connection;
    public static final VarHandle ServerPlayerEntity$pingMillis1_20_1;
    public static final Class<?> clazzNetworkStatePacketHandler;
    public static final VarHandle NetworkStatePacketHandler$backingHandler1_20_2;
    public static final MethodHandle NetworkState$Factory$bind1_20_5;
    public static final Class<?> clazzPlayStateFactories;
    public static final VarHandle PlayStateFactories$C2S1_20_5;
    public static final VarHandle PlayStateFactories$S2C1_20_5;
    public static final MethodHandle HungerManager$writeNbt1_21_5;
    public static final MethodHandle HungerManager$readNbt1_21_5;
    public static final MethodHandle Entity$getEntity1_21_8;

    static {
        try {
            final MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();

            {
                final List<Field> connFields = tryLocateFields(ServerPlayNetworkHandler.class, ClientConnection.class, false);
                if (connFields.size() != 1) {
                    throw new IllegalStateException("Ambiguous fields for ClientConnection in ServerPlayNetworkHandler: found " + Arrays.toString(connFields.toArray()));
                }
                if (connFields.isEmpty()) {
                    throw new IllegalStateException("Cannot find field for ClientConnection in ServerPlayNetworkHandler");
                }
                final Field connField = connFields.get(0);
                connField.setAccessible(true);
                ServerPlayNetworkHandler$connection = MethodHandles
                        .privateLookupIn(ServerPlayNetworkHandler.class, MethodHandles.lookup())
                        .unreflectVarHandle(connField);
            }

            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                final List<Field> connFields = tryLocateFields(ClientPlayNetworkHandler.class, ClientConnection.class, false);
                if (connFields.size() != 1) {
                    throw new IllegalStateException("Ambiguous fields for ClientConnection in ClientPlayNetworkHandler: found " + Arrays.toString(connFields.toArray()));
                }
                if (connFields.isEmpty()) {
                    throw new IllegalStateException("Cannot find field for ClientConnection in ClientPlayNetworkHandler");
                }
                final Field connField = connFields.get(0);
                connField.setAccessible(true);
                ClientPlayNetworkHandler$connection = MethodHandles
                        .privateLookupIn(ClientPlayNetworkHandler.class, MethodHandles.lookup())
                        .unreflectVarHandle(connField);
            } else {
                ClientPlayNetworkHandler$connection = null;
            }

            {
                final Field pingMillis1_20_1 = getOrNull(() -> ServerPlayerEntity.class.getDeclaredField(resolver.mapFieldName(INTERMEDIARY, "net.minecraft.class_3222", "field_13967", "I")), NoSuchFieldException.class);
                if (pingMillis1_20_1 != null) {
                    pingMillis1_20_1.setAccessible(true);
                    ServerPlayerEntity$pingMillis1_20_1 = MethodHandles
                            .privateLookupIn(ServerPlayerEntity.class, MethodHandles.lookup())
                            .unreflectVarHandle(pingMillis1_20_1);
                } else {
                    ServerPlayerEntity$pingMillis1_20_1 = null;
                }
            }

            {
                clazzNetworkStatePacketHandler = getOrNull(() -> Class.forName(resolver.mapClassName(INTERMEDIARY, CLASSNAME_NetworkStatePacketHandler)), ClassNotFoundException.class);
                if (clazzNetworkStatePacketHandler != null) {
                    final Field backingHandler1_20_2 = getOrNull(() -> clazzNetworkStatePacketHandler.getDeclaredField(resolver.mapFieldName(INTERMEDIARY, CLASSNAME_NetworkStatePacketHandler, "field_45674", "L" + resolver.mapClassName(INTERMEDIARY, CLASSNAME_NetworkState$InternalPacketHandler) + ";")), NoSuchFieldException.class);
                    if (backingHandler1_20_2 != null) {
                        backingHandler1_20_2.setAccessible(true);
                        NetworkStatePacketHandler$backingHandler1_20_2 = MethodHandles
                                .privateLookupIn(clazzNetworkStatePacketHandler, MethodHandles.lookup())
                                .unreflectVarHandle(backingHandler1_20_2);
                    } else {
                        NetworkStatePacketHandler$backingHandler1_20_2 = null;
                    }
                } else {
                    NetworkStatePacketHandler$backingHandler1_20_2 = null;
                }
            }

            {
                Method factoryBind = getOrNull(() -> NetworkState.Unbound.class.getDeclaredMethod("bind" /* actually not obfuscated in 1.20.6 */, Function.class), NoSuchMethodException.class, NoClassDefFoundError.class);
                if (factoryBind == null) {
                    factoryBind = getOrNull(() -> NetworkState.Unbound.class.getDeclaredMethod(resolver.mapMethodName(INTERMEDIARY, CLASSNAME_NetworkState$Factory, "method_61107", "(Ljava/util/function/Function;)Lnet/minecraft/class_9127;"), Function.class), NoSuchMethodException.class, NoClassDefFoundError.class);
                }
                if (factoryBind != null) {
                    factoryBind.setAccessible(true);
                    NetworkState$Factory$bind1_20_5 = MethodHandles
                            .privateLookupIn(NetworkState.Unbound.class, MethodHandles.lookup())
                            .unreflect(factoryBind);
                } else {
                    NetworkState$Factory$bind1_20_5 = null;
                }
            }

            clazzPlayStateFactories = getOrNull(() -> Class.forName(resolver.mapClassName(INTERMEDIARY, CLASSNAME_PlayStateFactories), false, MultiVersionUtil.class.getClassLoader()), ClassNotFoundException.class);

            {
                if (clazzPlayStateFactories != null) {
                    final Field playStateFactoriesC2S1_20_5 = getOrNull(() -> clazzPlayStateFactories.getDeclaredField(resolver.mapFieldName(INTERMEDIARY, CLASSNAME_PlayStateFactories, "field_48172", "L" + CLASSNAME_NetworkState$Factory.replace('.', '/') + ";")), NoSuchFieldException.class);
                    if (playStateFactoriesC2S1_20_5 != null) {
                        playStateFactoriesC2S1_20_5.setAccessible(true);
                        PlayStateFactories$C2S1_20_5 = MethodHandles
                                .privateLookupIn(clazzPlayStateFactories, MethodHandles.lookup())
                                .unreflectVarHandle(playStateFactoriesC2S1_20_5);
                    } else {
                        PlayStateFactories$C2S1_20_5 = null;
                    }
                } else {
                    PlayStateFactories$C2S1_20_5 = null;
                }
            }

            {
                if (clazzPlayStateFactories != null) {
                    final Field playStateFactoriesS2C1_20_5 = getOrNull(() -> clazzPlayStateFactories.getDeclaredField(resolver.mapFieldName(INTERMEDIARY, CLASSNAME_PlayStateFactories, "field_48173", "L" + CLASSNAME_NetworkState$Factory.replace('.', '/') + ";")), NoSuchFieldException.class);
                    if (playStateFactoriesS2C1_20_5 != null) {
                        playStateFactoriesS2C1_20_5.setAccessible(true);
                        PlayStateFactories$S2C1_20_5 = MethodHandles
                                .privateLookupIn(clazzPlayStateFactories, MethodHandles.lookup())
                                .unreflectVarHandle(playStateFactoriesS2C1_20_5);
                    } else {
                        PlayStateFactories$S2C1_20_5 = null;
                    }
                } else {
                    PlayStateFactories$S2C1_20_5 = null;
                }
            }

            {
                final Method writeNbt1_21_5 = getOrNull(() -> HungerManager.class.getDeclaredMethod(resolver.mapMethodName(INTERMEDIARY, CLASSNAME_HungerManager, "method_7582", "(Lnet/minecraft/class_2487;)V"), NbtCompound.class), NoSuchMethodException.class);
                if (writeNbt1_21_5 != null) {
                    writeNbt1_21_5.setAccessible(true);
                    HungerManager$writeNbt1_21_5 = MethodHandles
                            .privateLookupIn(HungerManager.class, MethodHandles.lookup())
                            .unreflect(writeNbt1_21_5);
                } else {
                    HungerManager$writeNbt1_21_5 = null;
                }
            }

            {
                final Method readNbt1_21_5 = getOrNull(() -> HungerManager.class.getDeclaredMethod(resolver.mapMethodName(INTERMEDIARY, CLASSNAME_HungerManager, "method_7584", "(Lnet/minecraft/class_2487;)V"), NbtCompound.class), NoSuchMethodException.class);
                if (readNbt1_21_5 != null) {
                    readNbt1_21_5.setAccessible(true);
                    HungerManager$readNbt1_21_5 = MethodHandles
                            .privateLookupIn(HungerManager.class, MethodHandles.lookup())
                            .unreflect(readNbt1_21_5);
                } else {
                    HungerManager$readNbt1_21_5 = null;
                }
            }

            {
                final Method getEntity1_21_8 = getOrNull(() -> Entity.class.getDeclaredMethod(resolver.mapFieldName(INTERMEDIARY, CLASSNAME_Entity, "method_37908", "()Lnet/minecraft/class_1937;")), NoSuchMethodException.class);
                if (getEntity1_21_8 != null) {
                    getEntity1_21_8.setAccessible(true);
                    Entity$getEntity1_21_8 = MethodHandles
                            .privateLookupIn(Entity.class, MethodHandles.lookup())
                            .unreflect(getEntity1_21_8);
                } else {
                    Entity$getEntity1_21_8 = null;
                }
            }

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static List<Field> tryLocateFields(Class<?> clazz, Class<?> fieldType, boolean includeMixinMerged) {
        final ImmutableList.Builder<Field> builder = ImmutableList.builder();
        final Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            builder.addAll(tryLocateFields(superclass, fieldType, includeMixinMerged));
        }
        __outerloop:
        for (Field field : clazz.getDeclaredFields()) {
            if (!includeMixinMerged) {
                for (Annotation annotation : field.getDeclaredAnnotations()) {
                    if (annotation.annotationType() == MixinMerged.class) {
                        continue __outerloop;
                    }
                }
            }

            if (field.getType() == fieldType) {
                builder.add(field);
            }
        }
        return builder.build();
    }

    public static void init() {
    }

    public static Class<?> tryLocateClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static <T> T getOrNull(SupplierThrowable<T> supplier, Class<? extends Throwable>... catchExceptions) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            for (Class<? extends Throwable> catchException : catchExceptions) {
                if (catchException.isInstance(t)) {
                    return null;
                }
            }

            throw new RuntimeException(t);
        }
    }

    private interface SupplierThrowable<T> {
        T get() throws Throwable;
    }

}
