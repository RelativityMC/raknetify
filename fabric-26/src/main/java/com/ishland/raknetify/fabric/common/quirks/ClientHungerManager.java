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

package com.ishland.raknetify.fabric.common.quirks;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;

public class ClientHungerManager extends FoodData {

    public static ClientHungerManager from(FoodData hungerManager) {
        ClientHungerManager clientHungerManager = new ClientHungerManager();
        for (Field field : FoodData.class.getFields()) {
            if ((field.getModifiers() & Modifier.STATIC) == 0 && (field.getModifiers() & Modifier.FINAL) == 0) {
                if (!field.trySetAccessible()) {
                    System.err.println("Failed to set field " + field.getName() + " accessible in HungerManager");
                    continue;
                }
                try {
                    Object value = field.get(hungerManager);
                    field.set(clientHungerManager, value);
                } catch (Throwable e) {
                    System.err.println("Failed to copy field " + field.getName() + " from HungerManager to ClientHungerManager");
                    e.printStackTrace();
                }
            }
        }
        return clientHungerManager;
    }

    public void eat(int food, float saturationModifier) {
        // nop
    }

    public void eat(FoodProperties foodComponent) {
        // nop
    }

}
