package org.rsmod.content.raids.toa.raid.encounter.wardens

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.CurrentTargetTile
import org.rsmod.api.bosses.dsl.ImpactTile
import org.rsmod.api.bosses.dsl.Magic
import org.rsmod.api.bosses.dsl.Melee
import org.rsmod.api.bosses.dsl.Ranged
import org.rsmod.api.bosses.dsl.after
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.chance
import org.rsmod.api.bosses.dsl.disablePrayers
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.mapSpotanim
import org.rsmod.api.bosses.dsl.message
import org.rsmod.api.bosses.dsl.oneOf
import org.rsmod.api.bosses.dsl.projectile
import org.rsmod.api.bosses.dsl.run
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.tile
import org.rsmod.api.bosses.dsl.whenever
import org.rsmod.api.bosses.dsl.withTile
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.content.raids.toa.raid.encounter.ToaRooms
import org.rsmod.content.raids.toa.raid.encounter.eachTarget
import org.rsmod.content.raids.toa.raid.encounter.onRoomExternal
import org.rsmod.content.raids.toa.raid.encounter.roomCondition
import org.rsmod.content.raids.toa.raid.encounter.roomDamage
import org.rsmod.content.raids.toa.raid.encounter.scaled
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
        deps.onRoomExternal<WardensEncounter>(ENTOMB_LAND) { room, ext ->
            room.entomb.land(ext.target, ext.tile)
        }
    }

    override val spec: BossSpec =
        boss(*WardenGod.entries.flatMap { it.bossTypes }.toTypedArray()) {
            ability(WardenAbilities.AUTO, auto())
            ability(WardenAbilities.MELEE, melee())
            ability(
                WardenAbilities.RANGED,
                projectileAttack(Ranged, WardenSeqs.RANGED, WardenSpots.RANGED),
            )
            ability(
                WardenAbilities.MAGIC,
                projectileAttack(Magic, WardenSeqs.MAGIC, WardenSpots.MAGIC),
            )
            ability(WardenAbilities.SPECIAL, special())
            ability(
                WardenAbilities.DIVINE_MELEE,
                divine(
                    Melee,
                    WardenSpots.PRAYER_MELEE_TRAVEL,
                    WardenSpots.PRAYER_MELEE_IMPACT,
                    WardenMessages.SCIMITAR,
                ),
            )
            ability(
                WardenAbilities.DIVINE_RANGED,
                divine(
                    Ranged,
                    WardenSpots.PRAYER_RANGED_TRAVEL,
                    WardenSpots.PRAYER_RANGED_IMPACT,
                    WardenMessages.ARROW,
                ),
            )
            ability(
                WardenAbilities.DIVINE_MAGIC,
                divine(
                    Magic,
                    WardenSpots.PRAYER_MAGIC_TRAVEL,
                    WardenSpots.PRAYER_MAGIC_IMPACT,
                    WardenMessages.SPELL,
                ),
            )
            ability(WardenAbilities.ENTOMB, entomb())

            phase(FIGHT_PHASE) { rotationSelector { +then(WardenAbilities.AUTO) } }
        }

    private fun auto(): Effect = whenever(specialRoll, run(WardenAbilities.SPECIAL), basic())

    private fun basic(): Effect =
        whenever(
            canMelee,
            whenever(chance(1, MELEE_ODDS), run(WardenAbilities.MELEE), rangedOrMagic()),
            rangedOrMagic(),
        )

    private fun rangedOrMagic(): Effect =
        oneOf(run(WardenAbilities.RANGED), run(WardenAbilities.MAGIC))

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
            after(
                PROJECTILE_DELAY,
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
            ),
        )

    private fun special(): Effect =
        sequence(
            anim(WardenSeqs.MAGIC),
            whenever(
                entombPhase,
                run(WardenAbilities.ENTOMB),
                oneOf(
                    run(WardenAbilities.DIVINE_MELEE),
                    run(WardenAbilities.DIVINE_RANGED),
                    run(WardenAbilities.DIVINE_MAGIC),
                ),
            ),
        )

    private fun divine(style: HitType, travel: String, impact: String, text: String): Effect =
        after(
            PROJECTILE_DELAY,
            eachTarget(
                sequence(
                    message(text),
                    disablePrayers(overheadsOnly = true),
                    message(WardenMessages.PRAYERS_DISABLED),
                    projectile(
                        spotanim = travel,
                        config = DIVINE_FLIGHT,
                        impact = impact,
                        resolveOnImpact = true,
                        hit =
                            hit {
                                damage(scaled(DIVINE_MIN_HIT, DIVINE_MAX_HIT))
                                type(style)
                            },
                    ),
                )
            ),
        )

    private fun entomb(): Effect =
        after(
            PROJECTILE_DELAY,
            eachTarget(
                withTile(
                    CELL,
                    CurrentTargetTile,
                    sequence(
                        mapSpotanim(WardenSpots.SHADOW, tile(CELL)),
                        projectile(
                            spotanim = WardenSpots.ENTOMB_TRAVEL,
                            config = ENTOMB_FLIGHT,
                            target = tile(CELL),
                            onImpact = external(ENTOMB_LAND, at = ImpactTile),
                        ),
                    ),
                )
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
        const val CELL = "cell"
        const val ENTOMB_LAND = "wardens.entomb_land"

        const val PROJECTILE_DELAY = 2
        const val MELEE_ODDS = 3
        const val MELEE_HIT_DELAY = 1
        const val PRAYER_PENETRATION = 15
        const val DIVINE_MIN_HIT = 22
        const val DIVINE_MAX_HIT = 32

        val FLIGHT =
            ProjectileConfig.fixed(
                startHeight = 400,
                endHeight = 100,
                delay = 0,
                travel = 90,
                angle = 12,
                progress = 128,
            )

        val DIVINE_FLIGHT =
            ProjectileConfig.fixed(
                startHeight = 100,
                endHeight = 25,
                delay = 0,
                travel = 120,
                angle = 12,
                progress = 64,
            )

        val ENTOMB_FLIGHT =
            ProjectileConfig.fixed(
                startHeight = 400,
                endHeight = 40,
                delay = 0,
                travel = 120,
                angle = 12,
                progress = 64,
            )

        val canMelee = targetCondition<WardensEncounter> { room, target -> room.canMelee(target) }
        val specialRoll = roomCondition<WardensEncounter> { room, _ -> room.rollSpecial() }
        val entombPhase = roomCondition<WardensEncounter> { room, _ -> room.entombPhase }
    }
}
