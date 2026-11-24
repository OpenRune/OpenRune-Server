package org.rsmod.content.other.bots

import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.FacialHairStylesRow
import org.rsmod.api.table.HairStylesRow
import org.rsmod.api.table.HandStylesRow
import org.rsmod.api.table.LeggingStylesRow
import org.rsmod.api.table.ShoeStylesRow
import org.rsmod.api.table.SleeveStylesRow
import org.rsmod.api.table.TorsoStylesRow
import org.rsmod.game.entity.player.Appearance

object BotLooks {
    private val KIT_STYLES: List<List<Pair<Int, Int>>> by lazy {
        listOf(
            HairStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
            FacialHairStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
            TorsoStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
            SleeveStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
            HandStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
            LeggingStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
            ShoeStylesRow.all().map { it.playerKitIdTypeA to it.playerKitIdTypeB },
        )
    }

    private val COLOUR_RANGES = listOf(0..11, 0..15, 0..15, 0..5, 0..7)

    fun randomise(appearance: Appearance, random: GameRandom) {
        val bodyTypeB = random.randomBoolean()
        appearance.bodyType = if (bodyTypeB) Appearance.BODY_TYPE_B else 0
        appearance.pronoun = if (bodyTypeB) 1 else 0
        for ((slot, styles) in KIT_STYLES.withIndex()) {
            val style = random.pickOrNull(styles) ?: continue
            appearance.setIdentKit(slot, if (bodyTypeB) style.second else style.first)
        }
        for ((slot, range) in COLOUR_RANGES.withIndex()) {
            appearance.setColour(slot, random.of(range))
        }
    }
}
