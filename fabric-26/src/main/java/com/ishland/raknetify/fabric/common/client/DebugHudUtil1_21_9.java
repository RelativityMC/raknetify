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

package com.ishland.raknetify.fabric.common.client;

import com.ishland.raknetify.fabric.mixin.access.IDebugHudEntries;
import com.ishland.raknetify.fabric.common.util.MinecraftVersions;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

public class DebugHudUtil1_21_9 {

    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static final Identifier ID = Identifier.fromNamespaceAndPath("raknetify", "raknetify_debug_info");

    public static void init() {
        if (!initialized.compareAndSet(false, true)) return;

        IDebugHudEntries.invokeRegister(ID, createEntry());
    }

    public static DebugScreenEntry createEntry() {
        return new DebugScreenEntry() {
            @Override
            public void display(DebugScreenDisplayer lines, @Nullable Level world, @Nullable LevelChunk clientChunk, @Nullable LevelChunk chunk) {
                if (MinecraftVersions.IS_26_4_SNAPSHOT) {
                    DebugHudUtil.getDebugString(s -> DebugHudLines26_4.addLine(lines, s));
                } else {
                    DebugHudUtil.getDebugString(s -> lines.addToGroup(ID, s));
                }
            }

            @Override
            public boolean isAllowed(boolean reducedDebugInfo) {
                return true;
            }
        };
    }

}
