package dtx.example

import dtx.example.rs_tables.ChampionType
import dtx.example.rs_tables.ClueTier
import dtx.example.rs_tables.MoonBoss

sealed interface Item {

    val itemId: String

    val itemAmount: Int

    companion object {
        operator fun invoke(itemId: String, amount: Int = 1): Item = ConcreteItem(itemId, amount)
        operator fun invoke(itemId: String, min: Int, max: Int): Item = RandAmtItem(itemId, min, max)
        operator fun invoke(itemId: String, range: RandomIntRange): Item = RandAmtItem(itemId, range.start, range.endInclusive)
        operator fun invoke(itemId: String, range: IntRange): Item = Item(itemId, range.toRandomIntRange())

    }

    fun copyItem() = if (this is ConcreteItem) {
        copy()
    } else {
        this as RandAmtItem
        copy()
    }
}

fun Item.concrete(): Item = ConcreteItem(itemId, itemAmount)

data class RandAmtItem(
    override val itemId: String,
    internal val minAmount: Int,
    internal val maxAmount: Int,
): Item {

    internal val range = (minAmount ..< maxAmount).toRandomIntRange()

    override val itemAmount: Int get() = range.random()
}

data class ConcreteItem(
    override val itemId: String,
    override val itemAmount: Int = 1
): Item

enum class Gender {
    PlatelegEnjoyer, PlateskirtEnthusiast;
}

 sealed interface QuestStatus {
     data object NotStarted : QuestStatus
     data class InProgress(val progressFlags: Long) : QuestStatus
     data object Completed : QuestStatus
}

val xmts_quest = "x_marks_the_spot"

class MoonProtection {

    val items = Array<MutableSet<String>>(MoonBoss.entries.size) { mutableSetOf() }

    fun protectedAgainst(boss: MoonBoss, item: String): Boolean {

        if (items[boss.ordinal].size == 4) {
            return false
        }

        return item in items[boss.ordinal]
    }

    fun addProtectionAgainst(boss: MoonBoss, item: String) {
        items[boss.ordinal].add(item)
    }
}

class MoonsProgress {

    val defeated = BooleanArray(3) { false }

    fun eligibleToOpenChest(): Boolean {
        return defeated.any()
    }

    fun forEach(action: (MoonBoss) -> Unit) {
        defeated.forEachIndexed { index, defeated ->
            if (defeated) {
                action(MoonBoss.entries[index])
            }
        }
    }

    fun howManyDefeated(): Int = defeated.count { it }

    fun defeat(boss: MoonBoss) {
        defeated[boss.ordinal] = true
    }

    fun reset() {
        defeated.fill(false)
    }
}

data class Player(
    val username: String,
    var dropRateBonus: Double = 0.0,
    val bank: MutableCollection<Item> = mutableListOf(),
    val inventory: MutableCollection<Item> = mutableListOf(),
    val equipment: MutableCollection<Item> = mutableListOf(),
    val quests: MutableMap<String, QuestStatus> = mutableMapOf(xmts_quest to QuestStatus.NotStarted),
    val scrollCapIncreases: MutableMap<ClueTier, Int> = buildMap {
        ClueTier.entries.forEach { put(it, 0) }
    }.toMutableMap(),
    var questPoints: Int = 0,
    var currentWorld: Int = 1,
    var gender: Gender = Gender.PlateskirtEnthusiast,
    val hasScrollCompleted: MutableMap<ChampionType, Boolean> = buildMap {
        ChampionType.entries.forEach { put(it, false) }
    }.toMutableMap(),
    val moonsTempProgress: MoonsProgress = MoonsProgress(),
    val moonsProtection: MoonProtection = MoonProtection()
) {

    fun isWearing(item: String): Boolean = equipment.any { it.itemId == item }

    fun isWearing(item: Item): Boolean = isWearing(item.itemId)

    fun posesses(item: String): Boolean = inventory.any { it.itemId == item } || bank.any { it.itemId == item } || isWearing(item)

    fun posesses(item: Item): Boolean = posesses(item.itemId)

    fun posessesHowMany(item: String): Int {
        val inventoryCount = inventory.filter { it.itemId == item }.sumOf { it.itemAmount }
        val bankCount = bank.filter { it.itemId == item }.sumOf { it.itemAmount }
        val equipmentCount = equipment.filter { it.itemId == item }.sumOf { it.itemAmount }
        return inventoryCount + bankCount + equipmentCount
    }

    fun posessesHowMany(checkItem: Item): Int = posessesHowMany(checkItem.itemId)

    fun isOnMemberWorld(): Boolean = currentWorld > 1

    fun updateQuestStatus(questName: String, status: QuestStatus) {
        quests[questName] = status
    }

    fun checkQuestStatus(questName: String): QuestStatus {
        return quests[questName] ?: QuestStatus.NotStarted
    }

    fun hasChampionScrollComplete(type: ChampionType): Boolean = hasScrollCompleted[type]!!

    fun scrollCapForTier(difficulty: ClueTier): Int = 2 + scrollCapIncreases[difficulty]!!
}

var allowPlayerMessages = true

fun Player.sendMessage(message: String) {
    if (allowPlayerMessages) {
        println("[to: $username]: $message")
    }
}


val examplePlayer = Player("player")
