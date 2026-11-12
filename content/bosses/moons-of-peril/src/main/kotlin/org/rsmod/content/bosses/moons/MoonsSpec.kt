package org.rsmod.content.bosses.moons

import dev.openrune.types.NpcMode
import org.rsmod.api.bosses.dsl.BossSpecBuilder
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.nextAttackIn
import org.rsmod.api.bosses.dsl.onEach
import org.rsmod.api.bosses.dsl.parallel
import org.rsmod.api.bosses.dsl.playersIn
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.sound
import org.rsmod.api.bosses.dsl.spawnTile
import org.rsmod.api.bosses.dsl.summon
import org.rsmod.api.bosses.spec.Effect

internal fun BossSpecBuilder.focusPhase(
    moon: Moon,
    focus: Int,
    next: String,
    burn: Effect,
    ticks: Int = 12,
    firstAttack: Int = 3,
    spawnCircle: Boolean = true,
) {
    val (dx, dz) = Moon.GLYPH_OFFSETS[focus]
    val arena = moon.arena()
    val standard =
        ability(
            "standard_attack_$focus",
            sequence(
                parallel(
                    *moon.attackSounds.map { (synth, delay) -> sound(synth, delay = delay) }.toTypedArray(),
                    anim(moon.attackAnim),
                ),
                onEach(playersIn(arena), external(MoonsExternals.STANDARD_ATTACK)),
            ),
        )
    val entryEffects = mutableListOf(external(MoonsExternals.FOCUS, params = focus))
    if (spawnCircle) {
        entryEffects +=
            summon(
                "npc.pmoon_boss_threat_circle",
                radius = 0,
                centeredOn = spawnTile(dx, dz),
                mode = NpcMode.None,
                duration = ticks + 1,
                onSummon = MoonsExternals.CIRCLE_SPAWN,
            )
    }
    entryEffects += nextAttackIn(firstAttack)
    val entry = ability("enter_focus_$focus", sequence(*entryEffects.toTypedArray()))
    phase("focus_$focus", lockMovement = true, exitAfter = ticks, nextPhase = next) {
        this.entry = entry.name
        weightedSelectorRandom { +random(standard, weight = 1) }
        every(1, burn)
    }
}

internal fun Moon.arena() =
    org.rsmod.api.bosses.dsl.area(
        spawnTile(Moon.ARENA_WEST, Moon.ARENA_SOUTH),
        spawnTile(Moon.ARENA_EAST, Moon.ARENA_NORTH),
    )
