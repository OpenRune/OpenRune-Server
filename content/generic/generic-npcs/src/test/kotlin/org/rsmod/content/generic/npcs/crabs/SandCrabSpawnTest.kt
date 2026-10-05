package org.rsmod.content.generic.npcs.crabs

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Map spawns are checked against the coordinates the wiki lists for each Sand Crab location. */
class SandCrabSpawnTest {
    @Test
    fun `hosidius southern coast has the wiki spawns`() {
        assertEquals(56, hosidiusSouthCoast.size)
        assertSpawned(hosidiusSouthCoast)
    }

    @Test
    fun `crabclaw isle has the wiki spawns`() {
        assertEquals(47, crabclawIsle.size)
        assertSpawned(crabclawIsle)
    }

    @Test
    fun `the other verified kourend sand crab sites have the wiki spawns`() {
        assertSpawned(hosidiusVinery)
        assertSpawned(crabclawCaves)
        assertSpawned(isleOfSouls)
    }

    @Test
    fun `every sand crab spawn is a disguised rock`() {
        for (spawn in spawns) {
            assertTrue(spawn.npc.endsWith("_inactive"), "${spawn.npc} at ${spawn.x},${spawn.z}")
        }
    }

    @Test
    fun `no two sand crabs share a tile`() {
        val tiles = spawns.map { Triple(it.x, it.z, it.level) }
        assertEquals(tiles.size, tiles.toSet().size)
    }

    private fun assertSpawned(expected: List<Pair<Int, Int>>) {
        val actual = spawns.filter { it.level == 0 }.map { it.x to it.z }.toSet()
        val missing = expected.filter { it !in actual }
        assertTrue(missing.isEmpty(), "missing sand crab spawns: $missing")
    }

    private data class Spawn(val npc: String, val level: Int, val x: Int, val z: Int)

    private companion object {
        val pattern =
            Regex(
                """npc = "npc\.(zeah_sandcrab\w*)"\s+coords = "(\d+)_(\d+)_(\d+)_(\d+)_(\d+)"""
            )

        val spawns: List<Spawn> by lazy {
            val files = File(".data/raw-cache/map/npcs").listFiles { it.extension == "toml" }!!
            files.flatMap { file ->
                pattern.findAll(file.readText()).map { match ->
                    val (npc, level, rx, rz, lx, lz) = match.destructured
                    val x = rx.toInt() * 64 + lx.toInt()
                    val z = rz.toInt() * 64 + lz.toInt()
                    Spawn(npc, level.toInt(), x, z)
                }
            }
        }

        /** Wiki: Sand Crab, Hosidius southern coast (56). */
        val hosidiusSouthCoast =
            listOf(
                1815 to 3456, 1822 to 3457, 1833 to 3457, 1834 to 3459, 1831 to 3460, 1773 to 3460,
                1817 to 3461, 1844 to 3461, 1772 to 3461, 1698 to 3461, 1842 to 3462, 1774 to 3462,
                1802 to 3464, 1844 to 3464, 1731 to 3464, 1748 to 3464, 1772 to 3465, 1786 to 3466,
                1694 to 3466, 1766 to 3467, 1790 to 3467, 1737 to 3468, 1750 to 3468, 1764 to 3468,
                1766 to 3468, 1775 to 3468, 1777 to 3468, 1797 to 3469, 1733 to 3469, 1738 to 3469,
                1757 to 3469, 1766 to 3469, 1776 to 3469, 1791 to 3469, 1733 to 3470, 1748 to 3470,
                1750 to 3470, 1694 to 3470, 1753 to 3471, 1787 to 3472, 1695 to 3472, 1741 to 3473,
                1688 to 3474, 1675 to 3477, 1676 to 3477, 1674 to 3478, 1684 to 3478, 1685 to 3479,
                1690 to 3479, 1698 to 3479, 1696 to 3482, 1697 to 3486, 1683 to 3487, 1694 to 3487,
                1685 to 3489, 1686 to 3490,
            )

        /** Wiki: Sand Crab, Crabclaw Isle (47). */
        val crabclawIsle =
            listOf(
                1786 to 3403, 1785 to 3404, 1787 to 3404, 1787 to 3405, 1780 to 3406, 1779 to 3407,
                1768 to 3408, 1780 to 3408, 1781 to 3408, 1767 to 3409, 1769 to 3409, 1787 to 3409,
                1752 to 3410, 1756 to 3410, 1749 to 3411, 1748 to 3412, 1761 to 3412, 1774 to 3412,
                1780 to 3412, 1783 to 3412, 1749 to 3413, 1750 to 3413, 1751 to 3424, 1750 to 3425,
                1752 to 3425, 1751 to 3426, 1763 to 3426, 1766 to 3427, 1780 to 3427, 1754 to 3429,
                1763 to 3429, 1776 to 3429, 1778 to 3429, 1777 to 3432, 1781 to 3432, 1780 to 3437,
                1779 to 3438, 1781 to 3438, 1757 to 3439, 1759 to 3439, 1780 to 3439, 1758 to 3440,
                1764 to 3444, 1763 to 3445, 1764 to 3446, 1765 to 3446, 1769 to 3447,
            )

        /** Wiki: Sand Crab, east of the Hosidius vinery (22). */
        val hosidiusVinery =
            listOf(
                1863 to 3536, 1861 to 3537, 1857 to 3542, 1863 to 3542, 1864 to 3544, 1868 to 3545,
                1874 to 3546, 1858 to 3547, 1879 to 3551, 1860 to 3552, 1870 to 3553, 1876 to 3554,
                1868 to 3555, 1876 to 3556, 1869 to 3557, 1874 to 3558, 1859 to 3559, 1861 to 3560,
                1863 to 3561, 1868 to 3561, 1871 to 3565, 1870 to 3566,
            )

        /** Wiki: Sand Crab, Crabclaw Caves (14). */
        val crabclawCaves =
            listOf(
                1678 to 9821, 1680 to 9821, 1672 to 9822, 1680 to 9822, 1671 to 9823, 1678 to 9823,
                1669 to 9825, 1670 to 9826, 1675 to 9826, 1672 to 9829, 1673 to 9831, 1667 to 9832,
                1669 to 9832, 1667 to 9833,
            )

        /** Wiki: Sand Crab, Isle of Souls (18). */
        val isleOfSouls =
            listOf(
                2285 to 2785, 2297 to 2785, 2252 to 2786, 2290 to 2789, 2257 to 2791, 2261 to 2791,
                2282 to 2791, 2267 to 2792, 2276 to 2792, 2278 to 2792, 2286 to 2792, 2287 to 2796,
                2273 to 2798, 2282 to 2804, 2284 to 2804, 2283 to 2805, 2277 to 2812, 2281 to 2815,
            )
    }
}
