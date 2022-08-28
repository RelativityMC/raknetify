/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022 ishland
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

package com.ishland.raknetify.bungee.init;

import com.google.common.collect.ForwardingSet;
import com.ishland.raknetify.bungee.RaknetifyBungeePlugin;
import io.netty.channel.Channel;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class InjectedSet extends ForwardingSet<Channel> {

    private final Set<Channel> delegate;

    public InjectedSet(Set<Channel> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean add(Channel element) {
        ProxyServer.getInstance().getScheduler().schedule(
                RaknetifyBungeePlugin.INSTANCE,
                () -> BungeeRaknetifyServer.injectChannel((BungeeCord) ProxyServer.getInstance(), element, false),
                100, TimeUnit.MILLISECONDS);

        return super.add(element);
    }

    @Override
    public void clear() {
        BungeeRaknetifyServer.stopAll();

        super.clear();
    }

    @Override
    protected Set<Channel> delegate() {
        return this.delegate;
    }
}
