package org.rsmod.content.other.pets.cats

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.api.table.cooking.CookingFoodsRow
import org.rsmod.api.table.fishing.FishingSpotRow
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.other.pets.followerObj
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Singleton
class CatCare @Inject constructor(private val followers: PetFollowers) {
    private val food: Set<String> = buildSet {
        val raw = FishingSpotRow.all().map { it.fish.internalName }.filter { it !in INEDIBLE }
        addAll(raw)
        addAll(CookingFoodsRow.all().filter { it.input.internalName in raw }.map { it.output.internalName })
        addAll(EXTRA_FOOD)
    }

    fun following(player: Player): CatForm? = Cats.forObj(player.followerObj)

    fun isFood(obj: ItemServerType): Boolean = obj.internalName in food

    fun isInedibleFish(obj: ItemServerType): Boolean = obj.internalName in INEDIBLE

    fun resetKitten(player: Player) {
        player.catGrowthEvents = 0
        player.catGrowthTicks = 0
        player.catRatsCaught = 0
        player.catStrokes = 0
        player.catHunger = HUNGER_FULL
        player.catAttention = ATTENTION_FRESH
    }

    fun feed(player: Player) {
        player.catHunger = HUNGER_FULL
    }

    fun stroke(player: Player) {
        val strokes = player.catStrokes + 1
        player.catStrokes = minOf(strokes, 2)
        player.catAttention = if (strokes >= 2) ATTENTION_FRESH else ATTENTION_STROKED
    }

    fun play(player: Player) {
        player.catAttention = ATTENTION_PLAYED
    }

    fun ageTicks(player: Player): Int = player.catGrowthEvents * GROWTH_TICKS + player.catGrowthTicks

    fun ticksUntilAdult(player: Player): Int = (KITTEN_EVENTS * GROWTH_TICKS - ageTicks(player)).coerceAtLeast(0)

    fun nearlyOvergrown(player: Player): Boolean = player.catGrowthEvents >= CAT_EVENTS - NEARLY_OVERGROWN_EVENTS

    fun isVeryHungry(player: Player): Boolean = player.catHunger <= HUNGER_VERY

    fun isLonely(player: Player): Boolean = player.catAttention <= ATTENTION_LONELY

    fun morph(player: Player, stage: CatStage, colour: CatColour): Npc? {
        followers.spawn(player, Cats.of(stage, colour).form)
        return followers.follower(player)
    }

    fun toHell(player: Player): Npc? {
        val cat = following(player) ?: return null
        if (cat.isHell) {
            return null
        }
        player.catOriginalColour = cat.colour.id
        return morph(player, cat.stage, CatColour.Hell)
    }

    fun fromHell(player: Player): Npc? {
        val cat = following(player) ?: return null
        if (!cat.isHell) {
            return null
        }
        return morph(player, cat.stage, CatColour.of(player.catOriginalColour))
    }

    private fun runAway(player: Player, message: String) {
        followers.dismiss(player)
        player.mes(message)
    }

    fun tick(player: Player) {
        val cat = following(player) ?: return
        val npc = followers.follower(player) ?: return
        grow(player, cat, npc)
        if (cat.stage == CatStage.Kitten) {
            hunger(player, npc)
            attention(player, npc)
        }
    }

    private fun grow(player: Player, cat: CatForm, npc: Npc) {
        val ticks = player.catGrowthTicks + 1
        if (ticks < GROWTH_TICKS) {
            player.catGrowthTicks = ticks
            return
        }
        player.catGrowthTicks = 0
        val events = player.catGrowthEvents + 1
        when {
            cat.stage == CatStage.Kitten && events >= KITTEN_EVENTS -> {
                player.catGrowthEvents = 0
                morph(player, CatStage.Cat, cat.colour)?.say("Meeeooow!")
                player.mes("Your kitten has grown into a healthy cat that can hunt for itself.")
            }
            cat.stage == CatStage.Cat && events >= CAT_EVENTS -> {
                player.catGrowthEvents = 0
                morph(player, CatStage.Overgrown, cat.colour)
                player.mes("Your cat has grown into a mighty feline, but it will no longer be able to chase vermin.")
            }
            else -> player.catGrowthEvents = minOf(events, MAX_EVENTS)
        }
    }

    private fun hunger(player: Player, npc: Npc) {
        val hunger = player.catHunger - 1
        if (hunger < 0) {
            return
        }
        player.catHunger = hunger
        when (hunger) {
            HUNGER_WARN -> {
                player.say("I think it's hungry!")
                player.mes("<col=ff0000>Your kitten is hungry.</col>")
            }
            HUNGER_VERY -> {
                npc.say("Meeeooowww!")
                player.say("I think it's very hungry!")
                player.mes("<col=ff0000>Your kitten is very hungry.</col>")
            }
            0 -> runAway(player, "Your kitten has run away.")
        }
    }

    private fun attention(player: Player, npc: Npc) {
        val attention = player.catAttention - 1
        if (attention < 0) {
            return
        }
        player.catAttention = attention
        when (attention) {
            ATTENTION_WARN -> {
                player.catStrokes = 0
                player.say("I think it wants some attention.")
                player.mes("<col=ff0000>Your kitten wants attention.</col>")
            }
            ATTENTION_LONELY -> {
                npc.say("Meeeooowww...")
                player.say("I think it's feeling lonely.")
                player.mes("<col=ff0000>Your kitten really wants attention.</col>")
            }
            0 -> runAway(player, "Your kitten has run away.")
        }
    }

    companion object {
        const val GROWTH_TICKS = 150
        const val KITTEN_EVENTS = 120
        const val CAT_EVENTS = 220
        const val MAX_EVENTS = 255
        const val NEARLY_OVERGROWN_EVENTS = 40

        const val HUNGER_FULL = 3000
        const val HUNGER_WARN = 600
        const val HUNGER_VERY = 300

        const val ATTENTION_FRESH = 3900
        const val ATTENTION_STROKED = 3200
        const val ATTENTION_PLAYED = 6500
        const val ATTENTION_WARN = 1400
        const val ATTENTION_LONELY = 700

        private val INEDIBLE =
            setOf(
                "obj.infernal_eel",
                "obj.raw_cave_eel",
                "obj.mort_slimey_eel",
                "obj.brut_spawning_trout",
                "obj.brut_spawning_salmon",
                "obj.brut_sturgeon",
            )

        private val EXTRA_FOOD =
            setOf(
                "obj.brut_caviar",
                "obj.brut_roe",
                "obj.giant_frogspawn",
                "obj.blighted_anglerfish",
                "obj.blighted_karambwan",
                "obj.tbwt_raw_karambwan",
                "obj.tbwt_cooked_karambwan",
                "obj.tbwt_poorly_cooked_karambwan",
                "obj.tbwt_cooked_karambwanji",
            )
    }
}
