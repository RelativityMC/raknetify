package com.ishland.raknetify.fabric;

import com.ishland.raknetify.fabric.common.connection.RakNetMultiChannel;
import com.ishland.raknetify.fabric.common.netif.NetworkInterfaceListener;
import net.fabricmc.api.ModInitializer;

public class RaknetifyFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        RakNetMultiChannel.init();
        NetworkInterfaceListener.init();
    }
}
