package org.rsmod.content.skills.magic.arceuus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal const val SHADOW_VEIL_OBJ = "obj.placeholder_skillpethunter_grey"

internal var Player.shadowVeilActive by boolVarBit("varbit.arceuus_shadow_veil_active")

internal var Player.shadowVeilCooldown by boolVarBit("varbit.arceuus_shadow_veil_cooldown")

class ShadowVeilScript
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueue(EXPIRE_QUEUE) { expire() }
        onPlayerQueue(COOLDOWN_QUEUE) { player.shadowVeilCooldown = false }
        onIfOverlayButton("component.magic_spellbook:shadow_veil") { cast() }
    }

    private fun ProtectedAccess.cast() {
        if (player.shadowVeilCooldown) {
            mes("You can only cast Shadow Veil every 30 seconds.")
            return
        }

        val spellObj = ServerCacheManager.getItem(SHADOW_VEIL_OBJ.asRSCM(RSCMType.OBJ)) ?: return
        val spell = spells.getObjSpell(spellObj) ?: return

        if (runes.attemptCast(player, spell).isFailure()) {
            return
        }

        statAdvance("stat.magic", spell.castXp)
        anim(CAST_ANIM)
        spotanim(CAST_SPOTANIM)
        mes("Your thieving abilities have been enhanced.")

        player.shadowVeilActive = true
        player.shadowVeilCooldown = true
        clearQueue(EXPIRE_QUEUE)
        clearQueue(COOLDOWN_QUEUE)
        queue(EXPIRE_QUEUE, player.statBase("stat.magic"))
        queue(COOLDOWN_QUEUE, COOLDOWN_TICKS)
        runClientScript(BUFF_BAR_START_CLIENTSCRIPT, ACTIVE_BUFF_STRUCT, mapClock)
        runClientScript(BUFF_BAR_START_CLIENTSCRIPT, COOLDOWN_BUFF_STRUCT, mapClock)
    }

    private fun ProtectedAccess.expire() {
        player.shadowVeilActive = false
        mes("Your Shadow Veil has faded away.")
    }

    internal companion object {
        const val EXPIRE_QUEUE = "queue.shadow_veil_expire"
        const val COOLDOWN_QUEUE = "queue.shadow_veil_cooldown"
        const val CAST_ANIM = "seq.human_spellcast_shadowveil"
        const val CAST_SPOTANIM = "spotanim.shadow_veil_cast_spotanim"
        const val BUFF_BAR_START_CLIENTSCRIPT = 5931
        const val ACTIVE_BUFF_STRUCT = 3122
        const val COOLDOWN_BUFF_STRUCT = 3121
        const val COOLDOWN_TICKS = 50
    }
}
