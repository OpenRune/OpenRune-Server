package org.rsmod.content.slayer

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.content.slayer.dialogue.SlayerMasters
import org.rsmod.content.slayer.dialogue.SlayerMasters.spriaStart
import org.rsmod.content.slayer.dialogue.SlayerMasters.steveStart
import org.rsmod.content.slayer.dialogue.StandardSlayerDialogue.openMain
import org.rsmod.content.slayer.dialogue.StandardSlayerDialogue.requestAssignment
import org.rsmod.content.slayer.dialogue.masters.KonarDialogue.needAnotherAssignment as konarNeedAssignment
import org.rsmod.content.slayer.dialogue.masters.KonarDialogue.start as konarStart
import org.rsmod.content.slayer.dialogue.masters.KrystiliaDialogue.needAnotherAssignment as krystiliaNeedAssignment
import org.rsmod.content.slayer.dialogue.masters.KrystiliaDialogue.start as krystiliaStart
import org.rsmod.content.slayer.dialogue.masters.TuradelDialogue.start as turaelStart
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SlayerEvents @Inject constructor(private val npcRepo: NpcRepository) : PluginScript() {


    override fun ScriptContext.startup() {
        spawnTurael()
        registerSlayerMasterHandlers()
    }

    private fun ScriptContext.spawnTurael() {
        val type = ServerCacheManager.getNpc(SlayerMasters.Npc.turael) ?: return
        val npc = Npc(type, CoordGrid(TURAEL_SPAWN_X, TURAEL_SPAWN_Z))
        npc.mode = NpcMode.None
        npcRepo.add(npc, Int.MAX_VALUE)
    }


    private fun ScriptContext.registerSlayerMasterHandlers() {
        val npcIds = SlayerTaskManager.slayerMasterNpcs.map { it.id }.toSet()
        for (npcId in npcIds) {
            val npcName = RSCM.getReverseMapping(RSCMType.NPC, npcId)
            onOpNpc1(npcName) { handleOp1(it.npc) }
            onOpNpc3(npcName) { handleOp3(it.npc) }
            onOpNpc4(npcName) { handleOp4() }
            onOpNpc5(npcName) { handleOp5() }
        }
    }

    private suspend fun ProtectedAccess.handleOp1(npc: Npc) {
        focusMaster(npc)
        startDialogue(npc) {
            when (npc.id) {
                SlayerMasters.Npc.turael -> turaelStart()
                SlayerMasters.Npc.krystilia -> krystiliaStart()
                SlayerMasters.Npc.konar -> konarStart()
                SlayerMasters.Npc.spria, SlayerMasters.Npc.spriaActive -> spriaStart()
                SlayerMasters.Npc.steve -> steveStart()
                else -> openMain(npc.id,
                    extras = SlayerMasters.extraMenuOptions(this, npc.id)
                )
            }
        }
    }

    private suspend fun ProtectedAccess.handleOp3(npc: Npc) {
        focusMaster(npc)
        startDialogue(npc) {
            when (npc.id) {
                SlayerMasters.Npc.turael -> requestAssignment(SlayerMasters.Npc.turael)
                SlayerMasters.Npc.krystilia -> krystiliaNeedAssignment()
                SlayerMasters.Npc.konar -> konarNeedAssignment()
                SlayerMasters.Npc.spria, SlayerMasters.Npc.spriaActive -> requestAssignment(
                    if (npc.id == SlayerMasters.Npc.spria) SlayerMasters.Npc.spria
                    else SlayerMasters.Npc.spriaActive
                )
                SlayerMasters.Npc.steve -> requestAssignment(SlayerMasters.Npc.steve)
                else -> requestAssignment(npc.id)
            }
        }
    }

    private fun ProtectedAccess.handleOp4() {
        SlayerInterfaces.openSlayerEquipment(this)
    }

    private fun ProtectedAccess.handleOp5() {
        SlayerInterfaces.openSlayerRewards(this)
    }

    private fun ProtectedAccess.focusMaster(npc: Npc) {
        val master = SlayerTaskManager.findMasterByNpc(npc.id) ?: return
        VarPlayerIntMapSetter.set(player, "varbit.slayer_master_in_focus", master.masterId)
    }

    private companion object {
        private const val TURAEL_SPAWN_X = 2931
        private const val TURAEL_SPAWN_Z = 3536
    }

}
