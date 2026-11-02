package org.rsmod.content.skills.hunter

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.table.hunter.HunterRumourHuntersRow
import org.rsmod.api.table.hunter.HunterRumoursRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player

@Singleton
class HunterRumours @Inject constructor(private val objRepo: ObjRepository, private val random: GameRandom) {
    internal val hunters: List<HunterRumourHuntersRow> by lazy { HunterRumourHuntersRow.all().sortedBy { it.index } }
    internal val rumours: List<HunterRumoursRow> by lazy { HunterRumoursRow.all().sortedBy { it.index } }

    internal fun hunterVarp(hunter: HunterRumourHuntersRow): String = "varp.hunter_rumour_${hunter.name.lowercase()}"

    internal fun rumourOf(player: Player, hunter: HunterRumourHuntersRow): HunterRumoursRow? =
        rumours.firstOrNull { it.index == player.vars[hunterVarp(hunter)] }

    internal fun currentHunter(player: Player): HunterRumourHuntersRow? =
        hunters.firstOrNull { it.index + 1 == player.vars[CURRENT] }

    internal fun currentRumour(player: Player): HunterRumoursRow? = currentHunter(player)?.let { rumourOf(player, it) }

    internal fun assign(player: Player, hunter: HunterRumourHuntersRow, forced: HunterRumoursRow? = null): HunterRumoursRow? {
        val level = player.statBase(HUNTER)
        if (level < hunter.level) return null
        val taken = hunters.filter { it != hunter }.mapNotNull { rumourOf(player, it)?.index }.toSet()
        val repeat = if (player.vars[BACK_TO_BACK] != BACK_TO_BACK_ON) player.vars[LAST] else -1
        val options =
            rumours.filter {
                it.hunters and (1 shl hunter.index) != 0 && level >= it.level && it.index !in taken &&
                    it.index != repeat && meetsRequirements(player, it)
            }
        val rumour = forced?.takeIf { it in options } ?: options.randomOrNull() ?: return null
        VarPlayerIntMapSetter.set(player, hunterVarp(hunter), rumour.index)
        VarPlayerIntMapSetter.set(player, CURRENT, hunter.index + 1)
        VarPlayerIntMapSetter.set(player, PITY, 0)
        return rumour
    }

    internal fun reshuffle(player: Player, gilman: HunterRumourHuntersRow) {
        for (hunter in hunters) VarPlayerIntMapSetter.set(player, hunterVarp(hunter), 0)
        VarPlayerIntMapSetter.set(player, RESHUFFLED, 1)
        VarPlayerIntMapSetter.set(player, CURRENT, gilman.index + 1)
    }

    internal fun reshuffled(player: Player): Boolean = player.vars[RESHUFFLED] != 0

    internal fun meetsRequirements(player: Player, rumour: HunterRumoursRow): Boolean =
        (rumour.quest.isEmpty() || QuestRequirements.hasCompleted(player, rumour.quest)) &&
            player.statBase(HERBLORE) >= rumour.herbloreLevel &&
            player.statBase(SAILING) >= rumour.sailingLevel

    internal fun resume(player: Player, hunter: HunterRumourHuntersRow) {
        if (rumourOf(player, hunter) == null) return
        VarPlayerIntMapSetter.set(player, CURRENT, hunter.index + 1)
    }

    internal fun hasPart(player: Player, hunter: HunterRumourHuntersRow): Boolean =
        rumourOf(player, hunter)?.let { it.part.internalName in player.inv && partFrom(player, it) } ?: false

    private fun partFrom(player: Player, rumour: HunterRumoursRow): Boolean =
        rumours.count { it.part.id == rumour.part.id } == 1 || player.vars[PART_SOURCE] == rumour.index

    internal fun onCatch(player: Player, creature: String) {
        val rumour = currentRumour(player) ?: return
        if (rumour.creature != creature) return
        val pity = player.vars[PITY] + 1
        VarPlayerIntMapSetter.set(player, PITY, pity)
        if (rumours.any { it.part.internalName in player.inv || player.bankHas(it.part.internalName) }) return
        val bonus = HunterOutfit.partBonusPermille(player)
        val threshold =
            if (HunterOutfit.wearsFullSet(player)) rumour.pityOutfit else rumour.pity * (PERMILLE - bonus) / PERMILLE
        val pityDrop = pity >= threshold
        if (!pityDrop && random.of(rumour.rate * PERMILLE) >= PERMILLE + bonus) return
        val part = rumour.part.internalName
        if (player.inv.freeSpace() == 0 && part !in player.inv) {
            if (pityDrop) return
            objRepo.add(part, player.coords, GROUND_DURATION, receiver = player)
            player.mes(FULL_MESSAGE)
        } else {
            player.invAdd(player.inv, part)
            player.mes(FOUND_MESSAGE)
        }
        VarPlayerIntMapSetter.set(player, PART_SOURCE, rumour.index)
        VarPlayerIntMapSetter.set(player, PITY, 0)
    }

