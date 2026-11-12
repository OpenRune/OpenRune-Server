package org.rsmod.content.travel.jewellery

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.area.checker.wildernessLevel
import org.rsmod.api.death.PlayerRespawnPoint
import org.rsmod.api.player.hook.ItemTeleportCaster
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldSubOp
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.script.onOpWorn4
import org.rsmod.api.script.onOpWorn5
import org.rsmod.api.script.onOpWorn6
import org.rsmod.api.script.onOpWorn7
import org.rsmod.api.script.onOpWornSubOp
import org.rsmod.api.table.TeleportJewelleryDestinationsRow
import org.rsmod.api.table.TeleportJewelleryPiecesRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.slayer.dialogue.SlayerContact.contactSlayerMaster
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TeleportJewelleryScript
@Inject
constructor(
    private val caster: ItemTeleportCaster,
    private val areaChecker: AreaChecker,
    private val respawnPoint: PlayerRespawnPoint,
    private val npcRepo: NpcRepository,
) : PluginScript() {
    private val pieces: List<Piece> = TeleportJewelleryPiecesRow.all().map(::Piece)
    private val byObj: Map<Int, Piece> =
        pieces
            .flatMap { piece -> piece.objs.map { it to piece } }
            .toMap()

    override fun ScriptContext.startup() {
        for (piece in pieces) {
            for (obj in piece.objs) {
                val name = RSCM.getReverseMapping(RSCMType.OBJ, obj)
                bindHeldOp(name, piece.row.heldOp)
                onOpHeldSubOp(name) { subOp(it.op.slot, it.slot, it.subop) }
            }
            for (obj in piece.chain.charged) {
                bindWornOps(RSCM.getReverseMapping(RSCMType.OBJ, obj), piece)
            }
        }
    }

    private fun ScriptContext.bindHeldOp(name: String, op: Int) {
        when (op) {
            2 -> onOpHeld2(name) { useJewellery(inv, it.slot, destination = null) }
            3 -> onOpHeld3(name) { useJewellery(inv, it.slot, destination = null) }
            4 -> onOpHeld4(name) { useJewellery(inv, it.slot, destination = null) }
            else -> error("Unsupported held op $op for $name")
        }
    }

    private fun ScriptContext.bindWornOps(name: String, piece: Piece) {
        val subopOp = piece.row.wornSubopOp
        if (subopOp != null) {
            onOpWornSubOp(name) {
                if (it.op == subopOp) useJewellery(player.worn, it.slot, it.subop, rubbed = true)
            }
        }
        val menuOp = piece.row.wornMenuOp
        if (menuOp != null) {
            bindWornOp(name, menuOp, destination = null)
            return
        }
        for (destination in piece.destinations.indices) {
            bindWornOp(name, destination + FIRST_WORN_OP, destination)
        }
    }

    private fun ScriptContext.bindWornOp(name: String, op: Int, destination: Int?) {
        when (op) {
            2 -> onOpWorn2(name) { useJewellery(player.worn, it.slot, destination) }
            3 -> onOpWorn3(name) { useJewellery(player.worn, it.slot, destination) }
            4 -> onOpWorn4(name) { useJewellery(player.worn, it.slot, destination) }
            5 -> onOpWorn5(name) { useJewellery(player.worn, it.slot, destination) }
            6 -> onOpWorn6(name) { useJewellery(player.worn, it.slot, destination) }
            7 -> onOpWorn7(name) { useJewellery(player.worn, it.slot, destination) }
            else -> error("Unsupported worn op $op for $name")
        }
    }

    private suspend fun ProtectedAccess.subOp(op: Int, slot: Int, subop: Int) {
        val piece = inv[slot]?.let { byObj[it.id] } ?: return
        when {
            op == piece.row.heldOp -> useJewellery(inv, slot, subop, rubbed = true)
            op == RUB_SUBMENU_OP && subop == CONTACT_MASTER -> contactSlayerMaster(npcRepo)
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
        val piece = byObj[obj.id] ?: return
        if (piece.chain.charges(obj.id) == null && !piece.chain.eternal) {
            mes(piece.row.emptyMessage)
            return
        }
        if (destination == null || rubbed) {
            piece.row.rubMessage?.let { mes(it) }
        }
        val target = target(piece, destination) ?: return
        if (piece.row.confirmWilderness == true && !confirmWilderness(target)) {
            return
        }
        with(caster) {
            cast(piece.teleportType, target) {
                when (val result = piece.chain.spend(container, slot, obj.id)) {
                    SpendResult.Vanished -> false
                    SpendResult.Unlimited -> true
                    SpendResult.Depleted -> {
                        mes(coloured(piece.row.lastChargeMessage))
                        true
                    }
                    is SpendResult.Remaining -> {
                        val left = countWord(result.charges, piece)
                        mes(coloured("Your ${piece.row.noun} has $left left."))
                        true
                    }
                }
            }
        }
    }

    private suspend fun ProtectedAccess.target(piece: Piece, destination: Int?): CoordGrid? {
        if (piece.row.respawn == true) {
            return respawnPoint.coords(player)
        }
        val options = piece.destinations
        val row =
            if (destination != null) {
                options.getOrNull(destination) ?: return null
            } else {
                options[pickDestination(options)]
            }
        if (!isUnlocked(row)) {
            mesbox(LOCKED_DESTINATION)
            return null
        }
        return landing(row)
    }

    private fun ProtectedAccess.isUnlocked(row: TeleportJewelleryDestinationsRow): Boolean {
        val quest = row.quest
        if (quest != null && !QuestRequirements.hasCompleted(player, quest)) {
            return false
        }
        val varbit = row.unlockVarbit?.let { ServerCacheManager.getVarbit(it) }
        return varbit == null || player.vars[varbit] != 0
    }

    private fun ProtectedAccess.landing(row: TeleportJewelleryDestinationsRow): CoordGrid {
        val stat = row.boostStat ?: return row.coords
        val level = row.boostLevel ?: return row.coords
        val boosted = row.boostCoords ?: return row.coords
        val statName = RSCM.getReverseMapping(RSCMType.STAT, stat.id)
        return if (statBase(statName) >= level) boosted else row.coords
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
        if (picked < page.size) {
            return offset + picked
        }
        return pickDestination(options, offset + page.size)
    }

    private suspend fun ProtectedAccess.choose(labels: List<String>): Int =
        when (labels.size) {
            1 -> 0
            2 -> choice2(labels[0], 0, labels[1], 1, TITLE)
            3 -> choice3(labels[0], 0, labels[1], 1, labels[2], 2, TITLE)
            4 -> choice4(labels[0], 0, labels[1], 1, labels[2], 2, labels[3], 3, TITLE)
            else ->
                choice5(labels[0], 0, labels[1], 1, labels[2], 2, labels[3], 3, labels[4], 4, TITLE)
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

    private fun countWord(remaining: Int, piece: Piece): String {
        val unit = if (remaining == 1) piece.row.unit else "${piece.row.unit}s"
        val amount = if (remaining < NUMBER_WORDS.size) NUMBER_WORDS[remaining] else "$remaining"
        return "$amount $unit"
    }

    private fun coloured(text: String): String = "<col=$CHARGE_COLOUR>$text</col>"

    private class Piece(val row: TeleportJewelleryPiecesRow) {
        val chain =
            ChargeChain(
                charged = row.charged.map { it.id },
                spent = row.spent?.id,
                remains = row.remains?.id,
                eternal = row.eternal == true,
            )
        val objs: List<Int> = chain.charged + listOfNotNull(chain.spent)
        val destinations: List<TeleportJewelleryDestinationsRow> = row.destinations
        val teleportType =
            if (row.wildernessLevel >= DRAGONSTONE_WILDERNESS) {
                TeleportType.MemberLevel30
            } else {
                TeleportType.Standard
            }
    }

    private companion object {
        private const val FIRST_WORN_OP = 2
        private const val DRAGONSTONE_WILDERNESS = 30
        private const val RUB_SUBMENU_OP = 2
        private const val CONTACT_MASTER = 0
        private const val TITLE = "Where would you like to teleport to?"
        private const val MORE = "More..."
        private const val MAX_CHOICES = 5
        private const val LOCKED_DESTINATION = "You have not yet unlocked that destination."
        private const val CHARGE_COLOUR = "7f007f"
        private val NUMBER_WORDS =
            listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
    }
}
