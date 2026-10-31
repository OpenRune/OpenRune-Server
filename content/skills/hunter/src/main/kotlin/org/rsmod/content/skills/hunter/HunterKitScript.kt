package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HunterKitScript
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(SPELL) { cast() }
        onOpHeld1(KIT) { open(it.slot) }
    }

    private suspend fun ProtectedAccess.cast() {
        val component = SPELL.asRSCM(RSCMType.COMPONENT)
        val spell = spells.allSpells().firstOrNull { it.component.packed == component } ?: return
        if (KIT in inv || CONTENTS.all { it in inv }) {
            mes("Why do you want to create a kit? You already have all the Hunter equipment it contains.")
            return
        }
        if (inv.freeSpace() == 0) {
            mes("You don't have enough inventory space.")
            return
        }
        if (runes.attemptCast(player, spell).isFailure()) return
        anim(CAST_SEQ)
        spotanim(CAST_SPOTANIM, height = CAST_SPOTANIM_HEIGHT)
        statAdvance(MAGIC, spell.castXp)
        invAdd(inv, KIT)
        delay(CAST_TICKS)
    }

    private fun ProtectedAccess.open(slot: Int) {
        if (inv.freeSpace() < CONTENTS.size - 1) {
            mes("You need ${CONTENTS.size - 1} free inventory spaces to open the kit.")
            return
        }
        if (invDel(inv, KIT, slot = slot).failure) return
        for (item in CONTENTS) invAdd(inv, item)
        soundSynth(OPEN_SYNTH)
    }

    private companion object {
        const val SPELL = "component.magic_spellbook:hunter_kit"
        const val KIT = "obj.dream_hunter_box"
        const val MAGIC = "stat.magic"
        const val CAST_SEQ = "seq.dream_player_hunter_box_spell"
        const val CAST_SPOTANIM = "spotanim.dream_hunter_spell_spotanim"
        const val CAST_SPOTANIM_HEIGHT = 92
        const val OPEN_SYNTH = "synth.hunter_kit_open"
        const val CAST_TICKS = 3
        val CONTENTS =
            listOf(
                "obj.noose_wand",
                "obj.hunting_butterfly_net",
                "obj.hunting_ojibway_bird_snare",
                "obj.hunting_snare",
                "obj.hunting_teasing_stick",
                "obj.torch_unlit",
                "obj.ii_impling_jar",
                "obj.hunting_box_trap",
                "obj.net_trap",
            )
    }
}
