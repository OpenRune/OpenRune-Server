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
    val attackAnim: String,
    val attackSounds: List<Pair<String, Int>> = emptyList(),
    val attackMessage: String? = null,
    val hitSynth: String? = null,
    val selfDamage: SelfDamage = SelfDamage.PerAttack,
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
        attackAnim = "seq.npc_djinn_01_combo",
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
        attackAnim = "seq.npc_djinn_03_combo",
        attackSounds =
            listOf(
                "synth.varl_pm_boss_blue_melee_swipe_01" to 50,
                "synth.varl_pm_boss_blue_melee_swipe_02" to 85,
                "synth.varl_pm_boss_blue_melee_swipe_03" to 115,
            ),
        attackMessage = "The Blue Moon pierces your defence and chills your joints!",
        hitSynth = "synth.varl_pm_boss_blue_hit_01",
        selfDamage = SelfDamage.PerChainHit,
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
        attackAnim = "seq.npc_djinn_02_combo",
        attackSounds =
            listOf(
                "synth.varl_pm_eclipse_melee_combo_swipe_04" to 48,
                "synth.varl_pm_eclipse_melee_combo_swipe_02" to 80,
                "synth.varl_pm_eclipse_melee_combo_swipe_03" to 103,
            ),
        attackMessage = "The Eclipse Moon's light pierces your defence and blinds you!",
        hitSynth = "synth.varl_pm_eclipse_damage_impact_01",
        selfDamage = SelfDamage.OnePerSwing,
    );

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
            when {
                this == Blue && focus == NEW_MOON -> 2 to -1
                this != Eclipse -> null
                focus == NEW_MOON -> 5 to -3
                focus == FULL_MOON -> 2 to 2
                else -> null
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

        val ANTECHAMBER = CoordGrid(1441, 9637, 1)
        val ANCIENT_PRISON_CAMP = CoordGrid(1351, 9582)
        val STREAMBOUND_CAMP = CoordGrid(1512, 9692)
        val EARTHBOUND_CAMP = CoordGrid(1376, 9709)
    }
}
