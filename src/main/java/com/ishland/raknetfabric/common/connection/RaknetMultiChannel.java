package com.ishland.raknetfabric.common.connection;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;

import java.util.HashSet;
import java.util.Set;

public class RaknetMultiChannel {

    private RaknetMultiChannel() {
    }

    private static Set<Class<?>> createClassSet(String[] classNames) {
        final MappingResolver mappingResolver = FabricLoader.getInstance().getMappingResolver();
        final boolean isIntermediaryNamespace = mappingResolver.getCurrentRuntimeNamespace().equals("intermediary");
        final HashSet<Class<?>> classHashSet = new HashSet<>();
        for (String className : classNames) {
            final String intermediary = mappingResolver.mapClassName("intermediary", className.replace('/', '.'));
            if (!isIntermediaryNamespace && intermediary.equals(className))
                //noinspection RedundantStringFormatCall
                System.err.println("Warning: Failed to remap %s for raknet multi-channel".formatted(intermediary));
            try {
                classHashSet.add(Class.forName(intermediary));
            } catch (ClassNotFoundException e) {
                //noinspection RedundantStringFormatCall
                System.out.println("Warning: %s not found for raknet multi-channel".formatted(intermediary));
            }
        }
        return classHashSet; // no read-only wrapper for performance
    }

    // Primarily used for interactions
    private static final Set<Class<?>> channel1 = createClassSet(new String[]{
            "net/minecraft/class_2629", // BossBarS2C
            "net/minecraft/class_5888", // ClearTitleS2C
            "net/minecraft/class_5903", // SubtitleS2C
            "net/minecraft/class_5904", // TitleS2C
            "net/minecraft/class_5905", // TitleFadeS2C
            "net/minecraft/class_5892", // DeathMessageS2C
            "net/minecraft/class_2635", // GameMessageS2CPacket
            "net/minecraft/class_5894", // OverlayMessageS2C
            "net/minecraft/class_2748", // ExperienceBarUpdateS2C
            "net/minecraft/class_2749", // HealthUpdateS2CPacket
            "net/minecraft/class_2772", // PlayerListHeaderS2C
            "net/minecraft/class_2703", // PlayerListS2C
            "net/minecraft/class_2736", // ScoreboardDisplayS2C
            "net/minecraft/class_2751", // ScoreboardDisplayObjectiveUpdateS2C
            "net/minecraft/class_2757", // ScoreboardPlayerUpdateS2C

            "net/minecraft/class_2641", // CommandTreeS2C
            "net/minecraft/class_2639", // CommandSuggestionsS2C

            "net/minecraft/class_2645", // CloseScreenS2C
            "net/minecraft/class_2648", // OpenHorseScreenS2C
            "net/minecraft/class_3944", // OpenScreenS2C
            "net/minecraft/class_2651", // ScreenHandlerPropertyUpdateS2C
            "net/minecraft/class_2653", // ScreenHandlerSlotUpdateS2C
            "net/minecraft/class_3895", // OpenWrittenBookS2C

            "net/minecraft/class_2649", // InventoryS2C

            "net/minecraft/class_2707", // LookAtS2C

            "net/minecraft/class_2761", // WorldTimeUpdateS2C

            "net/minecraft/class_2788", // SynchronizeRecipesS2C
            "net/minecraft/class_2790", // SynchronizeTagsS2C
            "net/minecraft/class_2713", // UnlockRecipesS2C
            "net/minecraft/class_2735", // UpdateSelectedSlotS2C

            "net/minecraft/class_2868", // UpdateSelectedSlotC2S
    });

    // Primarily for packets not very critical to interactions
    private static final Set<Class<?>> channel2 = createClassSet(new String[]{
            "net/minecraft/class_2683", // MapUpdateS2C
            "net/minecraft/class_2675", // ParticleS2C
            "net/minecraft/class_2660", // PlaySoundIdS2C
            "net/minecraft/class_2765", // PlaySoundFromEntityS2C
            "net/minecraft/class_2767", // PlaySoundS2C
            "net/minecraft/class_2770", // StopSoundS2C

    });

    private static final Set<Class<?>> unordered = createClassSet(new String[]{
            "net/minecraft/class_2670", // KeepAliveS2C
            "net/minecraft/class_2827", // KeepAliveC2S
            "net/minecraft/class_2661", // DisconnectS2C
            "net/minecraft/class_6373", // PlayPingS2C
            "net/minecraft/class_6374", // PlayPongC2S

            "net/minecraft/class_2720", // ResourcePackSendS2C
            "net/minecraft/class_2856", // ResourcePackStatusC2S

            "net/minecraft/class_2617", // StatisticsS2C
    });

}
