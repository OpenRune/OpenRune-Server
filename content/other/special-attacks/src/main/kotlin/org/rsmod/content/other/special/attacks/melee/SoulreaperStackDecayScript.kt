package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.mechanics.toxins.SoulreaperStackDecay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onEvent
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Decays one Soul Stack after 50 cycles (30s) without an axe attack. */
class SoulreaperStackDecayScript
@Inject
constructor(
    private val worldClock: MapClock,
    private val players: PlayerList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.LateCycle> { decayDueStacks() }
    }

    private fun decayDueStacks() {
        for (player in players) {
            decayOneSoulStack(player)
        }
    }

    private fun decayOneSoulStack(player: Player) {
        if (!SoulreaperStackDecay.isDue(player, worldClock.cycle)) {
            return
        }

        val stacks = player.vars[SOUL_STACKS].coerceIn(0, MAX_SOUL_STACKS)
        if (stacks == 0) {
            SoulreaperStackDecay.clear(player)
            return
        }

        val remaining = stacks - 1
        VarPlayerIntMapSetter.set(player, SOUL_STACKS, remaining)
        // No heal here - a stack lost to inactivity is a pure loss. The 8-per-stack heal belongs
        // only to Behead actually consuming stacks (SoulreaperAxeSpecialAttack), not to decay.
        if (remaining > 0) {
            SoulreaperStackDecay.reset(player, worldClock.cycle)
        } else {
            SoulreaperStackDecay.clear(player)
        }
    }

    private companion object {
        const val SOUL_STACKS = "varp.soulreaper_stacks"
        const val MAX_SOUL_STACKS = 5
    }
}