    internal fun turnIn(player: Player, hunter: HunterRumourHuntersRow): Boolean {
        val rumour = rumourOf(player, hunter) ?: return false
        if (!partFrom(player, rumour) || player.invDel(player.inv, rumour.part.id, 1).failure) return false
        VarPlayerIntMapSetter.set(player, hunterVarp(hunter), 0)
        if (player.vars[CURRENT] == hunter.index + 1) VarPlayerIntMapSetter.set(player, CURRENT, 0)
        val completed = player.vars[COMPLETED] + 1
        VarPlayerIntMapSetter.set(player, COMPLETED, completed)
        VarPlayerIntMapSetter.set(player, LAST, rumour.index)
        VarPlayerIntMapSetter.set(player, RESHUFFLED, 0)
        val noun = if (completed == 1) "rumour" else "rumours"
        player.mes("You have completed <col=ff3045>$completed</col> $noun for the Hunter Guild.")
        player.statAdvance(HUNTER, turnInXp(player.statBase(HUNTER), hunter.modifier).toDouble())
        if (player.invAdd(player.inv, hunter.lootSack.internalName).failure) {
            objRepo.add(hunter.lootSack.internalName, player.coords, GROUND_DURATION, receiver = player)
        }
        return true
    }

    internal fun whistleMessage(player: Player): String {
        val hunter = currentHunter(player)
        val rumour = hunter?.let { rumourOf(player, it) } ?: return NO_RUMOUR_MESSAGE
        return "Your current rumour target is a ${rumour.name}. You'll need to bring back ${rumour.partText}. " +
            "${hunter.name} was the source of the rumour."
    }

    private fun Player.bankHas(obj: String): Boolean = invMap[BANK]?.let { obj in it } ?: false

    private fun <T> List<T>.randomOrNull(): T? = if (isEmpty()) null else this[random.of(size)]

    internal companion object {
        const val HUNTER = "stat.hunter"
        const val HERBLORE = "stat.herblore"
        const val SAILING = "stat.sailing"
        const val BANK = "inv.bank"
        const val CURRENT = "varp.hunter_rumour_current"
        const val PITY = "varp.hunter_rumour_pity"
        const val COMPLETED = "varp.hunter_rumours_completed"
        const val UNLOCKED = "varp.hunter_rumour_unlocked"
        const val BACK_TO_BACK = "varp.hunter_rumour_back_to_back"
        const val BACK_TO_BACK_ON = 1
        const val LAST = "varp.hunter_rumour_last"
        const val RESHUFFLED = "varp.hunter_rumour_reshuffled"
        const val PART_SOURCE = "varp.hunter_rumour_part_source"
        const val PERMILLE = 1000
        const val GROUND_DURATION = 200
        const val FOUND_MESSAGE =
            "<col=229628>You find a rare piece of the creature! You should take it back to the Hunter Guild.</col>"
        const val FULL_MESSAGE =
            "<col=229628>You find a rare piece of the creature! Though without space in your inventory, it drops to " +
                "the ground.</col>"
        const val NO_RUMOUR_MESSAGE = "You don't have a rumour to investigate at the moment."

        fun turnInXp(level: Int, modifier: Int): Int = (level + 5) * modifier

        fun article(name: String): String = if (name.first().lowercaseChar() in "aeiou") "an" else "a"
    }
}

object HunterOutfit {
    internal const val HOOD = "obj.hg_hunter_hood"
    internal const val TOP = "obj.hg_hunter_top"
    internal const val LEGS = "obj.hg_hunter_legs"
    internal const val BOOTS = "obj.hg_hunter_boots"
    private val PIECES = listOf(HOOD to 2, TOP to 8, LEGS to 6, BOOTS to 4)
    private const val SET_BONUS = 5

    private fun owns(player: Player, obj: String): Boolean =
        obj in player.worn || obj in player.inv || (player.invMap[HunterRumours.BANK]?.let { obj in it } ?: false)

    internal fun catchBonusTenths(player: Player): Int {
        val worn = PIECES.filter { (obj, _) -> obj in player.worn }
        if (worn.isEmpty()) return 0
        val set = if (PIECES.all { (obj, _) -> owns(player, obj) }) SET_BONUS else 0
        return worn.sumOf { it.second } + set
    }

    internal fun partBonusPermille(player: Player): Int = catchBonusTenths(player) * 2

    internal fun wearsFullSet(player: Player): Boolean = PIECES.all { (obj, _) -> obj in player.worn }

    internal fun applyTo(chance256: Int, player: Player): Int = chance256 * (1000 + catchBonusTenths(player)) / 1000
}
