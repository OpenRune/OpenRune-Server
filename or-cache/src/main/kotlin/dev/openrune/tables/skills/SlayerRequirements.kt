package dev.openrune.tables.skills

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.types.SlayerNpcData

object SlayerRequirements {

    const val GEAR_TASKS = 0
    const val GEAR_VARIANT = 1
    const val GEAR_ITEM = 2
    const val GEAR_GROUP = 3
    const val GEAR_FIXED_HIT = 4
    const val GEAR_MAX_HIT = 5
    const val GEAR_HITPOINTS_PERCENT = 6
    const val GEAR_GUARANTEED = 7
    const val GEAR_PENETRATION = 8
    const val GEAR_DRAINS = 9
    const val GEAR_FIRST_HIT_ONLY = 10
    const val GEAR_REPEL_MESSAGE = 11

    const val FINISHER_TASK = 0
    const val FINISHER_VARIANT = 1
    const val FINISHER_NPCS = 2
    const val FINISHER_ITEMS = 3
    const val FINISHER_CONSUMED = 4
    const val FINISHER_AUTO_WORN = 5
    const val FINISHER_BYPASS_WORN = 6
    const val FINISHER_THRESHOLD = 7
    const val FINISHER_UNLOCK_BIT = 8
    const val FINISHER_WASTED_EARLY = 9
    const val FINISHER_THROWN = 10

    const val WEAPON_TASKS = 0
    const val WEAPON_VARIANT = 1
    const val WEAPON_NPCS = 2
    const val WEAPON_GROUP = 3
    const val WEAPON_AMMO = 4
    const val WEAPON_SPELLS = 5

    private const val SLAYER_HELMET = "content.slayer_helmet"
    private const val MIRROR_SHIELD = "content.slayer_mirror_shield"

    private const val ABERRANT_SPECTRES = 41
    private const val BANSHEES = 38
    private const val BASILISKS = 43
    private const val COCKATRICE = 44
    private const val DUST_DEVILS = 49
    private const val SMOKE_DEVILS = 95
    private const val THERMONUCLEAR_SMOKE_DEVIL_BOSS = "dbrow.slayer_boss_task_smokedevilboss"
    private const val WALL_BEASTS = 61
    private const val KILLERWATTS = 73
    private const val CAVE_HORRORS = 80

    private const val GARGOYLES = 46
    private const val ROCKSLUGS = 51
    private const val LIZARDS = 68
    private const val MUTATED_ZYGOMITES = 74

    private const val GARGOYLE_SMASHER = 0
    private const val SLUG_SALTER = 1
    private const val REPTILE_FREEZER = 2
    private const val SHROOM_SPRAYER = 3

    private const val TUROTH = 36
    private const val KURASK = 45

