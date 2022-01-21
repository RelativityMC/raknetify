package com.ishland.raknetfabric.mixin.access;

import io.netty.channel.Channel;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientConnection.class)
public interface IClientConnection {

    @Accessor
    Channel getChannel();

}
