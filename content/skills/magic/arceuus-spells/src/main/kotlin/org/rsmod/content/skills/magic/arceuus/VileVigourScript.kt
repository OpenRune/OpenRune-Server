package org.rsmod.content.skills.magic.arceuus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal const val VILE_VIGOUR_OBJ = "obj.placeholder_snakepet_blue"

internal var Player.vileVigourCooldown by boolVarBit("varbit.arceuus_vile_vigour_cooldown")

internal object VileVigour {
    const val ENERGY_PER_PRAYER_POINT = 100
    const val COOLDOWN_TICKS = 17

    fun prayerToSpend(runEnergy: Int, maxEnergy: Int, prayerPoints: Int): Int {
        val missing = maxEnergy - runEnergy
        val needed = (missing + ENERGY_PER_PRAYER_POINT - 1) / ENERGY_PER_PRAYER_POINT
        return min(prayerPoints, needed)
    }
}

class VileVigourScript
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueue(COOLDOWN_QUEUE) { player.vileVigourCooldown = false }
        onIfOverlayButton("component.magic_spellbook:vile_vigour") { cast() }
    }

    private fun ProtectedAccess.cast() {
        val maxEnergy = constants.run_max_energy
        if (player.runEnergy >= maxEnergy) {
            mes("You're already at maximum run energy.")
            return
        }
        if (player.vileVigourCooldown) {
            mes("You can only cast Vile Vigour every 10 seconds.")
            return
        }
        val prayerPoints = player.stat("stat.prayer")
        if (prayerPoints <= 0) {
            mes("You don't have enough prayer points to cast that spell.")
            return
        }

        val spellObj = ServerCacheManager.getItem(VILE_VIGOUR_OBJ.asRSCM(RSCMType.OBJ)) ?: return
        val spell = spells.getObjSpell(spellObj) ?: return

        if (runes.attemptCast(player, spell).isFailure()) {
            return
        }

        val spent = VileVigour.prayerToSpend(player.runEnergy, maxEnergy, prayerPoints)
        player.statSub("stat.prayer", constant = spent, percent = 0)
        player.runEnergy =
            min(maxEnergy, player.runEnergy + spent * VileVigour.ENERGY_PER_PRAYER_POINT)
        UpdateRun.energy(player, player.runEnergy)

        statAdvance("stat.magic", spell.castXp)
        anim(CAST_ANIM)
        spotanim(CAST_SPOTANIM)

        player.vileVigourCooldown = true
        clearQueue(COOLDOWN_QUEUE)
        queue(COOLDOWN_QUEUE, VileVigour.COOLDOWN_TICKS)
    }

    internal companion object {
        const val COOLDOWN_QUEUE = "queue.vile_vigour_cooldown"
        const val CAST_ANIM = "seq.human_cast_vilevigour"
        const val CAST_SPOTANIM = "spotanim.vile_vigour_cast_spotanim"
    }
}
