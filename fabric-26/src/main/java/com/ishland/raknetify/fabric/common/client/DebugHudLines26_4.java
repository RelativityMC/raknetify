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

import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import java.lang.reflect.Method;

final class DebugHudLines26_4 {
    private static final Object GROUP;
    private static final Method ADD_LINE;

    static {
        try {
            Class<?> builderClass = Class.forName("net.minecraft.client.gui.components.debug.DebugGroup$Builder");
            Object builder = builderClass.getMethod("titled", String.class).invoke(null, "Raknetify");
            GROUP = builderClass.getMethod("build").invoke(builder);
            ADD_LINE = DebugScreenDisplayer.class.getMethod("addToGroup", GROUP.getClass(), String.class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private DebugHudLines26_4() {
    }

    static void addLine(DebugScreenDisplayer lines, String text) {
        try {
            ADD_LINE.invoke(lines, GROUP, text);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot display Raknetify debug information", exception);
        }
    }
}
