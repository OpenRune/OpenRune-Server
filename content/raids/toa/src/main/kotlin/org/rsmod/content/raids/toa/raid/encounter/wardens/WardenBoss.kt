package org.rsmod.content.raids.toa.raid.encounter.wardens

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Magic
import org.rsmod.api.bosses.dsl.Melee
import org.rsmod.api.bosses.dsl.Ranged
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.chance
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.oneOf
import org.rsmod.api.bosses.dsl.projectile
import org.rsmod.api.bosses.dsl.run
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.whenever
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.content.raids.toa.raid.encounter.roomDamage
import org.rsmod.content.raids.toa.raid.encounter.targetCondition
import org.rsmod.plugin.scripts.ScriptContext

class WardenBoss @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = { ToaRooms.of<WardensEncounter>(npc)?.modifyWardenHit(npc, hit) },
        )
    }

    override val spec: BossSpec =
        boss(*WardenGod.entries.flatMap { it.bossTypes }.toTypedArray()) {
            ability(AUTO, auto())
            ability(MELEE, melee())
            ability(RANGED, projectileAttack(Ranged, WardenSeqs.RANGED, WardenSpots.RANGED))
            ability(MAGIC, projectileAttack(Magic, WardenSeqs.MAGIC, WardenSpots.MAGIC))

            phase(FIGHT_PHASE) { rotationSelector { +then(AUTO) } }
        }

    private fun auto(): Effect =
        whenever(
            canMelee,
            whenever(chance(1, MELEE_ODDS), run(MELEE), rangedOrMagic()),
            rangedOrMagic(),
        )

    private fun rangedOrMagic(): Effect = oneOf(run(RANGED), run(MAGIC))

    private fun melee(): Effect =
        sequence(
            anim(WardenSeqs.MELEE),
            hit {
                damage(basicDamage(Melee))
                type(Melee)
                delay = MELEE_HIT_DELAY
                resolveOnImpact()
            },
        )

    private fun projectileAttack(style: HitType, seq: String, spot: String): Effect =
        sequence(
            anim(seq),
            projectile(
                spotanim = spot,
                config = FLIGHT,
                resolveOnImpact = true,
                hit =
                    hit {
                        damage(basicDamage(style))
                        type(style)
                        penetration(PRAYER_PENETRATION)
                    },
            ),
        )

    private fun basicDamage(style: HitType) =
        roomDamage<WardensEncounter> { room, npc, target ->
            val combatant = room.combatantOf(npc) ?: return@roomDamage 0
            val max = room.basicMaxHit(npc)
            if (combatant.rollAccuracy(target, style, null)) combatant.rollScaled(max) else 0
        }

    private companion object {
        const val FIGHT_PHASE = "fight"
        const val AUTO = "auto"
        const val MELEE = "melee"
        const val RANGED = "ranged"
        const val MAGIC = "magic"

        const val MELEE_ODDS = 3
        const val MELEE_HIT_DELAY = 1
        const val PRAYER_PENETRATION = 15

        val FLIGHT =
            ProjectileConfig.fixed(
                startHeight = 400,
                endHeight = 100,
                delay = 60,
                travel = 90,
                angle = 12,
                progress = 128,
            )

        val canMelee =
            targetCondition<WardensEncounter> { room, target ->
                room.canMelee(target)
            }
    }
}
