package com.ishland.raknetfabric.mixin.access;

import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(NetworkState.class)
public interface INetworkState {

    @Accessor
    Map<NetworkSide, ? extends NetworkState.PacketHandler<?>> getPacketHandlers();

}
