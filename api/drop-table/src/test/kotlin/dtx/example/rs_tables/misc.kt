package dtx.example.rs_tables

import dtx.rs.*
import dtx.core.RollResult
import dtx.core.singleRollable
import dtx.example.*

val LongAndCurvedBoneTable = rsWeightedTable<Player, Item> {
    name("Long and Curved Bone table")
    400 weight Item("long_bone")
    1 weight Item("curved_bone")
}

enum class ClueTier(val isFreeToPlay: Boolean = false) {
    Beginner(true), Easy, Medium, Hard, Elite, Master;

    val scrollBox = Item("scroll_box_(${this.name.lowercase()})")

    val clueScroll = Item("clue_scroll_${this.name.lowercase()}")
}

inline fun Player.hasUnlockedScrollBoxes(): Boolean = checkQuestStatus(xmts_quest) is QuestStatus.Completed
inline fun Player.canCollectScroll(clueTier: ClueTier): Boolean = if (hasUnlockedScrollBoxes()) {
    val amountPosessed = posessesHowMany(clueTier.scrollBox)
    val cap = scrollCapForTier(clueTier)
    amountPosessed > cap
} else {
    val amountPosessed = posessesHowMany(clueTier.clueScroll)
    amountPosessed == 0
}

fun clueDrop(clueTier: ClueTier) = singleRollable<Player, Item> {

    vetoRoll { player, args ->

        if (!player.isOnMemberWorld() && !clueTier.isFreeToPlay) {
            return@vetoRoll true
        }

        if (player.canCollectScroll(clueTier)) {
            return@vetoRoll false
        }

        true
    }

    onVeto { player ->

        val sneakingSuspicioun = buildString {
            append("You have a sneaking suspicion that you would have received a $clueTier scroll")

            if (player.hasUnlockedScrollBoxes()) {
                append(" box")
            }
            append(".")
        }

        player.sendMessage(sneakingSuspicioun)

        RollResult.Nothing()
    }

    onRollCompleted { player, args, result ->
        result as RollResult.Single<Item>
        player.inventory.add(result.result)
        val msg = buildString {
            append("You find a $clueTier scroll ")
            if (player.hasUnlockedScrollBoxes()) {
                append("box")
            }
            append(" and pick it up.")
        }
        player.sendMessage(msg)
    }

    selectResult { player, otherArgs ->
        if (player.hasUnlockedScrollBoxes()) {
            RollResult.Single(clueTier.scrollBox)
        } else {
            RollResult.Single(clueTier.clueScroll)
        }
    }
}


enum class ChampionType {
    Imp, Goblin, Skeleton, Zombie,
    Giant, Hobgoblin, Ghoul, EarthWarrior,
    Jogre, LesserDemon, Human
}

fun championScroll(type: ChampionType) = singleRollable<Player, Item> {

    val scrollItem = Item("${type.name.lowercase()}_champion_scroll")

    vetoRoll { target, args ->

        if (target.hasChampionScrollComplete(type) || target.posesses(scrollItem)) {

            target.sendMessage("You have a funny feeling that you would have recieved a Champion's scroll...")

            return@vetoRoll false
        }

        target.questPoints > 32
                && target.isOnMemberWorld()
                && !target.hasChampionScrollComplete(type)

    }

    onRollCompleted { target, args, result ->
        target.sendMessage("A Champion's scroll falls to the ground as you slay your opponent.")
    }

    result(scrollItem)
}
