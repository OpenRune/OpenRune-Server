package org.rsmod.content.other.consumables.food

import jakarta.inject.Singleton
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess

@Singleton
class FoodEffectService {
    fun apply(
        access: ProtectedAccess,
        effect: String,
    ) {
        with(access) {
            when (effect) {
                "jug_of_wine" -> {
                    drainFlat(
                        stat = "stat.attack",
                        amount = 2,
                    )
                }

                "ixcoztic_white" -> {
                    boostFlat("stat.farming", 1)
                    drainFlat("stat.attack", 5)
                    drainFlat("stat.herblore", 1)
                }

                "beer" -> {
                    boostFormula(
                        stat = "stat.strength",
                        percent = 2,
                        constant = 1,
                    )
                    drainFormula(
                        stat = "stat.attack",
                        percent = 6,
                        constant = -1,
                    )
                }

                "asgarnian_ale" -> {
                    boostFlat("stat.strength", 2)
                    drainFormula(
                        stat = "stat.attack",
                        percent = 5,
                        constant = -2,
                    )
                }

                "wizards_mind_bomb" -> {
                    boostFormula(
                        stat = "stat.magic",
                        percent = 2,
                        constant = 2,
                    )
                    drainFormula("stat.attack", 5, -1)
                    drainFormula("stat.defence", 5, -1)
                    drainFormula("stat.strength", 5, -1)
                }

                "greenmans_ale" -> {
                    boostFlat("stat.herblore", 1)
                    drainFormula("stat.attack", 4, -2)
                    drainFormula("stat.defence", 4, -2)
                    drainFormula("stat.strength", 4, -2)
                }

                "dragon_bitter" -> {
                    boostFlat("stat.strength", 2)
                    drainFormula("stat.attack", 5, -2)
                }

                "dwarven_stout" -> {
                    boostFlat("stat.mining", 1)
                    boostFlat("stat.smithing", 1)
                    drainFormula("stat.attack", 4, -2)
                    drainFormula("stat.defence", 4, -2)
                    drainFormula("stat.strength", 4, -2)
                }

                "cider" -> {
                    boostFlat("stat.farming", 1)
                    drainFormula("stat.attack", 2, -2)
                    drainFormula("stat.strength", 2, -2)
                }

                "axemans_folly" -> {
                    boostFlat("stat.woodcutting", 1)
                    drainFormula("stat.attack", 2, -2)
                    drainFormula("stat.strength", 2, -2)
                }

                "chefs_delight" -> {
                    boostFormula(
                        stat = "stat.cooking",
                        percent = 5,
                        constant = 1,
                    )
                    drainFormula("stat.attack", 5, -2)
                    drainFormula("stat.strength", 5, -2)
                }

                "slayers_respite" -> {
                    boostFlat("stat.slayer", 2)
                    drainFormula("stat.attack", 2, -2)
                    drainFormula("stat.defence", 2, -2)
                    drainFormula("stat.strength", 2, -2)
                }

                "grog" -> {
                    boostFormula(
                        stat = "stat.strength",
                        percent = 4,
                        constant = 1,
                    )
                    drainFormula("stat.attack", 5, -3)
                }

                "short_green_guy",
                "premade_short_green_guy",
                "vodka",
                "whisky",
                "gin",
                "brandy",
                    -> {
                    boostFormula(
                        stat = "stat.strength",
                        percent = 5,
                        constant = 1,
                    )
                    drainFormula(
                        stat = "stat.attack",
                        percent = 2,
                        constant = -3,
                    )
                }

                "keg_of_beer" -> {
                    boostFormula(
                        stat = "stat.strength",
                        percent = 10,
                        constant = 2,
                    )
                    drainFormula("stat.attack", 50, -4)
                }

                "kovacs_grog" -> {
                    boostFlat("stat.smithing", 4)
                    drainFlat("stat.attack", 2)
                    drainFlat("stat.ranged", 2)
                    drainFlat("stat.magic", 2)
                }

                "garden_pie" ->
                    boostFlat("stat.farming", 3)

                "fish_pie" ->
                    boostFlat("stat.fishing", 3)

                "botanical_pie" ->
                    boostFlat("stat.herblore", 4)

                "mushroom_pie" ->
                    boostFlat("stat.crafting", 4)

                "admiral_pie" ->
                    boostFlat("stat.fishing", 5)

                "dragonfruit_pie" ->
                    boostFlat("stat.fletching", 4)

                "wild_pie" -> {
                    boostFlat("stat.slayer", 5)
                    boostFlat("stat.ranged", 4)
                }

                "summer_pie" -> {
                    boostFlat("stat.agility", 5)
                    restoreRunEnergy(10)
                }

                "jangerberries" -> {
                    boostFlat("stat.attack", 2)
                    boostFlat("stat.strength", 1)

                    statHeal(
                        stat = "stat.prayer",
                        constant = 1,
                        percent = 0,
                    )

                    drainFlat("stat.defence", 1)
                }

                "papaya_fruit" ->
                    restoreRunEnergy(5)

                "yellowfin" ->
                    restoreRunEnergy(20)

                "bluefin" ->
                    restoreStat(
                        stat = "stat.prayer",
                        amount = 5,
                    )

                "magic_cabbage" ->
                    boostFormula(
                        stat = "stat.defence",
                        percent = 2,
                        constant = 1,
                    )

                "cup_of_tea" ->
                    boostFormula(
                        stat = "stat.attack",
                        percent = 2,
                        constant = 2,
                    )

                "white_tree_fruit" ->
                    restoreRunEnergy(
                        random.of(
                            minInclusive = 5,
                            maxInclusive = 10,
                        ),
                    )

                "strange_fruit" ->
                    restoreRunEnergy(30)

                "mint_cake",
                "rare_tuber",
                    -> restoreRunEnergy(50)

                "tbone_steak" ->
                    boostFlat(
                        stat = "stat.strength",
                        amount = 2,
                    )

                "blurberry_special",
                "premade_blurberry_special",
                "drunk_dragon",
                "premade_drunk_dragon",
                "chocolate_saturday",
                "premade_chocolate_saturday",
                    -> {
                    boostFormula(
                        stat = "stat.strength",
                        percent = 5,
                        constant = 2,
                    )
                    drainFormula(
                        stat = "stat.attack",
                        percent = 2,
                        constant = -3,
                    )
                }

                "wizard_blizzard",
                "premade_wizard_blizzard",
                    -> {
                    boostFormula(
                        stat = "stat.strength",
                        percent = 6,
                        constant = 1,
                    )
                    drainFormula(
                        stat = "stat.attack",
                        percent = 2,
                        constant = -3,
                    )
                }

                else ->
                    error(
                        "Unknown food effect: '$effect'.",
                    )
            }
        }
    }

