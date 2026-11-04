package org.rsmod.content.minigames.motherlodemine.configs

import dev.openrune.types.ItemServerType
import dev.openrune.types.SequenceServerType
import org.rsmod.api.config.objParam
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

object MotherlodeConstants {

    const val VEIN_SINGLE = "loc.motherlode_ore_single"
    const val VEIN_LEFT = "loc.motherlode_ore_left"
    const val VEIN_MIDDLE = "loc.motherlode_ore_middle"
    const val VEIN_RIGHT = "loc.motherlode_ore_right"

    val ORE_VEINS: List<String> get() = DEPLETED_VEIN_OF.keys.toList()

    val DEPLETED_VEIN_OF: Map<String, String> get() = MotherlodeData.depletedVeins

    const val CONTENT_VEIN = "content.motherlode_vein"

    const val VEIN_LEVEL_REQ = 30
    const val VEIN_XP = 60.0
    const val VEIN_SUCCESS_LOW = 60
    const val VEIN_SUCCESS_HIGH = 105
    const val MAX_SKILL_LEVEL = 99

    val LOWER_VEIN_DEPLETE_TICKS = 40..45
    val UPPER_VEIN_DEPLETE_TICKS = 60..67

    // The raised ledge uses LINK_BELOW tiles, so players and veins remain on plane zero.
    fun CoordGrid.isMotherlodeUpperFloor(): Boolean = this in MotherlodeData.upperFloorTiles

    // TODO: distinguish lower-inner veins' 150-tick respawn; all veins currently use 100.
    const val VEIN_RESPAWN_TICKS = 100

    const val ROCKFALL_1 = "loc.motherlode_rockfall_1"
    const val ROCKFALL_2 = "loc.motherlode_rockfall_2"
    val ROCKFALLS = listOf(ROCKFALL_1, ROCKFALL_2)
    const val CONTENT_ROCKFALL = "content.motherlode_rockfall"

    const val ROCKFALL_XP = 10.0
    const val ROCKFALL_RESPAWN_TICKS = 30

    const val ROCKFALL_DAMAGE_PERCENT = 4
    const val ROCKFALL_DAMAGE_FLAT = 1

    const val HOPPER_LOC = "loc.motherlode_hopper"
    const val SACK_EMPTY_LOC = "loc.motherlode_sack_empty"
    const val SACK_FULL_LOC = "loc.motherlode_sack_full"

    const val SACK_CAPACITY_NORMAL = 108
    const val SACK_CAPACITY_BIG = 189

    const val SACK_FULL_MESSAGE =
        "Some ore is ready to be collected from the sack. It's getting full."

    const val VEIN_SWING_MESSAGE = "You swing your pick at the rock."

    const val VEIN_PAYDIRT_MESSAGE = "You manage to mine some pay-dirt."

    const val INV_FULL_MESSAGE = "Your inventory is too full to hold any more pay-dirt."

    const val SACK_READY_MESSAGE = "Some ore is ready to be collected from the sack."

    const val SACK_READY_FULL_MESSAGE =
        "Some ore is ready to be collected from the sack. The sack is now full."

    const val SACK_COLLECT_MESSAGE = "You collect your ore from the sack."

    const val SACK_COLLECT_EMPTY_MESSAGE = "You collect your ore from the sack. The sack is now empty."

    const val SACK_EMPTY_MESSAGE = "The sack is empty."

    const val HOPPER_NOTHING_MESSAGE = "You have nothing to deposit."

    // Reject deposits while the previous batch is in transit; preserve pay-dirt and omit splash sound.
    const val HOPPER_BUSY_MESSAGE =
        "You've already got some pay-dirt in the machine. You can put more in once the last batch comes out."

    const val MACHINE_BROKEN_MESSAGE =
        "The machine will need to be repaired before your pay-dirt can be cleaned."

    const val MAX_GOLDEN_NUGGETS_PER_DEPOSIT = 3

    const val NUGGET_NUMERATOR = 8
    const val NUGGET_DENOMINATOR = 256

    const val OBJ_RUNITE_ORE = "obj.runite_ore"
    const val OBJ_ADAMANTITE_ORE = "obj.adamantite_ore"
    const val OBJ_MITHRIL_ORE = "obj.mithril_ore"
    const val OBJ_GOLD_ORE = "obj.gold_ore"
    const val OBJ_COAL = "obj.coal"

    internal val ORE_TIERS by lazy { MotherlodeData.ores.filter { it.rollOrder > 0 }.sortedBy { it.rollOrder } }

    val payloadIdOf: Map<String, Int> by lazy { MotherlodeData.ores.associate { it.obj to it.item.id } }

    val payloadObjOf: Map<Int, String> by lazy { MotherlodeData.ores.associate { it.item.id to it.obj } }

