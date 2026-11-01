package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterPitfallTable {
    const val NAME = 0
    const val NPC = 1
    const val LEVEL = 2
    const val XP = 3
    const val FULL_LOCS = 4
    const val LOOT = 5
    const val FURS = 6
    const val MAX_HIT = 7
    const val ATTACK_SEQ = 8
    const val ATTACK_SYNTH = 9
    const val FALL_SEQ = 10
    const val CAUGHT_SYNTH = 11
    const val LEAP_SPOTANIMS = 12
    const val RESPAWN_TICKS = 13
    const val LOW = 14
    const val HIGH = 15
    const val EXTRA = 16
    const val GEAR = 17

    private class Creature(
        val key: String,
        val name: String,
        val npc: String,
        val level: Int,
        val xp: Int,
        val fullLocs: List<String>,
        val loot: List<String>,
        val furs: List<String>,
        val maxHit: Int,
        val attackSeq: String,
        val attackSynth: String,
        val fallSeq: String,
        val caughtSynth: String,
        val leap: List<String>,
        val respawnTicks: Int,
        val low: Int,
        val high: Int,
        val extra: Triple<String, Int, Int>? = null,
        val gear: List<Pair<List<String>, Int>> = emptyList(),
    )

    private const val BIG_CAT_FALL = "seq.hunter_bigcat_fall_in_pit"
    private const val ANTELOPE_FALL = "seq.unicorn_rework_pitfall_death"
    private val BIG_CAT_GEAR =
        listOf(
            listOf("obj.hunting_hat_jaguar", "obj.hunting_torso_jaguar", "obj.hunting_trousers_jaguar") to 20,
            listOf("obj.hunting_hat_leopard", "obj.hunting_torso_leopard", "obj.hunting_trousers_leopard") to 40,
            listOf("obj.hunting_hat_tiger", "obj.hunting_torso_tiger", "obj.hunting_trousers_tiger") to 60,
        )

    private val CREATURES =
        listOf(
            Creature(
                "larupia", "spined larupia", "npc.hunting_jaguar", 31, 180,
                listOf("loc.hunter_pitfall_full_larupia", "loc.hunter_pitfall_full_larupia_180"),
                listOf("obj.big_bones", "obj.hunting_larupia_meat"),
                listOf("obj.hunting_fur_jaguar_perfect", "obj.hunting_fur_jaguar_shabby"),
                5, "seq.hunter_big_cat_bite", "synth.hunting_kyatt_attack", BIG_CAT_FALL, "synth.hunting_kyatt_caught",
                listOf("spotanim.hunter_larupia_leaptrap_proj1", "spotanim.hunter_larupia_leaptrap_proj2"), 1, 120, 380,
                gear = BIG_CAT_GEAR,
            ),
            Creature(
                "graahk", "horned graahk", "npc.hunting_leopard", 41, 240,
                listOf("loc.hunter_pitfall_full_graahk", "loc.hunter_pitfall_full_graahk_180"),
                listOf("obj.big_bones", "obj.hunting_graahk_meat"),
                listOf("obj.hunting_fur_leopard_perfect", "obj.hunting_fur_leopard_shabby"),
                7, "seq.hunter_big_cat_butt", "synth.hunting_graahk_attack", BIG_CAT_FALL, "synth.hunting_graahk_caught",
                listOf("spotanim.hunter_graahk_leaptrap_proj1", "spotanim.hunter_graahk_leaptrap_proj2"), 1, 100, 380,
                gear = BIG_CAT_GEAR,
            ),
            Creature(
                "kyatt", "sabretoothed kyatt", "npc.hunting_snow_tiger", 55, 300,
                listOf("loc.hunter_pitfall_full_kyatt", "loc.hunter_pitfall_full_kyatt_180"),
                listOf("obj.big_bones", "obj.hunting_kyatt_meat"),
                listOf("obj.hunting_fur_tiger_perfect", "obj.hunting_fur_tiger_shabby"),
                7, "seq.hunter_big_cat_bite", "synth.hunting_kyatt_attack", BIG_CAT_FALL, "synth.hunting_kyatt_caught",
                listOf("spotanim.hunter_kyatt_leaptrap_proj1", "spotanim.hunter_kyatt_leaptrap_proj2"), 1, 80, 380,
                gear = BIG_CAT_GEAR,
            ),
            Creature(
                "sunlight_antelope", "sunlight antelope", "npc.sunlight_antelope", 72, 380,
                listOf("loc.hunter_pitfall_full", "loc.hunter_pitfall_full"),
                listOf("obj.hunting_antelopesun_horn", "obj.big_bones", "obj.hunting_antelopesun_fur", "obj.hunting_antelopesun_meat"),
                emptyList(),
                5, "seq.antelope_update_sunlight_attack", "synth.hunting_antelope_attack", ANTELOPE_FALL, "synth.hunting_antelope_caught",
                emptyList(), 14, 256, 256, Triple("obj.sunfiresplinter", 2, 6),
            ),
            Creature(
                "moonlight_antelope", "moonlight antelope", "npc.moonlight_antelope", 91, 450,
                listOf("loc.hunter_pitfall_full", "loc.hunter_pitfall_full"),
                listOf("obj.hunting_antelopemoon_horn", "obj.big_bones", "obj.hunting_antelopemoon_fur", "obj.hunting_antelopemoon_meat"),
                emptyList(),
                5, "seq.antelope_update_moonlight_attack", "synth.hunting_antelope_attack", ANTELOPE_FALL, "synth.hunting_antelope_caught",
                emptyList(), 14, 256, 256,
            ),
        )

    fun pitfalls() =
        dbTable("dbtable.hunter_pitfalls", serverOnly = true) {
            column("name", NAME, VarType.STRING)
            column("npc", NPC, VarType.NPC)
            column("level", LEVEL, VarType.INT)
            column("xp", XP, VarType.INT)
            column("full_locs", FULL_LOCS, VarType.LOC)
            column("loot", LOOT, VarType.OBJ)
            column("furs", FURS, VarType.OBJ)
            column("max_hit", MAX_HIT, VarType.INT)
            column("attack_seq", ATTACK_SEQ, VarType.SEQ)
            column("attack_synth", ATTACK_SYNTH, VarType.SYNTH)
            column("fall_seq", FALL_SEQ, VarType.SEQ)
            column("caught_synth", CAUGHT_SYNTH, VarType.SYNTH)
            column("leap_spotanims", LEAP_SPOTANIMS, VarType.STRING)
            column("respawn_ticks", RESPAWN_TICKS, VarType.INT)
            column("low", LOW, VarType.INT)
            column("high", HIGH, VarType.INT)
            column("extra", EXTRA, VarType.OBJ, VarType.INT, VarType.INT)
            column("gear", GEAR, VarType.OBJ, VarType.OBJ, VarType.OBJ, VarType.INT)

            for (c in CREATURES) {
                row("dbrow.hunter_pitfall_${c.key}") {
                    column(NAME, c.name)
                    columnRSCM(NPC, c.npc)
                    column(LEVEL, c.level)
                    column(XP, c.xp)
                    column(FULL_LOCS, c.fullLocs.map { ConstantProvider.getMapping(it) })
                    column(LOOT, c.loot.map { ConstantProvider.getMapping(it) })
                    if (c.furs.isNotEmpty()) column(FURS, c.furs.map { ConstantProvider.getMapping(it) })
                    column(MAX_HIT, c.maxHit)
                    columnRSCM(ATTACK_SEQ, c.attackSeq)
                    columnRSCM(ATTACK_SYNTH, c.attackSynth)
                    columnRSCM(FALL_SEQ, c.fallSeq)
                    columnRSCM(CAUGHT_SYNTH, c.caughtSynth)
                    column(LEAP_SPOTANIMS, c.leap.ifEmpty { listOf("") })
                    column(RESPAWN_TICKS, c.respawnTicks)
                    column(LOW, c.low)
                    column(HIGH, c.high)
                    c.extra?.let { (obj, min, max) -> column(EXTRA, listOf(ConstantProvider.getMapping(obj), min, max)) }
                    if (c.gear.isNotEmpty()) {
                        column(GEAR, c.gear.flatMap { (pieces, reduction) -> pieces.map { ConstantProvider.getMapping(it) } + reduction })
                    }
                }
            }
        }
}