    fun gear(bossKey: (String) -> Int) =
        dbTable("dbtable.slayer_gear", serverOnly = true) {
            column("tasks", GEAR_TASKS, VarType.INT)
            column("variant", GEAR_VARIANT, VarType.INT)
            column("item", GEAR_ITEM, VarType.OBJ)
            column("group", GEAR_GROUP, VarType.INT)
            column("fixed_hit", GEAR_FIXED_HIT, VarType.INT)
            column("max_hit", GEAR_MAX_HIT, VarType.INT)
            column("hitpoints_percent", GEAR_HITPOINTS_PERCENT, VarType.INT)
            column("guaranteed", GEAR_GUARANTEED, VarType.BOOLEAN)
            column("penetration", GEAR_PENETRATION, VarType.INT)
            column("drains", GEAR_DRAINS, VarType.ENUM)
            column("first_hit_only", GEAR_FIRST_HIT_ONLY, VarType.BOOLEAN)
            column("repel_message", GEAR_REPEL_MESSAGE, VarType.STRING)

            gear(
                ABERRANT_SPECTRES,
                1,
                item = "obj.slayer_nosepeg",
                group = SLAYER_HELMET,
                fixedHit = 14,
                penetration = 100,
                drains = "enum.slayer_drain_nose_peg",
            )
            gear(
                ABERRANT_SPECTRES,
                2,
                item = "obj.slayer_nosepeg",
                group = SLAYER_HELMET,
                drains = "enum.slayer_drain_nose_peg",
            )
            gear(
                BANSHEES,
                1,
                item = "obj.slayer_earmuffs",
                group = SLAYER_HELMET,
                fixedHit = 6,
                drains = "enum.slayer_drain_earmuffs",
            )
            gear(
                BANSHEES,
                2,
                item = "obj.slayer_earmuffs",
                group = SLAYER_HELMET,
                drains = "enum.slayer_drain_earmuffs",
            )
            gear(
                DUST_DEVILS,
                1,
                item = "obj.slayer_facemask",
                group = SLAYER_HELMET,
                fixedHit = 16,
                drains = "enum.slayer_drain_dust_devil",
            )
            gear(
                DUST_DEVILS,
                2,
                item = "obj.slayer_facemask",
                group = SLAYER_HELMET,
                drains = "enum.slayer_drain_dust_devil",
            )
            gear(
                SMOKE_DEVILS,
                1,
                extraTask = bossKey(THERMONUCLEAR_SMOKE_DEVIL_BOSS),
                item = "obj.slayer_facemask",
                group = SLAYER_HELMET,
                drains = "enum.slayer_drain_smoke_devil",
            )
            gear(
                COCKATRICE,
                1,
                group = MIRROR_SHIELD,
                drains = "enum.slayer_drain_gaze_cockatrice",
            )
            gear(
                BASILISKS,
                1,
                group = MIRROR_SHIELD,
                drains = "enum.slayer_drain_gaze_basilisk",
            )
            gear(
                BASILISKS,
                2,
                group = MIRROR_SHIELD,
                drains = "enum.slayer_drain_gaze_basilisk_knight",
            )
            gear(
                WALL_BEASTS,
                1,
                item = "obj.wallbeast_spike_helmet",
                group = SLAYER_HELMET,
                maxHit = 18,
                guaranteed = true,
                firstHitOnly = true,
                repelMessage = "Your helmet repels the wall beast!",
            )
            gear(CAVE_HORRORS, 1, item = "obj.witchwood_icon", hitpointsPercent = 10)
            gear(KILLERWATTS, 1, item = "obj.slayer_boots", maxHit = 15)
        }

    fun finishers(npcs: Map<Int, SlayerNpcData>) =
        dbTable("dbtable.slayer_finishers", serverOnly = true) {
            column("task", FINISHER_TASK, VarType.INT)
            column("variant", FINISHER_VARIANT, VarType.INT)
            column("npcs", FINISHER_NPCS, VarType.NPC)
            column("items", FINISHER_ITEMS, VarType.ENUM)
            column("consumed", FINISHER_CONSUMED, VarType.OBJ)
            column("auto_worn", FINISHER_AUTO_WORN, VarType.OBJ)
            column("bypass_worn", FINISHER_BYPASS_WORN, VarType.OBJ)
            column("threshold", FINISHER_THRESHOLD, VarType.INT)
            column("unlock_bit", FINISHER_UNLOCK_BIT, VarType.INT)
            column("wasted_early", FINISHER_WASTED_EARLY, VarType.BOOLEAN)
            column("thrown", FINISHER_THROWN, VarType.OBJ)

            finisher(
                npcs,
                GARGOYLES,
                items = "enum.slayer_finisher_gargoyle_items",
                consumed = "obj.slayer_rock_thrownhammer",
                autoWorn = "obj.granite_hammer",
                thrown = "obj.slayer_rock_thrownhammer",
                threshold = 9,
                unlockBit = GARGOYLE_SMASHER,
            )
            finisher(
                npcs,
                ROCKSLUGS,
                items = "enum.slayer_finisher_rockslug_items",
                consumed = "obj.slayer_bag_of_salt",
                bypassWorn = "obj.olaf2_brine_sabre",
                threshold = 4,
                unlockBit = SLUG_SALTER,
                wastedEarly = true,
            )
            finisher(
                npcs,
                LIZARDS,
                items = "enum.slayer_finisher_desert_lizard_items",
                consumed = "obj.slayer_icy_water",
                threshold = 4,
                unlockBit = REPTILE_FREEZER,
                wastedEarly = true,
            )
            finisher(
                npcs,
                MUTATED_ZYGOMITES,
                items = "enum.slayer_finisher_zygomite_items",
                threshold = 8,
                unlockBit = SHROOM_SPRAYER,
            )
        }

