package com.ishland.raknetify.fabric.common.netif;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;

import java.net.NetworkInterface;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class NetworkInterfaceListener {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("Raknetify IFListener").setDaemon(true).setPriority(Thread.NORM_PRIORITY - 1).build()
    );

    static {
        scheduler.scheduleAtFixedRate(NetworkInterfaceListener::pollChanges, 0, 10, TimeUnit.SECONDS);
    }

    public static void init() {
    }

    private static final Object2ObjectOpenHashMap<String, NetworkInterface> knownInterfaces = new Object2ObjectOpenHashMap<>();
    private static final ReferenceSet<Consumer<InterfaceChangeEvent>> listeners = ReferenceSets.synchronize(new ReferenceOpenHashSet<>());

    private static void pollChanges() {
        try {
            final List<NetworkInterface> networkInterfaces = NetworkInterface.networkInterfaces().toList();
            ObjectOpenHashSet<String> currentInterfaces = new ObjectOpenHashSet<>();
            for (NetworkInterface networkInterface : networkInterfaces) {
                if (networkInterface.isUp()) {
                    currentInterfaces.add(networkInterface.getName());
                    if (knownInterfaces.put(networkInterface.getName(), networkInterface) == null) {
                        listeners.forEach(consumer -> consumer.accept(new InterfaceChangeEvent(true, networkInterface)));
                    }
                }
            }
            final ObjectIterator<Map.Entry<String, NetworkInterface>> iterator = knownInterfaces.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry<String, NetworkInterface> entry = iterator.next();
                if (!currentInterfaces.contains(entry.getKey())) {
                    iterator.remove();
                    listeners.forEach(consumer -> consumer.accept(new InterfaceChangeEvent(false, entry.getValue())));
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static void addListener(Consumer<InterfaceChangeEvent> consumer) {
        listeners.add(consumer);
    }

    public static void removeListener(Consumer<InterfaceChangeEvent> consumer) {
        listeners.remove(consumer);
    }

    public record InterfaceChangeEvent(boolean added, NetworkInterface networkInterface) {
    }

}
