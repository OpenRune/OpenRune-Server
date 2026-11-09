package org.rsmod.content.bosses.brutus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpObj3
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BullBones @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    private var Player.skipBuryWarning by boolVarBit("varbit.giant_bone_bury_warning_disable")

    override fun ScriptContext.startup() {
        val bones = checkNotNull(ServerCacheManager.getItem(BONES.asRSCM(RSCMType.OBJ)))
        onOpObj3(bones) { bury(it.obj) }
    }

    internal suspend fun ProtectedAccess.bury(obj: Obj) {
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
        if (!objRepo.del(obj)) {
            return
        }
        anim("seq.human_pickupfloor")
        soundSynth(BURY_SYNTH)
        statAdvance("stat.prayer", PRAYER_XP)
    }

    private companion object {
        const val BONES = "obj.bull_bones"
        const val BURY_SYNTH = "synth.bull_bones_bury"
        const val PRAYER_XP = 40.0
        const val YES = 1
        const val YES_ALWAYS = 2
        const val NO = 3
    }
}
