package org.rsmod.content.bosses.moons

import org.rsmod.map.CoordGrid

enum class Moon(
    val label: String,
    val npc: String,
    val statue: String,
    val area: String,
    val deadVarbit: String,
    val killsVarp: String,
    val deathSpotanim: String,
    val southWest: CoordGrid,
    val flatArmour: Int,
) {
    Blood(
        label = "Blood Moon",
        npc = "npc.pmoon_boss_blood_moon",
        statue = "loc.pmoon_entry_statue_blood",
        area = "area.pmoon_blood_arena",
        deadVarbit = "varbit.pmoon_boss_blood_dead",
        killsVarp = "varp.pmoon_blood_kills",
        deathSpotanim = "spotanim.pmoon_boss_fake_blood_death",
        southWest = CoordGrid(1390, 9630),
        flatArmour = -2,
    ),
    Blue(
        label = "Blue Moon",
        npc = "npc.pmoon_boss_blue_moon",
        statue = "loc.pmoon_entry_statue_blue",
        area = "area.pmoon_blue_arena",
        deadVarbit = "varbit.pmoon_boss_blue_dead",
        killsVarp = "varp.pmoon_blue_kills",
        deathSpotanim = "spotanim.pmoon_boss_fake_blue_death",
        southWest = CoordGrid(1438, 9678),
        flatArmour = -5,
    ),
    Eclipse(
        label = "Eclipse Moon",
        npc = "npc.pmoon_boss_eclipse_moon",
        statue = "loc.pmoon_entry_statue_eclipse",
        area = "area.pmoon_eclipse_arena",
        deadVarbit = "varbit.pmoon_boss_eclipse_dead",
        killsVarp = "varp.pmoon_eclipse_kills",
        deathSpotanim = "spotanim.pmoon_boss_fake_eclipse_death",
        southWest = CoordGrid(1486, 9630),
        flatArmour = 6,
    );

    val centre: CoordGrid
        get() = southWest.translate(BOSS_SIZE / 2, BOSS_SIZE / 2)

    fun glyph(focus: Int): CoordGrid {
        val (dx, dz) = GLYPH_OFFSETS[focus]
        return southWest.translate(dx, dz)
    }

    fun onGlyph(focus: Int, coords: CoordGrid): Boolean {
        val sw = glyph(focus)
        return coords.level == sw.level &&
            coords.x in sw.x until sw.x + GLYPH_SIZE &&
            coords.z in sw.z until sw.z + GLYPH_SIZE
    }

    companion object {
        const val MAX_HITPOINTS = 500
        const val BOSS_SIZE = 5
        const val GLYPH_SIZE = 2
        const val NEW_MOON = 0
        const val FULL_MOON = 4

        /** South-west tile of each 2x2 glyph, relative to the Moon's south-west tile. */
        val GLYPH_OFFSETS =
            listOf(3 to -2, 0 to -2, -2 to 0, -2 to 3, 0 to 5, 3 to 5, 5 to 3, 5 to 0)

        val ANTECHAMBER = CoordGrid(1441, 9637, 1)
        val ANCIENT_PRISON_CAMP = CoordGrid(1351, 9582)
        val STREAMBOUND_CAMP = CoordGrid(1512, 9692)
        val EARTHBOUND_CAMP = CoordGrid(1376, 9709)
    }
}
