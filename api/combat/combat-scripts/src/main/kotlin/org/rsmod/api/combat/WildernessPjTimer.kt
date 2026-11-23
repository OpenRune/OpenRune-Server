package org.rsmod.api.combat

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.combat.npc.aggressivePlayer
import org.rsmod.api.combat.npc.attackingPlayer
import org.rsmod.api.combat.npc.lastCombat
import org.rsmod.api.combat.player.aggressiveNpc
import org.rsmod.api.combat.player.lastCombat
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.npc.owner.isSpawnOwnedBy
import org.rsmod.api.player.events.PlayerHitpointsChangedEvent
import org.rsmod.api.player.mapMultiway
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.PlayerUid

internal class WildernessPjTimer
@Inject
constructor(
    private val areas: AreaChecker,
    private val npcs: NpcList,
    private val players: PlayerList,
) : PlayerDeathCleanupHook {
    fun appliesTo(attacker: Player, target: Player): Boolean =
        attacker.coords.isInWilderness(areas) && target.coords.isInWilderness(areas)

    fun blocksAttack(attacker: Player, opponent: PlayerUid? = null): Boolean =
        attacker.attr[STATE]?.blocksAttack(attacker.currentMapClock, opponent) == true &&
            attacker.isInSingles()

    fun protects(target: Player, attacker: PlayerUid? = null): Boolean =
        target.attr[STATE]?.protects(target.currentMapClock, attacker) == true &&
            target.isInSingles()

    fun isSinglesPlusException(npc: Npc, target: Player): Boolean =
        npc.ignoresPvpInterruption() && target.isInSinglesPlus()

    fun hasNpcProtection(player: Player): Boolean {
        if (!player.isInSingles() || player.isInSinglesPlus()) {
            return false
        }
        val npc = player.activeNpc() ?: return false
        return !npc.isSpawnOwnedBy(player) &&
            npc.aggressivePlayer == player.uid &&
            npc.lastCombat + ACTIVE_COMBAT_DELAY > player.currentMapClock
    }

    /**
     * Refreshes protection when a PvP attack is committed, including attacks that deal zero damage.
     * A target in multicombat grants the attacker no protection until it attacks back into singles.
     */
    fun recordAttack(attacker: Player, target: Player) {
        if (!appliesTo(attacker, target)) {
            return
        }
        val attackerInSingles = !attacker.mapMultiway(areas)
        val targetInSingles = !target.mapMultiway(areas)
        if (attackerInSingles) {
            attacker.attr[STATE]?.endKillGrace()
            interruptNpcFight(attacker)
        }
        if (!targetInSingles) {
            return
        }
        target.pjState().attackedBy(attacker.uid, target.currentMapClock)
        interruptNpcFight(target)
        if (attackerInSingles) {
            attacker.pjState().attacked(target.uid, attacker.currentMapClock)
        }
    }

    fun onHitpointsChanged(event: PlayerHitpointsChangedEvent) {
        if (event.newHitpoints > 0 || event.oldHitpoints == 0 || !event.hit.isFromPlayer) {
            return
        }
        val victim = event.player
        val killer = event.hit.resolvePlayerSource(players) ?: return
        if (killer != victim && victim.coords.isInWilderness(areas) && killer.isInSingles()) {
            killer.pjState().killed(victim.uid, killer.currentMapClock)
        }
    }

    override fun cleanup(player: Player) {
        player.attr.remove(STATE)
    }

    private fun interruptNpcFight(player: Player) {
        if (!player.isInSinglesPlus()) {
            return
        }
        val npc = player.activeNpc() ?: return
        if (npc.isSpawnOwnedBy(player) || npc.ignoresPvpInterruption()) {
            return
        }
        npc.pvpInterruptionUntil = player.currentMapClock + PjState.DURATION
        npc.resetMode()
        player.aggressiveNpc = null
    }

    private fun Player.activeNpc(): Npc? {
        if (lastCombat + ACTIVE_COMBAT_DELAY <= currentMapClock) {
            return null
        }
        return aggressiveNpc?.resolve(npcs)?.takeIf {
            it.hitpoints > 0 && it.attackingPlayer == uid
        }
    }

    private fun Player.isInSingles(): Boolean =
        coords.isInWilderness(areas) && !mapMultiway(areas)

    private fun Player.isInSinglesPlus(): Boolean = areas.inArea("area.singles_plus", coords)

    private fun Player.pjState(): PjState = attr[STATE] ?: PjState().also { attr[STATE] = it }

    private fun Npc.ignoresPvpInterruption(): Boolean =
        isType("npc.callisto_singles") ||
            isType("npc.venenatis_singles") ||
            isType("npc.vetion_single") ||
            isType("npc.vetion_2_single") ||
            isType("npc.vetion_trans_single") ||
            isType("npc.vetion_trans_2_single") ||
            isType("npc.wild_cave_superior")

    private companion object {
        private val STATE = AttributeKey<PjState>()
    }
}

internal class PjState {
    private var received: Attack? = null
    private var protection: Attack? = null
    private var killGraceUntil = -1

    /**
     * Only incoming attacks restrict whom the player may attack next. Passing no opponent checks
     * whether the player may switch from PvP to an NPC.
     */
    fun blocksAttack(now: Int, opponent: PlayerUid?): Boolean =
        received?.blocks(now, opponent) == true

    /** Any attack between the pair protects against a third party, even without retaliation. */
    fun protects(now: Int, attacker: PlayerUid?): Boolean =
        now < killGraceUntil || protection?.blocks(now, attacker) == true

    fun attackedBy(opponent: PlayerUid, now: Int) {
        val attack = Attack(opponent, now + DURATION)
        received = attack
        protection = attack
    }

    fun attacked(opponent: PlayerUid, now: Int) {
        protection = Attack(opponent, now + DURATION)
    }

    fun killed(opponent: PlayerUid, now: Int) {
        if (received?.opponent == opponent) {
            received = null
        }
        if (protection?.opponent == opponent) {
            protection = null
        }
        killGraceUntil = now + DURATION
    }

    fun endKillGrace() {
        killGraceUntil = -1
    }

    private data class Attack(val opponent: PlayerUid, val expiresAt: Int) {
        fun blocks(now: Int, other: PlayerUid?): Boolean = now < expiresAt && opponent != other
    }

    companion object {
        const val DURATION = 20
    }
}
