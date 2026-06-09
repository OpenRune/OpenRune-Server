package org.rsmod.content.skills.runecrafting.magic

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCMType
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.content.skills.runecrafting.magic.MagicImbue.activate
import org.rsmod.content.skills.runecrafting.magic.MagicImbue.deactivate
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MagicImbueEvents @Inject constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueue(MagicImbue.EXPIRE_QUEUE) {
            deactivate()
            mes("Your Magic Imbue effect has worn off.")
        }

        val spellObj =
            ServerCacheManager.getItem("obj.82_magic_imbue".asRSCM(RSCMType.OBJ))
                ?: return
        val spell = spells.getObjSpell(spellObj) ?: return

        onIfOverlayButton(spell.component) {
            castMagicImbue()
        }
    }

    private fun ProtectedAccess.castMagicImbue() {
        val spellObj =
            ServerCacheManager.getItem("obj.82_magic_imbue".asRSCM(RSCMType.OBJ))
                ?: return
        val spell = spells.getObjSpell(spellObj) ?: return

        val result = runes.attemptCast(player, spell)
        if (result.isFailure()) {
            return
        }

        statAdvance("stat.magic", spell.castXp)
        activate()
    }
}
