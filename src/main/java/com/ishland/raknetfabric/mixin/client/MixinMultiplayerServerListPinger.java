package com.ishland.raknetfabric.mixin.client;

import com.ishland.raknetfabric.Constants;
import com.ishland.raknetfabric.common.connection.RaknetClientConnectionUtil;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetSocketAddress;

@Mixin(MultiplayerServerListPinger.class)
public abstract class MixinMultiplayerServerListPinger {

//    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
//    public void interceptAddRaknet(ServerInfo entry, Runnable runnable, CallbackInfo ci) {
//        if (!entry.address.startsWith(Constants.RAKNET_PREFIX)) return;
//        ci.cancel();
//
//        // [VanillaCopy] modified
//        ServerAddress serverAddress = ServerAddress.parse(entry.address.substring(Constants.RAKNET_PREFIX.length()));
//        Optional<InetSocketAddress> optional = AllowedAddressResolver.DEFAULT.resolve(serverAddress).map(Address::getInetSocketAddress);
//        if (optional.isEmpty()) {
//            this.showError(ConnectScreen.BLOCKED_HOST_TEXT, entry);
//        } else {
//            final InetSocketAddress inetSocketAddress = optional.get();
//            final ClientConnection clientConnection = RaknetClientConnectionUtil.connect(inetSocketAddress, false);
//            this.clientConnections.add(clientConnection);
//            entry.label = new TranslatableText("multiplayer.status.pinging");
//            entry.ping = -1L;
//            entry.playerListSummary = null;
//            clientConnection.setPacketListener(
//                    new ClientQueryPacketListener() {
//                        private boolean sentQuery;
//                        private boolean received;
//                        private long startTime;
//
//                        @Override
//                        public void onResponse(QueryResponseS2CPacket packet) {
//                            if (this.received) {
//                                clientConnection.disconnect(new TranslatableText("multiplayer.status.unrequested"));
//                            } else {
////                                this.received = true;
//                                ServerMetadata serverMetadata = packet.getServerMetadata();
//                                if (serverMetadata.getDescription() != null) {
//                                    entry.label = serverMetadata.getDescription();
//                                } else {
//                                    entry.label = LiteralText.EMPTY;
//                                }
//
//                                if (serverMetadata.getVersion() != null) {
//                                    entry.version = new LiteralText(serverMetadata.getVersion().getGameVersion());
//                                    entry.protocolVersion = serverMetadata.getVersion().getProtocolVersion();
//                                } else {
//                                    entry.version = new TranslatableText("multiplayer.status.old");
//                                    entry.protocolVersion = 0;
//                                }
//
//                                if (serverMetadata.getPlayers() != null) {
//                                    entry.playerCountLabel = MultiplayerServerListPinger.createPlayerCountText(
//                                            serverMetadata.getPlayers().getOnlinePlayerCount(), serverMetadata.getPlayers().getPlayerLimit()
//                                    );
//                                    List<Text> list = Lists.newArrayList();
//                                    GameProfile[] gameProfiles = serverMetadata.getPlayers().getSample();
//                                    if (gameProfiles != null && gameProfiles.length > 0) {
//                                        for(GameProfile gameProfile : gameProfiles) {
//                                            list.add(new LiteralText(gameProfile.getName()));
//                                        }
//
//                                        if (gameProfiles.length < serverMetadata.getPlayers().getOnlinePlayerCount()) {
//                                            list.add(
//                                                    new TranslatableText(
//                                                            "multiplayer.status.and_more", serverMetadata.getPlayers().getOnlinePlayerCount() - gameProfiles.length
//                                                    )
//                                            );
//                                        }
//
//                                        entry.playerListSummary = list;
//                                    }
//                                } else {
//                                    entry.playerCountLabel = (new TranslatableText("multiplayer.status.unknown")).formatted(Formatting.DARK_GRAY);
//                                }
//
//                                String list = null;
//                                if (serverMetadata.getFavicon() != null) {
//                                    String gameProfiles = serverMetadata.getFavicon();
//                                    if (gameProfiles.startsWith("data:image/png;base64,")) {
//                                        list = gameProfiles.substring("data:image/png;base64,".length());
//                                    } else {
//                                        System.err.println("Invalid server icon (unknown format)");
//                                    }
//                                }
//
//                                if (!Objects.equals(list, entry.getIcon())) {
//                                    entry.setIcon(list);
//                                    runnable.run();
//                                }
//
//                                clientConnection.send(new QueryPingC2SPacket(this.startTime));
//
//                                this.startTime = Util.getMeasuringTimeMs();
//                                clientConnection.send(new QueryPingC2SPacket(this.startTime));
//                                clientConnection.tick(); // RaknetFabric
//                                this.sentQuery = true;
//
//                                entry.ping = RakNet.config(((IClientConnection) clientConnection).getChannel()).getRTTNanos() / 1_000_000; // RaknetFabric start - ping impl
//                            }
//                        }
//
//                        @Override
//                        public void onPong(QueryPongS2CPacket packet) {
//                            long l = this.startTime;
//                            long m = Util.getMeasuringTimeMs();
//                            entry.ping = m - l;
//                            clientConnection.disconnect(new TranslatableText("multiplayer.status.finished"));
//                        }
//
//                        @Override
//                        public void onDisconnected(Text reason) {
//                            if (!this.sentQuery) {
//                                MixinMultiplayerServerListPinger.this.showError(reason, entry);
//                                RaknetClientConnectionUtil.ping(inetSocketAddress, entry);
//                            }
//
//                        }
//
//                        @Override
//                        public ClientConnection getConnection() {
//                            return clientConnection;
//                        }
//                    }
//            );
//
//            try {
//                clientConnection.send(new HandshakeC2SPacket(serverAddress.getAddress(), serverAddress.getPort(), NetworkState.STATUS));
//                clientConnection.send(new QueryRequestC2SPacket());
//            } catch (Throwable var8) {
//                var8.printStackTrace();
//            }
//
//        }
//    }

    @Redirect(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ServerAddress;parse(Ljava/lang/String;)Lnet/minecraft/client/network/ServerAddress;"))
    private ServerAddress modifyRaknetAddress(String address) {
        return ServerAddress.parse(address.startsWith(Constants.RAKNET_PREFIX) ? address.substring(Constants.RAKNET_PREFIX.length()) : address);
    }

    @Redirect(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetSocketAddress;Z)Lnet/minecraft/network/ClientConnection;"))
    private ClientConnection redirectConnect(InetSocketAddress address, boolean useEpoll, ServerInfo entry, Runnable runnable) {
        return entry.address.startsWith(Constants.RAKNET_PREFIX) ? RaknetClientConnectionUtil.connect(address, useEpoll) : ClientConnection.connect(address, useEpoll);
    }

}
