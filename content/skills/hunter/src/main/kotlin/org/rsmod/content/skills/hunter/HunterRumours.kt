package org.rsmod.content.skills.hunter

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.table.hunter.HunterRumourHuntersRow
import org.rsmod.api.table.hunter.HunterRumoursRow
import org.rsmod.game.entity.Player

@Singleton
class HunterRumours @Inject constructor(private val objRepo: ObjRepository, private val random: GameRandom) {
    val hunters: List<HunterRumourHuntersRow> by lazy { HunterRumourHuntersRow.all().sortedBy { it.index } }
    val rumours: List<HunterRumoursRow> by lazy { HunterRumoursRow.all().sortedBy { it.index } }

    fun hunterVarp(hunter: HunterRumourHuntersRow): String = "varp.hunter_rumour_${hunter.name.lowercase()}"

    fun rumourOf(player: Player, hunter: HunterRumourHuntersRow): HunterRumoursRow? =
        rumours.firstOrNull { it.index == player.vars[hunterVarp(hunter)] }

    fun currentHunter(player: Player): HunterRumourHuntersRow? =
        hunters.firstOrNull { it.index + 1 == player.vars[CURRENT] }

    fun currentRumour(player: Player): HunterRumoursRow? = currentHunter(player)?.let { rumourOf(player, it) }

    fun assign(player: Player, hunter: HunterRumourHuntersRow, forced: HunterRumoursRow? = null): HunterRumoursRow? {
        val level = player.statBase(HUNTER)
        if (level < hunter.level) return null
        val taken = hunters.filter { it != hunter }.mapNotNull { rumourOf(player, it)?.index }.toSet()
        val repeat = if (player.vars[BACK_TO_BACK] == BACK_TO_BACK_OFF) player.vars[LAST] else -1
        val options =
            rumours.filter {
                it.hunters and (1 shl hunter.index) != 0 && level >= it.level && it.index !in taken &&
                    it.index != repeat
            }
        val rumour = forced?.takeIf { it in options } ?: options.randomOrNull() ?: return null
        VarPlayerIntMapSetter.set(player, hunterVarp(hunter), rumour.index)
        VarPlayerIntMapSetter.set(player, CURRENT, hunter.index + 1)
        VarPlayerIntMapSetter.set(player, PITY, 0)
        return rumour
    }

    fun resume(player: Player, hunter: HunterRumourHuntersRow) {
        if (rumourOf(player, hunter) == null) return
        VarPlayerIntMapSetter.set(player, CURRENT, hunter.index + 1)
    }

    fun hasPart(player: Player, hunter: HunterRumourHuntersRow): Boolean =
        rumourOf(player, hunter)?.let { it.part.internalName in player.inv } ?: false

    fun onCatch(player: Player, creature: String) {
        val rumour = currentRumour(player) ?: return
        if (rumour.creature.internalName != creature) return
        val pity = player.vars[PITY] + 1
        VarPlayerIntMapSetter.set(player, PITY, pity)
        if (rumours.any { it.part.internalName in player.inv }) return
        val pityDrop = pity >= rumour.pity
        if (!pityDrop && random.of(rumour.rate) != 0) return
        if (player.inv.freeSpace() == 0 && pityDrop) return
        player.invAddOrDrop(objRepo, rumour.part.internalName)
        VarPlayerIntMapSetter.set(player, PITY, 0)
        player.mes(FOUND_MESSAGE)
    }

    fun turnIn(player: Player, hunter: HunterRumourHuntersRow): Boolean {
        val rumour = rumourOf(player, hunter) ?: return false
        if (player.invDel(player.inv, rumour.part.id, 1).failure) return false
        VarPlayerIntMapSetter.set(player, hunterVarp(hunter), 0)
        if (player.vars[CURRENT] == hunter.index + 1) VarPlayerIntMapSetter.set(player, CURRENT, 0)
        VarPlayerIntMapSetter.set(player, COMPLETED, player.vars[COMPLETED] + 1)
        VarPlayerIntMapSetter.set(player, LAST, rumour.index)
        player.statAdvance(HUNTER, turnInXp(player.statBase(HUNTER), hunter.modifier).toDouble())
        player.invAddOrDrop(objRepo, hunter.lootSack.internalName)
        return true
    }

    fun whistleMessage(player: Player): String {
        val hunter = currentHunter(player)
        val rumour = hunter?.let { rumourOf(player, it) } ?: return NO_RUMOUR_MESSAGE
        return "Your current rumour target is ${article(rumour.name)} ${rumour.name}. You'll need to bring back " +
            "some of its ${rumour.partText}. ${hunter.name} was the source of the rumour."
    }

    private fun <T> List<T>.randomOrNull(): T? = if (isEmpty()) null else this[random.of(size)]

    companion object {
        const val HUNTER = "stat.hunter"
        const val CURRENT = "varp.hunter_rumour_current"
        const val PITY = "varp.hunter_rumour_pity"
        const val COMPLETED = "varp.hunter_rumours_completed"
        const val UNLOCKED = "varp.hunter_rumour_unlocked"
        const val BACK_TO_BACK = "varp.hunter_rumour_back_to_back"
        const val BACK_TO_BACK_OFF = 1
        const val LAST = "varp.hunter_rumour_last"
        const val FOUND_MESSAGE =
            "<col=229628>You find a rare piece of the creature! You should take it back to the Hunter Guild.</col>"
        const val NO_RUMOUR_MESSAGE = "You don't have a rumour to investigate at the moment."

        fun turnInXp(level: Int, modifier: Int): Int = (level + 5) * modifier

        fun article(name: String): String = if (name.first().lowercaseChar() in "aeiou") "an" else "a"
    }
}
