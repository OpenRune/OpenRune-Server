package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterTrapsTable {
    const val ITEM = 0
    const val NAME = 1
    const val LEVEL = 2
    const val SET_LOC = 3
    const val FAILING_LOC = 4
    const val BROKEN_LOC = 5
    const val CHECK_SEQ = 6
    const val LAY_SYNTH = 7
    const val COLLAPSE_TEXT = 8
    const val QUEST = 9
    const val CONSUMED = 10
    const val SMOKE_REFUSAL = 11
    const val OVERLAY_TIMER = 12

    fun hunterTraps() =
        dbTable("dbtable.hunter_traps", serverOnly = true) {
            column("item", ITEM, VarType.OBJ)
            column("name", NAME, VarType.STRING)
            column("level", LEVEL, VarType.INT)
            column("set_loc", SET_LOC, VarType.LOC)
            column("failing_loc", FAILING_LOC, VarType.LOC)
            column("broken_loc", BROKEN_LOC, VarType.LOC)
            column("check_seq", CHECK_SEQ, VarType.SEQ)
            column("lay_synth", LAY_SYNTH, VarType.SYNTH)
            column("collapse_text", COLLAPSE_TEXT, VarType.STRING)
            column("quest", QUEST, VarType.STRING)
            column("consumed", CONSUMED, VarType.BOOLEAN)
            column("smoke_refusal", SMOKE_REFUSAL, VarType.STRING)
            column("overlay_timer", OVERLAY_TIMER, VarType.BOOLEAN)

            row("dbrow.hunter_trap_bird_snare") {
                columnRSCM(ITEM, "obj.hunting_ojibway_bird_snare")
                column(NAME, "bird snare")
                column(LEVEL, 1)
                columnRSCM(SET_LOC, "loc.hunting_ojibway_trap")
                columnRSCM(FAILING_LOC, "loc.hunting_ojibway_trap_failing")
                columnRSCM(BROKEN_LOC, "loc.hunting_ojibway_trap_broken")
                columnRSCM(CHECK_SEQ, "seq.human_hunting_dismantle_net")
                columnRSCM(LAY_SYNTH, "synth.hunting_lay_bird_snare")
                column(COLLAPSE_TEXT, "The bird snare that you laid has fallen over.")
                column(QUEST, "")
                column(CONSUMED, false)
                column(SMOKE_REFUSAL, "")
                column(OVERLAY_TIMER, false)
            }
            row("dbrow.hunter_trap_box_trap") {
                columnRSCM(ITEM, "obj.hunting_box_trap")
                column(NAME, "box trap")
                column(LEVEL, 27)
                columnRSCM(SET_LOC, "loc.hunting_boxtrap_empty")
                columnRSCM(FAILING_LOC, "loc.hunting_boxtrap_failing")
                columnRSCM(BROKEN_LOC, "loc.hunting_boxtrap_failed")
                columnRSCM(CHECK_SEQ, "seq.hunting_setting_trap_small")
                columnRSCM(LAY_SYNTH, "synth.hunting_lay_box_trap")
                column(COLLAPSE_TEXT, "The box trap that you laid has collapsed.")
                column(QUEST, "quest_eaglespeak")
                column(CONSUMED, false)
                column(SMOKE_REFUSAL, "")
                column(OVERLAY_TIMER, true)
            }
            row("dbrow.hunter_trap_magic_box") {
                columnRSCM(ITEM, "obj.magic_imp_box")
                column(NAME, "magic box")
                column(LEVEL, 71)
                columnRSCM(SET_LOC, "loc.hunting_imptrap_empty")
                columnRSCM(FAILING_LOC, "loc.hunting_imptrap_failed")
                columnRSCM(BROKEN_LOC, "loc.hunting_imptrap_failed")
                columnRSCM(CHECK_SEQ, "seq.hunting_setting_trap_small")
                columnRSCM(LAY_SYNTH, "synth.hunting_lay_box_trap")
                column(COLLAPSE_TEXT, "The magic box that you laid has collapsed.")
                column(QUEST, "")
                column(CONSUMED, true)
                column(SMOKE_REFUSAL, "Imps steal so many human items, they're unlikely to be worried by human smells.")
                column(OVERLAY_TIMER, false)
            }
            row("dbrow.hunter_trap_rabbit_snare") {
                columnRSCM(ITEM, "obj.hunting_snare")
                column(NAME, "rabbit snare")
                column(LEVEL, 27)
                columnRSCM(SET_LOC, "loc.hunting_snare")
                columnRSCM(FAILING_LOC, "loc.hunting_snare_failed")
                columnRSCM(BROKEN_LOC, "loc.hunting_snare_failed")
                columnRSCM(CHECK_SEQ, "seq.human_hunting_dismantle_net")
                columnRSCM(LAY_SYNTH, "synth.hunting_lay_bird_snare")
                column(COLLAPSE_TEXT, "The rabbit snare that you laid has fallen over.")
                column(QUEST, "quest_eaglespeak")
                column(CONSUMED, false)
                column(SMOKE_REFUSAL, "")
                column(OVERLAY_TIMER, false)
            }
        }
}

