package org.rsmod.content.travel.jewellery

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.area.checker.wildernessLevel
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldSubOp
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.script.onOpWorn4
import org.rsmod.api.script.onOpWorn5
import org.rsmod.api.script.onOpWorn6
import org.rsmod.api.script.onOpWorn7
import org.rsmod.api.table.TeleportJewelleryDestinationsRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TeleportJewelleryScript
@Inject
constructor(
    private val caster: ItemTeleportCaster,
    private val areaChecker: AreaChecker,
) : PluginScript() {
    private val pieces = Piece.entries
    private val entries: Map<Int, Entry> = buildEntries()
    private val destinations: Map<String, List<TeleportJewelleryDestinationsRow>> =
        TeleportJewelleryDestinationsRow.all()
            .sortedBy { it.rowId }
            .groupBy { it.piece }

    override fun ScriptContext.startup() {
        for (piece in pieces) {
            val count = destinations.getValue(piece.destinations).size
            for (item in piece.charged + piece.eternal) {
                onOpHeld4(item) { useJewellery(inv, it.slot, destination = null) }
                onOpHeldSubOp(item) { rubSubOp(it.op, it.slot, it.subop) }
                bindWornOps(item, count)
            }
            piece.spent?.let {
                onOpHeld4(it) { useJewellery(inv, it.slot, destination = null) }
                onOpHeldSubOp(it) { rubSubOp(it.op, it.slot, it.subop) }
            }
        }
    }

    private fun ScriptContext.bindWornOps(item: String, count: Int) {
        if (count >= 1) onOpWorn2(item) { useJewellery(player.worn, it.slot, 0) }
        if (count >= 2) onOpWorn3(item) { useJewellery(player.worn, it.slot, 1) }
        if (count >= 3) onOpWorn4(item) { useJewellery(player.worn, it.slot, 2) }
        if (count >= 4) onOpWorn5(item) { useJewellery(player.worn, it.slot, 3) }
        if (count >= 5) onOpWorn6(item) { useJewellery(player.worn, it.slot, 4) }
        if (count >= 6) onOpWorn7(item) { useJewellery(player.worn, it.slot, 5) }
    }

    private suspend fun ProtectedAccess.rubSubOp(op: HeldOp, slot: Int, subop: Int) {
        if (op == HeldOp.Op4) {
            useJewellery(inv, slot, subop - 1, rubbed = true)
        }
    }

    private suspend fun ProtectedAccess.useJewellery(
        container: Inventory,
        slot: Int,
        destination: Int?,
        rubbed: Boolean = false,
    ) {
        if (actionDelay > mapClock) {
            return
        }
        val obj = container[slot] ?: return
        val entry = entries[obj.id] ?: return
        val piece = entry.piece
        if (entry.charges == 0) {
            mes(piece.emptyMessage)
            return
        }

        val options = destinations.getValue(piece.destinations)
        val row =
            if (destination != null) {
                if (rubbed) {
                    piece.rubMessage?.let { mes(it) }
                }
                options.getOrNull(destination) ?: return
            } else {
                piece.rubMessage?.let { mes(it) }
                options[pickDestination(options)]
            }

        val questKey = row.quest
        if (questKey.isNotEmpty() && !QuestRequirements.hasCompleted(player, questKey)) {
            mesbox(LOCKED_DESTINATION)
            return
        }

        val target = landing(piece, row)
        if (piece.confirmWilderness && !confirmWilderness(target)) {
            return
        }
        with(caster) {
            cast(piece.teleportType, target) {
                val current = container[slot]
                if (current == null || current.id != obj.id) {
                    false
                } else {
                    spendCharge(container, slot, entry)
                    true
                }
            }
        }
    }

    private fun ProtectedAccess.landing(piece: Piece, row: TeleportJewelleryDestinationsRow): CoordGrid {
        val farmingGuildInside = piece == Piece.Skills && row.label == FARMING_GUILD
        if (farmingGuildInside && statBase("stat.farming") >= FARMING_GUILD_INNER_LEVEL) {
            return FARMING_GUILD_INNER
        }
        return row.coords
    }

    private suspend fun ProtectedAccess.pickDestination(
        options: List<TeleportJewelleryDestinationsRow>,
        offset: Int = 0,
    ): Int {
        if (options.size - offset <= MAX_CHOICES) {
            return offset + choose(options.drop(offset).map { "${it.label}." })
        }
        val page = options.subList(offset, offset + MAX_CHOICES - 1).map { "${it.label}." }
        val picked = choose(page + MORE)
        return if (picked < page.size) offset + picked else pickDestination(options, offset + page.size)
    }

    private suspend fun ProtectedAccess.choose(labels: List<String>): Int =
        when (labels.size) {
            1 -> 0
            2 -> choice2(labels[0], 0, labels[1], 1, TITLE)
            3 -> choice3(labels[0], 0, labels[1], 1, labels[2], 2, TITLE)
            4 -> choice4(labels[0], 0, labels[1], 1, labels[2], 2, labels[3], 3, TITLE)
            else -> choice5(labels[0], 0, labels[1], 1, labels[2], 2, labels[3], 3, labels[4], 4, TITLE)
        }

    private suspend fun ProtectedAccess.confirmWilderness(target: CoordGrid): Boolean {
        val level = target.wildernessLevel(areaChecker)
        if (level <= 0) {
            return true
        }
        return choice2(
            "Okay, teleport to level $level Wilderness.",
            true,
            "No.",
            false,
            "That's in level $level Wilderness.",
        )
    }

    private fun ProtectedAccess.spendCharge(container: Inventory, slot: Int, entry: Entry) {
        val piece = entry.piece
        if (entry.charges == UNLIMITED) {
            mes(UNLIMITED_MESSAGE)
            return
        }
        val remaining = entry.charges - 1
        if (remaining == 0) {
            container[slot] = piece.spent?.let { InvObj(it) }
            mes(coloured(piece.lastChargeMessage))
            return
        }
        container[slot] = InvObj(piece.charged[piece.charged.size - remaining])
        mes(coloured("Your ${piece.noun} has ${countWord(remaining, piece)} left."))
    }

    private fun countWord(remaining: Int, piece: Piece): String {
        val unit = if (remaining == 1) piece.unit else "${piece.unit}s"
        val amount = if (remaining < NUMBER_WORDS.size) NUMBER_WORDS[remaining] else "$remaining"
        return "$amount $unit"
    }

    private fun coloured(text: String): String = "<col=$CHARGE_COLOUR>$text</col>"

    private fun buildEntries(): Map<Int, Entry> {
        val map = HashMap<Int, Entry>()
        for (piece in Piece.entries) {
            piece.charged.forEachIndexed { index, item ->
                map[item.asRSCM(RSCMType.OBJ)] = Entry(piece, piece.charged.size - index)
            }
            for (item in piece.eternal) {
                map[item.asRSCM(RSCMType.OBJ)] = Entry(piece, UNLIMITED)
            }
            piece.spent?.let { map[it.asRSCM(RSCMType.OBJ)] = Entry(piece, 0) }
        }
        return map
    }

    private data class Entry(val piece: Piece, val charges: Int)

    private enum class Piece(
        val destinations: String,
        val noun: String,
        val unit: String,
        val charged: List<String>,
        val spent: String?,
        val rubMessage: String?,
        val emptyMessage: String,
        val lastChargeMessage: String,
        val teleportType: TeleportType = TeleportType.Standard,
        val eternal: List<String> = emptyList(),
        val confirmWilderness: Boolean = false,
    ) {
        Glory(
            destinations = "glory",
            noun = "amulet",
            unit = "charge",
            charged =
                listOf(
                    "obj.amulet_of_glory_6",
                    "obj.amulet_of_glory_5",
                    "obj.amulet_of_glory_4",
                    "obj.amulet_of_glory_3",
                    "obj.amulet_of_glory_2",
                    "obj.amulet_of_glory_1",
                ),
            spent = "obj.amulet_of_glory",
            rubMessage = "You rub the amulet...",
            emptyMessage = "Your amulet hasn't got any charges left.",
            lastChargeMessage = "You use your amulet's last charge.",
            teleportType = TeleportType.MemberLevel30,
            eternal = listOf("obj.amulet_of_glory_inf"),
        ),
        GloryTrimmed(
            destinations = "glory",
            noun = "amulet",
            unit = "charge",
            charged =
                listOf(
                    "obj.trail_amulet_of_glory_6",
                    "obj.trail_amulet_of_glory_5",
                    "obj.trail_amulet_of_glory_4",
                    "obj.trail_amulet_of_glory_3",
                    "obj.trail_amulet_of_glory_2",
                    "obj.trail_amulet_of_glory_1",
                ),
            spent = "obj.trail_amulet_of_glory",
            rubMessage = "You rub the amulet...",
            emptyMessage = "Your amulet hasn't got any charges left.",
            lastChargeMessage = "You use your amulet's last charge.",
            teleportType = TeleportType.MemberLevel30,
        ),
        Games(
            destinations = "games",
            noun = "games necklace",
            unit = "use",
            charged =
                listOf(
                    "obj.necklace_of_minigames_8",
                    "obj.necklace_of_minigames_7",
                    "obj.necklace_of_minigames_6",
                    "obj.necklace_of_minigames_5",
                    "obj.necklace_of_minigames_4",
                    "obj.necklace_of_minigames_3",
                    "obj.necklace_of_minigames_2",
                    "obj.necklace_of_minigames_1",
                ),
            spent = null,
            rubMessage = "You rub the necklace...",
            emptyMessage = "Your necklace hasn't got any charges left.",
            lastChargeMessage = "Your games necklace crumbles to dust.",
        ),
        Dueling(
            destinations = "dueling",
            noun = "ring of dueling",
            unit = "use",
            charged =
                listOf(
                    "obj.ring_of_dueling_8",
                    "obj.ring_of_dueling_7",
                    "obj.ring_of_dueling_6",
                    "obj.ring_of_dueling_5",
                    "obj.ring_of_dueling_4",
                    "obj.ring_of_dueling_3",
                    "obj.ring_of_dueling_2",
                    "obj.ring_of_dueling_1",
                ),
            spent = null,
            rubMessage = "You rub the ring...",
            emptyMessage = "Your ring hasn't got any charges left.",
            lastChargeMessage = "Your ring of dueling crumbles to dust.",
        ),
        Skills(
            destinations = "skills",
            noun = "skills necklace",
            unit = "charge",
            charged =
                listOf(
                    "obj.jewl_necklace_of_skills_6",
                    "obj.jewl_necklace_of_skills_5",
                    "obj.jewl_necklace_of_skills_4",
                    "obj.jewl_necklace_of_skills_3",
                    "obj.jewl_necklace_of_skills_2",
                    "obj.jewl_necklace_of_skills_1",
                ),
            spent = "obj.jewl_necklace_of_skills",
            rubMessage = null,
            emptyMessage =
                "You will need to recharge your skills necklace before you can use it again.",
            lastChargeMessage = "You use your skills necklace's last charge.",
            teleportType = TeleportType.MemberLevel30,
        ),
        Combat(
            destinations = "combat",
            noun = "combat bracelet",
            unit = "charge",
            charged =
                listOf(
                    "obj.jewl_bracelet_of_combat_6",
                    "obj.jewl_bracelet_of_combat_5",
                    "obj.jewl_bracelet_of_combat_4",
                    "obj.jewl_bracelet_of_combat_3",
                    "obj.jewl_bracelet_of_combat_2",
                    "obj.jewl_bracelet_of_combat_1",
                ),
            spent = "obj.jewl_bracelet_of_combat",
            rubMessage = "You rub the bracelet...",
            emptyMessage =
                "You will need to recharge your combat bracelet before you can use it again.",
            lastChargeMessage = "You use your combat bracelet's last charge.",
            teleportType = TeleportType.MemberLevel30,
        ),
        Burning(
            destinations = "burning",
            noun = "burning amulet",
            unit = "use",
            charged =
                listOf(
                    "obj.burning_amulet_5",
                    "obj.burning_amulet_4",
                    "obj.burning_amulet_3",
                    "obj.burning_amulet_2",
                    "obj.burning_amulet_1",
                ),
            spent = null,
            rubMessage = "You rub the amulet...",
            emptyMessage = "Your amulet hasn't got any charges left.",
            lastChargeMessage = "Your burning amulet crumbles away.",
            confirmWilderness = true,
        ),
        Digsite(
            destinations = "digsite",
            noun = "Digsite Pendant",
            unit = "use",
            charged =
                listOf(
                    "obj.necklace_of_digsite_5",
                    "obj.necklace_of_digsite_4",
                    "obj.necklace_of_digsite_3",
                    "obj.necklace_of_digsite_2",
                    "obj.necklace_of_digsite_1",
                ),
            spent = null,
            rubMessage = "You rub the pendant...",
            emptyMessage = "Your pendant hasn't got any charges left.",
            lastChargeMessage = "Your Digsite Pendant crumbles to dust.",
        ),
        Passage(
            destinations = "passage",
            noun = "necklace of passage",
            unit = "use",
            charged =
                listOf(
                    "obj.necklace_of_passage_5",
                    "obj.necklace_of_passage_4",
                    "obj.necklace_of_passage_3",
                    "obj.necklace_of_passage_2",
                    "obj.necklace_of_passage_1",
                ),
            spent = null,
            rubMessage = "You rub the necklace...",
            emptyMessage = "Your necklace hasn't got any charges left.",
            lastChargeMessage = "Your necklace of passage crumbles away.",
        ),
        Wealth(
            destinations = "wealth",
            noun = "ring of wealth",
            unit = "charge",
            charged =
                listOf(
                    "obj.ring_of_wealth_5",
                    "obj.ring_of_wealth_4",
                    "obj.ring_of_wealth_3",
                    "obj.ring_of_wealth_2",
                    "obj.ring_of_wealth_1",
                ),
            spent = "obj.ring_of_wealth",
            rubMessage = "You rub the ring...",
            emptyMessage = "Your ring hasn't got any charges left.",
            lastChargeMessage = "You use your ring's last charge.",
            teleportType = TeleportType.MemberLevel30,
        ),
        WealthImbued(
            destinations = "wealth",
            noun = "ring of wealth",
            unit = "charge",
            charged =
                listOf(
                    "obj.ring_of_wealth_i5",
                    "obj.ring_of_wealth_i4",
                    "obj.ring_of_wealth_i3",
                    "obj.ring_of_wealth_i2",
                    "obj.ring_of_wealth_i1",
                ),
            spent = "obj.ring_of_wealth_i",
            rubMessage = "You rub the ring...",
            emptyMessage = "Your ring hasn't got any charges left.",
            lastChargeMessage = "You use your ring's last charge.",
            teleportType = TeleportType.MemberLevel30,
        ),
    }

    private companion object {
        private const val UNLIMITED = -1
        private const val UNLIMITED_MESSAGE =
            "No charges were used since it holds unlimited charges."
        private const val TITLE = "Where would you like to teleport to?"
        private const val MORE = "More..."
        private const val MAX_CHOICES = 5
        private const val LOCKED_DESTINATION = "You have not yet unlocked that destination."
        private const val CHARGE_COLOUR = "7f007f"
        private val NUMBER_WORDS =
            listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")

        private const val FARMING_GUILD = "Farming Guild"
        private const val FARMING_GUILD_INNER_LEVEL = 45
        private val FARMING_GUILD_INNER = CoordGrid(1248, 3725, 0)
    }
}
