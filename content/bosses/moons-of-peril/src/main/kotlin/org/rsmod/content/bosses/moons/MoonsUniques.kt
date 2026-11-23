package org.rsmod.content.bosses.moons

internal object MoonsUniques {
    const val MASK_VARP = "varp.pmoon_unique_mask"
    const val CHANCE = 56
    const val SACRIFICE_CHANCE = 50

    private val SETS: Map<Moon, List<String>> =
        mapOf(
            Moon.Blood to
                listOf(
                    "obj.blood_moon_helm",
                    "obj.blood_moon_chestplate",
                    "obj.blood_moon_tassets",
                    "obj.dual_macuahuitl",
                ),
            Moon.Blue to
                listOf(
                    "obj.frost_moon_helm",
                    "obj.frost_moon_chestplate",
                    "obj.frost_moon_tassets",
                    "obj.frostmoon_spear",
                ),
            Moon.Eclipse to
                listOf(
                    "obj.eclipse_moon_helm",
                    "obj.eclipse_moon_chestplate",
                    "obj.eclipse_moon_tassets",
                    "obj.eclipse_atlatl",
                ),
        )

    val ALL: List<String> = Moon.entries.flatMap { SETS.getValue(it) }

    private fun bit(moon: Moon, index: Int): Int = 1 shl (moon.ordinal * SET_SIZE + index)

    fun pick(moon: Moon, obtained: Int, denominator: Int, nextInt: (Int) -> Int): Pick? {
        val set = SETS.getValue(moon)
        val missing = set.indices.filter { obtained and bit(moon, it) == 0 }
        val pool = missing.ifEmpty { set.indices.toList() }
        if (nextInt(denominator) != 0) return null
        val index = pool[nextInt(pool.size)]
        return Pick(set[index], obtained or bit(moon, index))
    }

    class Pick(val obj: String, val mask: Int)

    private const val SET_SIZE = 4
}
