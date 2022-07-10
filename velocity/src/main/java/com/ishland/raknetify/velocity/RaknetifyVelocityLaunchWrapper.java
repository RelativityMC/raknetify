package com.ishland.raknetify.velocity;

import com.ishland.raknetify.common.data.ProtocolMultiChannelMappings;
import com.ishland.raknetify.velocity.connection.RakNetVelocityConnectionUtil;
import com.ishland.raknetify.velocity.init.VelocityPacketRegistryInjector;
import com.ishland.raknetify.velocity.init.VelocityRaknetifyServer;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ListenerBoundEvent;
import com.velocitypowered.api.event.proxy.ListenerCloseEvent;

import static com.ishland.raknetify.velocity.RaknetifyVelocityPlugin.INSTANCE;
import static com.ishland.raknetify.velocity.RaknetifyVelocityPlugin.LOGGER;
import static com.ishland.raknetify.velocity.RaknetifyVelocityPlugin.PROXY;

public class RaknetifyVelocityLaunchWrapper {

    public static void launch() {
        if (!isCompatible()) {
            Runnable runnable = () -> {
                LOGGER.error("This version of Raknetify is NOT compatible with your version of Velocity");
                LOGGER.error("Please update your Velocity at https://papermc.io/downloads#Velocity");
            };
            runnable.run();
            PROXY.getEventManager().register(INSTANCE, ListenerBoundEvent.class, PostOrder.LAST, ignored -> runnable.run());
            return;
        }

        ProtocolMultiChannelMappings.init();
        VelocityPacketRegistryInjector.inject();

        PROXY.getEventManager().register(INSTANCE, LoginEvent.class, PostOrder.LAST, RakNetVelocityConnectionUtil::onPlayerLogin);
        PROXY.getEventManager().register(INSTANCE, ListenerBoundEvent.class, PostOrder.LAST, VelocityRaknetifyServer::start);
        PROXY.getEventManager().register(INSTANCE, ListenerCloseEvent.class, PostOrder.LAST, VelocityRaknetifyServer::stop);
        PROXY.getEventManager().register(INSTANCE, ServerPostConnectEvent.class, PostOrder.LAST, RakNetVelocityConnectionUtil::onServerSwitch);
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
