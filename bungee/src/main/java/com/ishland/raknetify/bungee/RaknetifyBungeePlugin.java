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

    @Override
    public void onEnable() {
        super.onEnable();

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
