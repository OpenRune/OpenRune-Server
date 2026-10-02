package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Accuracy
import org.rsmod.api.bosses.dsl.CurrentTarget
import org.rsmod.api.bosses.dsl.Magic
import org.rsmod.api.bosses.dsl.Melee
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.Ranged
import org.rsmod.api.bosses.dsl.after
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.bleed
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.mapSpotanim
import org.rsmod.api.bosses.dsl.message
import org.rsmod.api.bosses.dsl.onEach
import org.rsmod.api.bosses.dsl.parallel
import org.rsmod.api.bosses.dsl.projectile
import org.rsmod.api.bosses.dsl.repeat
import org.rsmod.api.bosses.dsl.run
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.soundTo
import org.rsmod.api.bosses.dsl.spawnTile
import org.rsmod.api.bosses.dsl.spotanim
import org.rsmod.api.bosses.dsl.transitionTo
import org.rsmod.api.bosses.dsl.transmog
import org.rsmod.api.bosses.dsl.wait
import org.rsmod.api.bosses.dsl.whenever
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossExtensionContext
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.content.raids.toa.raid.encounter.challengePlayers
import org.rsmod.content.raids.toa.raid.encounter.eachTarget
import org.rsmod.content.raids.toa.raid.encounter.onRoomExternal
import org.rsmod.content.raids.toa.raid.encounter.roomCombatTick
import org.rsmod.content.raids.toa.raid.encounter.roomCondition
import org.rsmod.content.raids.toa.raid.encounter.roomSummon
import org.rsmod.content.raids.toa.raid.encounter.scaled
import org.rsmod.content.raids.toa.raid.encounter.targetCondition
import org.rsmod.content.raids.toa.raid.encounter.timeline
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class ZebakBoss @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onCombatTick =
                roomCombatTick<ZebakEncounter> { room, access, target ->
                    room.combatTick(access, target)
                },
        )
        registerHandlers()
    }

    override val spec: BossSpec =
        boss(ZebakNpcs.ZEBAK, ZebakNpcs.ZEBAK_ENRAGED) {
            ability(AUTO, auto(MELEE))
            ability(AUTO_ENRAGED, auto(MELEE_ENRAGED))
            ability(MELEE, melee(ZebakSeqs.MELEE, ZebakSeqs.TAIL_MELEE))
            ability(MELEE_ENRAGED, melee(ZebakSeqs.MELEE_ENRAGED, ZebakSeqs.TAIL_MELEE_ENRAGED))
            ability(MAGIC, split(mage = true))
            ability(RANGED, split(mage = false))
            ability(SPECIAL, sequence(wait(1), whenever(fighting, run(SPECIAL_NOW))))
            ability(SPECIAL_NOW, whenever(nextIsRoar, run(GREAT_ROAR), run(TIDAL_WAVES)))
            ability(GREAT_ROAR, greatRoar())
            ability(ROAR_SCREAM, roarScream())
            ability(TIDAL_WAVES, tidalWaves())
            ability(BLOOD_CAST, bloodCast())
            ability(ENRAGE, enrage())

            phase(MAIN_PHASE) {
                forceWhen(enrageDue, ENRAGE, once = true)
                forceWhen(specialReady, SPECIAL)
                rotationSelector { +then(AUTO) }
            }

            phase(ENRAGED_PHASE, keepFacingLock = true) {
                rotationSelector { +then(AUTO_ENRAGED) }
            }
        }

    private fun auto(melee: String): Effect =
        whenever(
            fighting,
            sequence(
                external(ROLL_STYLE),
                whenever(styleMelee, run(melee), whenever(styleMagic, run(MAGIC), run(RANGED))),
            ),
        )

    private fun melee(seq: String, tailSeq: String): Effect =
        sequence(
            anim(seq),
            external(TAIL, tailSeq),
            eachMeleeTarget(
                bleed(
                    duration = BLEED_TICKS,
                    movingDamage = scaled(BLEED_MOVING_MIN, BLEED_MOVING_MAX),
                    applyDamage = scaled(BLEED_APPLY_MIN, BLEED_APPLY_MAX),
                    chance = BLEED_CHANCE,
                    outOf = BLEED_OUT_OF,
                    onApply = message(BLEED_MESSAGE),
                    onMovingHit = external(BLEED_SPLAT),
                    otherwise =
                        hit {
                            damage(Accuracy(scaled(MELEE_MAX_HIT), meleeAttackType = MeleeAttackType.Slash))
                            type(Melee)
                            delay = MELEE_HIT_DELAY
                            penetration(MELEE_PENETRATION)
                            resolveOnImpact()
                            reactOnLanding()
                        },
                ),
            ),
        )

    private fun split(mage: Boolean): Effect {
        val shoot = if (mage) ZebakSynths.MAGE_SHOOT else ZebakSynths.RANGE_SHOOT
        val splitSound = if (mage) ZebakSynths.MAGE_SPLIT else ZebakSynths.RANGE_SPLIT
        val initial = if (mage) ZebakSpots.MAGE_INITIAL else ZebakSpots.RANGE_INITIAL
        val burst = if (mage) ZebakSpots.MAGE_SPLIT else ZebakSpots.RANGE_SPLIT
        return sequence(
            anim(ZebakSeqs.RANGED),
            external(TAIL, ZebakSeqs.TAIL_RANGED),
            eachTarget(sequence(soundTo(shoot), soundTo(splitSound, delay = SPLIT_SOUND_DELAY))),
            projectile(
                spotanim = initial,
                travel = ZebakProjs.INITIAL,
                target = offset(ZebakCoords.PROJECTILE_BASE),
                from = offset(ZebakCoords.PROJECTILE_START),
            ),
            after(
                SPLIT_DELAY,
                whenever(
                    fighting,
                    sequence(
                        roomSummon(
                            ZebakNpcs.SPLIT_HELPER,
                            radius = 0,
                            centeredOn = offset(ZebakCoords.SPLIT_HELPER),
                            duration = SPLIT_HELPER_TICKS,
                            onSummon = SPLIT_BURST,
                            onSummonParams = burst,
                        ),
                        eachTarget(fragment(mage)),
                    ),
                ),
            ),
        )
    }

    private fun fragment(mage: Boolean): Effect {
        val fragment = if (mage) ZebakSpots.MAGE_FRAGMENT else ZebakSpots.RANGE_FRAGMENT
        val impact = if (mage) ZebakSpots.MAGE_IMPACT else ZebakSpots.RANGE_IMPACT
        return sequence(
            soundTo(ZebakSynths.PROJECTILE_IMPACT, delay = IMPACT_DELAY),
            projectile(
                spotanim = fragment,
                travel = ZebakProjs.SPLIT,
                from = offset(ZebakCoords.PROJECTILE_BASE),
            ),
            spotanim(impact, height = IMPACT_HEIGHT, delay = IMPACT_DELAY, target = CurrentTarget),
            hit {
                damage(Accuracy(scaled(RANGED_MAGIC_MAX_HIT)))
                type(if (mage) Magic else Ranged)
                delay = SPLIT_HIT_DELAY
                resolveOnImpact()
                reactOnLanding()
            },
            whenever(isSwimming, external(SWIM_RETALIATE)),
        )
    }

    private fun endSpecial(): Effect =
        sequence(external(END_SPECIAL), whenever(specialReady, run(SPECIAL)))

    private fun greatRoar(): Effect =
        sequence(
            external(BEGIN_SPECIAL, true),
            anim(ZebakSeqs.RANGED),
            external(TAIL),
            parallel(
                sequence(wait(ROAR_NEXT_AUTO)),
                after(
                    1,
                    whenever(
                        bossAlive,
                        sequence(
                            external(ROAR_LAUNCH),
                            whenever(
                                roarLaunched,
                                after(ROAR_SCREAM_DELAY, whenever(bossAlive, run(ROAR_SCREAM))),
                                endSpecial(),
                            ),
                        ),
                    ),
                ),
            ),
        )

    private fun roarScream(): Effect {
        val sounds = ZebakSynths.SCREAM.map { (synth, delay) -> soundTo(synth, delay = delay) }
        return sequence(
            anim(ZebakSeqs.ROAR),
            external(TAIL, ZebakSeqs.TAIL_ROAR),
            eachTarget(sequence(*sounds.toTypedArray())),
            parallel(
                sequence(wait(SCREAM_NEXT_AUTO)),
                timeline(
                    ROAR_FIRST_WAVE to whenever(bossAlive, external(ROAR_WAVE, true)),
                    ROAR_WAVE_GAP to whenever(bossAlive, external(ROAR_WAVE, false)),
                    ROAR_WAVE_GAP to whenever(bossAlive, external(ROAR_WAVE, false)),
                    ROAR_END_DELAY to whenever(bossAlive, sequence(external(ROAR_END), endSpecial())),
                ),
            ),
        )
    }

    private fun tidalWaves(): Effect =
        sequence(
            external(BEGIN_SPECIAL, false),
            anim(ZebakSeqs.RANGED),
            external(TAIL),
            parallel(
                sequence(wait(WAVES_NEXT_AUTO)),
                timeline(
                    1 to whenever(bossAlive, external(WAVES_LAUNCH)),
                    WAVES_CALL_DELAY to whenever(bossAlive, callWaves()),
                    WAVES_ROCKS_DELAY to whenever(bossAlive, whenever(hasTargets, wavesRocks(), endSpecial())),
                ),
            ),
        )

    private fun callWaves(): Effect =
        sequence(anim(ZebakSeqs.CALL_WAVES), external(TAIL, ZebakSeqs.TAIL_CALL_WAVES))

    private fun wavesRocks(): Effect {
        val sounds = ZebakSynths.WAVES_LAND.map { (synth, delay) -> soundTo(synth, delay = delay) }
        return sequence(
            external(WAVES_SHAKE),
            eachTarget(sequence(*sounds.toTypedArray())),
            whenever(
                wavesFromSouth,
                rockFall(ZebakCoords.WAVE_SOUTH),
                rockFall(ZebakCoords.WAVE_NORTH),
            ),
            after(WAVES_CAMERA_DELAY, whenever(bossAlive, whenever(hasTargets, wavesRows(), endSpecial()))),
        )
    }

    private fun rockFall(base: CoordGrid): Effect {
        val spots =
            (0 until ROCK_SPOTS).flatMap { i ->
                val tile = offset(base.translate(i * ROCK_SPACING, 0))
                listOf(
                    mapSpotanim(ZebakSpots.WATER_SPLASH, tile, delay = SPLASH_DELAY),
                    mapSpotanim(ZebakSpots.ROCK_FALL, tile),
                )
            }
        return parallel(*spots.toTypedArray())
    }

    private fun wavesRows(): Effect =
        sequence(
            external(WAVES_CAMERA),
            after(
                WAVES_ROW_DELAY,
                sequence(
                    repeat(WAVE_ROWS, gap = WAVE_ROW_GAP, effect = whenever(bossAlive, external(WAVE_ROW))),
                    parallel(
                        sequence(wait(LAST_ROW_HOLD)),
                        after(WAVES_END_DELAY, whenever(bossAlive, sequence(external(WAVES_END), endSpecial()))),
                    ),
                ),
            ),
        )

    private fun bloodCast(): Effect =
        sequence(
            mapSpotanim(ZebakSpots.BLOOD_BARRAGE, offset(ZebakCoords.BLOOD_SPELL[0])),
            mapSpotanim(ZebakSpots.BLOOD_BARRAGE, offset(ZebakCoords.BLOOD_SPELL[1])),
            after(
                BLOOD_CAST_DELAY,
                whenever(
                    fighting,
                    sequence(
                        whenever(nextIsBarrage, external(BLOOD_BARRAGE), clouds()),
                        external(FLIP_BLOOD_SPELL),
                    ),
                ),
            ),
        )

    private fun clouds(): Effect {
        val north = ZebakCoords.BLOOD_CLOUDS[0]
        val south = ZebakCoords.BLOOD_CLOUDS[1]
        return sequence(
            whenever(
                bloodThinners,
                whenever(cloudsFromSouth, smallClouds(south, 1), smallClouds(north, -1)),
                whenever(
                    cloudsFromSouth,
                    cloud(ZebakNpcs.BLOOD_CLOUD, south),
                    cloud(ZebakNpcs.BLOOD_CLOUD, north),
                ),
            ),
            external(FLIP_CLOUD_SIDE),
        )
    }

    private fun smallClouds(base: CoordGrid, dz: Int): Effect {
        val spawns =
            (0 until SMALL_CLOUDS).map { i ->
                cloud(ZebakNpcs.BLOOD_CLOUD_SMALL, base.translate(i, dz * (i / 2)))
            }
        return parallel(*spawns.toTypedArray())
    }

    private fun cloud(type: String, tile: CoordGrid): Effect =
        roomSummon(
            type,
            radius = 0,
            centeredOn = offset(tile),
            duration = Int.MAX_VALUE,
            onSummon = TRACK_CLOUD,
        )

    private fun enrage(): Effect =
        sequence(
            transitionTo(ENRAGED_PHASE),
            transmog(ZebakNpcs.ZEBAK_ENRAGED, Int.MAX_VALUE),
            external(ENRAGE_ZEBAK),
            eachTarget(soundTo(ZebakSynths.FINAL_PHASE)),
            run(AUTO_ENRAGED),
        )

    private fun registerHandlers() {
        onZebak(ROLL_STYLE) { room, ext -> room.autos.rollStyle(ext.npc) }
        onZebak(TAIL) { room, ext -> room.tailAnim(ext.params as String?) }
        onZebak(BEGIN_SPECIAL) { room, ext -> room.beginSpecial(ext.params as Boolean) }
        onZebak(END_SPECIAL) { room, _ -> room.endSpecial() }
        onZebak(ROAR_LAUNCH) { room, _ -> room.greatRoar?.launch() }
        onZebak(ROAR_WAVE) { room, ext -> room.greatRoar?.roarWave(ext.params as Boolean) }
        onZebak(ROAR_END) { room, _ -> room.greatRoar?.end() }
        onZebak(WAVES_LAUNCH) { room, _ -> room.tidalWaves?.launch() }
        onZebak(WAVES_SHAKE) { room, _ -> room.tidalWaves?.shakeCameras() }
        onZebak(WAVES_CAMERA) { room, _ -> room.tidalWaves?.resetCameras() }
        onZebak(WAVE_ROW) { room, _ -> room.tidalWaves?.spawnRow() }
        onZebak(WAVES_END) { room, _ -> room.tidalWaves?.end() }
        onZebak(BLOOD_BARRAGE) { room, _ -> room.bloodMagic.barrage() }
        onZebak(FLIP_BLOOD_SPELL) { room, _ -> room.bloodMagic.flipSpell() }
        onZebak(FLIP_CLOUD_SIDE) { room, _ -> room.bloodMagic.flipCloudSide() }
        onZebak(ENRAGE_ZEBAK) { room, ext -> room.enrage(ext.npc) }
        onZebak(SPLIT_BURST) { _, ext ->
            ext.npc.spotanim(ext.params as String, height = SPLIT_HEIGHT)
        }
        onZebak(TRACK_CLOUD) { room, ext -> room.bloodMagic.trackCloud(ext.npc) }
        onZebak(BLEED_SPLAT) { room, ext -> room.autos.splat(ext.target.coords) }
        deps.extensionRegistry.register(SWIM_RETALIATE) { _, npc, target, _ ->
            target.queueCombatRetaliate(npc, SWIM_REACT_DELAY)
        }
    }

    private fun onZebak(name: String, handler: (ZebakEncounter, BossExtensionContext) -> Unit) {
        deps.onRoomExternal<ZebakEncounter>(name, handler)
    }

    internal companion object {
        const val BLOOD_CAST = "blood_cast"

        private const val MAIN_PHASE = "main"
        private const val ENRAGED_PHASE = "enraged"
        private const val AUTO = "auto"
        private const val AUTO_ENRAGED = "auto_enraged"
        private const val MELEE = "melee"
        private const val MELEE_ENRAGED = "melee_enraged"
        private const val MAGIC = "magic"
        private const val RANGED = "ranged"
        private const val SPECIAL = "special"
        private const val SPECIAL_NOW = "special_now"
        private const val GREAT_ROAR = "great_roar"
        private const val ROAR_SCREAM = "great_roar_scream"
        private const val TIDAL_WAVES = "tidal_waves"
        private const val ENRAGE = "enrage"

        private const val ROLL_STYLE = "zebak.roll_style"
        private const val BLEED_SPLAT = "zebak.bleed_splat"
        private const val TAIL = "zebak.tail"
        private const val BEGIN_SPECIAL = "zebak.begin_special"
        private const val END_SPECIAL = "zebak.end_special"
        private const val ROAR_LAUNCH = "zebak.roar_launch"
        private const val ROAR_WAVE = "zebak.roar_wave"
        private const val ROAR_END = "zebak.roar_end"
        private const val WAVES_LAUNCH = "zebak.waves_launch"
        private const val WAVES_SHAKE = "zebak.waves_shake"
        private const val WAVES_CAMERA = "zebak.waves_camera"
        private const val WAVE_ROW = "zebak.wave_row"
        private const val WAVES_END = "zebak.waves_end"
        private const val BLOOD_BARRAGE = "zebak.blood_barrage"
        private const val FLIP_BLOOD_SPELL = "zebak.flip_blood_spell"
        private const val FLIP_CLOUD_SIDE = "zebak.flip_cloud_side"
        private const val ENRAGE_ZEBAK = "zebak.enrage"
        private const val SPLIT_BURST = "zebak.split_burst"
        private const val SWIM_RETALIATE = "zebak.swim_retaliate"
        private const val TRACK_CLOUD = "zebak.track_cloud"

        private const val MELEE_MAX_HIT = 38
        private const val RANGED_MAGIC_MAX_HIT = 16
        private const val MELEE_HIT_DELAY = 2
        private const val MELEE_PENETRATION = 50
        private const val BLEED_CHANCE = 1
        private const val BLEED_OUT_OF = 4
        private const val BLEED_TICKS = 10
        private const val BLEED_APPLY_MIN = 5
        private const val BLEED_APPLY_MAX = 10
        private const val BLEED_MOVING_MIN = 1
        private const val BLEED_MOVING_MAX = 8
        private const val BLEED_MESSAGE =
            "<col=ff3045>Zebak's fangs tear into your flesh, causing you to bleed.</col>"
        private const val SPLIT_DELAY = 4
        private const val SPLIT_HIT_DELAY = 5
        private const val SWIM_REACT_DELAY = SPLIT_HIT_DELAY - 1
        private const val SPLIT_HELPER_TICKS = 3
        private const val SPLIT_HEIGHT = 750
        private const val SPLIT_SOUND_DELAY = 120
        private const val IMPACT_DELAY = 90
        private const val IMPACT_HEIGHT = 90

        private const val ROAR_NEXT_AUTO = 10
        private const val ROAR_SCREAM_DELAY = 32
        private const val SCREAM_NEXT_AUTO = 11
        private const val ROAR_FIRST_WAVE = 3
        private const val ROAR_WAVE_GAP = 2
        private const val ROAR_END_DELAY = 18

        private const val WAVES_NEXT_AUTO = 15
        private const val WAVES_CALL_DELAY = 4
        private const val WAVES_ROCKS_DELAY = 2
        private const val WAVES_CAMERA_DELAY = 2
        private const val WAVES_ROW_DELAY = 5
        private const val WAVE_ROWS = 3
        private const val WAVE_ROW_GAP = 7
        private const val LAST_ROW_HOLD = 4
        private const val WAVES_END_DELAY = 17
        private const val ROCK_SPOTS = 7
        private const val ROCK_SPACING = 3
        private const val SPLASH_DELAY = 200

        private const val BLOOD_CAST_DELAY = 3
        private const val SMALL_CLOUDS = 3

        private fun offset(tile: CoordGrid): TargetExpr.Single =
            spawnTile(tile.x - ZebakCoords.ZEBAK.x, tile.z - ZebakCoords.ZEBAK.z)

        private val isMeleeTarget =
            targetCondition<ZebakEncounter> { room, target -> target in room.autos.meleeTargets }
        private val isSwimming =
            targetCondition<ZebakEncounter> { room, target -> room.water.isSwimming(target) }

        private fun eachMeleeTarget(effect: Effect): Effect =
            onEach(challengePlayers, whenever(isMeleeTarget, effect))

        private val bossAlive = roomCondition<ZebakEncounter> { room, npc -> room.bossAlive(npc) }
        private val fighting = roomCondition<ZebakEncounter> { room, npc -> room.fighting(npc) }
        private val hasTargets =
            roomCondition<ZebakEncounter> { room, _ -> room.targets().isNotEmpty() }
        private val enrageDue = roomCondition<ZebakEncounter> { room, npc -> room.enrageDue(npc) }
        private val specialReady =
            roomCondition<ZebakEncounter> { room, npc -> room.specialReady(npc) }
        private val nextIsRoar = roomCondition<ZebakEncounter> { room, _ -> room.nextSpecialIsRoar }
        private val roarLaunched =
            roomCondition<ZebakEncounter> { room, _ -> room.greatRoar?.launched == true }
        private val wavesFromSouth =
            roomCondition<ZebakEncounter> { room, _ -> room.tidalWaves?.fromSouth == true }
        private val styleMelee =
            roomCondition<ZebakEncounter> { room, _ -> room.autos.style == ZebakAutos.Style.MELEE }
        private val styleMagic =
            roomCondition<ZebakEncounter> { room, _ -> room.autos.style == ZebakAutos.Style.MAGIC }
        private val nextIsBarrage =
            roomCondition<ZebakEncounter> { room, _ -> room.bloodMagic.nextIsBarrage }
        private val cloudsFromSouth =
            roomCondition<ZebakEncounter> { room, _ -> room.bloodMagic.cloudsFromSouth }
        private val bloodThinners = roomCondition<ZebakEncounter> { room, _ ->
            room.raid.isActive(ZebakInvocations.BLOOD_THINNERS)
        }
    }
}
