package org.rsmod.content.areas.misc.tutorialisland

import jakarta.inject.Inject
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.loc.locNearby
import org.rsmod.game.entity.Player

internal class TutorialRules @Inject constructor(private val locRepo: LocRepository) {

    fun nextStage(player: Player): Int? {
        val stage = player.tutorialStage
        return when {
            stage == TutorialStage.INVENTORY_VIEWED && player.inv.contains(TutorialObj.RAW_SHRIMP) ->
                TutorialStage.SHRIMP_CAUGHT
            stage == TutorialStage.AXE_GIVEN && player.inv.contains(TutorialObj.LOGS) ->
                TutorialStage.LOGS_CUT
            stage == TutorialStage.LOGS_CUT && hasFireNearby(player) -> TutorialStage.FIRE_LIT
            stage == TutorialStage.FIRE_LIT && player.inv.contains(TutorialObj.SHRIMP) ->
                TutorialStage.SHRIMP_COOKED
            stage == TutorialStage.CHEF_TALKED && player.inv.contains(TutorialObj.BREAD_DOUGH) ->
                TutorialStage.DOUGH_MADE
            stage == TutorialStage.DOUGH_MADE && player.inv.contains(TutorialObj.BREAD) ->
                TutorialStage.BREAD_BAKED
            stage == TutorialStage.TIN_MINED && player.inv.contains("obj.tin_ore") ->
                TutorialStage.COPPER_MINED
            stage == TutorialStage.COPPER_MINED && player.inv.contains("obj.copper_ore") ->
                TutorialStage.BAR_SMELTED
            stage == TutorialStage.BAR_SMELTED && player.inv.contains(TutorialObj.BRONZE_BAR) ->
                TutorialStage.MINING_GUIDE_BAR
            stage == TutorialStage.DAGGER_SMITHED && player.inv.contains(TutorialObj.BRONZE_DAGGER) ->
                TutorialStage.DAGGER_SELECTED
            stage == TutorialStage.DAGGER_SELECTED && player.inv.contains(TutorialObj.BRONZE_DAGGER) ->
                TutorialStage.COMBAT_GATE
            stage == TutorialStage.SWORD_EQUIPPED && player.worn.contains(TutorialObj.BRONZE_SWORD) ->
                TutorialStage.COMBAT_TAB_OPENED
            else -> null
        }
    }

    private fun hasFireNearby(player: Player): Boolean =
        locRepo.locNearby(player.coords, FIRE_RADIUS, null, listOf(TutorialLoc.FIRE))

    private companion object {
        private const val FIRE_RADIUS = 4
    }
}
