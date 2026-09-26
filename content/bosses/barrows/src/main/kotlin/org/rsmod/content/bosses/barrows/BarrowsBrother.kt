package org.rsmod.content.bosses.barrows

import org.rsmod.map.CoordGrid

enum class BarrowsBrother(
    val npc: String,
    val killedVarbit: String,
    val moundCenter: CoordGrid,
    val chamber: CoordGrid,
    val bySarcophagus: CoordGrid,
    val sarcophagus: String,
    val staircase: String,
    val combatLevel: Int,
    val armour: List<String>,
    val cryptNodModel: Int,
    val tunnelNodModel: Int,
) {
    AHRIM(
        npc = "npc.barrows_ahrim",
        killedVarbit = "varbit.barrows_killed_ahrim",
        moundCenter = CoordGrid(3565, 3288, 0),
        chamber = CoordGrid(3557, 9703, 3),
        bySarcophagus = CoordGrid(3557, 9699, 3),
        sarcophagus = "loc.barrow_ahrim_sarcophagus",
        staircase = "loc.barrows_stairs_ahrim",
        combatLevel = 98,
        armour =
            listOf(
                "obj.barrows_ahrim_head",
                "obj.barrows_ahrim_weapon",
                "obj.barrows_ahrim_body",
                "obj.barrows_ahrim_legs",
            ),
        cryptNodModel = 6737,
        tunnelNodModel = 29228,
    ),
    DHAROK(
        npc = "npc.barrows_dharok",
        killedVarbit = "varbit.barrows_killed_dharok",
        moundCenter = CoordGrid(3575, 3298, 0),
        chamber = CoordGrid(3556, 9718, 3),
        bySarcophagus = CoordGrid(3555, 9716, 3),
        sarcophagus = "loc.barrow_dharok_sarcophagus",
        staircase = "loc.barrows_stairs_dharok",
        combatLevel = 115,
        armour =
            listOf(
                "obj.barrows_dharok_head",
                "obj.barrows_dharok_weapon",
                "obj.barrows_dharok_body",
                "obj.barrows_dharok_legs",
            ),
        cryptNodModel = 6738,
        tunnelNodModel = 29225,
    ),
    GUTHAN(
        npc = "npc.barrows_guthan",
        killedVarbit = "varbit.barrows_killed_guthan",
        moundCenter = CoordGrid(3577, 3281, 0),
        chamber = CoordGrid(3534, 9704, 3),
        bySarcophagus = CoordGrid(3539, 9705, 3),
        sarcophagus = "loc.barrow_guthan_sarcophagus",
        staircase = "loc.barrows_stairs_guthan",
        combatLevel = 115,
        armour =
            listOf(
                "obj.barrows_guthan_head",
                "obj.barrows_guthan_weapon",
                "obj.barrows_guthan_body",
                "obj.barrows_guthan_legs",
            ),
        cryptNodModel = 6739,
        tunnelNodModel = 29224,
    ),
    KARIL(
        npc = "npc.barrows_karil",
        killedVarbit = "varbit.barrows_killed_karil",
        moundCenter = CoordGrid(3565, 3275, 0),
        chamber = CoordGrid(3546, 9684, 3),
        bySarcophagus = CoordGrid(3549, 9683, 3),
        sarcophagus = "loc.barrow_karil_sarcophagus",
        staircase = "loc.barrows_stairs_karil",
        combatLevel = 98,
        armour =
            listOf(
                "obj.barrows_karil_head",
                "obj.barrows_karil_weapon",
                "obj.barrows_karil_body",
                "obj.barrows_karil_legs",
            ),
        cryptNodModel = 6740,
        tunnelNodModel = 29226,
    ),
    TORAG(
        npc = "npc.barrows_torag",
        killedVarbit = "varbit.barrows_killed_torag",
        moundCenter = CoordGrid(3553, 3283, 0),
        chamber = CoordGrid(3568, 9683, 3),
        bySarcophagus = CoordGrid(3568, 9686, 3),
        sarcophagus = "loc.barrow_torag_sarcophagus",
        staircase = "loc.barrows_stairs_torag",
        combatLevel = 115,
        armour =
            listOf(
                "obj.barrows_torag_head",
                "obj.barrows_torag_weapon",
                "obj.barrows_torag_body",
                "obj.barrows_torag_legs",
            ),
        cryptNodModel = 6741,
        tunnelNodModel = 29227,
    ),
    VERAC(
        npc = "npc.barrows_verac",
        killedVarbit = "varbit.barrows_killed_verac",
        moundCenter = CoordGrid(3556, 3298, 0),
        chamber = CoordGrid(3578, 9706, 3),
        bySarcophagus = CoordGrid(3575, 9706, 3),
        sarcophagus = "loc.barrow_verac_sarcophagus",
        staircase = "loc.barrows_stairs_verac",
        combatLevel = 115,
        armour =
            listOf(
                "obj.barrows_verac_head",
                "obj.barrows_verac_weapon",
                "obj.barrows_verac_body",
                "obj.barrows_verac_legs",
            ),
        cryptNodModel = 6742,
        tunnelNodModel = 29223,
    );

    companion object {
        val entries: List<BarrowsBrother> = values().toList()
    }
}