    fun weaponRules(npcs: Map<Int, SlayerNpcData>) =
        dbTable("dbtable.slayer_weapon_rules", serverOnly = true) {
            column("tasks", WEAPON_TASKS, VarType.INT)
            column("variant", WEAPON_VARIANT, VarType.INT)
            column("npcs", WEAPON_NPCS, VarType.NPC)
            column("weapons", WEAPON_GROUP, VarType.INT)
            column("ammo", WEAPON_AMMO, VarType.ENUM)
            column("spells", WEAPON_SPELLS, VarType.ENUM)

            row("dbrow.slayer_weapon_${TUROTH}_1") {
                column(WEAPON_TASKS, TUROTH, KURASK)
                column(WEAPON_VARIANT, 1)
                column(WEAPON_NPCS, *npcsOf(npcs, listOf(TUROTH, KURASK), 1))
                columnRSCM(WEAPON_GROUP, "content.slayer_leaf_weapon")
                columnRSCM(WEAPON_AMMO, "enum.slayer_leafy_ammo")
                columnRSCM(WEAPON_SPELLS, "enum.slayer_leafy_spells")
            }
        }

    private fun DBTableBuilder.gear(
        task: Int,
        variant: Int,
        extraTask: Int? = null,
        item: String? = null,
        group: String? = null,
        fixedHit: Int? = null,
        maxHit: Int? = null,
        hitpointsPercent: Int? = null,
        guaranteed: Boolean = false,
        penetration: Int = 0,
        drains: String? = null,
        firstHitOnly: Boolean = false,
        repelMessage: String? = null,
    ) {
        row("dbrow.slayer_gear_${task}_$variant") {
            column(GEAR_TASKS, *listOfNotNull(task, extraTask).toTypedArray())
            column(GEAR_VARIANT, variant)
            item?.let { columnRSCM(GEAR_ITEM, it) }
            group?.let { columnRSCM(GEAR_GROUP, it) }
            fixedHit?.let { column(GEAR_FIXED_HIT, it) }
            maxHit?.let { column(GEAR_MAX_HIT, it) }
            hitpointsPercent?.let { column(GEAR_HITPOINTS_PERCENT, it) }
            if (guaranteed) column(GEAR_GUARANTEED, true)
            if (penetration > 0) column(GEAR_PENETRATION, penetration)
            drains?.let { columnRSCM(GEAR_DRAINS, it) }
            if (firstHitOnly) column(GEAR_FIRST_HIT_ONLY, true)
            repelMessage?.let { column(GEAR_REPEL_MESSAGE, it) }
        }
    }

    private fun npcsOf(data: Map<Int, SlayerNpcData>, tasks: List<Int>, variant: Int): Array<Int> =
        data
            .filter { (_, npc) -> npc.variant == variant && npc.keys.any { it in tasks } }
            .keys
            .sorted()
            .toTypedArray()

    private fun DBTableBuilder.finisher(
        data: Map<Int, SlayerNpcData>,
        task: Int,
        items: String,
        threshold: Int,
        unlockBit: Int,
        consumed: String? = null,
        autoWorn: String? = null,
        bypassWorn: String? = null,
        wastedEarly: Boolean = false,
        thrown: String? = null,
    ) {
        row("dbrow.slayer_finisher_${task}_1") {
            column(FINISHER_TASK, task)
            column(FINISHER_VARIANT, 1)
            column(FINISHER_NPCS, *npcsOf(data, listOf(task), 1))
            columnRSCM(FINISHER_ITEMS, items)
            consumed?.let { columnRSCM(FINISHER_CONSUMED, it) }
            autoWorn?.let { columnRSCM(FINISHER_AUTO_WORN, it) }
            bypassWorn?.let { columnRSCM(FINISHER_BYPASS_WORN, it) }
            column(FINISHER_THRESHOLD, threshold)
            column(FINISHER_UNLOCK_BIT, unlockBit)
            if (wastedEarly) column(FINISHER_WASTED_EARLY, true)
            thrown?.let { columnRSCM(FINISHER_THROWN, it) }
        }
    }
}
