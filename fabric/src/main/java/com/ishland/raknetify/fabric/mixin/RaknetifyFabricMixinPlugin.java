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

package com.ishland.raknetify.fabric.mixin;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class RaknetifyFabricMixinPlugin implements IMixinConfigPlugin {

    public static final boolean AFTER_1_20_1;
    public static final boolean AFTER_1_20_2;
    public static final boolean AFTER_1_20_4;
    public static final boolean AFTER_1_20_5;
    public static final boolean AFTER_1_21_4;

    static {
        try {
            AFTER_1_20_1 = VersionPredicate.parse(">1.20.1").test(FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion());
            AFTER_1_20_2 = VersionPredicate.parse(">1.20.2").test(FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion());
            AFTER_1_20_4 = VersionPredicate.parse(">1.20.4").test(FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion());
            AFTER_1_20_5 = VersionPredicate.parse(">1.20.5").test(FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion());
            AFTER_1_21_4 = VersionPredicate.parse(">1.21.4").test(FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion());
        } catch (VersionParsingException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void onLoad(String mixinPackage) {
        System.setProperty("raknetserver.maxPacketLoss", String.valueOf(Integer.MAX_VALUE));
        MixinExtrasBootstrap.init();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith("com.ishland.raknetify.fabric.mixin.client.")) {
            if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                if (mixinClassName.equals("com.ishland.raknetify.fabric.mixin.client.MixinMultiplayerServerListPinger1_20_2"))
                    return AFTER_1_20_1 && !AFTER_1_20_4;
                if (mixinClassName.equals("com.ishland.raknetify.fabric.mixin.client.MixinMultiplayerServerListPinger1_20_5"))
                    return AFTER_1_20_4;
                return true;
            } else {
                return false;
            }
        }
        if (mixinClassName.equals("com.ishland.raknetify.fabric.mixin.server.MixinServerPlayNetworkHandler1_20_1"))
            return !AFTER_1_20_1;
        if (mixinClassName.equals("com.ishland.raknetify.fabric.mixin.server.MixinServerCommonNetworkHandler"))
            return AFTER_1_20_1;
        if (mixinClassName.equals("com.ishland.raknetify.fabric.mixin.server.MixinPlayerManager1_20_2"))
            return AFTER_1_20_1;
        if (mixinClassName.equals("com.ishland.raknetify.fabric.mixin.common.MixinClientConnection1_20_2"))
            return AFTER_1_20_1;
        if (mixinClassName.equals("com.ishland.raknetify.fabric.mixin.server.MixinPlayerManager1_20_1"))
            return !AFTER_1_20_1;
        if (mixinClassName.equals("com.ishland.raknetify.fabric.mixin.access.INetworkState1_20_4"))
            return !AFTER_1_20_4;
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
