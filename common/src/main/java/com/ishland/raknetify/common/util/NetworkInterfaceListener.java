package com.ishland.raknetify.common.util;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class NetworkInterfaceListener {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> {
                final Thread thread = new Thread(r, "Raknetify IFListener");
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            }
    );

    static {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                pollChanges();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public static void init() {
    }

    private static final ObjectOpenHashSet<InetAddress> knownAddresses = new ObjectOpenHashSet<>();
    private static final ReferenceSet<Consumer<InterfaceAddressChangeEvent>> listeners = ReferenceSets.synchronize(new ReferenceOpenHashSet<>());

    private static void pollChanges() {
        try {
            final List<NetworkInterface> networkInterfaces = NetworkInterface.networkInterfaces().toList();
            ObjectOpenHashSet<InetAddress> currentAddresses = new ObjectOpenHashSet<>();
            for (NetworkInterface networkInterface : networkInterfaces) {
                if (networkInterface.isUp()) {
                    for (InetAddress address : networkInterface.inetAddresses().toList()) {
                        currentAddresses.add(address);
                        if (knownAddresses.add(address)) {
                            listeners.forEach(consumer -> consumer.accept(new InterfaceAddressChangeEvent(true, address)));
                        }
                    }
                }
            }
            final ObjectIterator<InetAddress> iterator = knownAddresses.iterator();
            while (iterator.hasNext()) {
                final InetAddress address = iterator.next();
                if (!currentAddresses.contains(address)) {
                    iterator.remove();
                    listeners.forEach(consumer -> consumer.accept(new InterfaceAddressChangeEvent(false, address)));
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void addListener(Consumer<InterfaceAddressChangeEvent> consumer) {
        listeners.add(consumer);
    }

    public static void removeListener(Consumer<InterfaceAddressChangeEvent> consumer) {
        listeners.remove(consumer);
    }

    public record InterfaceAddressChangeEvent(boolean added, InetAddress address) {
    }

}
