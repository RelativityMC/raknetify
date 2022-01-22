package com.ishland.raknetfabric.mixin.client;

import net.minecraft.client.gui.screen.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ConnectScreen.class)
public abstract class MixinConnectScreen {

//    @Inject(method = "connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;)V", at = @At("HEAD"), cancellable = true)
//    private void interceptRaknet(MinecraftClient client, ServerAddress rawAddress, CallbackInfo ci) {
//        if (!rawAddress.getAddress().startsWith(Constants.RAKNET_PREFIX)) return;
//        ci.cancel();
//
//        ServerAddress address = new ServerAddress(rawAddress.getAddress().substring(Constants.RAKNET_PREFIX.length()), rawAddress.getPort());
//        // [VanillaCopy] modified
//        System.out.println(String.format("Connecting to %s, %d via raknet", address.getAddress(), address.getPort()));
//        Thread thread = new Thread("Server Connector #" + CONNECTOR_THREADS_COUNT.incrementAndGet()) {
//            public void run() {
//                InetSocketAddress inetSocketAddress = null;
//
//                try {
//                    if (MixinConnectScreen.this.connectingCancelled) {
//                        return;
//                    }
//
//                    Optional<InetSocketAddress> optional = AllowedAddressResolver.DEFAULT.resolve(address).map(Address::getInetSocketAddress);
//                    if (MixinConnectScreen.this.connectingCancelled) {
//                        return;
//                    }
//
//                    if (optional.isEmpty()) {
//                        client.execute(
//                                () -> client.setScreen(
//                                        new DisconnectedScreen(MixinConnectScreen.this.parent, ScreenTexts.CONNECT_FAILED, ConnectScreen.BLOCKED_HOST_TEXT)
//                                )
//                        );
//                        return;
//                    }
//
//                    inetSocketAddress = optional.get();
//                    MixinConnectScreen.this.connection = RaknetClientConnectionUtil.connect(inetSocketAddress, client.options.shouldUseNativeTransport());
//                    MixinConnectScreen.this.connection
//                            .setPacketListener(
//                                    new ClientLoginNetworkHandler(MixinConnectScreen.this.connection, client, MixinConnectScreen.this.parent, MixinConnectScreen.this::setStatus)
//                            );
//                    MixinConnectScreen.this.connection
//                            .send(new HandshakeC2SPacket(inetSocketAddress.getHostName(), inetSocketAddress.getPort(), NetworkState.LOGIN));
//                    MixinConnectScreen.this.connection.send(new LoginHelloC2SPacket(client.getSession().getProfile()));
//                } catch (Exception var6) {
//                    if (MixinConnectScreen.this.connectingCancelled) {
//                        return;
//                    }
//
//                    Throwable var5 = var6.getCause();
//                    Exception exception2;
//                    if (var5 instanceof Exception exception) {
//                        exception2 = exception;
//                    } else {
//                        exception2 = var6;
//                    }
//
//                    System.err.println("Couldn't connect to server");
//                    var6.printStackTrace();
//                    String exception = inetSocketAddress == null
//                            ? exception2.getMessage()
//                            : exception2.getMessage()
//                            .replaceAll(inetSocketAddress.getHostName() + ":" + inetSocketAddress.getPort(), "")
//                            .replaceAll(inetSocketAddress.toString(), "");
//                    client.execute(
//                            () -> client.setScreen(
//                                    new DisconnectedScreen(
//                                            MixinConnectScreen.this.parent, ScreenTexts.CONNECT_FAILED, new TranslatableText("disconnect.genericReason", exception)
//                                    )
//                            )
//                    );
//                }
//
//            }
//        };
////        thread.setUncaughtExceptionHandler(new UncaughtExceptionLogger(LOGGER));
//        thread.start();
//    }

}
