package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterDeadfallTable {
    const val NAME = 0
    const val NPC = 1
    const val LEVEL = 2
    const val XP_TENTHS = 3
    const val TRAPPING_LOCS = 4
    const val FULL_LOC = 5
    const val LOOT = 6
    const val LOW = 7
    const val HIGH = 8
    const val BAIT = 9
    const val APPROACH_SEQ = 10
    const val RESPAWN_TICKS = 11
    const val FULL_SYNTH = 12

    private class Creature(
        val key: String,
        val name: String,
        val npc: String,
        val level: Int,
        val xpTenths: Int,
        val locKey: String,
        val loot: List<String>,
        val low: Int,
        val high: Int,
        val bait: String,
        val approachSeq: String = "seq.huntingbeast_approach",
        val respawnTicks: Int = 1,
        val fullSynth: String = "synth.hunting_deadfall_kebbit",
    )

    private val CREATURES =
        listOf(
            Creature("wild", "wild kebbit", "npc.huntingbeast_claws", 23, 1024, "claw", listOf("obj.bones", "obj.huntingbeast_claws", "obj.huntingbeast_wild_meat"), 29, 385, "obj.raw_beef"),
            Creature("barbed", "barb-tailed kebbit", "npc.huntingbeast_barbedtail", 33, 1344, "barbed", listOf("obj.bones", "obj.hunting_barbed_harpoon", "obj.huntingbeast_barbed_meat"), -226, 1048, "obj.hunting_raw_fish_special"),
            Creature("prickly", "prickly kebbit", "npc.huntingbeast_spiky", 37, 1472, "spike", listOf("obj.bones", "obj.huntingbeast_spike"), -70, 331, "obj.barley"),
            Creature("sabre", "sabre-toothed kebbit", "npc.huntingbeast_sabreteeth", 51, 1600, "sabre", listOf("obj.bones", "obj.huntingbeast_sabreteeth"), -424, 811, "obj.raw_beef"),
            Creature(
                "pyre_fox",
                "pyre fox",
                "npc.varlamore_hunterfox01",
                57,
                1776,
                "fennec",
                listOf("obj.bones", "obj.hunting_fennecfox_fur", "obj.hunting_fennecfox_meat"),
                -477,
                752,
                "obj.hunting_jerboa_tail",
                approachSeq = "seq.huntingfox_approach",
                respawnTicks = 2,
                fullSynth = "synth.hunting_deadfall_fox",
            ),
        )

    fun deadfalls() =
        dbTable("dbtable.hunter_deadfalls", serverOnly = true) {
            column("name", NAME, VarType.STRING)
            column("npc", NPC, VarType.NPC)
            column("level", LEVEL, VarType.INT)
            column("xp_tenths", XP_TENTHS, VarType.INT)
            column("trapping_locs", TRAPPING_LOCS, VarType.LOC)
            column("full_loc", FULL_LOC, VarType.LOC)
            column("loot", LOOT, VarType.OBJ)
            column("low", LOW, VarType.INT)
            column("high", HIGH, VarType.INT)
            column("bait", BAIT, VarType.OBJ)
            column("approach_seq", APPROACH_SEQ, VarType.SEQ)
            column("respawn_ticks", RESPAWN_TICKS, VarType.INT)
            column("full_synth", FULL_SYNTH, VarType.SYNTH)

            for (c in CREATURES) {
                row("dbrow.hunter_deadfall_${c.key}") {
                    column(NAME, c.name)
                    columnRSCM(NPC, c.npc)
                    column(LEVEL, c.level)
                    column(XP_TENTHS, c.xpTenths)
                    column(
                        TRAPPING_LOCS,
                        listOf("loc.hunting_deadfall_trapping_${c.locKey}", "loc.hunting_deadfall_trapping_${c.locKey}_m")
                            .map { ConstantProvider.getMapping(it) },
                    )
                    columnRSCM(FULL_LOC, "loc.hunting_deadfall_full_${c.locKey}")
                    column(LOOT, c.loot.map { ConstantProvider.getMapping(it) })
                    column(LOW, c.low)
                    column(HIGH, c.high)
                    columnRSCM(BAIT, c.bait)
                    columnRSCM(APPROACH_SEQ, c.approachSeq)
                    column(RESPAWN_TICKS, c.respawnTicks)
                    columnRSCM(FULL_SYNTH, c.fullSynth)
                }
            }
        }
}
