package org.rsmod.content.skills.magic.arceuus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal var Player.wardOfArceuusActive by boolVarBit("varbit.ward_of_arceuus_active")

internal var Player.wardOfArceuusCooldown by boolVarBit("varbit.arceuus_ward_cooldown")

internal const val WARD_OF_ARCEUUS_OBJ = "obj.placeholder_skillcape_ardy_hood_firecape"

class WardOfArceuusScript
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueue(EXPIRE_QUEUE) { expire() }
        onPlayerQueue(COOLDOWN_QUEUE) { player.wardOfArceuusCooldown = false }
        onIfOverlayButton("component.magic_spellbook:ward_of_arceuus") { cast() }
    }

    private fun ProtectedAccess.cast() {
        if (player.wardOfArceuusCooldown) {
            mes("You can only cast Ward of Arceuus every 30 seconds.")
            return
        }

        val spellObj = ServerCacheManager.getItem(WARD_OF_ARCEUUS_OBJ.asRSCM(RSCMType.OBJ)) ?: return
        val spell = spells.getObjSpell(spellObj) ?: return

        if (runes.attemptCast(player, spell).isFailure()) {
            return
        }

        statAdvance("stat.magic", spell.castXp)
        anim("seq.human_cast_selfimbue")
        spotanim("spotanim.ward_of_arceuus_cast_spotanim")
        mes("Your defence against Arceuus magic has been strengthened.")

        if (player.isCorrupted) {
            player.clearTimer(CORRUPTION_TIMER)
            player.clearCorruption()
            mes("Your ward cleanses you of corruption.")
        }

        val duration = player.magicLvl
        player.wardOfArceuusActive = true
        player.wardOfArceuusCooldown = true
        clearQueue(EXPIRE_QUEUE)
        clearQueue(COOLDOWN_QUEUE)
        queue(EXPIRE_QUEUE, duration)
        queue(COOLDOWN_QUEUE, COOLDOWN_TICKS)
        runClientScript(BUFF_BAR_START_CLIENTSCRIPT, ACTIVE_BUFF_STRUCT, mapClock)
        runClientScript(BUFF_BAR_START_CLIENTSCRIPT, COOLDOWN_BUFF_STRUCT, mapClock)
    }

    private fun ProtectedAccess.expire() {
        player.wardOfArceuusActive = false
        mes("Your Ward of Arceuus has expired.")
    }

    private companion object {
        const val CORRUPTION_TIMER = Corruption.TIMER
        const val EXPIRE_QUEUE = "queue.ward_of_arceuus_expire"
        const val COOLDOWN_QUEUE = "queue.ward_of_arceuus_cooldown"
        const val BUFF_BAR_START_CLIENTSCRIPT = 5931
        const val ACTIVE_BUFF_STRUCT = 3126
        const val COOLDOWN_BUFF_STRUCT = 3125
        const val COOLDOWN_TICKS = 50
    }
}
