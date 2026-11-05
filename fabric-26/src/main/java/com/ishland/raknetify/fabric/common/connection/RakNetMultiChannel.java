/*
 * This file is a part of the Raknetify project, licensed under MIT.
 *
 * Copyright (c) 2022-2025 ishland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ishland.raknetify.fabric.common.connection;

import com.google.common.collect.Sets;
import net.minecraft.network.protocol.BundleDelimiterPacket;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundPostEffectsPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAddTransientBlockPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundBundleDelimiterPacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundDebugBlockValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugChunkValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEntityValuePacket;
import net.minecraft.network.protocol.game.ClientboundDebugEventPacket;
import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;
import net.minecraft.network.protocol.game.ClientboundDeleteChatPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameRuleValuesPacket;
import net.minecraft.network.protocol.game.ClientboundGameTestHighlightPosPacket;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundLowDiskSpaceWarningPacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundMountScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundMoveMinecartPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.network.protocol.game.ClientboundProjectilePowerPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundServerDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerInventoryPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSwingAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTestInstanceBlockStatus;
import net.minecraft.network.protocol.game.ClientboundTickingStatePacket;
import net.minecraft.network.protocol.game.ClientboundTickingStepPacket;
import net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import net.minecraft.network.protocol.game.ServerboundChatAckPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundDebugSubscriptionRequestPacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetGameRulePacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetTestBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSpectatorActionPacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundTestInstanceBlockActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import com.ishland.raknetify.fabric.common.util.MinecraftVersions;

import java.util.Set;

public class RakNetMultiChannel {

    private RakNetMultiChannel() {
    }

    private static final Set<Class<?>> unordered = Set.of(
            ClientboundKeepAlivePacket.class,
            ServerboundKeepAlivePacket.class,
            ClientboundDisconnectPacket.class,
            ClientboundPingPacket.class,
            ServerboundPongPacket.class,
            ClientboundResourcePackPushPacket.class,
            ServerboundResourcePackPacket.class,
            ClientboundResourcePackPopPacket.class,
            ClientboundAwardStatsPacket.class,
            ServerboundSeenAdvancementsPacket.class,
            ClientboundSelectAdvancementsTabPacket.class,
            ServerboundPingRequestPacket.class,
            ClientboundPongResponsePacket.class,
            ClientboundCustomReportDetailsPacket.class,
            ClientboundServerLinksPacket.class
    );

    private static final Set<Class<?>> channel0 = Set.of(
            ClientboundStartConfigurationPacket.class,
            ServerboundConfigurationAcknowledgedPacket.class,
            ClientboundTransferPacket.class
    );

    // Primarily used for interactions independent to world
    private static final Set<Class<?>> channel1 = Set.of(
            ClientboundLowDiskSpaceWarningPacket.class,
            ServerboundSetGameRulePacket.class,
            ClientboundGameRuleValuesPacket.class,
            ClientboundBossEventPacket.class,
            ClientboundClearTitlesPacket.class,
            ClientboundSetSubtitleTextPacket.class,
            ClientboundSetTitleTextPacket.class,
            ClientboundSetTitlesAnimationPacket.class,
            ClientboundPlayerCombatKillPacket.class,
            ClientboundSystemChatPacket.class,
            ClientboundDisguisedChatPacket.class,
            ClientboundPlayerChatPacket.class,
            ClientboundCustomChatCompletionsPacket.class,
            ClientboundDeleteChatPacket.class,
            ServerboundChatSessionUpdatePacket.class,
            ServerboundChatAckPacket.class,
            ClientboundServerDataPacket.class,
            ClientboundSetActionBarTextPacket.class,
            ClientboundSetExperiencePacket.class,
            ClientboundSetHealthPacket.class,
            ClientboundCooldownPacket.class,
            ClientboundSetDisplayObjectivePacket.class,
            ClientboundSetObjectivePacket.class,
            ClientboundSetScorePacket.class,
            ClientboundResetScorePacket.class,
            ClientboundUpdateAdvancementsPacket.class,
            ClientboundCommandsPacket.class,
            ClientboundCommandSuggestionsPacket.class,
            ServerboundCommandSuggestionPacket.class,
            ServerboundChatCommandPacket.class,
            ServerboundChatCommandSignedPacket.class,
            ServerboundContainerButtonClickPacket.class,
            ServerboundChatPacket.class,
            ServerboundContainerClickPacket.class,
            ServerboundPlayerCommandPacket.class,
            ServerboundClientInformationPacket.class,
            ServerboundChangeDifficultyPacket.class,
            ServerboundLockDifficultyPacket.class,
            ClientboundPlayerPositionPacket.class,
            ServerboundPlayerActionPacket.class,
            ServerboundPlayerInputPacket.class,
            ServerboundPickItemFromBlockPacket.class,
            ServerboundPickItemFromEntityPacket.class,
            ClientboundPlayerLookAtPacket.class,
            ClientboundSetTimePacket.class,
            ClientboundChangeDifficultyPacket.class,
            ClientboundSetPlayerTeamPacket.class,
            ClientboundUpdateRecipesPacket.class,
            ClientboundUpdateTagsPacket.class,
            ClientboundPlayerInfoUpdatePacket.class,
            ClientboundTabListPacket.class,
            ClientboundPlayerInfoRemovePacket.class,
            ClientboundCookieRequestPacket.class,
            ClientboundStoreCookiePacket.class,
            ServerboundCookieResponsePacket.class,
            ClientboundDebugSamplePacket.class,
            ServerboundDebugSubscriptionRequestPacket.class,
            ClientboundTrackedWaypointPacket.class,
            ClientboundClearDialogPacket.class,
            ClientboundShowDialogPacket.class,
            ServerboundCustomClickActionPacket.class
    );

    // Entities related stuff
    private static final Set<Class<?>> channel2 = Set.of(
            ServerboundAttackPacket.class,
            ClientboundPlayerCombatEndPacket.class,
            ClientboundPlayerCombatEnterPacket.class,
            ClientboundRemoveEntitiesPacket.class,
            ClientboundAddEntityPacket.class,
            ClientboundAnimatePacket.class,
            ClientboundEntityEventPacket.class,
            ClientboundMoveEntityPacket.Pos.class,
            ClientboundMoveEntityPacket.Rot.class,
            ClientboundMoveEntityPacket.PosRot.class,
            ClientboundRotateHeadPacket.class,
            ClientboundSetEntityDataPacket.class,
            ClientboundSetEntityLinkPacket.class,
            ClientboundSetEntityMotionPacket.class,
            ClientboundSetEquipmentPacket.class,
            ClientboundSetPassengersPacket.class,
            ClientboundTeleportEntityPacket.class,
            ClientboundEntityPositionSyncPacket.class,
            ClientboundUpdateAttributesPacket.class,
            ClientboundUpdateMobEffectPacket.class,
            ClientboundRemoveMobEffectPacket.class,
            ClientboundHurtAnimationPacket.class,
            ClientboundDamageEventPacket.class,
            ClientboundMoveMinecartPacket.class,
            ServerboundUseItemOnPacket.class,
            ServerboundUseItemPacket.class,
            ServerboundInteractPacket.class,
            ServerboundMovePlayerPacket.class,
            ServerboundMovePlayerPacket.StatusOnly.class,
            ServerboundMovePlayerPacket.Pos.class,
            ServerboundMovePlayerPacket.PosRot.class,
            ServerboundMovePlayerPacket.Rot.class,
            ServerboundClientTickEndPacket.class,
            ServerboundChangeGameModePacket.class,
            ClientboundExplodePacket.class,
            ClientboundLoginPacket.class,
            ClientboundUpdateEnabledFeaturesPacket.class,
            ClientboundGameEventPacket.class,
            ClientboundSetDefaultSpawnPositionPacket.class,
            ClientboundTakeItemEntityPacket.class,
            ClientboundPlayerAbilitiesPacket.class,
            ClientboundSetCameraPacket.class,
            ClientboundMoveVehiclePacket.class,
            ServerboundPaddleBoatPacket.class,
            ServerboundMoveVehiclePacket.class,
            ClientboundPlayerRotationPacket.class,
            ClientboundMoveEntityPacket.class,
            ClientboundProjectilePowerPacket.class,
            ClientboundDebugEntityValuePacket.class,
            ClientboundDebugEventPacket.class
    );

    // Primarily used for interactions dependent to world
    private static final Set<Class<?>> channel3 = Set.of(
            ServerboundTeleportToEntityPacket.class,
            ServerboundAcceptTeleportationPacket.class,
            ServerboundBlockEntityTagQueryPacket.class,
            ServerboundEntityTagQueryPacket.class,
            ClientboundTagQueryPacket.class,
            ClientboundContainerClosePacket.class,
            ClientboundMountScreenOpenPacket.class,
            ClientboundOpenScreenPacket.class,
            ClientboundContainerSetDataPacket.class,
            ClientboundContainerSetSlotPacket.class,
            ClientboundOpenBookPacket.class,
            ClientboundContainerSetContentPacket.class,
            ClientboundSetHeldSlotPacket.class,
            ClientboundMerchantOffersPacket.class,
            ClientboundRecipeBookAddPacket.class,
            ClientboundRecipeBookRemovePacket.class,
            ClientboundRecipeBookSettingsPacket.class,
            ServerboundEditBookPacket.class,
            ServerboundRecipeBookSeenRecipePacket.class,
            ServerboundRecipeBookChangeSettingsPacket.class,
            ServerboundRenameItemPacket.class,
            ServerboundContainerClosePacket.class,
            ServerboundSetCreativeModeSlotPacket.class,
            ServerboundPlaceRecipePacket.class,
            ServerboundSelectTradePacket.class,
            ServerboundSetBeaconPacket.class,
            ServerboundSetCommandBlockPacket.class,
            ServerboundSetCommandMinecartPacket.class,
            ServerboundSetTestBlockPacket.class,
            ServerboundTestInstanceBlockActionPacket.class,
            ServerboundSetJigsawBlockPacket.class,
            ServerboundPlayerAbilitiesPacket.class,
            ServerboundSetCarriedItemPacket.class,
            ServerboundSetStructureBlockPacket.class,
            ClientboundPlaceGhostRecipePacket.class,
            ServerboundContainerSlotStateChangedPacket.class,
            ClientboundSetCursorItemPacket.class,
            ClientboundSetPlayerInventoryPacket.class,
            ServerboundSelectBundleItemPacket.class,
            ClientboundInitializeBorderPacket.class,
            ClientboundSetBorderCenterPacket.class,
            ClientboundSetBorderLerpSizePacket.class,
            ClientboundSetBorderSizePacket.class,
            ClientboundSetBorderWarningDelayPacket.class,
            ClientboundSetBorderWarningDistancePacket.class,
            ServerboundClientCommandPacket.class,
            ClientboundRespawnPacket.class,
            ServerboundCustomPayloadPacket.class,
            ClientboundCustomPayloadPacket.class,
            ClientboundTickingStepPacket.class,
            ClientboundTickingStatePacket.class
    );

    // Primarily for packets not very critical to interactions
    private static final Set<Class<?>> channel4 = Set.of(
            ClientboundMapItemDataPacket.class,
            ClientboundSoundEntityPacket.class,
            ClientboundSoundPacket.class,
            ClientboundStopSoundPacket.class,
            ClientboundLevelParticlesPacket.class
    );

    // Used for worlds
    private static final Set<Class<?>> channel7 = Set.of(
            ServerboundJigsawGeneratePacket.class,
            ClientboundOpenSignEditorPacket.class,
            ServerboundSignUpdatePacket.class,
            ClientboundBlockEventPacket.class,
            ClientboundSetChunkCacheCenterPacket.class,
            ClientboundSetChunkCacheRadiusPacket.class,
            ClientboundSetSimulationDistancePacket.class,
            ClientboundForgetLevelChunkPacket.class,
            ClientboundBlockUpdatePacket.class,
            ClientboundSectionBlocksUpdatePacket.class,
            ClientboundLevelEventPacket.class,
            ClientboundBlockDestructionPacket.class,
            ClientboundLevelChunkWithLightPacket.class,
            ClientboundBlockEntityDataPacket.class,
            ClientboundLightUpdatePacket.class,
            ClientboundChunksBiomesPacket.class,
            ClientboundChunkBatchFinishedPacket.class,
            ClientboundChunkBatchStartPacket.class,
            ServerboundChunkBatchReceivedPacket.class,
            ClientboundBlockChangedAckPacket.class,
            ServerboundPlayerLoadedPacket.class,
            ClientboundTestInstanceBlockStatus.class,
            ClientboundDebugBlockValuePacket.class,
            ClientboundDebugChunkValuePacket.class,
            ClientboundGameTestHighlightPosPacket.class
    );

    private static final Set<Class<?>> unreliable = Set.of();

    private static final Set<Class<?>> theVoid = Set.of(
            BundleDelimiterPacket.class,
            ClientboundBundleDelimiterPacket.class
    );

    private static final Object2IntOpenHashMap<Class<?>> classToChannelIdOverride = new Object2IntOpenHashMap<>();

    static {
        classToChannelIdOverride.defaultReturnValue(Integer.MAX_VALUE);
        unordered.forEach(clazz -> classToChannelIdOverride.put(clazz, -1));
        channel0.forEach(clazz -> classToChannelIdOverride.put(clazz, 0));
        channel1.forEach(clazz -> classToChannelIdOverride.put(clazz, 1));
        channel2.forEach(clazz -> classToChannelIdOverride.put(clazz, 2));
        channel3.forEach(clazz -> classToChannelIdOverride.put(clazz, 3));
        channel4.forEach(clazz -> classToChannelIdOverride.put(clazz, 4));
        channel7.forEach(clazz -> classToChannelIdOverride.put(clazz, 7));
        unreliable.forEach(clazz -> classToChannelIdOverride.put(clazz, -2));
        theVoid.forEach(clazz -> classToChannelIdOverride.put(clazz, Integer.MIN_VALUE));
        if (!MinecraftVersions.AT_LEAST_26_3) {
            addEarlierPacket("ServerboundSwingPacket", 2);
        }
        if (!MinecraftVersions.AT_LEAST_26_2) {
            addEarlierPacket("ServerboundSpectateEntityPacket", 3);
        }
        if (MinecraftVersions.AT_LEAST_26_3) classToChannelIdOverride.put(ServerboundPunchPacket.class, 2);
        if (MinecraftVersions.AT_LEAST_26_3) classToChannelIdOverride.put(ClientboundSwingAnimationPacket.class, 2);
        if (MinecraftVersions.AT_LEAST_26_2) classToChannelIdOverride.put(ServerboundSpectatorActionPacket.class, 3);
        if (MinecraftVersions.AT_LEAST_26_3) classToChannelIdOverride.put(ClientboundPostEffectsPacket.class, 4);
        if (MinecraftVersions.AT_LEAST_26_3) classToChannelIdOverride.put(ClientboundAddTransientBlockPacket.class, 7);
    }

    private static void addEarlierPacket(String name, int channel) {
        try {
            Class<?> packet = Class.forName("net.minecraft.network.protocol.game." + name, false,
                    RakNetMultiChannel.class.getClassLoader());
            classToChannelIdOverride.put(packet, channel);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Missing expected packet " + name, e);
        }
    }

    private static final Set<Class<?>> foundUnknownClasses = Sets.newConcurrentHashSet();

    public static int getPacketChannelOverride(Class<?> clazz, boolean suppressWarning) {
        if (clazz == null) {
            System.err.println("Raknetify: Warning: Tried to send packet without setting packet class");
            return 0;
        }
        int channelOverride = classToChannelIdOverride.getInt(clazz);
        if (channelOverride == Integer.MAX_VALUE) {
            if (!suppressWarning) {
                if (foundUnknownClasses.add(clazz)) {
                    System.err.println("Raknetify: Warning: unknown packet type %s for raknet multi-channel".formatted(clazz.getName()));
                }
            }
            channelOverride = 7;
        }
        return channelOverride;
    }

    public static void init() {
    }

}
