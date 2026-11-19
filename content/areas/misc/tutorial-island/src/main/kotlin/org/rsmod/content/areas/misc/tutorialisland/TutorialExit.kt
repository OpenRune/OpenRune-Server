package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invDel
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
        player.tutorialStage = TutorialStage.LEFT_ISLAND
        player.tutorialFreeStuffCheck = 1
        player.hasDisplayNameTransmitter = true
        player.usedHomeTeleport = 1
        invClear(inv)
        invClear(worn)
        val bank = player.invMap.getOrPut("inv.bank")
        for (item in TutorialObj.TUTORIAL_ITEMS) {
            val count = bank.count(item)
            if (count > 0) {
                player.invDel(bank, item, count)
            }
        }
        val quest = Quest.get("quest_learningtheropes")
        if (quest != null && !quest.isQuestCompleted(player)) {
            quest.completeQuest(this)
        }
        player.mes("Welcome to Old School RuneScape.", ChatType.Welcome)
        player.ifClose(eventBus)
        player.ifCloseSub("interface.tutorial_overlay", eventBus)
        player.runClientScript(TUTORIAL_END)
        if (player.adventurePaths == ADVENTURE_PATHS_PARTICIPATING) {
            val jon =
                npcRepo.findAll(ZoneKey.from(player.coords), 2).firstOrNull {
                    it.visType.internalName in AP_GUIDES || it.type.internalName in AP_GUIDES
                }
            if (jon != null) {
                delay(1)
                startDialogue(jon) {
                    chatNpc(neutral, "Hey ${player.displayName}! Come over here.")
                }
            }
        }
    }

    private companion object {
        private const val ADVENTURE_PATHS_PARTICIPATING = 1
        private val AP_GUIDES =
            setOf("npc.ap_guide_parent", "npc.ap_guide_active", "npc.ap_guide_opt_out")
        private val TUTORIAL_END =
            "clientscript.[clientscript,tutorial_end]".asRSCM(RSCMType.CLIENTSCRIPT)
    }
}
