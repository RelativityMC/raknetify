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

package com.ishland.raknetify.bungee;

import com.ishland.raknetify.bungee.connection.RakNetBungeeConnectionUtil;
import com.ishland.raknetify.bungee.init.BungeeRaknetifyServer;
import com.ishland.raknetify.common.data.ProtocolMultiChannelMappings;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.logging.Logger;

public class RaknetifyBungeePlugin extends Plugin implements Listener {

    public static Logger LOGGER;
    public static RaknetifyBungeePlugin INSTANCE;

    @Override
    public void onEnable() {
        super.onEnable();

        INSTANCE = this;
        LOGGER = this.getLogger();

        ProtocolMultiChannelMappings.init();

        BungeeRaknetifyServer.inject();

        this.getProxy().getPluginManager().registerListener(this, this);
    }

    @Override
    public void onDisable() {
        super.onDisable();

        BungeeRaknetifyServer.disable();
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent evt) {
        RakNetBungeeConnectionUtil.onPlayerLogin(evt);
    }

    @EventHandler
    public void handleServerSwitch(ServerConnectedEvent evt) {
        RakNetBungeeConnectionUtil.handleServerSwitch(evt);
    }

}
