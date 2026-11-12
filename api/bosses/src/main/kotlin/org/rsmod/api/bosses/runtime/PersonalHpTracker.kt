package org.rsmod.api.bosses.runtime

import java.util.IdentityHashMap
import org.rsmod.api.bosses.spec.PersonalHpSpec
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

class PersonalHpState(val player: Player, var hitpoints: Int, var dead: Boolean = false)

/** The per-player pools of a [PersonalHpSpec] boss; see there. */
class PersonalHpTracker(
    private val spec: PersonalHpSpec,
    private val showHeadbar: (Player, Int) -> Unit = { _, _ -> },
    private val writeVarbit: (Player, String, Int) -> Unit = VarPlayerIntMapSetter::set,
) {
    private val states = IdentityHashMap<Player, PersonalHpState>()

    val maxHitpoints: Int
        get() = spec.maxHitpoints

    fun add(player: Player): PersonalHpState {
        val state = PersonalHpState(player, spec.maxHitpoints)
        states[player] = state
        sync(state)
        return state
    }

    fun remove(player: Player) {
        val state = states.remove(player) ?: return
        state.hitpoints = spec.maxHitpoints
        spec.hpVarbit?.let { writeVarbit(player, it, state.hitpoints) }
    }

    fun of(player: Player): PersonalHpState? = states[player]

    /** Returns true only for the hit that empties [player]'s pool. */
    fun damage(player: Player, amount: Int): Boolean {
        val state = states[player] ?: return false
        if (state.dead || amount <= 0) return false
        state.hitpoints = (state.hitpoints - amount).coerceAtLeast(0)
        sync(state)
        if (state.hitpoints > 0) return false
        state.dead = true
        return true
    }

    fun heal(player: Player, amount: Int) {
        val state = states[player] ?: return
        if (state.dead || amount <= 0) return
        state.hitpoints = (state.hitpoints + amount).coerceAtMost(spec.maxHitpoints)
        sync(state)
    }

    fun set(player: Player, hitpoints: Int) {
        val state = states[player] ?: return
        state.hitpoints = hitpoints.coerceIn(0, spec.maxHitpoints)
        state.dead = state.hitpoints == 0
        sync(state)
    }

    fun reset() {
        for (player in states.keys.toList()) remove(player)
    }

    fun players(): Collection<Player> = states.keys.toList()

    private fun sync(state: PersonalHpState) {
        spec.hpVarbit?.let { writeVarbit(state.player, it, state.hitpoints) }
        if (spec.headbar != null) showHeadbar(state.player, state.hitpoints)
    }
}
