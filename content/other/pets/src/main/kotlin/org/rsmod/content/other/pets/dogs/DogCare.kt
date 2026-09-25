package org.rsmod.content.other.pets.dogs

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.api.table.FoodRow
import org.rsmod.api.table.PotionRow
import org.rsmod.api.table.cooking.CookingAlesRow
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.other.pets.followerObj
import org.rsmod.content.other.pets.storesAnyObj
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Singleton
class DogCare @Inject constructor(private val followers: PetFollowers) {
    private val food: Set<String> = FoodRow.all().flatMap { row -> row.items.map { it.internalName } }.toSet()
    private val potions: Set<String> = PotionRow.all().flatMap { row -> row.items.map { it.internalName } }.toSet()
    private val alcohol: Set<String> = CookingAlesRow.all().flatMap { row -> row.output.map { it.internalName } }.toSet()

    fun following(player: Player): DogForm? = Dogs.forObj(player.followerObj)

    fun ownsDog(player: Player, puppy: Boolean): Boolean =
        following(player)?.puppy == puppy || player.storesAnyObj(Dogs.objsOf(puppy))

    fun resetPuppy(player: Player) {
        player.dogGrowthEvents = 0
        player.dogGrowthTicks = 0
        player.dogFedTicks = 0
    }

    fun feed(player: Player) {
        player.dogFedTicks = 0
    }

    fun isUpsetting(obj: ItemServerType): Boolean {
        val name = obj.name.lowercase()
        if (obj.internalName in alcohol || UPSET_PHRASES.any { it in name }) {
            return true
        }
        return name.split(' ', '(', ')', '-').any { it in UPSET_WORDS }
    }

    fun isPotion(obj: ItemServerType): Boolean {
        val isGuthixRest = obj.internalName.startsWith(GUTHIX_REST)
        return (obj.internalName in potions && !isGuthixRest) || obj.name.endsWith(" mix", ignoreCase = true)
    }

    fun isFood(obj: ItemServerType): Boolean {
        val name = obj.name.lowercase()
        return obj.internalName in food ||
            obj.internalName.startsWith(GUTHIX_REST) ||
            name == "egg" ||
            name.endsWith("bones") ||
            name in BONES
    }

    fun ageTicks(player: Player): Int = player.dogGrowthEvents * GROWTH_TICKS + player.dogGrowthTicks

    fun ticksUntilAdult(player: Player): Int = (PUPPY_EVENTS * GROWTH_TICKS - ageTicks(player)).coerceAtLeast(0)

    fun isGrowthPaused(player: Player): Boolean = player.dogFedTicks >= HUNGER_STOP

    fun tick(player: Player) {
        val dog = following(player) ?: return
        if (!dog.puppy) {
            return
        }
        val npc = followers.follower(player) ?: return
        hunger(player)
        if (!isGrowthPaused(player)) {
            grow(player, dog, npc)
        }
    }

    private fun hunger(player: Player) {
        val fed = player.dogFedTicks + 1
        if (fed > HUNGER_STOP) {
            return
        }
        player.dogFedTicks = fed
        when (fed) {
            HUNGER_WARN -> player.mes("<col=ff0000>Your puppy is getting quite hungry.</col>")
            HUNGER_VERY -> player.mes("<col=ff0000>Your puppy is very hungry.</col>")
            HUNGER_STOP ->
                player.mes(
                    "<col=ff0000>Your puppy has stopped growing. You'll need to feed it for it to continue doing so.</col>"
                )
        }
    }

    private fun grow(player: Player, dog: DogForm, npc: Npc) {
        val ticks = player.dogGrowthTicks + 1
        if (ticks < GROWTH_TICKS) {
            player.dogGrowthTicks = ticks
            return
        }
        player.dogGrowthTicks = 0
        val events = player.dogGrowthEvents + 1
        if (events < PUPPY_EVENTS) {
            player.dogGrowthEvents = events
            return
        }
        player.dogGrowthEvents = 0
        followers.spawn(player, Dogs.adult(dog).form)
        player.mes("Your puppy has grown into an adult dog.")
    }

    companion object {
        const val GROWTH_TICKS = 150
        const val PUPPY_EVENTS = 120

        const val HUNGER_WARN = 1500
        const val HUNGER_VERY = 2500
        const val HUNGER_STOP = 3000

        private const val GUTHIX_REST = "obj.cup_guthix_rest"
        private val BONES = setOf("long bone", "curved bone")
        private val UPSET_WORDS =
            setOf(
                "chocolate",
                "chocolatey",
                "onion",
                "onions",
                "grape",
                "grapes",
                "wine",
                "beer",
                "garlic",
                "sweets",
                "satchel",
                "chilli",
                "chili",
                "brandy",
                "whisky",
                "gin",
                "vodka",
                "rum",
                "cider",
                "mead",
                "stout",
                "bitter",
                "ale",
            )
        private val UPSET_PHRASES = listOf("easter egg", "poison karambwan")
    }
}
