package com.ishland.raknetfabric.common.connection.encryption;

import io.netty.channel.ChannelHandlerContext;

public interface PacketEncryptionManagerInterface {

    void setContext(ChannelHandlerContext ctx);

}
