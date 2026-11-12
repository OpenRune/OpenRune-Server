package org.rsmod.content.travel.teleports

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldSubOp
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.script.onOpWorn4
import org.rsmod.api.script.onOpWorn5
import org.rsmod.api.script.onOpWorn6
import org.rsmod.api.script.onOpWorn7
import org.rsmod.api.script.onOpWorn8
import org.rsmod.api.script.onOpWorn9
import org.rsmod.content.travel.teleports.TeleportCharges.checkCharges
import org.rsmod.content.travel.teleports.TeleportCharges.hasCharge
import org.rsmod.content.travel.teleports.TeleportCharges.spendCharge
import org.rsmod.game.inv.Inventory
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * Every item in the `teleport_item` table: Kharedst's memoirs, Drakan's medallion, achievement
 * diary rewards, the royal and grand seed pods, the camulet and the ectophial.
 *
 * Which op does what is read from the obj's own cache ops, so the table only says where to go:
 * - the held `Teleport` op (or `Rub`, `Reminisce`, `Commune`, `Empty`) opens a menu of the
 *   destinations, and the client's sub-menu under it sends the clicked entry as a sub-op;
 * - held ops named `... Teleport` (Ardougne cloaks) go straight to a destination each;
 * - worn ops go to the destinations in order, unless one is called `Teleport`, which opens the
 *   menu.
 */
