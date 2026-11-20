package org.rsmod.content.areas.misc.tutorialisland

import jakarta.inject.Inject
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.loc.locNearby
import org.rsmod.game.entity.Player

internal class TutorialRules @Inject constructor(private val locRepo: LocRepository) {

    fun nextStage(player: Player): Int? {
        val stage = player.tutorialStage
        return when {
            stage == TutorialStage.AXE_GIVEN && player.inv.contains(TutorialObj.LOGS) ->
                TutorialStage.LOGS_CUT
            stage == TutorialStage.LOGS_CUT && hasFireNearby(player) -> TutorialStage.FIRE_LIT
            stage == TutorialStage.FIRE_LIT && player.inv.contains(TutorialObj.SHRIMP) ->
                TutorialStage.SHRIMP_COOKED
            stage == TutorialStage.CHEF_TALKED && player.inv.contains(TutorialObj.BREAD_DOUGH) ->
                TutorialStage.DOUGH_MADE
            stage == TutorialStage.DOUGH_MADE && player.inv.contains(TutorialObj.BREAD) ->
                TutorialStage.BREAD_BAKED
            stage == TutorialStage.MINING_GUIDE_TALKED &&
                player.inv.contains(TutorialObj.TIN_ORE) -> TutorialStage.TIN_MINED
            stage == TutorialStage.TIN_MINED && player.inv.contains(TutorialObj.COPPER_ORE) ->
                TutorialStage.COPPER_MINED
            stage == TutorialStage.COPPER_MINED &&
                player.inv.contains(TutorialObj.BRONZE_BAR) -> TutorialStage.BAR_SMELTED
            stage in TutorialStage.BAR_SMELTED..TutorialStage.ANVIL_OPENED &&
                player.inv.contains(TutorialObj.BRONZE_DAGGER) -> TutorialStage.DAGGER_SMITHED
            stage in TutorialStage.DAGGER_EQUIP_TASK..TutorialStage.EQUIPMENT_STATS_OPENED &&
                player.worn.contains(TutorialObj.BRONZE_DAGGER) -> TutorialStage.DAGGER_EQUIPPED
            stage == TutorialStage.COMBAT_GUIDE_SWORD &&
                player.worn.contains(TutorialObj.BRONZE_SWORD) -> TutorialStage.SWORD_EQUIPPED
            stage == TutorialStage.QUEST_HOUSE_ENTERED && isInsideQuestHouse(player) ->
                TutorialStage.QUEST_HOUSE_INSIDE
            else -> null
        }
    }

    private fun isInsideQuestHouse(player: Player): Boolean {
        val coords = player.coords
        return coords.x in QUEST_HOUSE_X && coords.z in QUEST_HOUSE_Z && coords.level == 0
    }

    private fun hasFireNearby(player: Player): Boolean =
        locRepo.locNearby(player.coords, FIRE_RADIUS, null, listOf(TutorialLoc.FIRE))

    private companion object {
        private const val FIRE_RADIUS = 4
        private val QUEST_HOUSE_X = 3085..3089
        private val QUEST_HOUSE_Z = 3120..3126
    }
}
