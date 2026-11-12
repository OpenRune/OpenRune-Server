package org.rsmod.content.bosses.moons.blood

import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.content.bosses.moons.Moon
import org.rsmod.content.bosses.moons.MoonsFights
import org.rsmod.content.bosses.moons.MoonsExternals

class BloodMoon @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    override val spec: BossSpec =
        boss(Moon.Blood.npc) {
            ambient()
            stats(attackRate = STANDARD_ATTACK_RATE)

            val subdued = ability("subdued", external(MoonsExternals.SUBDUED))
            personalHp(Moon.MAX_HITPOINTS, hpVarbit = "varbit.hpbar_hud_hp", onDeath = subdued)

            val burn = onEach(playersIn(ARENA), external(MoonsExternals.BURN))
            every(
                CONDITION_DECAY_TICKS,
                onEach(playersIn(ARENA), addPlayerVar(MoonsFights.CONDITION, -CONDITION_DECAY)),
            )

            fun focusPhase(focus: Int, next: String) {
                val (dx, dz) = Moon.GLYPH_OFFSETS[focus]
                val standard =
                    ability(
                        "standard_attack_$focus",
                        sequence(
                            anim("seq.npc_djinn_01_combo"),
                            onEach(playersIn(ARENA), external(MoonsExternals.STANDARD_ATTACK)),
                        ),
                    )
                val entry =
                    ability(
                        "enter_focus_$focus",
                        sequence(
                            external(MoonsExternals.FOCUS, params = focus),
                            summon(
                                "npc.pmoon_boss_threat_circle",
                                radius = 0,
                                centeredOn = spawnTile(dx, dz),
                                mode = NpcMode.None,
                                duration = FOCUS_TICKS + 1,
                                onSummon = MoonsExternals.CIRCLE_SPAWN,
                            ),
                            nextAttackIn(FIRST_ATTACK_OFFSET),
                        ),
                    )
                phase("focus_$focus", lockMovement = true, exitAfter = FOCUS_TICKS, nextPhase = next) {
                    this.entry = entry.name
                    weightedSelectorRandom { +random(standard, weight = 1) }
                    every(1, burn)
                }
            }

            val jaguars = ability("jaguars", BloodJaguars.timeline)
            val rain = ability("rain", BloodRain.timeline)

            phase("jaguars", lockMovement = true, exitAfter = JAGUARS_TICKS, nextPhase = "focus_1") {
                entry = jaguars.name
            }
            focusPhase(1, next = "focus_2")
            focusPhase(2, next = "focus_3")
            focusPhase(3, next = "rain")
            phase("rain", lockMovement = true, exitAfter = RAIN_TICKS, nextPhase = "focus_5") {
                entry = rain.name
            }
            focusPhase(5, next = "focus_6")
            focusPhase(6, next = "focus_7")
            focusPhase(7, next = "jaguars")
        }

    private companion object {
        private const val STANDARD_ATTACK_RATE = 6
        private const val FIRST_ATTACK_OFFSET = 3
        private const val FOCUS_TICKS = 12
        private const val CONDITION_DECAY_TICKS = 5
        private const val CONDITION_DECAY = 2
        private const val JAGUARS_TICKS = 53
        private const val RAIN_TICKS = 31
        private val ARENA = area(spawnTile(-12, -14), spawnTile(18, 15))
    }
}
