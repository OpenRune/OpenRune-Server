package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.quest.manager.Quest
import org.rsmod.events.EventBus
import org.rsmod.map.zone.ZoneKey

internal class TutorialExit
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val eventBus: EventBus,
) {

    suspend fun ProtectedAccess.complete() {
        if (player.tutorialStage != TutorialStage.READY_TO_LEAVE) {
            return
        }
        val quest = Quest.get("quest_learningtheropes")
        if (quest != null && !quest.isQuestCompleted(player)) {
            quest.completeQuest(this)
        }
        player.tutorialStage = TutorialStage.LEFT_ISLAND
        TutorialTabs.unlockAll(player, eventBus)
        player.tutorialFreeStuffCheck = 1
        player.hasDisplayNameTransmitter = true
        player.usedHomeTeleport = 1
        player.runEnergy = 10_000
        invClear(inv)
        invClear(worn)
        val bank = player.invMap.getOrPut("inv.bank")
        invClear(bank)
        invAdd(bank, TutorialObj.COINS, 25)
        for ((item, count) in COMPLETION_KIT) {
            invAdd(inv, item, count)
        }
        player.mes("Welcome to Old School RuneScape.", ChatType.Welcome)
        player.ifClose(eventBus)
        player.ifCloseSub("interface.tutorial_overlay", eventBus)
        player.ifCloseSub("interface.mesoverlay", eventBus)
        TutorialHighlights.clearAll(player)
        camReset()
        player.flashSide = 0
        player.runClientScript(TUTORIAL_END)
    }

    suspend fun ProtectedAccess.calloutJon() {
        if (player.tutorialStage != TutorialStage.LEFT_ISLAND ||
            player.adventurePaths != ADVENTURE_PATHS_PARTICIPATING ||
            player.vars["varbit.ap_guide_tutorial"] != 0 ||
            player.attr[JON_CALLOUT_SHOWN] == true
        ) {
            return
        }
        val jon =
            npcRepo.findAll(ZoneKey.from(player.coords), 2).firstOrNull {
                it.visType.internalName in AP_GUIDES || it.type.internalName in AP_GUIDES
            } ?: return
        player.attr[JON_CALLOUT_SHOWN] = true
        startDialogue(jon) { chatNpc(neutral, "Hey ${player.displayName}! Come over here.") }
    }

    private companion object {
        private const val ADVENTURE_PATHS_PARTICIPATING = 1
        private val AP_GUIDES =
            setOf("npc.ap_guide_parent", "npc.ap_guide_active", "npc.ap_guide_opt_out")
        private val TUTORIAL_END =
            "clientscript.[clientscript,tutorial_end]".asRSCM(RSCMType.CLIENTSCRIPT)
        private val JON_CALLOUT_SHOWN = AttributeKey<Boolean>(temp = true)
        private val COMPLETION_KIT =
            listOf(
                TutorialObj.BRONZE_AXE to 1,
                TutorialObj.BRONZE_PICKAXE to 1,
                TutorialObj.TINDERBOX to 1,
                TutorialObj.NET to 1,
                TutorialObj.SHRIMP to 1,
                TutorialObj.BRONZE_DAGGER to 1,
                TutorialObj.BRONZE_SWORD to 1,
                TutorialObj.WOODEN_SHIELD to 1,
                TutorialObj.SHORTBOW to 1,
                TutorialObj.BRONZE_ARROW to 25,
                TutorialObj.AIR_RUNE to 25,
                TutorialObj.MIND_RUNE to 15,
                TutorialObj.BUCKET_EMPTY to 1,
                TutorialObj.POT_EMPTY to 1,
                TutorialObj.BREAD to 1,
                TutorialObj.WATER_RUNE to 6,
                TutorialObj.EARTH_RUNE to 4,
                TutorialObj.BODY_RUNE to 2,
            )
    }
}
