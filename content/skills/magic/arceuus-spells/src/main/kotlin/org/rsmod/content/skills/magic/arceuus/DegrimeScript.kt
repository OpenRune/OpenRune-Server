package org.rsmod.content.skills.magic.arceuus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.table.herblore.HerbloreCleaningRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal const val DEGRIME_OBJ = "obj.placeholder_skillpethunter_black"

class DegrimeScript
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) :
    PluginScript() {
    private val herbs: List<HerbloreCleaningRow> by lazy {
        HerbloreCleaningRow.all().filter { it.category == HERB_CATEGORY }
    }

    override fun ScriptContext.startup() {
        onIfOverlayButton("component.magic_spellbook:degrime") { cast() }
    }

    private suspend fun ProtectedAccess.cast() {
        if (cleanable().isEmpty()) {
            mes("You don't have any suitable herbs to clean.")
            return
        }

        val spellObj = ServerCacheManager.getItem(DEGRIME_OBJ.asRSCM(RSCMType.OBJ)) ?: return
        val spell = spells.getObjSpell(spellObj) ?: return

        if (runes.attemptCast(player, spell).isFailure()) {
            return
        }

        anim(PREPARE_ANIM)
        delay(PREPARE_TICKS)
        anim(CAST_ANIM)
        spotanim(CAST_SPOTANIM)
        delay(CAST_TICKS)

        statAdvance("stat.magic", spell.castXp)
        for (row in cleanable()) {
            clean(row)
        }
    }

    private fun ProtectedAccess.cleanable(): List<HerbloreCleaningRow> =
        herbs.filter { row ->
            inv.contains(row.input.internalName) &&
                row.statReq.all { statBase(it.t0.internalName) >= it.t1 }
        }

    private fun ProtectedAccess.clean(row: HerbloreCleaningRow) {
        val count = inv.count(row.input.internalName)
        if (count == 0 || invDel(inv, row.input.internalName, count).failure) {
            return
        }
        if (invAdd(inv, row.output.internalName, count).failure) {
            invAdd(inv, row.input.internalName, count)
            return
        }
        statAdvance("stat.herblore", row.xp / 2.0 * count)
    }

    internal companion object {
        const val HERB_CATEGORY = "Herbs"
        const val PREPARE_ANIM = "seq.human_prepare_degrime"
        const val CAST_ANIM = "seq.human_spellcast_degrime"
        const val CAST_SPOTANIM = "spotanim.degrime_cast_spotanim"
        const val PREPARE_TICKS = 4
        const val CAST_TICKS = 2
    }
}
