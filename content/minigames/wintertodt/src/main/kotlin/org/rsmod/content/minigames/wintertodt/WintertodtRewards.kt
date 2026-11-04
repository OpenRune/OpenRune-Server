package org.rsmod.content.minigames.wintertodt

import kotlin.math.floor
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.WintertodtRewardRow
import org.rsmod.game.entity.Player

internal data class WintertodtReward(val item: String, val count: Int, val noted: Boolean = false)

internal object WintertodtRewards {
    private const val PHOENIX = "obj.phoenixpet"
    private const val DRAGON_AXE = "obj.dragon_axe"
    private const val TOME = "obj.tome_of_fire_uncharged"
    private const val WARM_GLOVES = "obj.pyromancer_gloves"
    private const val MAGIC_SEED = "obj.magic_tree_seed"
    private const val TORCH = "obj.wint_torch"
    private const val TORSTOL_SEED = "obj.torstol_seed"
    private const val BURNT_PAGE = "obj.wint_burnt_page"

    private val PYRO_PIECES =
        listOf(
            "obj.pyromancer_top",
            "obj.pyromancer_hood",
            "obj.pyromancer_bottom",
            "obj.pyromancer_boots",
        )

    val UNIQUES: Set<String> =
        setOf(
            PHOENIX,
            DRAGON_AXE,
            TOME,
            WARM_GLOVES,
            TORCH,
            BURNT_PAGE,
            "obj.pyromancer_top",
            "obj.pyromancer_hood",
            "obj.pyromancer_bottom",
            "obj.pyromancer_boots",
        )

    private val CATEGORIES = listOf("logs", "gems", "ores", "herbs", "seeds", "fish")

    private val RESOURCES: Map<String, List<WintertodtRewardRow>> by lazy {
        WintertodtRewardRow.all()
            .groupBy { it.category }
            .mapValues { (_, rows) -> rows.sortedBy { it.ord } }
    }

    fun roll(player: Player, random: GameRandom): WintertodtReward {
        if (random.of(1, 5000) == 1) return WintertodtReward(PHOENIX, 1)
        if (random.of(1, 10_000) == 1) return WintertodtReward(DRAGON_AXE, 1)
        if (random.of(1, 1000) == 1) return WintertodtReward(TOME, 1)
        if (random.of(1, 150) == 1) {
            return if (owned(player, WARM_GLOVES) >= 3) {
                WintertodtReward(MAGIC_SEED, 1)
            } else {
                WintertodtReward(WARM_GLOVES, 1)
            }
        }
        if (random.of(1, 150) == 1) {
            return if (owned(player, TORCH) >= 3) {
                WintertodtReward(TORSTOL_SEED, random.of(2, 3))
            } else {
                WintertodtReward(TORCH, 1)
            }
        }
        if (random.of(1, 150) == 1) return pyroPiece(player)
        if (random.of(1, 45) == 1) {
            return WintertodtReward(BURNT_PAGE, random.of(7, 29), noted = true)
        }
        return resource(player, random)
    }

    fun rollExtra(player: Player, random: GameRandom): WintertodtReward {
        if (random.of(1, 10_000) == 1) return WintertodtReward(DRAGON_AXE, 1)
        return resource(player, random)
    }

    fun add(player: Player, reward: WintertodtReward): Boolean =
        runCatching {
                player.invAdd(player.inv, reward.item, reward.count, cert = reward.noted).success
            }
            .getOrDefault(false)

    private fun pyroPiece(player: Player): WintertodtReward {
        val owned = PYRO_PIECES.map { owned(player, it) }
        val least = owned.min()
        val index = owned.indexOf(least)
        return WintertodtReward(PYRO_PIECES[index], 1)
    }

    private fun owned(player: Player, item: String): Int =
        player.inv.count(item) + player.worn.count(item)

    private fun resource(player: Player, random: GameRandom): WintertodtReward {
        val roll = random.of(0, 24)
        return when {
            roll < 5 -> WintertodtReward("obj.coins", random.of(2000, 4999))
            roll == 5 -> WintertodtReward("obj.lovakengj_dynamite_fused", random.of(3, 5), noted = true)
            roll == 6 -> WintertodtReward("obj.hosidius_saltpetre", random.of(3, 5), noted = true)
            else -> {
                val category = CATEGORIES[(roll - 7) / 3]
                rollResource(player, levelFor(player, category), category, random)
            }
        }
    }

    private fun levelFor(player: Player, category: String): Int =
        when (category) {
            "logs" -> player.woodcuttingLvl
            "gems" -> player.craftingLvl
            "ores" -> player.miningLvl
            "herbs" -> player.herbloreLvl
            "seeds" -> player.farmingLvl
            else -> player.fishingLvl
        }

    private fun rollResource(
        player: Player,
        level: Int,
        category: String,
        random: GameRandom,
    ): WintertodtReward {
        val entries = RESOURCES[category].orEmpty()
        if (entries.isEmpty()) return WintertodtReward("obj.coins", 1)
        var remaining = 1.0
        val roll = random.randomDouble()
        for (entry in entries) {
            val chance = remaining * interpolate(level, entry.low, entry.high) / 256.0
            if (roll < chance) {
                return WintertodtReward(
                    entry.obj.internalName,
                    random.of(entry.min, entry.max),
                    noted = true,
                )
            }
            remaining -= chance
        }
        val last = entries.last()
        return WintertodtReward(
            last.obj.internalName,
            random.of(last.min, last.max),
            noted = true,
        )
    }

    private fun interpolate(level: Int, low: Int, high: Int): Int {
        val unclamped = floor(low + (high - low) * (level - 1) / 98.0).toInt()
        return (unclamped + 1).coerceIn(0, 256)
    }
}
