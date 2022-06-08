package com.ishland.raknetify.velocity;

import com.google.inject.Inject;
import com.ishland.raknetify.common.data.ProtocolMultiChannelMappings;
import com.ishland.raknetify.velocity.connection.RakNetVelocityConnectionUtil;
import com.ishland.raknetify.velocity.init.VelocityPacketRegistryInjector;
import com.ishland.raknetify.velocity.init.VelocityRaknetifyServer;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ListenerBoundEvent;
import com.velocitypowered.api.event.proxy.ListenerCloseEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(
        id = "raknetify"
)
public class RaknetifyVelocityPlugin {

    public static ProxyServer PROXY;
    public static Logger LOGGER;
    public static RaknetifyVelocityPlugin INSTANCE;

    @Inject
    private ProxyServer proxy;
    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent e) {
        INSTANCE = this;
        PROXY = this.proxy;
        LOGGER = this.logger;

        if (!isCompatible()) {
            Runnable runnable = () -> {
                LOGGER.error("This version of Raknetify is NOT compatible with your version of Velocity");
                LOGGER.error("Please update your Velocity at https://papermc.io/downloads#Velocity");
            };
            runnable.run();
            PROXY.getEventManager().register(this, ListenerBoundEvent.class, PostOrder.LAST, ignored -> runnable.run());
            return;
        }

        ProtocolMultiChannelMappings.init();
        VelocityPacketRegistryInjector.inject();

        PROXY.getEventManager().register(this, LoginEvent.class, PostOrder.LAST, RakNetVelocityConnectionUtil::onPlayerLogin);
        PROXY.getEventManager().register(this, ListenerBoundEvent.class, PostOrder.LAST, VelocityRaknetifyServer::start);
        PROXY.getEventManager().register(this, ListenerCloseEvent.class, PostOrder.LAST, VelocityRaknetifyServer::stop);
        PROXY.getEventManager().register(this, ServerPostConnectEvent.class, PostOrder.LAST, RakNetVelocityConnectionUtil::onServerSwitch);
    }

    private static boolean isCompatible() {
        try {
            Class.forName("com.velocitypowered.proxy.crypto.EncryptionUtils");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