object HunterTrapCreaturesTable {
    const val TRAP = 0
    const val NAME = 1
    const val NPC = 2
    const val LEVEL = 3
    const val XP_TENTHS = 4
    const val TRAPPING_LOCS = 5
    const val FULL_LOC = 6
    const val LOOT = 7
    const val LOW = 8
    const val HIGH = 9
    const val RESPAWN_TICKS = 10
    const val APPROACH_SEQ = 11
    const val BAIT = 12
    const val PET = 13
    const val PET_BASE = 14
    const val WILDERNESS_TRAP = 15
    const val BAIT_LEFTOVER = 16

    private const val SNARE = 0
    private const val BOX = 1
    private const val MAGIC_BOX = 2
    private const val RABBIT_SNARE = 3

    private const val NEXT_TICK = 1

    private class Row(
        val key: String,
        val trap: Int,
        val name: String,
        val npcs: List<String>,
        val level: Int,
        val xpTenths: Int,
        val trappingLocs: List<String>,
        val fullLoc: String,
        val loot: List<Triple<String, Int, Int>>,
        val low: Int,
        val high: Int,
        val respawnTicks: Int,
        val approachSeq: String,
        val bait: List<String> = emptyList(),
        val pet: String = "",
        val petBase: Int = 0,
        val wilderness: Boolean = false,
        val baitLeftover: String = "",
    )

    private fun bird(
        key: String,
        name: String,
        npc: String,
        level: Int,
        xpTenths: Int,
        loc: String,
        feather: String,
        low: Int,
        high: Int,
    ) =
        Row(
            key = key,
            trap = SNARE,
            name = name,
            npcs = listOf(npc),
            level = level,
            xpTenths = xpTenths,
            trappingLocs = listOf("loc.hunting_ojibway_trap_trapping_$loc"),
            fullLoc = "loc.hunting_ojibway_trap_full_$loc",
            loot =
                listOf(
                    Triple("obj.bones", 1, 1),
                    Triple("obj.spit_raw_bird_meat", 1, 1),
                    Triple(feather, 5, 10),
                ),
            low = low,
            high = high,
            respawnTicks = NEXT_TICK,
            approachSeq = "seq.bird_landontrap",
        )

    private fun boxed(
        key: String,
        name: String,
        npc: String,
        level: Int,
        xpTenths: Int,
        obj: String,
        low: Int,
        high: Int,
        respawnTicks: Int,
        approachSeq: String,
        bait: String = "",
        pet: String = "",
        petBase: Int = 0,
        wilderness: Boolean = false,
    ) =
        Row(
            key = key,
            trap = BOX,
            name = name,
            npcs = listOf(npc),
            level = level,
            xpTenths = xpTenths,
            trappingLocs = listOf("n", "e", "s", "w").map { "loc.hunting_boxtrap_trapping_${key}_$it" },
            fullLoc = "loc.hunting_boxtrap_full_$key",
            loot = listOf(Triple(obj, 1, 1)),
            low = low,
            high = high,
            respawnTicks = respawnTicks,
            approachSeq = approachSeq,
            bait = if (bait.isEmpty()) emptyList() else listOf(bait),
            pet = pet,
            petBase = petBase,
            wilderness = wilderness,
            baitLeftover = if (bait.isEmpty()) "" else EMPTY_BOWL,
        )

    private const val CHINCHOMPA_APPROACH = "seq.hunting_chinchompa_approaching"
    private const val SPICY_TOMATO = "obj.bowl_spicytomato"
    private const val SPICY_MEAT = "obj.bowl_spicymeat"
    private const val EMPTY_BOWL = "obj.bowl_empty"

