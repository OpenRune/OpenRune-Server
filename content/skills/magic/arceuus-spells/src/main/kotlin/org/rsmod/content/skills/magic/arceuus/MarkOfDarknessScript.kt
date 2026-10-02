package org.rsmod.content.skills.magic.arceuus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getOrNull
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal var Player.markOfDarknessActive by boolVarBit("varbit.mark_of_darkness_active")

internal const val MARK_OF_DARKNESS_OBJ = "obj.br_bass"

class MarkOfDarknessScript
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueue(EXPIRE_QUEUE) { expire() }
        onPlayerQueue(WARN_QUEUE) { mes("Your Mark of Darkness is about to run out.") }
        onIfOverlayButton("component.magic_spellbook:mark_of_darkness") { cast() }
    }

    private fun ProtectedAccess.cast() {
        val spellObj = ServerCacheManager.getItem(MARK_OF_DARKNESS_OBJ.asRSCM(RSCMType.OBJ)) ?: return
        val spell = spells.getObjSpell(spellObj) ?: return

        if (runes.attemptCast(player, spell).isFailure()) {
            return
        }

        statAdvance("stat.magic", spell.castXp)
        anim("seq.human_cast_selfimbue")
        spotanim("spotanim.mark_of_darkness_cast_spotanim")
        mes("You have placed a Mark of Darkness upon yourself.")

        val duration = player.durationTicks()
        player.markOfDarknessActive = true
        clearQueue(EXPIRE_QUEUE)
        clearQueue(WARN_QUEUE)
        queue(EXPIRE_QUEUE, duration)
        if (duration > WARN_TICKS) {
            queue(WARN_QUEUE, duration - WARN_TICKS)
        }
        runClientScript(BUFF_BAR_START_CLIENTSCRIPT, BUFF_BAR_STRUCT, duration)
    }

    private fun ProtectedAccess.expire() {
        player.markOfDarknessActive = false
        spotanim("spotanim.mark_of_darkness_end_spotanim")
        mes("Your Mark of Darkness has faded away.")
    }

    private fun Player.durationTicks(): Int {
        val base = statBase("stat.magic") * TICKS_PER_MAGIC_LEVEL
        val purging = getOrNull(righthand)?.isType("obj.purging_staff") == true
        return if (purging) base * PURGING_STAFF_MULTIPLIER else base
    }

    private companion object {
        const val EXPIRE_QUEUE = "queue.mark_of_darkness_expire"
        const val WARN_QUEUE = "queue.mark_of_darkness_warn"
        const val BUFF_BAR_START_CLIENTSCRIPT = 5931
        const val BUFF_BAR_STRUCT = 3120
        const val TICKS_PER_MAGIC_LEVEL = 3
        const val PURGING_STAFF_MULTIPLIER = 5
        const val WARN_TICKS = 10
    }
}