    private fun ProtectedAccess.boostFlat(
        stat: String,
        amount: Int,
    ) {
        if (amount > 0) {
            statBoost(
                stat = stat,
                constant = amount,
                percent = 0,
            )
        }
    }

    private fun ProtectedAccess.boostFormula(
        stat: String,
        percent: Int,
        constant: Int,
    ) {
        val amount =
            (
                statBase(stat) * percent / 100 +
                    constant
                ).coerceAtLeast(0)

        boostFlat(
            stat = stat,
            amount = amount,
        )
    }

    private fun ProtectedAccess.drainFlat(
        stat: String,
        amount: Int,
    ) {
        if (amount > 0) {
            statSub(
                stat = stat,
                constant = amount,
                percent = 0,
            )
        }
    }

    private fun ProtectedAccess.drainFormula(
        stat: String,
        percent: Int,
        constant: Int,
    ) {
        val amount =
            (
                statBase(stat) * percent / 100 +
                    constant
                ).coerceAtLeast(0)

        drainFlat(
            stat = stat,
            amount = amount,
        )
    }

    private fun ProtectedAccess.restoreStat(
        stat: String,
        amount: Int,
    ) {
        if (amount <= 0) {
            return
        }

        statHeal(
            stat = stat,
            constant = amount,
            percent = 0,
        )
    }

    private fun ProtectedAccess.restoreRunEnergy(
        percent: Int,
    ) {
        val restored =
            (
                player.runEnergy +
                    percent * RUN_ENERGY_PERCENT_SCALE
                ).coerceAtMost(
                    constants.run_max_energy,
                )

        if (restored == player.runEnergy) {
            return
        }

        player.runEnergy = restored
        UpdateRun.energy(player, restored)
    }

    private companion object {
        const val RUN_ENERGY_PERCENT_SCALE = 10
    }
}