    private val ROWS =
        listOf(
            bird("crimson_swift", "crimson swift", "npc.hunting_bird_jungle", 1, 340, "jungle", "obj.hunting_jungle_feather", 100, 420),
            bird("golden_warbler", "golden warbler", "npc.hunting_bird_desert", 5, 470, "desert", "obj.hunting_desert_feather", 92, 400),
            bird("copper_longtail", "copper longtail", "npc.hunting_bird_woodland", 9, 612, "woodland", "obj.hunting_woodland_feather", 85, 390),
            bird("cerulean_twitch", "cerulean twitch", "npc.hunting_bird_polar", 11, 645, "polar", "obj.hunting_polar_feather", 82, 380),
            bird("tropical_wagtail", "tropical wagtail", "npc.multicoloured_bird", 19, 952, "coloured", "obj.hunting_stripy_bird_feather", 75, 370),
            boxed("ferret", "ferret", "npc.hunting_ferret", 27, 1152, "obj.hunting_ferret", 15, 335, NEXT_TICK, "seq.hunting_ferret_approaching"),
            boxed("jerboa", "embertailed jerboa", "npc.varlamore_hunterjerboa01", 39, 1370, "obj.hunting_jerboa_tail", 120, 290, 49, "seq.jerboa_approach"),
            boxed("chinchompa", "chinchompa", "npc.hunting_chinchompa", 53, 1984, "obj.chinchompa_captured", 6, 268, NEXT_TICK, CHINCHOMPA_APPROACH, SPICY_TOMATO, "obj.skillpethunter_grey", 131_395),
            boxed("chinchompa_big", "carnivorous chinchompa", "npc.hunting_chinchompa_big", 63, 2650, "obj.chinchompa_big_captured", -78, 228, NEXT_TICK, CHINCHOMPA_APPROACH, SPICY_MEAT, "obj.skillpethunter_red", 98_373),
            boxed("chinchompa_black", "black chinchompa", "npc.hunting_chinchompa_black", 73, 3150, "obj.chinchompa_black", -78, 228, NEXT_TICK, CHINCHOMPA_APPROACH, SPICY_MEAT, "obj.skillpethunter_black", 82_758, wilderness = true),
            boxed("letvek", "letvek", "npc.hunting_letvek", 76, 2085, "obj.letvek", -78, 228, NEXT_TICK, "seq.npc_letvek_approach"),
            Row(
                key = "imp",
                trap = MAGIC_BOX,
                name = "imp",
                npcs = listOf("npc.imp", "npc.godwars_ancient_imp"),
                level = 71,
                xpTenths = 4500,
                trappingLocs = listOf("loc.hunting_imptrap_trapping"),
                fullLoc = "loc.hunting_imptrap_full",
                loot = listOf(Triple("obj.magic_imp_box_full", 1, 1)),
                low = 0,
                high = 197,
                respawnTicks = 50,
                approachSeq = "seq.hunting_imp_caught",
                bait = listOf("obj.red_bead", "obj.yellow_bead", "obj.black_bead", "obj.white_bead"),
            ),
            Row(
                key = "white_rabbit",
                trap = RABBIT_SNARE,
                name = "white rabbit",
                npcs = listOf("npc.hunting_snare_trap_npc"),
                level = 27,
                xpTenths = 1440,
                trappingLocs = listOf("loc.hunting_snare_rabbit", "loc.hunting_snare_rabbit_m"),
                fullLoc = "loc.hunting_snare_rabbit",
                loot = listOf(Triple("obj.bones", 1, 1), Triple("obj.raw_rabbit", 1, 1), Triple("obj.hunting_rabbit_foot", 1, 1)),
                low = 256,
                high = 256,
                respawnTicks = 0,
                approachSeq = "seq.hunting_snare_capture_rabbit0",
            ),
        )

    fun hunterTrapCreatures() =
        dbTable("dbtable.hunter_trap_creatures", serverOnly = true) {
            column("trap", TRAP, VarType.INT)
            column("name", NAME, VarType.STRING)
            column("npc", NPC, VarType.NPC)
            column("level", LEVEL, VarType.INT)
            column("xp_tenths", XP_TENTHS, VarType.INT)
            column("trapping_locs", TRAPPING_LOCS, VarType.LOC)
            column("full_loc", FULL_LOC, VarType.LOC)
            column("loot", LOOT, VarType.OBJ, VarType.INT, VarType.INT)
            column("low", LOW, VarType.INT)
            column("high", HIGH, VarType.INT)
            column("respawn_ticks", RESPAWN_TICKS, VarType.INT)
            column("approach_seq", APPROACH_SEQ, VarType.SEQ)
            column("bait", BAIT, VarType.OBJ)
            column("pet", PET, VarType.OBJ)
            column("pet_base", PET_BASE, VarType.INT)
            column("wilderness_trap", WILDERNESS_TRAP, VarType.BOOLEAN)
            column("bait_leftover", BAIT_LEFTOVER, VarType.OBJ)

            for (r in ROWS) {
                row("dbrow.hunter_trap_creature_${r.key}") {
                    column(TRAP, r.trap)
                    column(NAME, r.name)
                    column(NPC, r.npcs.map { ConstantProvider.getMapping(it) })
                    column(LEVEL, r.level)
                    column(XP_TENTHS, r.xpTenths)
                    column(TRAPPING_LOCS, r.trappingLocs.map { ConstantProvider.getMapping(it) })
                    columnRSCM(FULL_LOC, r.fullLoc)
                    column(
                        LOOT,
                        r.loot.flatMap { (obj, min, max) -> listOf(ConstantProvider.getMapping(obj), min, max) },
                    )
                    column(LOW, r.low)
                    column(HIGH, r.high)
                    column(RESPAWN_TICKS, r.respawnTicks)
                    columnRSCM(APPROACH_SEQ, r.approachSeq)
                    if (r.bait.isNotEmpty()) column(BAIT, r.bait.map { ConstantProvider.getMapping(it) })
                    if (r.pet.isNotEmpty()) columnRSCM(PET, r.pet)
                    column(PET_BASE, r.petBase)
                    column(WILDERNESS_TRAP, r.wilderness)
                    if (r.baitLeftover.isNotEmpty()) columnRSCM(BAIT_LEFTOVER, r.baitLeftover)
                }
            }
        }
}
