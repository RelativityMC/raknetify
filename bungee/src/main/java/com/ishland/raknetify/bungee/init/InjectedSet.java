package com.ishland.raknetify.bungee.init;

import com.google.common.collect.ForwardingSet;
import io.netty.channel.Channel;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;

import java.util.Set;

public class InjectedSet extends ForwardingSet<Channel> {

    private final Set<Channel> delegate;

    public InjectedSet(Set<Channel> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean add(Channel element) {
        BungeeRaknetifyServer.injectChannel((BungeeCord) ProxyServer.getInstance(), element);

        return super.add(element);
    }

    @Override
    public void clear() {
        BungeeRaknetifyServer.stopAll();

        super.clear();
    }

    @Override
    protected Set<Channel> delegate() {
        return this.delegate;
    }
}
