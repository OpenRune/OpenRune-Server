package org.rsmod.content.bosses.zulrah

import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect

internal object ZulrahSpec {
    val tail = ZulrahTailAttack(impactDelay = 4, stunTicks = 5, damage = 20..30)
    private val MIXED_ATTACK_WEIGHTS = mapOf(
        "spotanim.snakeboss_fireball" to 23,
        "spotanim.snakeboss_orb" to 5,
    )

    val boss: BossSpec = rotating()

    fun rotating(enabledRotations: List<Int> = (1..4).toList()): BossSpec {
        require(enabledRotations.isNotEmpty() && enabledRotations.distinct() == enabledRotations)
        require(enabledRotations.all { it in 1..4 })
        return boss(*ZulrahEncounterController.BOSS_FORMS.toTypedArray()) {
            stats(attackRate = 1, aggressionRadius = 64, retaliateOnHit = false)
            val pause = ability("pause", Effect.NoOp)
            opening(pause)
            if (1 in enabledRotations) rotation1(pause)
            if (2 in enabledRotations) rotation2(pause)
            if (3 in enabledRotations) rotation3(pause)
            if (4 in enabledRotations) rotation4(pause)
            recurring(pause)

            val select = ability("select_rotation") {
                include(
                    choose(
                        weightedRandom(noRepeatBias = 0.0) {
                            for (rotation in enabledRotations) {
                                +random("rotation_$rotation", weight = 1)
                            }
                        },
                        enabledRotations.associate { rotation ->
                            "rotation_$rotation" to TransitionTo("rotation_$rotation")
                        },
                    ),
                )
            }
            phase("select_rotation", lockMovement = true) {
                rotationSelector { +then(select) }
            }
        }
    }

    // Each selector entry takes one BossCombat tick. No NPC delay or separate timeline runner.
    private fun BossSpecBuilder.opening(pause: AbilityRef) {
        val t0 = ability("opening_t0") {
            include(
                external(
                    "zulrah.emerge",
                    ZulrahRoutineEvent(
                        tick = 0,
                        kind = "emerge",
                        symbol = "npc.snakeboss_boss_ranged",
                        initial = true,
                    ),
                ),
            )
            anim("seq.snakeboss_spawn")
        }
        val t13 = ability("opening_t13") {
            anim("seq.snakeboss_attack_acidx1")
            include(
                external(
                    "zulrah.hazard",
                    ZulrahRoutineEvent(
                        tick = 0,
                        kind = "gas",
                        symbol = "spotanim.snakeboss_double_orb",
                        source = ZulrahPoint(2, 1),
                        target = ZulrahPoint(2, -5),
                        starttime = 40,
                        endtime = 90,
                        startheight = 370,
                        endheight = 20,
                        impactDelay = 3,
                        rotation = 1,
                        cloudLifetime = 30,
                    ),
                ),
            )
            include(
                external(
                    "zulrah.hazard",
                    ZulrahRoutineEvent(
                        tick = 0,
                        kind = "gas",
                        symbol = "spotanim.snakeboss_double_orb",
                        source = ZulrahPoint(2, 1),
                        target = ZulrahPoint(5, -5),
                        starttime = 45,
                        endtime = 120,
                        angle = 10,
                        startheight = 370,
                        endheight = 20,
                        impactDelay = 4,
                        rotation = 3,
                        cloudLifetime = 30,
                    ),
                ),
            )
        }
        val t16 = ability("opening_t16") {
            anim("seq.snakeboss_attack_acidx1")
            include(
                external(
                    "zulrah.hazard",
                    ZulrahRoutineEvent(
                        tick = 0,
                        kind = "gas",
                        symbol = "spotanim.snakeboss_double_orb",
                        source = ZulrahPoint(1, 1),
                        target = ZulrahPoint(-4, -4),
                        starttime = 40,
                        endtime = 90,
                        startheight = 370,
                        endheight = 20,
                        impactDelay = 3,
                        rotation = 3,
                        cloudLifetime = 30,
                    ),
                ),
            )
            include(
                external(
                    "zulrah.hazard",
                    ZulrahRoutineEvent(
                        tick = 0,
                        kind = "gas",
                        symbol = "spotanim.snakeboss_double_orb",
                        source = ZulrahPoint(1, 1),
                        target = ZulrahPoint(-1, -5),
                        starttime = 45,
                        endtime = 120,
                        angle = 10,
                        startheight = 370,
                        endheight = 20,
                        impactDelay = 4,
                        rotation = 0,
                        cloudLifetime = 30,
                    ),
                ),
            )
        }
        val t19 = ability("opening_t19") {
            anim("seq.snakeboss_attack_acidx1")
            include(
                external(
                    "zulrah.hazard",
                    ZulrahRoutineEvent(
                        tick = 0,
                        kind = "gas",
                        symbol = "spotanim.snakeboss_double_orb",
                        source = ZulrahPoint(3, 1),
                        target = ZulrahPoint(6, -2),
                        starttime = 40,
                        endtime = 90,
                        startheight = 370,
                        endheight = 20,
                        impactDelay = 3,
                        rotation = 2,
                        cloudLifetime = 30,
                    ),
                ),
            )
            include(
                external(
                    "zulrah.hazard",
                    ZulrahRoutineEvent(
                        tick = 0,
                        kind = "gas",
                        symbol = "spotanim.snakeboss_double_orb",
                        source = ZulrahPoint(3, 1),
                        target = ZulrahPoint(6, 1),
                        starttime = 45,
                        endtime = 120,
                        angle = 10,
                        startheight = 370,
                        endheight = 20,
                        impactDelay = 4,
                        rotation = 1,
                        cloudLifetime = 30,
                    ),
                ),
            )
        }
        val t22 = ability("opening_t22") {
            anim("seq.snakeboss_attack_acidx1")
            include(
                external(
                    "zulrah.hazard",
                    ZulrahRoutineEvent(
                        tick = 0,
                        kind = "gas",
                        symbol = "spotanim.snakeboss_double_orb",
                        source = ZulrahPoint(1, 2),
                        target = ZulrahPoint(-4, 2),
                        starttime = 40,
                        endtime = 90,
                        startheight = 370,
                        endheight = 20,
                        impactDelay = 3,
                        rotation = 2,
                        cloudLifetime = 30,
                    ),
                ),
            )
            include(
                external(
                    "zulrah.hazard",
                    ZulrahRoutineEvent(
                        tick = 0,
                        kind = "gas",
                        symbol = "spotanim.snakeboss_double_orb",
                        source = ZulrahPoint(1, 2),
                        target = ZulrahPoint(-4, -1),
                        starttime = 45,
                        endtime = 120,
                        angle = 10,
                        startheight = 370,
                        endheight = 20,
                        impactDelay = 10,
                        rotation = 0,
                        cloudLifetime = 30,
                    ),
                ),
            )
        }
        val t26 = ability("opening_t26") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t29 = ability("opening_t29") {
            transitionTo("select_rotation")
        }
        phase("opening", lockMovement = true) {
            rotationSelector {
                +then(t0)
                kotlin.repeat(12) { +then(pause) }
                +then(t13)
                kotlin.repeat(2) { +then(pause) }
                +then(t16)
                kotlin.repeat(2) { +then(pause) }
                +then(t19)
                kotlin.repeat(2) { +then(pause) }
                +then(t22)
                kotlin.repeat(3) { +then(pause) }
                +then(t26)
                kotlin.repeat(2) { +then(pause) }
                +then(t29)
            }
        }
    }

