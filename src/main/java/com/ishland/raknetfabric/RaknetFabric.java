package com.ishland.raknetfabric;

import com.ishland.raknetfabric.common.connection.RaknetMultiChannel;
import net.fabricmc.api.ModInitializer;

public class RaknetFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        RaknetMultiChannel.init();
    }
}
