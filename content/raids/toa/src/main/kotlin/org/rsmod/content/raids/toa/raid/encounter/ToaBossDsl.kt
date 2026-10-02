package org.rsmod.content.raids.toa.raid.encounter

import dev.openrune.types.NpcMode
import org.rsmod.api.bosses.dsl.after
import org.rsmod.api.bosses.dsl.area
import org.rsmod.api.bosses.dsl.onEach
import org.rsmod.api.bosses.dsl.playersIn
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.summon
import org.rsmod.api.bosses.dsl.whenever
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossExtensionContext
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal const val ADOPT_SUMMON = "toa.adopt_summon"

internal class RoomSummon(val onSummon: String?, val params: Any?)

internal val challengePlayers: TargetExpr.Multi =
    playersIn(area(challengeCorner(ToaRoom::challengeMin), challengeCorner(ToaRoom::challengeMax)))

private val isTarget = targetCondition<ToaEncounter> { room, target -> target in room.targets() }

internal inline fun <reified R : ToaEncounter> roomCondition(
    noinline test: (R, Npc) -> Boolean,
): Condition = Condition.Custom { npc, _ -> ToaRooms.of<R>(npc)?.let { test(it, npc) } == true }

internal inline fun <reified R : ToaEncounter> targetCondition(
    noinline test: (R, Player) -> Boolean,
): Condition =
    Condition.Custom { npc, target ->
        target != null && ToaRooms.of<R>(npc)?.let { test(it, target) } == true
    }

internal inline fun <reified R : ToaEncounter> roomDamage(
    noinline roll: (R, Npc, Player) -> Int,
): DamageExpr =
    DamageExpr.Custom { npc, target -> ToaRooms.of<R>(npc)?.let { roll(it, npc, target) } ?: 0 }

internal fun scaled(base: Int): DamageExpr = combatantDamage { it.rollScaled(base) }

internal fun scaled(min: Int, base: Int): DamageExpr = combatantDamage { it.rollScaled(min, base) }

internal inline fun <reified R : ToaEncounter> BossDeps.onRoomExternal(
    name: String,
    noinline handler: (R, BossExtensionContext) -> Unit,
) {
    extensionRegistry.register(name) { context: BossExtensionContext ->
        val room = ToaRooms.of<R>(context.npc) ?: return@register
        handler(room, context)
    }
}

internal inline fun <reified R : ToaEncounter> BossDeps.roomCombatGate(
    noinline mayAttack: (R, Npc, Player) -> Boolean,
): suspend StandardNpcAccess.(Player) -> Unit = { target ->
    val room = ToaRooms.of<R>(npc)
    if (room == null || !mayAttack(room, npc, target)) suppressAttacks(npc, 1)
}

internal inline fun <reified R : ToaEncounter> roomCombatTick(
    noinline tick: (R, StandardNpcAccess, Player) -> Unit,
): suspend StandardNpcAccess.(Player) -> Unit = { target ->
    val room = ToaRooms.of<R>(npc)
    if (room != null) tick(room, this, target)
}

internal fun roomSummon(
    npc: String,
    count: Int = 1,
    radius: Int = 3,
    centeredOn: TargetExpr.Single = TargetExpr.Self,
    mode: NpcMode? = null,
    duration: Int = 100,
    onSummon: String? = null,
    onSummonParams: Any? = null,
    owned: Boolean = false,
): Effect =
    summon(
        npc,
        count,
        radius,
        centeredOn,
        mode,
        duration,
        ADOPT_SUMMON,
        RoomSummon(onSummon, onSummonParams),
        owned,
    )

internal fun eachTarget(effect: Effect): Effect =
    onEach(challengePlayers, whenever(isTarget, effect))

internal fun timeline(vararg steps: Pair<Int, Effect>): Effect =
    checkNotNull(
        steps.reversed().fold(null as Effect?) { rest, (ticks, effect) ->
            after(ticks, if (rest == null) effect else sequence(effect, rest))
        }
    )

private fun combatantDamage(roll: (ToaCombatant) -> Int): DamageExpr =
    DamageExpr.Custom { npc, _ -> ToaRooms.roomOf(npc)?.combatantOf(npc)?.let(roll) ?: 0 }

private fun challengeCorner(corner: (ToaRoom) -> CoordGrid?): TargetExpr.Single =
    TargetExpr.Custom { npc, _ ->
        val room = ToaRooms.roomOf(npc)
        val static = room?.room?.let(corner)
        if (room == null || static == null) CoordGrid.NULL else room.coords(static)
    }
