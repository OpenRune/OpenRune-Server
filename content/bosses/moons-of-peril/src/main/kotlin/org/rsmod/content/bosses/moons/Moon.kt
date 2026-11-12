package org.rsmod.content.bosses.moons

import org.rsmod.api.player.hook.NeypotzliMoon
import org.rsmod.api.table.PmoonMoonsRow
import org.rsmod.map.CoordGrid

enum class Moon(rowName: String, val hook: NeypotzliMoon, val selfDamage: SelfDamage) {
    Blood("dbrow.pmoon_moon_blood", NeypotzliMoon.Blood, SelfDamage.PerAttack),
    Blue("dbrow.pmoon_moon_blue", NeypotzliMoon.Blue, SelfDamage.PerChainHit),
    Eclipse("dbrow.pmoon_moon_eclipse", NeypotzliMoon.Eclipse, SelfDamage.OnePerSwing);

    private val row = PmoonMoonsRow.getRow(rowName)

    val label: String = row.label
    val npc: String = row.npc
    val statue: String = row.statue
    val area: String = row.area
    val deadVarbit: String = row.deadVarbit
    val killsVarp: String = row.killsVarp
    val deathSpotanim: String = row.deathSpotanim
    val southWest: CoordGrid = row.southWest
    val flatArmour: Int = row.flatArmour
    val attackAnim: String = row.attackAnim
    val attackSounds: List<Pair<String, Int>> = row.attackSounds.map { it.t0 to it.t1 }
    val attackMessage: String? = row.attackMessage
    val hitSynth: String? = row.hitSynth
    val portalModel: Int = row.portalModel
    val portalColour: Int = row.portalColour

    enum class SelfDamage {
        PerAttack,
        OnePerSwing,
        PerChainHit,
    }

    val centre: CoordGrid
        get() = southWest.translate(BOSS_SIZE / 2, BOSS_SIZE / 2)

    fun chainCurse(index: Int, dealt: Int): Int =
        if (this == Eclipse) dealt * ECLIPSE_CURSE_MULTIPLIER[index] else CHAIN_CURSE[index]

    /** Where a player lands when joining during the New Moon / Full Moon special. */
    fun landing(focus: Int): CoordGrid {
        val special =
            when (this) {
                Blood -> null
                Blue -> BLUE_STORM_LANDING.takeIf { focus == NEW_MOON }
                Eclipse ->
                    when (focus) {
                        NEW_MOON -> ECLIPSE_RAYS_LANDING
                        FULL_MOON -> ECLIPSE_CLONES_LANDING
                        else -> null
                    }
            }
        return special?.let { (dx, dz) -> southWest.translate(dx, dz) } ?: glyph(focus)
    }

    fun glyph(focus: Int): CoordGrid {
        val (dx, dz) = GLYPH_OFFSETS[focus]
        return southWest.translate(dx, dz)
    }

    fun inArena(coords: CoordGrid): Boolean =
        coords.level == southWest.level &&
            coords.x - southWest.x in ARENA_WEST..ARENA_EAST &&
            coords.z - southWest.z in ARENA_SOUTH..ARENA_NORTH

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
        const val ARENA_WEST = -12
        const val ARENA_SOUTH = -14
        const val ARENA_EAST = 18
        const val ARENA_NORTH = 15

        private val CHAIN_CURSE = listOf(2, 4, 10)
        private val ECLIPSE_CURSE_MULTIPLIER = listOf(1, 2, 5)

        val SAFE_GLYPHS = listOf(1, 2, 3, 5, 6, 7)

        /** One jaguar per glyph, standing on the arena side of it, facing the Moon. */
        val JAGUAR_OFFSETS =
            listOf(3 to -4, 0 to -4, -4 to 0, -4 to 3, 0 to 7, 3 to 7, 7 to 3, 7 to 0)
        val JAGUAR_FACING =
            listOf(0 to 1, 0 to 1, 1 to 0, 1 to 0, 0 to -1, 0 to -1, -1 to 0, -1 to 0)

        /** South-west tile of each 2x2 glyph, relative to the Moon's south-west tile. */
        val GLYPH_OFFSETS =
            listOf(3 to -2, 0 to -2, -2 to 0, -2 to 3, 0 to 5, 3 to 5, 5 to 3, 5 to 0)

        val BLUE_STORM_LANDING = 2 to -1
        val ECLIPSE_RAYS_LANDING = 5 to -3
        val ECLIPSE_CLONES_LANDING = 2 to 2

        const val LEAVE_PORTAL_MODEL = 52841
        const val LEAVE_PORTAL_COLOUR = 1355188

        val ANTECHAMBER = CoordGrid(1441, 9637, 1)
        val ANCIENT_PRISON_CAMP = CoordGrid(1351, 9582)
        val STREAMBOUND_CAMP = CoordGrid(1512, 9692)
        val EARTHBOUND_CAMP = CoordGrid(1376, 9709)
    }
}