    private val arrivalXpByPayload by lazy { MotherlodeData.ores.associate { it.item.id to it.xp } }

    fun arrivalXpFor(payload: Int): Double = arrivalXpByPayload[payload] ?: 0.0

    val sackTierOrderIds: List<Int> by lazy { MotherlodeData.ores.sortedBy { it.sackOrder }.map { it.item.id } }

    const val OBJ_PAYDIRT = "obj.paydirt"
    const val OBJ_NUGGET = "obj.motherlode_nugget"

    const val NPC_PERCY = "npc.motherlode_percy"

    // Pay-dirt is a concrete NPC; forced routing must remain within the water lane.
    const val NPC_PAYDIRT_FLOATER = "npc.motherlode_paydirt"

    val FLOAT_CHANNEL: List<CoordGrid> =
        (5671 downTo 5660).map { z -> CoordGrid(x = 3748, z = z, level = 0) }

    // Include the tick that removes the floater as it enters the sack.
    val TRANSIT_TICKS = FLOAT_CHANNEL.size

    const val FLOATER_CAP = 16

    const val STRUT_FIXED_LOC = "loc.motherlode_wheel_strut_fixed"
    const val STRUT_BROKEN_LOC = "loc.motherlode_wheel_strut_broken"
    const val WHEEL_FIXED_LOC = "loc.motherlode_wheel_fixed"
    const val WHEEL_BROKEN_LOC = "loc.motherlode_wheel_broken"

    const val STRUT_ROUND_TICKS = 97

    // Strut break-round weights are unknown; selection is uniform.
    val STRUT_BREAK_ROUNDS = 2..4

    const val STRUT_REPAIR_LOW = 30
    const val STRUT_REPAIR_HIGH = 70
    const val STRUT_REPAIR_XP_MULTIPLIER = 1.5

    val HAMMER_TYPES = listOf("obj.hammer", "obj.imcando_hammer", "obj.imcando_hammer_offhand")

    const val STRUT_REPAIR_ANIM = "seq.human_hammer_hit"
    const val STRUT_REPAIR_ANIM_IMCANDO = "seq.human_hammer_hit_imcando_hammer"

    const val SCAN_X1 = 3710
    const val SCAN_X2 = 3780
    const val SCAN_Z1 = 5630
    const val SCAN_Z2 = 5700

    const val CRATE_LOC = "loc.crate2_old"
    const val OBJ_HAMMER = "obj.hammer"
    const val OBJ_BRONZE_PICKAXE = "obj.bronze_pickaxe"

    val CRATE_CONTENTS: Map<CoordGrid, String> =
        mapOf(
            CoordGrid(3752, 5674, 0) to OBJ_HAMMER,
            CoordGrid(3752, 5664, 0) to OBJ_HAMMER,
            CoordGrid(3753, 5659, 0) to OBJ_HAMMER,
            CoordGrid(3755, 5665, 0) to OBJ_BRONZE_PICKAXE,
            CoordGrid(3756, 5668, 0) to OBJ_BRONZE_PICKAXE,
            CoordGrid(3758, 5671, 0) to OBJ_BRONZE_PICKAXE,
        )

    const val SHOP_INV = "inv.motherlode_shop"
    const val CURRENCY_NUGGET = "currency.golden_nugget"

    const val OBJ_PROSPECTOR_HAT = "obj.motherlode_reward_hat"
    const val OBJ_PROSPECTOR_TOP = "obj.motherlode_reward_top"
    const val OBJ_PROSPECTOR_LEGS = "obj.motherlode_reward_legs"
    const val OBJ_PROSPECTOR_BOOTS = "obj.motherlode_reward_boots"
    const val OBJ_COAL_BAG = "obj.coal_bag"
    const val OBJ_SOFTCLAY_PACK = "obj.pack_softclay"
    // TODO: implement gem-bag handling before adding it to the shop.

    const val UPPER_FLOOR_COST = 100
    const val UPPER_FLOOR_MINING_REQ = 57
    const val SUPER_HOPPER_COST = 50
    const val SACK_UPGRADE_COST = 200

    const val VARBIT_BIGGER_SACK = "varbit.motherlode_biggersack"

    // Sack appearance is varbit-driven: zero is empty, positive stored totals select full.
    const val VARBIT_SACK_TRANSMIT = "varbit.motherlode_sack_transmit"

    const val VARBIT_IN_RESTRICTED_AREA = "varbit.motherlode_in_restricted_area"

    var Player.motherlodeInRestrictedArea: Boolean by boolVarBit(VARBIT_IN_RESTRICTED_AREA)

    // MusicAreaScript owns this area's enter/exit hooks; a second binding would fail startup.
    const val AREA_MOTHERLODE = "area.motherlode_mine"

    val ItemServerType.pickaxeSwingAnim: SequenceServerType by objParam(params.skill_anim)
}
