package org.rsmod.content.bosses.bryophyta

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpObj3
import org.rsmod.api.table.prayer.SkillPrayerRow
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BryophytaBones @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {

    private var Player.skipBuryWarning by boolVarBit("varbit.giant_bone_bury_warning_disable")

    override fun ScriptContext.startup() {
        val bones =
            ServerCacheManager.getItem(GIANT_BONES.asRSCM(RSCMType.OBJ))
                ?: error("Missing obj type: $GIANT_BONES")
        onOpObj3(bones) { bury(it.obj) }
    }

    private suspend fun ProtectedAccess.bury(obj: Obj) {
        if (!player.skipBuryWarning) {
            var choice = NO
            startDialogue {
                choice =
                    choice3(
                        "Yes.",
                        YES,
                        "Yes, and don't ask again.",
                        YES_ALWAYS,
                        "No.",
                        NO,
                        title = "Bury the bones? (This grants Prayer XP)",
                    )
            }
            when (choice) {
                YES_ALWAYS -> player.skipBuryWarning = true
                NO -> return
            }
        }
        val row = SkillPrayerRow.all().firstOrNull { it.item.internalName == GIANT_BONES } ?: return
        if (!objRepo.del(obj)) {
            return
        }
        anim("seq.human_pickupfloor")
        soundSynth("synth.bones_down")
        statAdvance("stat.prayer", row.exp.toDouble())
    }

    private companion object {
        private const val GIANT_BONES = "obj.giant_bones"
        private const val YES = 1
        private const val YES_ALWAYS = 2
        private const val NO = 3
    }
}
