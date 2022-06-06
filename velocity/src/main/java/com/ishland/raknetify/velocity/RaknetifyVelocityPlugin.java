package com.ishland.raknetify.velocity;

import com.google.inject.Inject;
import com.ishland.raknetify.common.connection.RakNetSimpleMultiChannelCodec;
import com.ishland.raknetify.common.data.ProtocolMultiChannelMappings;
import com.ishland.raknetify.velocity.connection.RakNetVelocityConnectionUtil;
import com.ishland.raknetify.velocity.init.VelocityPacketRegistryInjector;
import com.ishland.raknetify.velocity.init.VelocityRaknetifyServer;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
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

    @Inject
    private ProxyServer proxy;
    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent e) {
        PROXY = this.proxy;
        LOGGER = this.logger;

        ProtocolMultiChannelMappings.init();
        VelocityPacketRegistryInjector.inject();

        PROXY.getEventManager().register(this, LoginEvent.class, PostOrder.LAST, RakNetVelocityConnectionUtil::onPlayerLogin);
        PROXY.getEventManager().register(this, ListenerBoundEvent.class, PostOrder.LAST, VelocityRaknetifyServer::start);
        PROXY.getEventManager().register(this, ListenerCloseEvent.class, PostOrder.LAST, VelocityRaknetifyServer::stop);
    }

}
