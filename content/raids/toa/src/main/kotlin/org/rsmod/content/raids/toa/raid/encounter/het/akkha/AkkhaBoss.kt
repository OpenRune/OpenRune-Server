package org.rsmod.content.raids.toa.raid.encounter.het.akkha

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Magic
import org.rsmod.api.bosses.dsl.Melee
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.Ranged
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.projectile
import org.rsmod.api.bosses.dsl.run
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.spotanim
import org.rsmod.api.bosses.dsl.whenever
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.bosses.spec.ImpactRounding
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.content.raids.toa.raid.encounter.crondis.zebak.isBeside
import org.rsmod.content.raids.toa.raid.encounter.eachTarget
import org.rsmod.content.raids.toa.raid.encounter.targetCondition
import org.rsmod.content.raids.toa.raid.encounter.onRoomExternal
import org.rsmod.content.raids.toa.raid.encounter.raidAccuracy
import org.rsmod.content.raids.toa.raid.encounter.roomCondition
import org.rsmod.plugin.scripts.ScriptContext

class AkkhaBoss @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = {
                ToaRooms.of<AkkhaEncounter>(npc)?.modifyAkkhaHit(npc, hit)
            },
        )
        deps.onRoomExternal<AkkhaEncounter>(COUNT_AUTO) { room, _ -> room.countAuto() }
        deps.onRoomExternal<AkkhaEncounter>(MEMORY_START) { room, ext -> room.startMemory(ext.npc) }
        deps.onRoomExternal<AkkhaEncounter>(PICK_SPECIAL) { room, _ -> room.pickSpecial() }
        deps.onRoomExternal<AkkhaEncounter>(TRAIL_START) { room, _ -> room.startTrail() }
        deps.onRoomExternal<AkkhaEncounter>(DETONATE_START) { room, _ -> room.startDetonate() }
        deps.onRoomExternal<AkkhaEncounter>(BEGIN_SPECIAL) { room, ext ->
            room.beginSpecial(ext.npc)
        }
    }

    override val spec: BossSpec =
        boss(AkkhaNpcs.MELEE, AkkhaNpcs.RANGE, AkkhaNpcs.MAGE, AkkhaNpcs.SPAWN) {
            ability(AUTO, auto())
            ability(MELEE, melee())
            ability(RANGED, projectileAttack(magic = false))
            ability(MAGIC, projectileAttack(magic = true))
            ability(
                SPECIAL,
                sequence(
                    external(BEGIN_SPECIAL),
                    external(PICK_SPECIAL),
                    whenever(
                        pickedMemory,
                        run(MEMORY_BLAST),
                        whenever(pickedTrail, run(TRAIL), run(DETONATE)),
                    ),
                ),
            )
            ability(TRAIL, sequence(anim(AkkhaSeqs.TRAIL), external(TRAIL_START)))
            ability(DETONATE, sequence(anim(AkkhaSeqs.DETONATE), external(DETONATE_START)))
            ability(MEMORY_BLAST, sequence(anim(AkkhaSeqs.MEMORY), external(MEMORY_START)))

            phase(MAIN_PHASE) {
                forceWhen(specialDue, SPECIAL)
                rotationSelector { +then(AUTO) }
            }
        }

    private fun auto(): Effect =
        whenever(
            fighting,
            sequence(
                external(COUNT_AUTO),
                whenever(
                    styleMelee,
                    run(MELEE),
                    whenever(styleRanged, run(RANGED), run(MAGIC)),
                ),
            ),
        )

    private fun melee(): Effect = sequence(anim(AkkhaSeqs.MELEE), cleave())

    private fun cleave(): Effect = eachTarget(whenever(besideAkkha, meleeHit()))

    private fun meleeHit(): Effect =
        hit {
            damage(raidAccuracy(MELEE_MAX_HIT, Melee, MeleeAttackType.Slash))
            type(Melee)
            delay = MELEE_HIT_DELAY
            penetration(MELEE_PENETRATION)
            resolveOnImpact()
            reactOnLanding()
        }

    private fun projectileAttack(magic: Boolean): Effect {
        val style: HitType = if (magic) Magic else Ranged
        return sequence(
            whenever(keepBack, cleave()),
            anim(if (magic) AkkhaSeqs.MAGIC else AkkhaSeqs.RANGED),
            projectile(
                spotanim = if (magic) AkkhaSpots.MAGIC_TRAVEL else AkkhaSpots.RANGED_TRAVEL,
                config = PROJECTILE,
                resolveOnImpact = true,
                impactRounding = ImpactRounding.Up,
                hit =
                    hit {
                        damage(raidAccuracy(RANGED_MAGIC_MAX_HIT, style))
                        type(style)
                        penetration(RANGED_MAGIC_PENETRATION)
                        if (magic) {
                            spotanim(AkkhaSpots.MAGIC_IMPACT, height = MAGIC_IMPACT_HEIGHT)
                        }
                    },
            ),
        )
    }

    private companion object {
        const val MAIN_PHASE = "main"
        const val AUTO = "auto"
        const val MELEE = "melee"
        const val RANGED = "ranged"
        const val MAGIC = "magic"
        const val SPECIAL = "special"
        const val COUNT_AUTO = "akkha.count_auto"
        const val MEMORY_BLAST = "memory_blast"
        const val BEGIN_SPECIAL = "akkha.begin_special"
        const val MEMORY_START = "akkha.memory_start"
        const val PICK_SPECIAL = "akkha.pick_special"
        const val TRAIL = "trail"
        const val DETONATE = "detonate"
        const val TRAIL_START = "akkha.trail_start"
        const val DETONATE_START = "akkha.detonate_start"

        const val MELEE_MAX_HIT = 22
        const val RANGED_MAGIC_MAX_HIT = 22
        const val MELEE_HIT_DELAY = 1
        const val MELEE_PENETRATION = 25
        const val RANGED_MAGIC_PENETRATION = 15
        const val MAGIC_IMPACT_HEIGHT = 124

        val PROJECTILE =
            ProjectileConfig(
                startHeight = 404,
                endHeight = 138,
                startDelay = 0,
                travelTime = 30,
                angle = 12,
                progress = 32,
                stepMultiplier = 5,
            )

        val fighting = roomCondition<AkkhaEncounter> { room, npc -> room.fighting(npc) }
        val specialDue = roomCondition<AkkhaEncounter> { room, npc ->
            room.fighting(npc) && room.specialDue
        }
        val pickedMemory =
            roomCondition<AkkhaEncounter> { room, _ -> room.picked == AkkhaEncounter.Special.MEMORY }
        val pickedTrail =
            roomCondition<AkkhaEncounter> { room, _ -> room.picked == AkkhaEncounter.Special.TRAIL }
        val keepBack = roomCondition<AkkhaEncounter> { room, _ ->
            room.raid.isActive(ToaInvocationKey.KeepBack)
        }
        val besideAkkha = targetCondition<AkkhaEncounter> { room, target ->
            room.akkha?.isBeside(target) == true
        }
        val styleMelee =
            roomCondition<AkkhaEncounter> { room, _ -> room.style == AkkhaEncounter.Style.MELEE }
        val styleRanged =
            roomCondition<AkkhaEncounter> { room, _ -> room.style == AkkhaEncounter.Style.RANGED }
    }
}
