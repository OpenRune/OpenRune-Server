package org.rsmod.content.bosses.moons.neypotzli

import jakarta.inject.Inject
import org.rsmod.api.player.stat.statRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.herblore.HerbloreCrushingRow
import org.rsmod.api.table.herblore.HerbloreFinishedRow
import org.rsmod.content.skills.herblore.BrewBonus
import org.rsmod.content.skills.herblore.CrushBonus
import org.rsmod.content.skills.herblore.HerbloreHook
import org.rsmod.game.entity.Player

// TODO: port to the SkillingProduct event from feat/leagues-foundation (#456) and delete HerbloreHook.
class MoonsHerbloreHook @Inject constructor(private val random: GameRandom) : HerbloreHook {

    override fun crushBonus(player: Player, recipe: HerbloreCrushingRow): CrushBonus? {
        if (recipe.input.internalName != GRUB) return null
        if (player.inv.count(GRUB) < 2) return null
        if (!roll(player)) return null
        return CrushBonus(1, "Your herblore knowledge allows you to crush an extra grub.")
    }

    override fun brewBonus(player: Player, potion: HerbloreFinishedRow): BrewBonus? {
        if (potion.output.internalName != POTION_2) return null
        if (!roll(player)) return null
        if (!roll(player)) {
            return BrewBonus(
                POTION_3,
                "Your potion making knowledge allows you to squeeze out an extra dose!",
            )
        }
        return BrewBonus(
            POTION_4,
            "Your potion making knowledge allows you to squeeze out two extra doses!",
        )
    }

    private fun roll(player: Player): Boolean =
        player.statRandom(random, "stat.herblore", EXTRA_LOW, EXTRA_HIGH, 0)

    private companion object {
        private const val GRUB = "obj.moonlight_grub"
        private const val POTION_2 = "obj.2dosemoonlightpotion"
        private const val POTION_3 = "obj.3dosemoonlightpotion"
        private const val POTION_4 = "obj.4dosemoonlightpotion"

        // UNVERIFIED: the odds of each extra; only the fact that it scales with level is known.
        private const val EXTRA_LOW = 51
        private const val EXTRA_HIGH = 179
    }
}
