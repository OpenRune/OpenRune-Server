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
import org.rsmod.game.entity.Player

internal data class WintertodtReward(val item: String, val count: Int, val noted: Boolean = false)

private data class ResourceEntry(
    val item: String,
    val min: Int,
    val max: Int,
    val low: Int,
    val high: Int,
)

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

    private val LOGS =
        listOf(
            ResourceEntry("obj.magic_logs", 10, 20, -60, 60),
            ResourceEntry("obj.yew_logs", 10, 20, -50, 90),
            ResourceEntry("obj.mahogany_logs", 10, 20, -40, 130),
            ResourceEntry("obj.maple_logs", 10, 20, 0, 160),
            ResourceEntry("obj.teak_logs", 10, 20, 30, 200),
            ResourceEntry("obj.willow_logs", 10, 20, 120, 100),
            ResourceEntry("obj.oak_logs", 10, 20, 255, 255),
        )

    private val GEMS =
        listOf(
            ResourceEntry("obj.uncut_diamond", 1, 3, 10, 120),
            ResourceEntry("obj.uncut_ruby", 2, 4, 50, 140),
            ResourceEntry("obj.uncut_emerald", 1, 3, 90, 160),
            ResourceEntry("obj.uncut_sapphire", 1, 3, 255, 255),
        )

    private val ORES =
        listOf(
            ResourceEntry("obj.runite_ore", 1, 2, -60, 40),
            ResourceEntry("obj.adamantite_ore", 2, 3, -50, 80),
            ResourceEntry("obj.mithril_ore", 3, 5, -20, 140),
            ResourceEntry("obj.gold_ore", 8, 11, 0, 160),
            ResourceEntry("obj.coal", 10, 14, 0, 180),
            ResourceEntry("obj.blankrune_high", 20, 70, 40, 190),
            ResourceEntry("obj.silver_ore", 10, 12, 140, 10),
            ResourceEntry("obj.limestone", 2, 7, 200, -20),
            ResourceEntry("obj.iron_ore", 5, 15, 255, 255),
        )

    private val HERBS =
        listOf(
            ResourceEntry("obj.unidentified_torstol", 1, 3, -70, 40),
            ResourceEntry("obj.unidentified_dwarf_weed", 2, 4, -50, 50),
            ResourceEntry("obj.unidentified_lantadyme", 2, 4, -30, 60),
            ResourceEntry("obj.unidentified_cadantine", 2, 4, -10, 70),
            ResourceEntry("obj.unidentified_kwuarm", 2, 4, 10, 85),
            ResourceEntry("obj.unidentified_avantoe", 3, 5, 20, 100),
            ResourceEntry("obj.unidentified_irit", 3, 5, 30, 115),
            ResourceEntry("obj.unidentified_ranarr", 1, 3, 10, 170),
            ResourceEntry("obj.unidentified_tarromin", 3, 6, 70, -20),
            ResourceEntry("obj.unidentified_marentill", 3, 6, 100, -30),
            ResourceEntry("obj.unidentified_guam", 3, 6, 170, -40),
            ResourceEntry("obj.unidentified_harralander", 3, 6, 255, 255),
        )

    private val SEEDS =
        listOf(
            ResourceEntry("obj.spirit_tree_seed", 1, 1, -20, 5),
            ResourceEntry("obj.dwarf_weed_seed", 1, 3, -60, 25),
            ResourceEntry("obj.lantadyme_seed", 1, 3, -60, 30),
            ResourceEntry("obj.cadantine_seed", 1, 3, -40, 40),
            ResourceEntry("obj.snapdragon_seed", 1, 3, -15, 60),
            ResourceEntry("obj.yew_seed", 1, 2, -10, 70),
            ResourceEntry("obj.snape_grass_seed", 3, 7, -8, 78),
            ResourceEntry("obj.kwuarm_seed", 1, 3, -10, 80),
            ResourceEntry("obj.ranarr_seed", 1, 3, 0, 110),
            ResourceEntry("obj.avantoe_seed", 1, 3, 0, 130),
            ResourceEntry("obj.watermelon_seed", 3, 7, -10, 180),
            ResourceEntry("obj.irit_seed", 1, 3, 0, 170),
            ResourceEntry("obj.teak_seed", 1, 2, 10, 160),
            ResourceEntry("obj.maple_seed", 1, 2, 15, 190),
            ResourceEntry("obj.mahogany_seed", 1, 2, 20, 190),
            ResourceEntry("obj.toadflax_seed", 1, 3, 20, 190),
            ResourceEntry("obj.banana_tree_seed", 1, 2, 30, 180),
            ResourceEntry("obj.willow_seed", 1, 2, 60, 120),
            ResourceEntry("obj.harralander_seed", 1, 3, 80, -150),
            ResourceEntry("obj.tarromin_seed", 1, 3, 150, -150),
            ResourceEntry("obj.acorn", 1, 1, 255, 255),
        )

    private val FISH =
        listOf(
            ResourceEntry("obj.raw_shark", 6, 11, -60, 80),
            ResourceEntry("obj.raw_swordfish", 6, 11, -50, 100),
            ResourceEntry("obj.raw_lobster", 6, 11, -20, 130),
            ResourceEntry("obj.raw_tuna", 6, 11, 10, 160),
            ResourceEntry("obj.raw_salmon", 6, 11, 40, 180),
            ResourceEntry("obj.raw_anchovies", 6, 11, 160, 0),
            ResourceEntry("obj.raw_trout", 6, 11, 255, 255),
        )

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
                val category = (roll - 7) / 3
                when (category) {
                    0 -> rollResource(player, player.woodcuttingLvl, LOGS, random)
                    1 -> rollResource(player, player.craftingLvl, GEMS, random)
                    2 -> rollResource(player, player.miningLvl, ORES, random)
                    3 -> rollResource(player, player.herbloreLvl, HERBS, random)
                    4 -> rollResource(player, player.farmingLvl, SEEDS, random)
                    else -> rollResource(player, player.fishingLvl, FISH, random)
                }
            }
        }
    }

    private fun rollResource(
        player: Player,
        level: Int,
        entries: List<ResourceEntry>,
        random: GameRandom,
    ): WintertodtReward {
        var remaining = 1.0
        val roll = random.randomDouble()
        for (entry in entries) {
            val chance = remaining * interpolate(level, entry.low, entry.high) / 256.0
            if (roll < chance) {
                return WintertodtReward(
                    entry.item,
                    random.of(entry.min, entry.max),
                    noted = true,
                )
            }
            remaining -= chance
        }
        val last = entries.last()
        return WintertodtReward(last.item, random.of(last.min, last.max), noted = true)
    }

    private fun interpolate(level: Int, low: Int, high: Int): Int {
        val unclamped = floor(low + (high - low) * (level - 1) / 98.0).toInt()
        return (unclamped + 1).coerceIn(0, 256)
    }
}
