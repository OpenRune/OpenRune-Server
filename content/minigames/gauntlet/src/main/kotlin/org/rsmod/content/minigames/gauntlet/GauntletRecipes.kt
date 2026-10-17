package org.rsmod.content.minigames.gauntlet

internal data class GauntletRecipe(
    val product: String,
    val shards: Int,
    val materials: List<Pair<String, Int>>,
    val xp: Int,
)

internal object GauntletRecipes {
    private val ARMOUR = listOf("helmet", "chestplate", "platelegs")
    private val WEAPONS = listOf("melee" to "melee_component", "magic" to "magic_component", "ranged" to "ranged_component")
    private val BODY_PIECE = "chestplate"

    fun resolve(corrupted: Boolean, holds: (String) -> Boolean): List<GauntletRecipe> {
        fun obj(name: String) = gauntletObj(name, corrupted)

        fun nextTier(piece: String): Int {
            val highest = (1..3).lastOrNull { holds(obj("${piece}_t$it")) } ?: 0
            return (highest + 1).coerceAtMost(3)
        }

        fun materials(shards: Int, mats: Int, previous: String?): List<Pair<String, Int>> = buildList {
            add(obj("crystal_shard") to shards)
            add(obj("ore") to mats)
            add(obj("bark") to mats)
            add(obj("fibre") to mats)
            previous?.let { add(it to 1) }
        }

        val armour =
            ARMOUR.map { piece ->
                val tier = nextTier(piece)
                val previous = if (tier > 1) obj("${piece}_t${tier - 1}") else null
                val heavy = piece == BODY_PIECE
                val (shards, mats, xp) =
                    when (tier) {
                        1 -> Triple(50, 1, 20)
                        2 -> Triple(if (heavy) 100 else 50, if (heavy) 2 else 1, 30)
                        else -> Triple(100, 2, 40)
                    }
                GauntletRecipe(obj("${piece}_t$tier"), shards, materials(shards, mats, previous).filter { it.second > 0 }, xp)
            }

        val weapons =
            WEAPONS.map { (style, component) ->
                val tier = nextTier(style)
                val previous = if (tier > 1) obj("${style}_t${tier - 1}") else null
                val (mats, xp) =
                    when (tier) {
                        1 -> listOf(obj("generic_component") to 1) to 10
                        2 -> listOfNotNull(obj("crystal_shard") to 50, previous?.let { it to 1 }) to 30
                        else -> listOfNotNull(previous?.let { it to 1 }, obj(component) to 1) to 0
                    }
                GauntletRecipe(obj("${style}_t$tier"), 0, mats, xp)
            }

        fun shardsAnd(vararg extra: Pair<String, Int>, shards: Int) =
            listOf(obj("crystal_shard") to shards) + extra

        return listOf(
            GauntletRecipe(obj("teleport_crystal"), 50, shardsAnd(shards = 50), 20),
            GauntletRecipe(obj("vial_empty"), 10, shardsAnd(shards = 10), 5),
            armour[0],
            armour[1],
            armour[2],
            weapons[0],
            weapons[1],
            weapons[2],
            GauntletRecipe(obj("combo_food"), 10, shardsAnd(obj("food") to 1, shards = 10), 5),
            GauntletRecipe(obj("escape_crystal"), 200, shardsAnd(shards = 200), 100),
        )
    }
}