class TeleportItemScript
@Inject
constructor(
    private val teleportValidator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (item in TeleportItems.all) {
            item.objs.forEach { register(item, it) }
        }
    }

    private fun ScriptContext.register(item: TeleportItem, obj: String) {
        val type = obj.type()
        val ops = type.interfaceOptions
        val wearOp = type.paramOrNull(params.wear_op_index)
        fun usable(index: Int) = index != -1 && index + 1 != wearOp

        val directOps = directHeldOps(item, ops, ::usable)
        val menuOp =
            MENU_OPS.firstNotNullOfOrNull { name -> ops.indexOf(name).takeIf(::usable) } ?: -1
        if (directOps.isNotEmpty()) {
            for ((op, destination) in directOps) {
                onHeldOp(op, obj) { inv, slot -> teleport(item, inv, slot, destination) }
            }
        } else if (menuOp != -1) {
            onHeldOp(menuOp, obj) { inv, slot -> menu(item, inv, slot) }
            onOpHeldSubOp(obj) { event ->
                if (event.op.slot != menuOp + 1) {
                    mes("That option is not available yet.")
                    return@onOpHeldSubOp
                }
                val destination = item.destinations.getOrNull(event.subop)
                if (destination == null) {
                    logger.warn { "Unknown sub-op ${event.subop} on $obj" }
                    return@onOpHeldSubOp
                }
                teleport(item, event.inventory, event.slot, destination)
            }
        } else {
            logger.warn { "No teleport op on $obj: $ops" }
        }

        if (item.chargeVarbit != null) {
            CHECK_OPS.firstNotNullOfOrNull { ops.indexOf(it).takeIf(::usable) }?.let { check ->
                onHeldOp(check, obj) { _, _ -> checkCharges(item) }
            }
        }
        registerWorn(item, obj, type)
    }

    /**
     * Held ops that go straight to one destination: an op named after a destination ("Gem Mine",
     * "Nardah"), or an op called "... Teleport" ("Monastery Teleport"), which takes the next
     * destination in order.
     */
    private fun directHeldOps(
        item: TeleportItem,
        ops: List<String?>,
        usable: (Int) -> Boolean,
    ): Map<Int, TeleportDestination> {
        val byName = item.destinations.associateBy { it.name }
        var next = 0
        val direct = linkedMapOf<Int, TeleportDestination>()
        for ((index, op) in ops.withIndex()) {
            if (op == null || !usable(index)) {
                continue
            }
            val destination =
                byName[op]
                    ?: item.destinations.getOrNull(next).takeIf { op.endsWith(" Teleport") }
                    ?: continue
            if (byName[op] == null) {
                next++
            }
            direct[index] = destination
        }
        return direct
    }

    private fun ScriptContext.registerWorn(item: TeleportItem, obj: String, type: ItemServerType) {
        val names = WORN_OP_PARAMS.map { type.paramOrNull(it) }
        val hasMenu = "Teleport" in names
        var next = 0
        for ((index, name) in names.withIndex()) {
            val op = index + FIRST_WORN_PARAM_OP
            when {
                name == null -> continue
                name in CHECK_OPS ->
                    if (item.chargeVarbit != null) onWornOp(op, obj) { checkCharges(item) }
                name == "Teleport" -> onWornOp(op, obj) { slot -> menu(item, worn, slot) }
                !hasMenu && next < item.destinations.size -> {
                    val destination = item.destinations[next++]
                    onWornOp(op, obj) { slot -> teleport(item, worn, slot, destination) }
                }
            }
        }
    }

    private fun ScriptContext.onHeldOp(
        index: Int,
        obj: String,
        action: suspend ProtectedAccess.(inv: Inventory, slot: Int) -> Unit,
    ) {
        when (index) {
            0 -> onOpHeld1(obj) { action(it.inventory, it.slot) }
            1 -> onOpHeld2(obj) { action(it.inventory, it.slot) }
            2 -> onOpHeld3(obj) { action(it.inventory, it.slot) }
            3 -> onOpHeld4(obj) { action(it.inventory, it.slot) }
            4 -> onOpHeld5(obj) { action(it.inventory, it.slot) }
            else -> logger.warn { "Unsupported held op slot $index for $obj" }
        }
    }

    private fun ScriptContext.onWornOp(
        op: Int,
        obj: String,
        action: suspend ProtectedAccess.(slot: Int) -> Unit,
    ) {
        when (op) {
            2 -> onOpWorn2(obj) { action(it.slot) }
            3 -> onOpWorn3(obj) { action(it.slot) }
            4 -> onOpWorn4(obj) { action(it.slot) }
            5 -> onOpWorn5(obj) { action(it.slot) }
            6 -> onOpWorn6(obj) { action(it.slot) }
            7 -> onOpWorn7(obj) { action(it.slot) }
            8 -> onOpWorn8(obj.type()) { action(it.slot) }
            9 -> onOpWorn9(obj.type()) { action(it.slot) }
            else -> logger.warn { "Unsupported worn op $op for $obj" }
        }
    }

    private suspend fun ProtectedAccess.menu(item: TeleportItem, inv: Inventory, slot: Int) {
        val destinations = item.destinations
        if (destinations.size == 1) {
            teleport(item, inv, slot, destinations[0])
            return
        }
        val names = destinations.map { it.name }
        val choice =
            when (names.size) {
                2 -> choice3(names[0], 0, names[1], 1, NOWHERE, -1, title = MENU_TITLE)
                3 -> choice4(names[0], 0, names[1], 1, names[2], 2, NOWHERE, -1, title = MENU_TITLE)
                4 ->
                    choice5(
                        names[0],
                        0,
                        names[1],
                        1,
                        names[2],
                        2,
                        names[3],
                        3,
                        NOWHERE,
                        -1,
                        title = MENU_TITLE,
                    )
                else -> menu(MENU_TITLE, hotkeys = true, choices = names + NOWHERE)
            }
        val destination = destinations.getOrNull(choice) ?: return
        teleport(item, inv, slot, destination)
    }

    private suspend fun ProtectedAccess.teleport(
        item: TeleportItem,
        inv: Inventory,
        slot: Int,
        destination: TeleportDestination,
    ) {
        if (actionDelay > mapClock) {
            return
        }
        if (!collision.isZoneValid(destination.coords)) {
            mes("${destination.name} is not on this server's map yet.")
            return
        }
        if (!hasCharge(item)) {
            return
        }
        if (destination.wilderness && !confirmWilderness(destination)) {
            return
        }
        val denial = teleportValidator.validate(player, item.teleportType, areaChecker)
        if (denial != null) {
            mes(denial, ChatType.Engine)
            return
        }
        if (!spendCharge(item, inv, slot)) {
            return
        }
        ifClose()
        actionDelay = mapClock + ACTION_DELAY
        item.departMessage?.let(::mes)
        anim(TELEPORT_ANIM)
        spotanim(TELEPORT_SPOTANIM, height = TELEPORT_SPOTANIM_HEIGHT)
        soundSynth(TELEPORT_SOUND)
        delay(TELEPORT_DELAY)
        // The teleport was validated before the charge was spent; a teleblock landing during the
        // cast must not strand a charge that is already gone.
        telejump(destination.coords, TeleportType.Exempt)
        anim(TELEPORT_END_ANIM)
        item.arriveMessage?.let(::mes)
    }

    private suspend fun ProtectedAccess.confirmWilderness(destination: TeleportDestination) =
        choice2(
            "Yes, teleport me into the Wilderness.",
            true,
            "No, I will stay here.",
            false,
            title = "${destination.name} is in the Wilderness. Teleport anyway?",
        )

    private fun String.type(): ItemServerType =
        ServerCacheManager.getItem(asRSCM(RSCMType.OBJ)) ?: error("Missing obj: $this")

    private companion object {
        private val logger = InlineLogger()

        private const val MENU_TITLE = "Where would you like to teleport to?"
        private const val NOWHERE = "Nowhere"

        private val MENU_OPS = listOf("Teleport", "Rub", "Reminisce", "Commune", "Empty", "Squash")
        private val CHECK_OPS = listOf("Check", "Check-charge")

        /** Worn op 1 is `Remove`; the obj's `wear_op1` param is worn op 2. */
        private const val FIRST_WORN_PARAM_OP = 2
        private val WORN_OP_PARAMS =
            listOf(
                params.wear_op1,
                params.wear_op2,
                params.wear_op3,
                params.wear_op4,
                params.wear_op5,
                params.wear_op6,
                params.wear_op7,
                params.wear_op8,
            )

        private const val TELEPORT_ANIM = "seq.human_castteleport"
        private const val TELEPORT_END_ANIM = "seq.human_castteleport_reverse"
        private const val TELEPORT_SPOTANIM = "spotanim.teleport_casting"
        private const val TELEPORT_SPOTANIM_HEIGHT = 92
        private const val TELEPORT_SOUND = "synth.teleport_all"
        private const val TELEPORT_DELAY = 3
        private const val ACTION_DELAY = 4
    }
}
