package org.rsmod.content.skills.magic.spellbook.altars

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.prayerLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.spells.autocast.MagicSpellbookManager
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AncientAltarScript @Inject constructor(private val spellbooks: MagicSpellbookManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.dt_zaros_altar") { prayAt() }
    }

    private suspend fun ProtectedAccess.prayAt() {
        arriveDelay()
        if (!QuestRequirements.hasCompleted(player, "quest_deserttreasure")) {
            mes("You need to complete Desert Treasure I to use this altar.")
            return
        }
        anim("seq.human_pray")
        soundSynth("synth.prayer_altar_recharge")
        statSub("stat.prayer", constant = player.prayerLvl, percent = 0)
        if (spellbooks.activeSpellbook(player) == Spellbook.Ancients) {
            spellbooks.setSpellbook(player, Spellbook.Standard)
            mes("You feel a strange drain upon your memory...")
        } else {
            spellbooks.setSpellbook(player, Spellbook.Ancients)
            mes("You feel a strange wisdom fill your mind...")
        }
    }
}