    private fun BossSpecBuilder.rotation1(pause: AbilityRef) {
        val t0 = ability("rotation_1_t0") {
            // Rotation 1 - phase 1
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_melee", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t3 = ability("rotation_1_t3") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_right")
        }
        val t10 = ability("rotation_1_t10") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_left")
        }
        val t19 = ability("rotation_1_t19") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t22 = ability("rotation_1_t22") {
            // Rotation 1 - phase 2
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t25 = ability("rotation_1_t25") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(2, 1))))
        }
        val t28 = ability("rotation_1_t28") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, 3))))
        }
        val t31 = ability("rotation_1_t31") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, 3))))
        }
        val t34 = ability("rotation_1_t34") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, 3))))
        }
        val t37 = ability("rotation_1_t37") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t40 = ability("rotation_1_t40") {
            // Rotation 1 - phase 3
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", 0, -11)))
            anim("seq.snakeboss_emergefast")
        }
        val t43 = ability("rotation_1_t43") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(2, -8))))
        }
        val t46 = ability("rotation_1_t46") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(1, -8))))
        }
        val t49 = ability("rotation_1_t49") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(1, -8))))
        }
        val t52 = ability("rotation_1_t52") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(1, -8))))
        }
        val t55 = ability("rotation_1_t55") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(1, -8))))
        }
        val t58 = ability("rotation_1_t58") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, -8), ZulrahPoint(-3, 3), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t61 = ability("rotation_1_t61") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, -8), ZulrahPoint(-3, 0), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t64 = ability("rotation_1_t64") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, -8), ZulrahPoint(-4, -4), starttime = 40, endtime = 90, impactDelay = 3, rotation = 0)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, -8), ZulrahPoint(-1, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 1)))
        }
        val t67 = ability("rotation_1_t67") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(5, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 3)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(6, -2), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 3)))
        }
        val t71 = ability("rotation_1_t71") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(3, -8), ZulrahPoint(7, 2), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t74 = ability("rotation_1_t74") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(2, -8), ZulrahPoint(7, 5), "npc.snakeboss_minion_melee", impactDelay = 10)))
        }
        val t78 = ability("rotation_1_t78") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t81 = ability("rotation_1_t81") {
            // Rotation 1 - phase 4
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_melee", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t84 = ability("rotation_1_t84") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_right")
        }
        val t91 = ability("rotation_1_t91") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_left")
        }
        val t100 = ability("rotation_1_t100") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t103 = ability("rotation_1_t103") {
            // Rotation 1 - phase 5
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", -10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t106 = ability("rotation_1_t106") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t109 = ability("rotation_1_t109") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t112 = ability("rotation_1_t112") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t115 = ability("rotation_1_t115") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t118 = ability("rotation_1_t118") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t121 = ability("rotation_1_t121") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t124 = ability("rotation_1_t124") {
            // Rotation 1 - phase 6
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", 0, -11)))
            anim("seq.snakeboss_emergefast")
        }
        val t127 = ability("rotation_1_t127") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(6, 1), starttime = 40, endtime = 90, impactDelay = 3, rotation = 0)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(6, 4), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 1)))
        }
        val t130 = ability("rotation_1_t130") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(5, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 3)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(6, -2), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 2)))
        }
        val t133 = ability("rotation_1_t133") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, -8), ZulrahPoint(2, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 2)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, -8), ZulrahPoint(-1, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 10, rotation = 2)))
        }
        val t137 = ability("rotation_1_t137") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, -8), ZulrahPoint(-3, 3), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t140 = ability("rotation_1_t140") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, -8), ZulrahPoint(-3, 0), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t143 = ability("rotation_1_t143") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, -8), ZulrahPoint(-3, -3), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t146 = ability("rotation_1_t146") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(3, -8), ZulrahPoint(7, -1), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t150 = ability("rotation_1_t150") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t153 = ability("rotation_1_t153") {
            // Rotation 1 - phase 7
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", 0, -11)))
            anim("seq.snakeboss_emergefast")
        }
        val t156 = ability("rotation_1_t156") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(2, -8))))
        }
        val t159 = ability("rotation_1_t159") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, -8))))
        }
        val t162 = ability("rotation_1_t162") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, -8))))
        }
        val t165 = ability("rotation_1_t165") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, -8))))
        }
        val t168 = ability("rotation_1_t168") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, -8))))
        }
        val t171 = ability("rotation_1_t171") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, -8), ZulrahPoint(-3, 3), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t174 = ability("rotation_1_t174") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, -8), ZulrahPoint(-4, -4), starttime = 40, endtime = 90, impactDelay = 3, rotation = 0)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, -8), ZulrahPoint(-1, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 1)))
        }
        val t178 = ability("rotation_1_t178") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, -8), ZulrahPoint(-3, 0), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t181 = ability("rotation_1_t181") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(5, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 3)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(6, -2), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 3)))
        }
        val t185 = ability("rotation_1_t185") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(3, -8), ZulrahPoint(7, 2), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t189 = ability("rotation_1_t189") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t192 = ability("rotation_1_t192") {
            // Rotation 1 - phase 8
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", -10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t195 = ability("rotation_1_t195") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t198 = ability("rotation_1_t198") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t201 = ability("rotation_1_t201") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t204 = ability("rotation_1_t204") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t207 = ability("rotation_1_t207") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t210 = ability("rotation_1_t210") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t213 = ability("rotation_1_t213") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t216 = ability("rotation_1_t216") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t219 = ability("rotation_1_t219") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t222 = ability("rotation_1_t222") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t225 = ability("rotation_1_t225") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(6, -2), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(5, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 2)))
        }
        val t228 = ability("rotation_1_t228") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(6, 1), starttime = 40, endtime = 90, impactDelay = 3, rotation = 0)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(6, 4), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 1)))
        }
        val t231 = ability("rotation_1_t231") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(2, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(-1, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 3)))
        }
        val t234 = ability("rotation_1_t234") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(6, -2), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(5, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 2)))
        }
        val t238 = ability("rotation_1_t238") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t241 = ability("rotation_1_t241") {
            // Rotation 1 - phase 9
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_melee", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t244 = ability("rotation_1_t244") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_right")
        }
        val t251 = ability("rotation_1_t251") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_left")
        }
        val t260 = ability("rotation_1_t260") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t263 = ability("rotation_1_t263") {
            transitionTo("recurring")
        }
        phase("rotation_1", lockMovement = true) {
            rotationSelector {
                +then(t0)
                kotlin.repeat(2) { +then(pause) }
                +then(t3)
                kotlin.repeat(6) { +then(pause) }
                +then(t10)
                kotlin.repeat(8) { +then(pause) }
                +then(t19)
                kotlin.repeat(2) { +then(pause) }
                +then(t22)
                kotlin.repeat(2) { +then(pause) }
                +then(t25)
                kotlin.repeat(2) { +then(pause) }
                +then(t28)
                kotlin.repeat(2) { +then(pause) }
                +then(t31)
                kotlin.repeat(2) { +then(pause) }
                +then(t34)
                kotlin.repeat(2) { +then(pause) }
                +then(t37)
                kotlin.repeat(2) { +then(pause) }
                +then(t40)
                kotlin.repeat(2) { +then(pause) }
                +then(t43)
                kotlin.repeat(2) { +then(pause) }
                +then(t46)
                kotlin.repeat(2) { +then(pause) }
                +then(t49)
                kotlin.repeat(2) { +then(pause) }
                +then(t52)
                kotlin.repeat(2) { +then(pause) }
                +then(t55)
                kotlin.repeat(2) { +then(pause) }
                +then(t58)
                kotlin.repeat(2) { +then(pause) }
                +then(t61)
                kotlin.repeat(2) { +then(pause) }
                +then(t64)
                kotlin.repeat(2) { +then(pause) }
                +then(t67)
                kotlin.repeat(3) { +then(pause) }
                +then(t71)
                kotlin.repeat(2) { +then(pause) }
                +then(t74)
                kotlin.repeat(3) { +then(pause) }
                +then(t78)
                kotlin.repeat(2) { +then(pause) }
                +then(t81)
                kotlin.repeat(2) { +then(pause) }
                +then(t84)
                kotlin.repeat(6) { +then(pause) }
                +then(t91)
                kotlin.repeat(8) { +then(pause) }
                +then(t100)
                kotlin.repeat(2) { +then(pause) }
                +then(t103)
                kotlin.repeat(2) { +then(pause) }
                +then(t106)
                kotlin.repeat(2) { +then(pause) }
                +then(t109)
                kotlin.repeat(2) { +then(pause) }
                +then(t112)
                kotlin.repeat(2) { +then(pause) }
                +then(t115)
                kotlin.repeat(2) { +then(pause) }
                +then(t118)
                kotlin.repeat(2) { +then(pause) }
                +then(t121)
                kotlin.repeat(2) { +then(pause) }
                +then(t124)
                kotlin.repeat(2) { +then(pause) }
                +then(t127)
                kotlin.repeat(2) { +then(pause) }
                +then(t130)
                kotlin.repeat(2) { +then(pause) }
                +then(t133)
                kotlin.repeat(3) { +then(pause) }
                +then(t137)
                kotlin.repeat(2) { +then(pause) }
                +then(t140)
                kotlin.repeat(2) { +then(pause) }
                +then(t143)
                kotlin.repeat(2) { +then(pause) }
                +then(t146)
                kotlin.repeat(3) { +then(pause) }
                +then(t150)
                kotlin.repeat(2) { +then(pause) }
                +then(t153)
                kotlin.repeat(2) { +then(pause) }
                +then(t156)
                kotlin.repeat(2) { +then(pause) }
                +then(t159)
                kotlin.repeat(2) { +then(pause) }
                +then(t162)
                kotlin.repeat(2) { +then(pause) }
                +then(t165)
                kotlin.repeat(2) { +then(pause) }
                +then(t168)
                kotlin.repeat(2) { +then(pause) }
                +then(t171)
                kotlin.repeat(2) { +then(pause) }
                +then(t174)
                kotlin.repeat(3) { +then(pause) }
                +then(t178)
                kotlin.repeat(2) { +then(pause) }
                +then(t181)
                kotlin.repeat(3) { +then(pause) }
                +then(t185)
                kotlin.repeat(3) { +then(pause) }
                +then(t189)
                kotlin.repeat(2) { +then(pause) }
                +then(t192)
                kotlin.repeat(2) { +then(pause) }
                +then(t195)
                kotlin.repeat(2) { +then(pause) }
                +then(t198)
                kotlin.repeat(2) { +then(pause) }
                +then(t201)
                kotlin.repeat(2) { +then(pause) }
                +then(t204)
                kotlin.repeat(2) { +then(pause) }
                +then(t207)
                kotlin.repeat(2) { +then(pause) }
                +then(t210)
                kotlin.repeat(2) { +then(pause) }
                +then(t213)
                kotlin.repeat(2) { +then(pause) }
                +then(t216)
                kotlin.repeat(2) { +then(pause) }
                +then(t219)
                kotlin.repeat(2) { +then(pause) }
                +then(t222)
                kotlin.repeat(2) { +then(pause) }
                +then(t225)
                kotlin.repeat(2) { +then(pause) }
                +then(t228)
                kotlin.repeat(2) { +then(pause) }
                +then(t231)
                kotlin.repeat(2) { +then(pause) }
                +then(t234)
                kotlin.repeat(3) { +then(pause) }
                +then(t238)
                kotlin.repeat(2) { +then(pause) }
                +then(t241)
                kotlin.repeat(2) { +then(pause) }
                +then(t244)
                kotlin.repeat(6) { +then(pause) }
                +then(t251)
                kotlin.repeat(8) { +then(pause) }
                +then(t260)
                kotlin.repeat(2) { +then(pause) }
                +then(t263)
            }
        }
    }

    private fun BossSpecBuilder.rotation2(pause: AbilityRef) {
        val t0 = ability("rotation_2_t0") {
            // Rotation 2 - phase 1
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_melee", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t3 = ability("rotation_2_t3") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_right")
        }
        val t10 = ability("rotation_2_t10") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_left")
        }
        val t19 = ability("rotation_2_t19") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t22 = ability("rotation_2_t22") {
            // Rotation 2 - phase 2
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t25 = ability("rotation_2_t25") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(2, 1))))
        }
        val t28 = ability("rotation_2_t28") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, 3))))
        }
        val t31 = ability("rotation_2_t31") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, 3))))
        }
        val t34 = ability("rotation_2_t34") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, 3))))
        }
        val t37 = ability("rotation_2_t37") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t40 = ability("rotation_2_t40") {
            // Rotation 2 - phase 3
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", -10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t43 = ability("rotation_2_t43") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(6, -2), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(5, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 2)))
        }
        val t46 = ability("rotation_2_t46") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(6, 1), starttime = 40, endtime = 90, impactDelay = 3, rotation = 0)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(6, 4), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 1)))
        }
        val t49 = ability("rotation_2_t49") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(2, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(-1, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 3)))
        }
        val t53 = ability("rotation_2_t53") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(-7, -1), ZulrahPoint(0, -4), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t56 = ability("rotation_2_t56") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(-7, -1), ZulrahPoint(-3, -3), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t59 = ability("rotation_2_t59") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(-7, 0), ZulrahPoint(-3, 0), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t62 = ability("rotation_2_t62") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(-7, 1), ZulrahPoint(-3, 3), "npc.snakeboss_minion_melee", impactDelay = 10)))
        }
        val t66 = ability("rotation_2_t66") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t69 = ability("rotation_2_t69") {
            // Rotation 2 - phase 4
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", 0, -11)))
            anim("seq.snakeboss_emergefast")
        }
        val t72 = ability("rotation_2_t72") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_orb", ZulrahPoint(2, -8))))
        }
        val t75 = ability("rotation_2_t75") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_orb", ZulrahPoint(1, -8))))
        }
        val t78 = ability("rotation_2_t78") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, -8))))
        }
        val t81 = ability("rotation_2_t81") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, -8))))
        }
        val t84 = ability("rotation_2_t84") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, -8))))
        }
        val t87 = ability("rotation_2_t87") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, -8), ZulrahPoint(-3, 3), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t90 = ability("rotation_2_t90") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, -8), ZulrahPoint(-3, 0), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t93 = ability("rotation_2_t93") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, -8), ZulrahPoint(-4, -4), starttime = 40, endtime = 90, impactDelay = 3, rotation = 0)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, -8), ZulrahPoint(-1, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 1)))
        }
        val t96 = ability("rotation_2_t96") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(5, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 3)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(6, -2), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 3)))
        }
        val t99 = ability("rotation_2_t99") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(3, -8), ZulrahPoint(7, 2), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t102 = ability("rotation_2_t102") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(2, -8), ZulrahPoint(7, 5), "npc.snakeboss_minion_melee", impactDelay = 10)))
        }
        val t106 = ability("rotation_2_t106") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t109 = ability("rotation_2_t109") {
            // Rotation 2 - phase 5
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_melee", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t112 = ability("rotation_2_t112") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_left")
        }
        val t120 = ability("rotation_2_t120") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_right")
        }
        val t128 = ability("rotation_2_t128") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t131 = ability("rotation_2_t131") {
            // Rotation 2 - phase 6
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", 10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t134 = ability("rotation_2_t134") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t137 = ability("rotation_2_t137") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 1))))
        }
        val t140 = ability("rotation_2_t140") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t143 = ability("rotation_2_t143") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t146 = ability("rotation_2_t146") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t149 = ability("rotation_2_t149") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t152 = ability("rotation_2_t152") {
            // Rotation 2 - phase 7
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", 0, -11)))
            anim("seq.snakeboss_emergefast")
        }
        val t155 = ability("rotation_2_t155") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(2, -8))))
        }
        val t158 = ability("rotation_2_t158") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, -8))))
        }
        val t161 = ability("rotation_2_t161") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, -8))))
        }
        val t164 = ability("rotation_2_t164") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, -8))))
        }
        val t167 = ability("rotation_2_t167") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, -8))))
        }
        val t170 = ability("rotation_2_t170") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, -8), ZulrahPoint(-3, 3), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t173 = ability("rotation_2_t173") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, -8), ZulrahPoint(-4, -4), starttime = 40, endtime = 90, impactDelay = 3, rotation = 0)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, -8), ZulrahPoint(-1, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 1)))
        }
        val t177 = ability("rotation_2_t177") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, -8), ZulrahPoint(-3, 0), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t180 = ability("rotation_2_t180") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(5, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 3)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(6, -2), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 3)))
        }
        val t184 = ability("rotation_2_t184") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(3, -8), ZulrahPoint(7, 2), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t188 = ability("rotation_2_t188") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t191 = ability("rotation_2_t191") {
            // Rotation 2 - phase 8
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", -10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t194 = ability("rotation_2_t194") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t197 = ability("rotation_2_t197") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t200 = ability("rotation_2_t200") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t203 = ability("rotation_2_t203") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t206 = ability("rotation_2_t206") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t209 = ability("rotation_2_t209") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t212 = ability("rotation_2_t212") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t215 = ability("rotation_2_t215") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t218 = ability("rotation_2_t218") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t221 = ability("rotation_2_t221") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t224 = ability("rotation_2_t224") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(6, -2), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(5, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 2)))
        }
        val t227 = ability("rotation_2_t227") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(6, 1), starttime = 40, endtime = 90, impactDelay = 3, rotation = 0)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(6, 4), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 1)))
        }
        val t230 = ability("rotation_2_t230") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(2, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(-1, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 3)))
        }
        val t233 = ability("rotation_2_t233") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(6, -2), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(-7, 0), ZulrahPoint(5, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 2)))
        }
        val t237 = ability("rotation_2_t237") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t240 = ability("rotation_2_t240") {
            // Rotation 2 - phase 9
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_melee", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t243 = ability("rotation_2_t243") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_right")
        }
        val t250 = ability("rotation_2_t250") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_left")
        }
        val t259 = ability("rotation_2_t259") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t262 = ability("rotation_2_t262") {
            transitionTo("recurring")
        }
        phase("rotation_2", lockMovement = true) {
            rotationSelector {
                +then(t0)
                kotlin.repeat(2) { +then(pause) }
                +then(t3)
                kotlin.repeat(6) { +then(pause) }
                +then(t10)
                kotlin.repeat(8) { +then(pause) }
                +then(t19)
                kotlin.repeat(2) { +then(pause) }
                +then(t22)
                kotlin.repeat(2) { +then(pause) }
                +then(t25)
                kotlin.repeat(2) { +then(pause) }
                +then(t28)
                kotlin.repeat(2) { +then(pause) }
                +then(t31)
                kotlin.repeat(2) { +then(pause) }
                +then(t34)
                kotlin.repeat(2) { +then(pause) }
                +then(t37)
                kotlin.repeat(2) { +then(pause) }
                +then(t40)
                kotlin.repeat(2) { +then(pause) }
                +then(t43)
                kotlin.repeat(2) { +then(pause) }
                +then(t46)
                kotlin.repeat(2) { +then(pause) }
                +then(t49)
                kotlin.repeat(3) { +then(pause) }
                +then(t53)
                kotlin.repeat(2) { +then(pause) }
                +then(t56)
                kotlin.repeat(2) { +then(pause) }
                +then(t59)
                kotlin.repeat(2) { +then(pause) }
                +then(t62)
                kotlin.repeat(3) { +then(pause) }
                +then(t66)
                kotlin.repeat(2) { +then(pause) }
                +then(t69)
                kotlin.repeat(2) { +then(pause) }
                +then(t72)
                kotlin.repeat(2) { +then(pause) }
                +then(t75)
                kotlin.repeat(2) { +then(pause) }
                +then(t78)
                kotlin.repeat(2) { +then(pause) }
                +then(t81)
                kotlin.repeat(2) { +then(pause) }
                +then(t84)
                kotlin.repeat(2) { +then(pause) }
                +then(t87)
                kotlin.repeat(2) { +then(pause) }
                +then(t90)
                kotlin.repeat(2) { +then(pause) }
                +then(t93)
                kotlin.repeat(2) { +then(pause) }
                +then(t96)
                kotlin.repeat(2) { +then(pause) }
                +then(t99)
                kotlin.repeat(2) { +then(pause) }
                +then(t102)
                kotlin.repeat(3) { +then(pause) }
                +then(t106)
                kotlin.repeat(2) { +then(pause) }
                +then(t109)
                kotlin.repeat(2) { +then(pause) }
                +then(t112)
                kotlin.repeat(7) { +then(pause) }
                +then(t120)
                kotlin.repeat(7) { +then(pause) }
                +then(t128)
                kotlin.repeat(2) { +then(pause) }
                +then(t131)
                kotlin.repeat(2) { +then(pause) }
                +then(t134)
                kotlin.repeat(2) { +then(pause) }
                +then(t137)
                kotlin.repeat(2) { +then(pause) }
                +then(t140)
                kotlin.repeat(2) { +then(pause) }
                +then(t143)
                kotlin.repeat(2) { +then(pause) }
                +then(t146)
                kotlin.repeat(2) { +then(pause) }
                +then(t149)
                kotlin.repeat(2) { +then(pause) }
                +then(t152)
                kotlin.repeat(2) { +then(pause) }
                +then(t155)
                kotlin.repeat(2) { +then(pause) }
                +then(t158)
                kotlin.repeat(2) { +then(pause) }
                +then(t161)
                kotlin.repeat(2) { +then(pause) }
                +then(t164)
                kotlin.repeat(2) { +then(pause) }
                +then(t167)
                kotlin.repeat(2) { +then(pause) }
                +then(t170)
                kotlin.repeat(2) { +then(pause) }
                +then(t173)
                kotlin.repeat(3) { +then(pause) }
                +then(t177)
                kotlin.repeat(2) { +then(pause) }
                +then(t180)
                kotlin.repeat(3) { +then(pause) }
                +then(t184)
                kotlin.repeat(3) { +then(pause) }
                +then(t188)
                kotlin.repeat(2) { +then(pause) }
                +then(t191)
                kotlin.repeat(2) { +then(pause) }
                +then(t194)
                kotlin.repeat(2) { +then(pause) }
                +then(t197)
                kotlin.repeat(2) { +then(pause) }
                +then(t200)
                kotlin.repeat(2) { +then(pause) }
                +then(t203)
                kotlin.repeat(2) { +then(pause) }
                +then(t206)
                kotlin.repeat(2) { +then(pause) }
                +then(t209)
                kotlin.repeat(2) { +then(pause) }
                +then(t212)
                kotlin.repeat(2) { +then(pause) }
                +then(t215)
                kotlin.repeat(2) { +then(pause) }
                +then(t218)
                kotlin.repeat(2) { +then(pause) }
                +then(t221)
                kotlin.repeat(2) { +then(pause) }
                +then(t224)
                kotlin.repeat(2) { +then(pause) }
                +then(t227)
                kotlin.repeat(2) { +then(pause) }
                +then(t230)
                kotlin.repeat(2) { +then(pause) }
                +then(t233)
                kotlin.repeat(3) { +then(pause) }
                +then(t237)
                kotlin.repeat(2) { +then(pause) }
                +then(t240)
                kotlin.repeat(2) { +then(pause) }
                +then(t243)
                kotlin.repeat(6) { +then(pause) }
                +then(t250)
                kotlin.repeat(8) { +then(pause) }
                +then(t259)
                kotlin.repeat(2) { +then(pause) }
                +then(t262)
            }
        }
    }

    private fun BossSpecBuilder.rotation3(pause: AbilityRef) {
        val t0 = ability("rotation_3_t0") {
            // Rotation 3 - phase 1
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", 10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t3 = ability("rotation_3_t3") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t6 = ability("rotation_3_t6") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 1))))
        }
        val t9 = ability("rotation_3_t9") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t12 = ability("rotation_3_t12") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t15 = ability("rotation_3_t15") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t18 = ability("rotation_3_t18") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(11, -1), ZulrahPoint(6, -4), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t21 = ability("rotation_3_t21") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(11, 1), ZulrahPoint(7, 5), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t24 = ability("rotation_3_t24") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(11, 0), ZulrahPoint(7, 2), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t28 = ability("rotation_3_t28") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t31 = ability("rotation_3_t31") {
            // Rotation 3 - phase 2
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_melee", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t34 = ability("rotation_3_t34") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 1), ZulrahPoint(-4, -4), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 1), ZulrahPoint(2, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 2)))
        }
        val t38 = ability("rotation_3_t38") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, 2), ZulrahPoint(-3, 3), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t41 = ability("rotation_3_t41") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, 1), ZulrahPoint(-1, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, 1), ZulrahPoint(5, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 10, rotation = 0)))
        }
        val t45 = ability("rotation_3_t45") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, 2), ZulrahPoint(-3, 0), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t48 = ability("rotation_3_t48") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 1), ZulrahPoint(-4, -4), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 1), ZulrahPoint(2, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 2)))
        }
        val t52 = ability("rotation_3_t52") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(3, 2), ZulrahPoint(7, 2), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t55 = ability("rotation_3_t55") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_right")
        }
        val t62 = ability("rotation_3_t62") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_right")
        }
        val t71 = ability("rotation_3_t71") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t74 = ability("rotation_3_t74") {
            // Rotation 3 - phase 3
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", -10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t77 = ability("rotation_3_t77") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t80 = ability("rotation_3_t80") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t83 = ability("rotation_3_t83") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t86 = ability("rotation_3_t86") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t89 = ability("rotation_3_t89") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t92 = ability("rotation_3_t92") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t95 = ability("rotation_3_t95") {
            // Rotation 3 - phase 4
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", 0, -11)))
            anim("seq.snakeboss_emergefast")
        }
        val t98 = ability("rotation_3_t98") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(2, -8))))
        }
        val t101 = ability("rotation_3_t101") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(2, -8))))
        }
        val t104 = ability("rotation_3_t104") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(2, -8))))
        }
        val t107 = ability("rotation_3_t107") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(2, -8))))
        }
        val t110 = ability("rotation_3_t110") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(2, -8))))
        }
        val t113 = ability("rotation_3_t113") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t116 = ability("rotation_3_t116") {
            // Rotation 3 - phase 5
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", 10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t119 = ability("rotation_3_t119") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 0))))
        }
        val t122 = ability("rotation_3_t122") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 1))))
        }
        val t125 = ability("rotation_3_t125") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 1))))
        }
        val t128 = ability("rotation_3_t128") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 1))))
        }
        val t131 = ability("rotation_3_t131") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 1))))
        }
        val t134 = ability("rotation_3_t134") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t137 = ability("rotation_3_t137") {
            // Rotation 3 - phase 6
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t140 = ability("rotation_3_t140") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, 1), ZulrahPoint(2, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 2)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, 1), ZulrahPoint(5, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 0)))
        }
        val t143 = ability("rotation_3_t143") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 1), ZulrahPoint(-4, -4), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 1), ZulrahPoint(-1, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 1)))
        }
        val t146 = ability("rotation_3_t146") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, 1), ZulrahPoint(6, -2), starttime = 40, endtime = 90, impactDelay = 3, rotation = 2)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, 1), ZulrahPoint(6, 1), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 0)))
        }
        val t150 = ability("rotation_3_t150") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, 2), ZulrahPoint(-3, 3), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t153 = ability("rotation_3_t153") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, 2), ZulrahPoint(-3, 0), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t156 = ability("rotation_3_t156") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(3, 2), ZulrahPoint(7, 2), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t160 = ability("rotation_3_t160") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t163 = ability("rotation_3_t163") {
            // Rotation 3 - phase 7
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", -10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t166 = ability("rotation_3_t166") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t169 = ability("rotation_3_t169") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t172 = ability("rotation_3_t172") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t175 = ability("rotation_3_t175") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t178 = ability("rotation_3_t178") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t181 = ability("rotation_3_t181") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t184 = ability("rotation_3_t184") {
            // Rotation 3 - phase 8
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t187 = ability("rotation_3_t187") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(2, 1))))
        }
        val t190 = ability("rotation_3_t190") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, 1))))
        }
        val t193 = ability("rotation_3_t193") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(2, 1))))
        }
        val t196 = ability("rotation_3_t196") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(2, 1))))
        }
        val t199 = ability("rotation_3_t199") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(2, 1))))
        }
        val t202 = ability("rotation_3_t202") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, 1), ZulrahPoint(-4, -1), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, 1), ZulrahPoint(-1, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 0)))
        }
        val t205 = ability("rotation_3_t205") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 2), ZulrahPoint(-4, -4), starttime = 40, endtime = 90, impactDelay = 3, rotation = 3)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 2), ZulrahPoint(-4, 2), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 1)))
        }
        val t209 = ability("rotation_3_t209") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, 2), ZulrahPoint(-3, 3), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t212 = ability("rotation_3_t212") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, 2), ZulrahPoint(-3, 0), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t215 = ability("rotation_3_t215") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(3, 2), ZulrahPoint(7, 2), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t219 = ability("rotation_3_t219") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t222 = ability("rotation_3_t222") {
            // Rotation 3 - phase 9
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", 10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t225 = ability("rotation_3_t225") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 0))))
        }
        val t228 = ability("rotation_3_t228") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t231 = ability("rotation_3_t231") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 0))))
        }
        val t234 = ability("rotation_3_t234") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t237 = ability("rotation_3_t237") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 0))))
        }
        val t240 = ability("rotation_3_t240") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t243 = ability("rotation_3_t243") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 0))))
        }
        val t246 = ability("rotation_3_t246") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t249 = ability("rotation_3_t249") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 0))))
        }
        val t252 = ability("rotation_3_t252") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t255 = ability("rotation_3_t255") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t258 = ability("rotation_3_t258") {
            // Rotation 3 - phase 10
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t261 = ability("rotation_3_t261") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, 2), ZulrahPoint(-3, 3), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t264 = ability("rotation_3_t264") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, 2), ZulrahPoint(-3, 0), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t267 = ability("rotation_3_t267") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(3, 2), ZulrahPoint(7, 2), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t270 = ability("rotation_3_t270") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(3, 3), ZulrahPoint(7, 5), "npc.snakeboss_minion_melee", impactDelay = 10)))
        }
        val t274 = ability("rotation_3_t274") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t277 = ability("rotation_3_t277") {
            transitionTo("recurring")
        }
        phase("rotation_3", lockMovement = true) {
            rotationSelector {
                +then(t0)
                kotlin.repeat(2) { +then(pause) }
                +then(t3)
                kotlin.repeat(2) { +then(pause) }
                +then(t6)
                kotlin.repeat(2) { +then(pause) }
                +then(t9)
                kotlin.repeat(2) { +then(pause) }
                +then(t12)
                kotlin.repeat(2) { +then(pause) }
                +then(t15)
                kotlin.repeat(2) { +then(pause) }
                +then(t18)
                kotlin.repeat(2) { +then(pause) }
                +then(t21)
                kotlin.repeat(2) { +then(pause) }
                +then(t24)
                kotlin.repeat(3) { +then(pause) }
                +then(t28)
                kotlin.repeat(2) { +then(pause) }
                +then(t31)
                kotlin.repeat(2) { +then(pause) }
                +then(t34)
                kotlin.repeat(3) { +then(pause) }
                +then(t38)
                kotlin.repeat(2) { +then(pause) }
                +then(t41)
                kotlin.repeat(3) { +then(pause) }
                +then(t45)
                kotlin.repeat(2) { +then(pause) }
                +then(t48)
                kotlin.repeat(3) { +then(pause) }
                +then(t52)
                kotlin.repeat(2) { +then(pause) }
                +then(t55)
                kotlin.repeat(6) { +then(pause) }
                +then(t62)
                kotlin.repeat(8) { +then(pause) }
                +then(t71)
                kotlin.repeat(2) { +then(pause) }
                +then(t74)
                kotlin.repeat(2) { +then(pause) }
                +then(t77)
                kotlin.repeat(2) { +then(pause) }
                +then(t80)
                kotlin.repeat(2) { +then(pause) }
                +then(t83)
                kotlin.repeat(2) { +then(pause) }
                +then(t86)
                kotlin.repeat(2) { +then(pause) }
                +then(t89)
                kotlin.repeat(2) { +then(pause) }
                +then(t92)
                kotlin.repeat(2) { +then(pause) }
                +then(t95)
                kotlin.repeat(2) { +then(pause) }
                +then(t98)
                kotlin.repeat(2) { +then(pause) }
                +then(t101)
                kotlin.repeat(2) { +then(pause) }
                +then(t104)
                kotlin.repeat(2) { +then(pause) }
                +then(t107)
                kotlin.repeat(2) { +then(pause) }
                +then(t110)
                kotlin.repeat(2) { +then(pause) }
                +then(t113)
                kotlin.repeat(2) { +then(pause) }
                +then(t116)
                kotlin.repeat(2) { +then(pause) }
                +then(t119)
                kotlin.repeat(2) { +then(pause) }
                +then(t122)
                kotlin.repeat(2) { +then(pause) }
                +then(t125)
                kotlin.repeat(2) { +then(pause) }
                +then(t128)
                kotlin.repeat(2) { +then(pause) }
                +then(t131)
                kotlin.repeat(2) { +then(pause) }
                +then(t134)
                kotlin.repeat(2) { +then(pause) }
                +then(t137)
                kotlin.repeat(2) { +then(pause) }
                +then(t140)
                kotlin.repeat(2) { +then(pause) }
                +then(t143)
                kotlin.repeat(2) { +then(pause) }
                +then(t146)
                kotlin.repeat(3) { +then(pause) }
                +then(t150)
                kotlin.repeat(2) { +then(pause) }
                +then(t153)
                kotlin.repeat(2) { +then(pause) }
                +then(t156)
                kotlin.repeat(3) { +then(pause) }
                +then(t160)
                kotlin.repeat(2) { +then(pause) }
                +then(t163)
                kotlin.repeat(2) { +then(pause) }
                +then(t166)
                kotlin.repeat(2) { +then(pause) }
                +then(t169)
                kotlin.repeat(2) { +then(pause) }
                +then(t172)
                kotlin.repeat(2) { +then(pause) }
                +then(t175)
                kotlin.repeat(2) { +then(pause) }
                +then(t178)
                kotlin.repeat(2) { +then(pause) }
                +then(t181)
                kotlin.repeat(2) { +then(pause) }
                +then(t184)
                kotlin.repeat(2) { +then(pause) }
                +then(t187)
                kotlin.repeat(2) { +then(pause) }
                +then(t190)
                kotlin.repeat(2) { +then(pause) }
                +then(t193)
                kotlin.repeat(2) { +then(pause) }
                +then(t196)
                kotlin.repeat(2) { +then(pause) }
                +then(t199)
                kotlin.repeat(2) { +then(pause) }
                +then(t202)
                kotlin.repeat(2) { +then(pause) }
                +then(t205)
                kotlin.repeat(3) { +then(pause) }
                +then(t209)
                kotlin.repeat(2) { +then(pause) }
                +then(t212)
                kotlin.repeat(2) { +then(pause) }
                +then(t215)
                kotlin.repeat(3) { +then(pause) }
                +then(t219)
                kotlin.repeat(2) { +then(pause) }
                +then(t222)
                kotlin.repeat(2) { +then(pause) }
                +then(t225)
                kotlin.repeat(2) { +then(pause) }
                +then(t228)
                kotlin.repeat(2) { +then(pause) }
                +then(t231)
                kotlin.repeat(2) { +then(pause) }
                +then(t234)
                kotlin.repeat(2) { +then(pause) }
                +then(t237)
                kotlin.repeat(2) { +then(pause) }
                +then(t240)
                kotlin.repeat(2) { +then(pause) }
                +then(t243)
                kotlin.repeat(2) { +then(pause) }
                +then(t246)
                kotlin.repeat(2) { +then(pause) }
                +then(t249)
                kotlin.repeat(2) { +then(pause) }
                +then(t252)
                kotlin.repeat(2) { +then(pause) }
                +then(t255)
                kotlin.repeat(2) { +then(pause) }
                +then(t258)
                kotlin.repeat(2) { +then(pause) }
                +then(t261)
                kotlin.repeat(2) { +then(pause) }
                +then(t264)
                kotlin.repeat(2) { +then(pause) }
                +then(t267)
                kotlin.repeat(2) { +then(pause) }
                +then(t270)
                kotlin.repeat(3) { +then(pause) }
                +then(t274)
                kotlin.repeat(2) { +then(pause) }
                +then(t277)
            }
        }
    }

    private fun BossSpecBuilder.rotation4(pause: AbilityRef) {
        val t0 = ability("rotation_4_t0") {
            // Rotation 4 - phase 1
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", 10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t3 = ability("rotation_4_t3") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(11, -1), ZulrahPoint(6, -4), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t6 = ability("rotation_4_t6") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(11, 1), ZulrahPoint(7, 5), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t9 = ability("rotation_4_t9") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(11, 0), ZulrahPoint(7, 2), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t12 = ability("rotation_4_t12") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(11, 0), ZulrahPoint(7, -1), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t16 = ability("rotation_4_t16") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t19 = ability("rotation_4_t19") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 1))))
        }
        val t22 = ability("rotation_4_t22") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 1))))
        }
        val t25 = ability("rotation_4_t25") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 1))))
        }
        val t28 = ability("rotation_4_t28") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 1))))
        }
        val t31 = ability("rotation_4_t31") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 1))))
        }
        val t34 = ability("rotation_4_t34") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t37 = ability("rotation_4_t37") {
            // Rotation 4 - phase 2
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", 0, -11)))
            anim("seq.snakeboss_emergefast")
        }
        val t40 = ability("rotation_4_t40") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(2, -8))))
        }
        val t43 = ability("rotation_4_t43") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(2, -8))))
        }
        val t46 = ability("rotation_4_t46") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(2, -8))))
        }
        val t49 = ability("rotation_4_t49") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(2, -8))))
        }
        val t52 = ability("rotation_4_t52") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, -8), ZulrahPoint(-4, -4), starttime = 40, endtime = 90, impactDelay = 3, rotation = 0)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, -8), ZulrahPoint(2, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 1)))
        }
        val t55 = ability("rotation_4_t55") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, -8), ZulrahPoint(-1, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 3)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, -8), ZulrahPoint(5, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 10, rotation = 0)))
        }
        val t59 = ability("rotation_4_t59") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t62 = ability("rotation_4_t62") {
            // Rotation 4 - phase 3
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", -10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t65 = ability("rotation_4_t65") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(-7, 1), ZulrahPoint(-3, 3), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t68 = ability("rotation_4_t68") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(-7, 0), ZulrahPoint(-3, 0), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t71 = ability("rotation_4_t71") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(-7, -1), ZulrahPoint(0, -4), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t74 = ability("rotation_4_t74") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(-7, 0), ZulrahPoint(3, -4), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t78 = ability("rotation_4_t78") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t81 = ability("rotation_4_t81") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t84 = ability("rotation_4_t84") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t87 = ability("rotation_4_t87") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t90 = ability("rotation_4_t90") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t93 = ability("rotation_4_t93") {
            // Rotation 4 - phase 4
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_melee", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t96 = ability("rotation_4_t96") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_right")
        }
        val t103 = ability("rotation_4_t103") {
            include(external("zulrah.tail_attack", tail))
            anim("seq.snakeboss_attack_tail_right")
        }
        val t111 = ability("rotation_4_t111") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 1), ZulrahPoint(-4, -4), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 1), ZulrahPoint(2, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 2)))
        }
        val t114 = ability("rotation_4_t114") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, 1), ZulrahPoint(-1, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, 1), ZulrahPoint(5, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 10, rotation = 0)))
        }
        val t118 = ability("rotation_4_t118") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t121 = ability("rotation_4_t121") {
            // Rotation 4 - phase 5
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", 10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t124 = ability("rotation_4_t124") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t127 = ability("rotation_4_t127") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t130 = ability("rotation_4_t130") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t133 = ability("rotation_4_t133") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t136 = ability("rotation_4_t136") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t139 = ability("rotation_4_t139") {
            // Rotation 4 - phase 6
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", 0, -11)))
            anim("seq.snakeboss_emergefast")
        }
        val t142 = ability("rotation_4_t142") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, -8), ZulrahPoint(-3, 3), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t145 = ability("rotation_4_t145") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, -8), ZulrahPoint(-3, 0), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t148 = ability("rotation_4_t148") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, -8), ZulrahPoint(-3, -3), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t151 = ability("rotation_4_t151") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(3, -8), ZulrahPoint(7, -1), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t154 = ability("rotation_4_t154") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(3, -8), ZulrahPoint(7, 2), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t157 = ability("rotation_4_t157") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(2, -8), ZulrahPoint(7, 5), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t161 = ability("rotation_4_t161") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(6, 1), starttime = 40, endtime = 90, impactDelay = 3, rotation = 0)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(6, 4), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 1)))
        }
        val t164 = ability("rotation_4_t164") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(5, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 3)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, -8), ZulrahPoint(6, -2), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 2)))
        }
        val t167 = ability("rotation_4_t167") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, -8), ZulrahPoint(2, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 2)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, -8), ZulrahPoint(-1, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 10, rotation = 2)))
        }
        val t171 = ability("rotation_4_t171") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t174 = ability("rotation_4_t174") {
            // Rotation 4 - phase 7
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", -10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t177 = ability("rotation_4_t177") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_orb", ZulrahPoint(-7, 0))))
        }
        val t180 = ability("rotation_4_t180") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t183 = ability("rotation_4_t183") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t186 = ability("rotation_4_t186") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t189 = ability("rotation_4_t189") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(-7, 0))))
        }
        val t192 = ability("rotation_4_t192") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(-7, 1), ZulrahPoint(-3, 3), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t195 = ability("rotation_4_t195") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(-7, 0), ZulrahPoint(-3, 0), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t198 = ability("rotation_4_t198") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(-7, -1), ZulrahPoint(-3, -3), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t201 = ability("rotation_4_t201") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(-7, -1), ZulrahPoint(0, -4), "npc.snakeboss_minion_melee", impactDelay = 10)))
        }
        val t205 = ability("rotation_4_t205") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t208 = ability("rotation_4_t208") {
            // Rotation 4 - phase 8
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t211 = ability("rotation_4_t211") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(2, 1))))
        }
        val t214 = ability("rotation_4_t214") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(1, 1))))
        }
        val t217 = ability("rotation_4_t217") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(1, 1))))
        }
        val t220 = ability("rotation_4_t220") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(1, 1))))
        }
        val t223 = ability("rotation_4_t223") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t226 = ability("rotation_4_t226") {
            // Rotation 4 - phase 9
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t229 = ability("rotation_4_t229") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(2, 1))))
        }
        val t232 = ability("rotation_4_t232") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(1, 1))))
        }
        val t235 = ability("rotation_4_t235") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(2, 1))))
        }
        val t238 = ability("rotation_4_t238") {
            anim("seq.snakeboss_attack_acidx1")
            include(mixedAttack(attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(2, 1))))
        }
        val t241 = ability("rotation_4_t241") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, 1), ZulrahPoint(-4, -1), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, 1), ZulrahPoint(-1, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 0)))
        }
        val t244 = ability("rotation_4_t244") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 2), ZulrahPoint(-4, -4), starttime = 40, endtime = 90, impactDelay = 3, rotation = 3)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 2), ZulrahPoint(-4, 2), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 1)))
        }
        val t247 = ability("rotation_4_t247") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 1), ZulrahPoint(2, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 2)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 1), ZulrahPoint(5, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 10, rotation = 1)))
        }
        val t251 = ability("rotation_4_t251") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t254 = ability("rotation_4_t254") {
            // Rotation 4 - phase 10
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", 10, -2)))
            anim("seq.snakeboss_emergefast")
        }
        val t257 = ability("rotation_4_t257") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 0))))
        }
        val t260 = ability("rotation_4_t260") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t263 = ability("rotation_4_t263") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 0))))
        }
        val t266 = ability("rotation_4_t266") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t269 = ability("rotation_4_t269") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 0))))
        }
        val t272 = ability("rotation_4_t272") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t275 = ability("rotation_4_t275") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_fireball", ZulrahPoint(11, 0))))
        }
        val t278 = ability("rotation_4_t278") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(11, 0))))
        }
        val t281 = ability("rotation_4_t281") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t284 = ability("rotation_4_t284") {
            // Rotation 4 - phase 11
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_magic", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t287 = ability("rotation_4_t287") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, 2), ZulrahPoint(-3, 3), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t290 = ability("rotation_4_t290") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(1, 2), ZulrahPoint(-3, 0), "npc.snakeboss_minion_magic", impactDelay = 4)))
        }
        val t293 = ability("rotation_4_t293") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(3, 2), ZulrahPoint(7, 2), "npc.snakeboss_minion_melee", impactDelay = 4)))
        }
        val t296 = ability("rotation_4_t296") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", eggEvent(ZulrahPoint(3, 3), ZulrahPoint(7, 5), "npc.snakeboss_minion_melee", impactDelay = 10)))
        }
        val t300 = ability("rotation_4_t300") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t303 = ability("rotation_4_t303") {
            transitionTo("recurring")
        }
        phase("rotation_4", lockMovement = true) {
            rotationSelector {
                +then(t0)
                kotlin.repeat(2) { +then(pause) }
                +then(t3)
                kotlin.repeat(2) { +then(pause) }
                +then(t6)
                kotlin.repeat(2) { +then(pause) }
                +then(t9)
                kotlin.repeat(2) { +then(pause) }
                +then(t12)
                kotlin.repeat(3) { +then(pause) }
                +then(t16)
                kotlin.repeat(2) { +then(pause) }
                +then(t19)
                kotlin.repeat(2) { +then(pause) }
                +then(t22)
                kotlin.repeat(2) { +then(pause) }
                +then(t25)
                kotlin.repeat(2) { +then(pause) }
                +then(t28)
                kotlin.repeat(2) { +then(pause) }
                +then(t31)
                kotlin.repeat(2) { +then(pause) }
                +then(t34)
                kotlin.repeat(2) { +then(pause) }
                +then(t37)
                kotlin.repeat(2) { +then(pause) }
                +then(t40)
                kotlin.repeat(2) { +then(pause) }
                +then(t43)
                kotlin.repeat(2) { +then(pause) }
                +then(t46)
                kotlin.repeat(2) { +then(pause) }
                +then(t49)
                kotlin.repeat(2) { +then(pause) }
                +then(t52)
                kotlin.repeat(2) { +then(pause) }
                +then(t55)
                kotlin.repeat(3) { +then(pause) }
                +then(t59)
                kotlin.repeat(2) { +then(pause) }
                +then(t62)
                kotlin.repeat(2) { +then(pause) }
                +then(t65)
                kotlin.repeat(2) { +then(pause) }
                +then(t68)
                kotlin.repeat(2) { +then(pause) }
                +then(t71)
                kotlin.repeat(2) { +then(pause) }
                +then(t74)
                kotlin.repeat(3) { +then(pause) }
                +then(t78)
                kotlin.repeat(2) { +then(pause) }
                +then(t81)
                kotlin.repeat(2) { +then(pause) }
                +then(t84)
                kotlin.repeat(2) { +then(pause) }
                +then(t87)
                kotlin.repeat(2) { +then(pause) }
                +then(t90)
                kotlin.repeat(2) { +then(pause) }
                +then(t93)
                kotlin.repeat(2) { +then(pause) }
                +then(t96)
                kotlin.repeat(6) { +then(pause) }
                +then(t103)
                kotlin.repeat(7) { +then(pause) }
                +then(t111)
                kotlin.repeat(2) { +then(pause) }
                +then(t114)
                kotlin.repeat(3) { +then(pause) }
                +then(t118)
                kotlin.repeat(2) { +then(pause) }
                +then(t121)
                kotlin.repeat(2) { +then(pause) }
                +then(t124)
                kotlin.repeat(2) { +then(pause) }
                +then(t127)
                kotlin.repeat(2) { +then(pause) }
                +then(t130)
                kotlin.repeat(2) { +then(pause) }
                +then(t133)
                kotlin.repeat(2) { +then(pause) }
                +then(t136)
                kotlin.repeat(2) { +then(pause) }
                +then(t139)
                kotlin.repeat(2) { +then(pause) }
                +then(t142)
                kotlin.repeat(2) { +then(pause) }
                +then(t145)
                kotlin.repeat(2) { +then(pause) }
                +then(t148)
                kotlin.repeat(2) { +then(pause) }
                +then(t151)
                kotlin.repeat(2) { +then(pause) }
                +then(t154)
                kotlin.repeat(2) { +then(pause) }
                +then(t157)
                kotlin.repeat(3) { +then(pause) }
                +then(t161)
                kotlin.repeat(2) { +then(pause) }
                +then(t164)
                kotlin.repeat(2) { +then(pause) }
                +then(t167)
                kotlin.repeat(3) { +then(pause) }
                +then(t171)
                kotlin.repeat(2) { +then(pause) }
                +then(t174)
                kotlin.repeat(2) { +then(pause) }
                +then(t177)
                kotlin.repeat(2) { +then(pause) }
                +then(t180)
                kotlin.repeat(2) { +then(pause) }
                +then(t183)
                kotlin.repeat(2) { +then(pause) }
                +then(t186)
                kotlin.repeat(2) { +then(pause) }
                +then(t189)
                kotlin.repeat(2) { +then(pause) }
                +then(t192)
                kotlin.repeat(2) { +then(pause) }
                +then(t195)
                kotlin.repeat(2) { +then(pause) }
                +then(t198)
                kotlin.repeat(2) { +then(pause) }
                +then(t201)
                kotlin.repeat(3) { +then(pause) }
                +then(t205)
                kotlin.repeat(2) { +then(pause) }
                +then(t208)
                kotlin.repeat(2) { +then(pause) }
                +then(t211)
                kotlin.repeat(2) { +then(pause) }
                +then(t214)
                kotlin.repeat(2) { +then(pause) }
                +then(t217)
                kotlin.repeat(2) { +then(pause) }
                +then(t220)
                kotlin.repeat(2) { +then(pause) }
                +then(t223)
                kotlin.repeat(2) { +then(pause) }
                +then(t226)
                kotlin.repeat(2) { +then(pause) }
                +then(t229)
                kotlin.repeat(2) { +then(pause) }
                +then(t232)
                kotlin.repeat(2) { +then(pause) }
                +then(t235)
                kotlin.repeat(2) { +then(pause) }
                +then(t238)
                kotlin.repeat(2) { +then(pause) }
                +then(t241)
                kotlin.repeat(2) { +then(pause) }
                +then(t244)
                kotlin.repeat(2) { +then(pause) }
                +then(t247)
                kotlin.repeat(3) { +then(pause) }
                +then(t251)
                kotlin.repeat(2) { +then(pause) }
                +then(t254)
                kotlin.repeat(2) { +then(pause) }
                +then(t257)
                kotlin.repeat(2) { +then(pause) }
                +then(t260)
                kotlin.repeat(2) { +then(pause) }
                +then(t263)
                kotlin.repeat(2) { +then(pause) }
                +then(t266)
                kotlin.repeat(2) { +then(pause) }
                +then(t269)
                kotlin.repeat(2) { +then(pause) }
                +then(t272)
                kotlin.repeat(2) { +then(pause) }
                +then(t275)
                kotlin.repeat(2) { +then(pause) }
                +then(t278)
                kotlin.repeat(2) { +then(pause) }
                +then(t281)
                kotlin.repeat(2) { +then(pause) }
                +then(t284)
                kotlin.repeat(2) { +then(pause) }
                +then(t287)
                kotlin.repeat(2) { +then(pause) }
                +then(t290)
                kotlin.repeat(2) { +then(pause) }
                +then(t293)
                kotlin.repeat(2) { +then(pause) }
                +then(t296)
                kotlin.repeat(3) { +then(pause) }
                +then(t300)
                kotlin.repeat(2) { +then(pause) }
                +then(t303)
            }
        }
    }

    private fun BossSpecBuilder.recurring(pause: AbilityRef) {
        val t0 = ability("recurring_t0") {
            include(external("zulrah.emerge", emergeEvent("npc.snakeboss_boss_ranged", 0, 0)))
            anim("seq.snakeboss_emergefast")
        }
        val t3 = ability("recurring_t3") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(2, 1))))
        }
        val t6 = ability("recurring_t6") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(3, 2))))
        }
        val t9 = ability("recurring_t9") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(3, 2))))
        }
        val t12 = ability("recurring_t12") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(3, 2))))
        }
        val t15 = ability("recurring_t15") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.attack", attackEvent("spotanim.snakeboss_orb", ZulrahPoint(3, 1))))
        }
        val t19 = ability("recurring_t19") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, 1), ZulrahPoint(2, -5), starttime = 40, endtime = 90, impactDelay = 3, rotation = 2)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(2, 1), ZulrahPoint(5, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 0)))
        }
        val t22 = ability("recurring_t22") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 1), ZulrahPoint(-4, -4), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 1), ZulrahPoint(-1, -5), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 1)))
        }
        val t25 = ability("recurring_t25") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, 1), ZulrahPoint(6, -2), starttime = 40, endtime = 90, impactDelay = 3, rotation = 2)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(3, 1), ZulrahPoint(6, 1), starttime = 45, endtime = 120, angle = 10, impactDelay = 4, rotation = 0)))
        }
        val t28 = ability("recurring_t28") {
            anim("seq.snakeboss_attack_acidx1")
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 2), ZulrahPoint(-4, 2), starttime = 40, endtime = 90, impactDelay = 3, rotation = 1)))
            include(external("zulrah.hazard", gasEvent(ZulrahPoint(1, 2), ZulrahPoint(-4, -1), starttime = 45, endtime = 120, angle = 10, impactDelay = 10, rotation = 3)))
        }
        val t32 = ability("recurring_t32") {
            include(external("zulrah.dive"))
            anim("seq.snakeboss_sinkfast")
        }
        val t35 = ability("recurring_t35") {
            transitionTo("select_rotation")
        }
        phase("recurring", lockMovement = true) {
            rotationSelector {
                +then(t0)
                kotlin.repeat(2) { +then(pause) }
                +then(t3)
                kotlin.repeat(2) { +then(pause) }
                +then(t6)
                kotlin.repeat(2) { +then(pause) }
                +then(t9)
                kotlin.repeat(2) { +then(pause) }
                +then(t12)
                kotlin.repeat(2) { +then(pause) }
                +then(t15)
                kotlin.repeat(3) { +then(pause) }
                +then(t19)
                kotlin.repeat(2) { +then(pause) }
                +then(t22)
                kotlin.repeat(2) { +then(pause) }
                +then(t25)
                kotlin.repeat(2) { +then(pause) }
                +then(t28)
                kotlin.repeat(3) { +then(pause) }
                +then(t32)
                kotlin.repeat(2) { +then(pause) }
                +then(t35)
            }
        }
    }

    private fun emergeEvent(symbol: String, x: Int, z: Int, initial: Boolean = false) =
        ZulrahRoutineEvent(
            tick = 0,
            kind = "emerge",
            symbol = symbol,
            x = x,
            z = z,
            initial = initial,
        )

    private fun attackEvent(symbol: String, source: ZulrahPoint) =
        ZulrahRoutineEvent(
            tick = 0,
            kind = "attack",
            symbol = symbol,
            source = source,
            starttime = 40,
            endtime = 90,
            startheight = 370,
            endheight = 50,
        )

    private fun gasEvent(
        source: ZulrahPoint,
        target: ZulrahPoint,
        starttime: Int,
        endtime: Int,
        angle: Int = 0,
        impactDelay: Int,
        rotation: Int,
    ) =
        ZulrahRoutineEvent(
            tick = 0,
            kind = "gas",
            symbol = "spotanim.snakeboss_double_orb",
            source = source,
            target = target,
            starttime = starttime,
            endtime = endtime,
            angle = angle,
            startheight = 370,
            endheight = 20,
            impactDelay = impactDelay,
            cloudLifetime = 30,
            rotation = rotation,
        )

    private fun eggEvent(
        source: ZulrahPoint,
        target: ZulrahPoint,
        spawn: String,
        impactDelay: Int,
    ) =
        ZulrahRoutineEvent(
            tick = 0,
            kind = "egg",
            symbol = "spotanim.snakeboss_egg",
            source = source,
            target = target,
            starttime = 40,
            endtime = 120,
            startheight = 370,
            endheight = 20,
            impactDelay = impactDelay,
            spawn = spawn,
        )

    private fun mixedAttack(event: ZulrahRoutineEvent) =
        choose(
            weightedRandom(noRepeatBias = 0.0) {
                for ((symbol, weight) in MIXED_ATTACK_WEIGHTS) {
                    +random(symbol, weight)
                }
            },
            MIXED_ATTACK_WEIGHTS.keys.associateWith { symbol ->
                external("zulrah.attack", event.copy(symbol = symbol))
            },
        )

    val snakelings: BossSpec =
        boss(ZulrahEncounterController.MELEE_SNAKE, ZulrahEncounterController.MAGIC_SNAKE) {
            stats(attackRate = 3, aggressionRadius = 64, retaliateOnHit = false)
            val attack =
                ability("attack_owner") {
                    anim("seq.snakeboss_pet_attack")
                    include(external("zulrah.snake_attack"))
                }
            phase("combat") {
                rotationSelector {
                    +then(attack)
                }
            }
        }
}

internal data class ZulrahTailAttack(
    val impactDelay: Int,
    val stunTicks: Int,
    val damage: IntRange,
)
