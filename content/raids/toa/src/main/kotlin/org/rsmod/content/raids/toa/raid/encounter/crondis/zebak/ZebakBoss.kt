package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Accuracy
import org.rsmod.api.bosses.dsl.CurrentTarget
import org.rsmod.api.bosses.dsl.Magic
import org.rsmod.api.bosses.dsl.Melee
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.Ranged
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.mapSpotanim
import org.rsmod.api.bosses.dsl.onEach
import org.rsmod.api.bosses.dsl.parallel
import org.rsmod.api.bosses.dsl.projectile
import org.rsmod.api.bosses.dsl.repeat
import org.rsmod.api.bosses.dsl.run
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.dsl.sound
import org.rsmod.api.bosses.dsl.spawnTile
import org.rsmod.api.bosses.dsl.spotanim
import org.rsmod.api.bosses.dsl.summon
import org.rsmod.api.bosses.dsl.transitionTo
import org.rsmod.api.bosses.dsl.transmog
import org.rsmod.api.bosses.dsl.wait
import org.rsmod.api.bosses.dsl.whenever
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class ZebakBoss @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onCombatTick = { target -> ZebakEncounter.onCombatTick(this, target) },
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
            hit {
                target = meleeTargets
                damage(Accuracy(scaled(MELEE_MAX_HIT), meleeAttackType = MeleeAttackType.Slash))
                type(Melee)
                delay = MELEE_HIT_DELAY
                resolveOnImpact()
                reactOnLanding()
            },
            external(BLEED),
        )

    private fun split(mage: Boolean): Effect {
        val shoot = if (mage) ZebakSynths.MAGE_SHOOT else ZebakSynths.RANGE_SHOOT
        val splitSound = if (mage) ZebakSynths.MAGE_SPLIT else ZebakSynths.RANGE_SPLIT
        val initial = if (mage) ZebakSpots.MAGE_INITIAL else ZebakSpots.RANGE_INITIAL
        val burst = if (mage) ZebakSpots.MAGE_SPLIT else ZebakSpots.RANGE_SPLIT
        return sequence(
            anim(ZebakSeqs.RANGED),
            external(TAIL, ZebakSeqs.TAIL_RANGED),
            sound(shoot, target = roomTargets),
            sound(splitSound, delay = SPLIT_SOUND_DELAY, target = roomTargets),
            projectile(
                spotanim = initial,
                travel = ZebakProjs.INITIAL,
                target = offset(ZebakCoords.PROJECTILE_BASE),
                source = offset(ZebakCoords.PROJECTILE_START),
            ),
            wait(SPLIT_DELAY, suppressAttacks = false),
            whenever(
                fighting,
                sequence(
                    summon(
                        ZebakNpcs.SPLIT_HELPER,
                        radius = 0,
                        centeredOn = offset(ZebakCoords.SPLIT_HELPER),
                        duration = SPLIT_HELPER_TICKS,
                        onSummon = SPLIT_BURST,
                        onSummonParams = burst,
                    ),
                    onEach(roomTargets, fragment(mage)),
                ),
            ),
        )
    }

    private fun fragment(mage: Boolean): Effect {
        val fragment = if (mage) ZebakSpots.MAGE_FRAGMENT else ZebakSpots.RANGE_FRAGMENT
        val impact = if (mage) ZebakSpots.MAGE_IMPACT else ZebakSpots.RANGE_IMPACT
        return sequence(
            sound(ZebakSynths.PROJECTILE_IMPACT, delay = IMPACT_DELAY, target = CurrentTarget),
            projectile(
                spotanim = fragment,
                travel = ZebakProjs.SPLIT,
                source = offset(ZebakCoords.PROJECTILE_BASE),
            ),
            spotanim(impact, height = IMPACT_HEIGHT, delay = IMPACT_DELAY, target = CurrentTarget),
            hit {
                damage(Accuracy(scaled(RANGED_MAGIC_MAX_HIT)))
                type(if (mage) Magic else Ranged)
                delay = SPLIT_HIT_DELAY
                resolveOnImpact()
                reactOnLanding()
            },
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
                sequence(
                    wait(1, suppressAttacks = false),
                    whenever(
                        bossAlive,
                        sequence(
                            external(ROAR_LAUNCH),
                            whenever(
                                roarLaunched,
                                sequence(
                                    wait(ROAR_SCREAM_DELAY, suppressAttacks = false),
                                    whenever(bossAlive, run(ROAR_SCREAM)),
                                ),
                                endSpecial(),
                            ),
                        ),
                    ),
                ),
            ),
        )

    private fun roarScream(): Effect {
        val sounds = ZebakSynths.SCREAM.map { (synth, delay) ->
            sound(synth, delay = delay, target = roomTargets)
        }
        return sequence(
            anim(ZebakSeqs.ROAR),
            external(TAIL, ZebakSeqs.TAIL_ROAR),
            sequence(*sounds.toTypedArray()),
            parallel(
                sequence(wait(SCREAM_NEXT_AUTO)),
                sequence(
                    wait(ROAR_FIRST_WAVE, suppressAttacks = false),
                    whenever(bossAlive, external(ROAR_WAVE, true)),
                    wait(ROAR_WAVE_GAP, suppressAttacks = false),
                    whenever(bossAlive, external(ROAR_WAVE, false)),
                    wait(ROAR_WAVE_GAP, suppressAttacks = false),
                    whenever(bossAlive, external(ROAR_WAVE, false)),
                    wait(ROAR_END_DELAY, suppressAttacks = false),
                    whenever(bossAlive, sequence(external(ROAR_END), endSpecial())),
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
                sequence(
                    wait(1, suppressAttacks = false),
                    whenever(bossAlive, external(WAVES_LAUNCH)),
                    wait(WAVES_CALL_DELAY, suppressAttacks = false),
                    whenever(
                        bossAlive,
                        sequence(
                            anim(ZebakSeqs.CALL_WAVES),
                            external(TAIL, ZebakSeqs.TAIL_CALL_WAVES),
                        ),
                    ),
                    wait(WAVES_ROCKS_DELAY, suppressAttacks = false),
                    whenever(bossAlive, whenever(hasTargets, wavesRocks(), endSpecial())),
                ),
            ),
        )

    private fun wavesRocks(): Effect {
        val sounds = ZebakSynths.WAVES_LAND.map { (synth, delay) ->
            sound(synth, delay = delay, target = roomTargets)
        }
        return sequence(
            external(WAVES_SHAKE),
            sequence(*sounds.toTypedArray()),
            whenever(
                wavesFromSouth,
                rockFall(ZebakCoords.WAVE_SOUTH),
                rockFall(ZebakCoords.WAVE_NORTH),
            ),
            wait(WAVES_CAMERA_DELAY, suppressAttacks = false),
            whenever(bossAlive, whenever(hasTargets, wavesRows(), endSpecial())),
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
            wait(WAVES_ROW_DELAY, suppressAttacks = false),
            repeat(WAVE_ROWS, gap = WAVE_ROW_GAP, effect = whenever(bossAlive, external(WAVE_ROW))),
            parallel(
                sequence(wait(LAST_ROW_HOLD)),
                sequence(
                    wait(WAVES_END_DELAY, suppressAttacks = false),
                    whenever(bossAlive, sequence(external(WAVES_END), endSpecial())),
                ),
            ),
        )

    private fun bloodCast(): Effect =
        sequence(
            mapSpotanim(ZebakSpots.BLOOD_BARRAGE, offset(ZebakCoords.BLOOD_SPELL[0])),
            mapSpotanim(ZebakSpots.BLOOD_BARRAGE, offset(ZebakCoords.BLOOD_SPELL[1])),
            wait(BLOOD_CAST_DELAY, suppressAttacks = false),
            whenever(
                fighting,
                sequence(
                    whenever(nextIsBarrage, external(BLOOD_BARRAGE), clouds()),
                    external(FLIP_BLOOD_SPELL),
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
        summon(
            type,
            radius = 0,
            centeredOn = offset(tile),
            duration = Int.MAX_VALUE,
            onSummon = REGISTER_CLOUD,
        )

    private fun enrage(): Effect =
        sequence(
            transitionTo(ENRAGED_PHASE),
            transmog(ZebakNpcs.ZEBAK_ENRAGED, Int.MAX_VALUE),
            external(ENRAGE_ZEBAK),
            sound(ZebakSynths.FINAL_PHASE, target = roomTargets),
            run(AUTO_ENRAGED),
        )

    private fun registerHandlers() {
        onZebak(ROLL_STYLE) { room, npc, _ -> room.autos.rollStyle(npc) }
        onZebak(BLEED) { room, _, _ -> room.autos.queueBleedRolls() }
        onZebak(TAIL) { room, _, params -> room.tailAnim(params as String?) }
        onZebak(BEGIN_SPECIAL) { room, _, params -> room.beginSpecial(params as Boolean) }
        onZebak(END_SPECIAL) { room, _, _ -> room.endSpecial() }
        onZebak(ROAR_LAUNCH) { room, _, _ -> room.greatRoar?.launch() }
        onZebak(ROAR_WAVE) { room, _, params -> room.greatRoar?.roarWave(params as Boolean) }
        onZebak(ROAR_END) { room, _, _ -> room.greatRoar?.end() }
        onZebak(WAVES_LAUNCH) { room, _, _ -> room.tidalWaves?.launch() }
        onZebak(WAVES_SHAKE) { room, _, _ -> room.tidalWaves?.shakeCameras() }
        onZebak(WAVES_CAMERA) { room, _, _ -> room.tidalWaves?.resetCameras() }
        onZebak(WAVE_ROW) { room, _, _ -> room.tidalWaves?.spawnRow() }
        onZebak(WAVES_END) { room, _, _ -> room.tidalWaves?.end() }
        onZebak(BLOOD_BARRAGE) { room, _, _ -> room.bloodMagic.barrage() }
        onZebak(FLIP_BLOOD_SPELL) { room, _, _ -> room.bloodMagic.flipSpell() }
        onZebak(FLIP_CLOUD_SIDE) { room, _, _ -> room.bloodMagic.flipCloudSide() }
        onZebak(ENRAGE_ZEBAK) { room, npc, _ -> room.enrage(npc) }
        onZebak(SPLIT_BURST) { _, helper, params ->
            helper.spotanim(params as String, height = SPLIT_HEIGHT)
        }
        onZebak(REGISTER_CLOUD) { room, cloud, _ -> room.bloodMagic.registerCloud(cloud) }
    }

    private fun onZebak(name: String, block: (ZebakEncounter, Npc, Any?) -> Unit) {
        deps.extensionRegistry.register(name) { access, npc, _, params ->
            val room = ZebakEncounter.roomOf(access.npc) ?: return@register
            block(room, npc, params)
        }
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
        private const val BLEED = "zebak.bleed"
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
        private const val REGISTER_CLOUD = "zebak.register_cloud"

        private const val MELEE_MAX_HIT = 38
        private const val RANGED_MAGIC_MAX_HIT = 16
        private const val MELEE_HIT_DELAY = 2
        private const val SPLIT_DELAY = 4
        private const val SPLIT_HIT_DELAY = 5
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

        private fun scaled(base: Int): DamageExpr =
            DamageExpr.Custom { npc, _ -> ZebakEncounter.roomOf(npc)?.rollScaled(base) ?: 0 }

        private fun room(test: (ZebakEncounter, Npc) -> Boolean): Condition =
            Condition.Custom { npc -> ZebakEncounter.roomOf(npc)?.let { test(it, npc) } == true }

        private fun players(select: (ZebakEncounter) -> List<Player>): TargetExpr =
            TargetExpr.Custom { npc, _ -> ZebakEncounter.roomOf(npc)?.let(select).orEmpty() }

        private val roomTargets = players { it.targets() }
        private val meleeTargets = players { it.autos.meleeTargets }

        private val bossAlive = room { room, npc -> room.bossAlive(npc) }
        private val fighting = room { room, npc -> room.fighting(npc) }
        private val hasTargets = room { room, _ -> room.targets().isNotEmpty() }
        private val enrageDue = room { room, npc -> room.enrageDue(npc) }
        private val specialReady = room { room, npc -> room.specialReady(npc) }
        private val nextIsRoar = room { room, _ -> room.nextSpecialIsRoar }
        private val roarLaunched = room { room, _ -> room.greatRoar?.launched == true }
        private val wavesFromSouth = room { room, _ -> room.tidalWaves?.fromSouth == true }
        private val styleMelee = room { room, _ -> room.autos.style == ZebakAutos.Style.MELEE }
        private val styleMagic = room { room, _ -> room.autos.style == ZebakAutos.Style.MAGIC }
        private val nextIsBarrage = room { room, _ -> room.bloodMagic.nextIsBarrage }
        private val cloudsFromSouth = room { room, _ -> room.bloodMagic.cloudsFromSouth }
        private val bloodThinners = room { room, _ ->
            room.raid.isActive(ZebakInvocations.BLOOD_THINNERS)
        }
    }
}
