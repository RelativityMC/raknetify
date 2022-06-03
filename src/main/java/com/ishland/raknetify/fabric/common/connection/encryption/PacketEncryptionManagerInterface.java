package com.ishland.raknetify.fabric.common.connection.encryption;

import io.netty.channel.ChannelHandlerContext;

public interface PacketEncryptionManagerInterface {

    void setContext(ChannelHandlerContext ctx);

}
