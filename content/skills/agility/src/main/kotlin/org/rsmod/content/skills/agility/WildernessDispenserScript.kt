package org.rsmod.content.skills.agility

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import java.util.Locale
import org.rsmod.api.enums.AgilityEnums
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.table.agility.AgilityDispenserLootRow
import org.rsmod.content.other.lootingbag.LootingBag
import org.rsmod.content.skills.agility.WildernessDispenser.FEE
import org.rsmod.content.skills.agility.WildernessDispenser.Proceed
import org.rsmod.content.skills.agility.WildernessDispenser.feePaid
import org.rsmod.content.skills.agility.WildernessDispenser.paymentWarningOff
import org.rsmod.content.skills.agility.WildernessDispenser.rewardAvailable
import org.rsmod.content.skills.agility.WildernessDispenser.streak
import org.rsmod.content.skills.agility.WildernessDispenser.ticketAvailable
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Wilderness Agility dispenser (https://oldschool.runescape.wiki/w/Agility_dispenser). Tag
 * collects the last finished lap's ticket and, once the fee is paid, its loot for the streak of
 * paid laps into an open looting bag; Pay takes the fee; Redeem turns every carried ticket into
 * Agility xp.
 */
class WildernessDispenserScript @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    private val brackets by lazy { AgilityDispenserLootRow.all() }
    private val supplies by lazy {
        AgilityEnums.agility_dispenser_supplies.backing.mapNotNull { (obj, weight) ->
            weight?.let { obj to it }
        }
    }
    private val ticketRates by lazy {
        AgilityEnums.agility_ticket_xp.backing
            .mapNotNull { (tickets, xp) -> xp?.let { tickets to it } }
            .toMap()
    }

    override fun ScriptContext.startup() {
        onOpLoc1(DISPENSER) { tag() }
        onOpLoc2(DISPENSER) { pay() }
        onOpLoc3(DISPENSER) { redeem() }
        onPlayerLogout { WildernessDispenser.loggedOut(player) }
    }

    private suspend fun ProtectedAccess.tag() {
        val ticket = player.ticketAvailable
        if (!player.rewardAvailable) {
            if (!ticket) {
                mes(NO_LAP)
                return
            }
            giveTicket()
            return
        }
        if (!LootingBag.isOpen(player)) {
            mesbox(NO_OPEN_BAG)
            return
        }
        val bracket = WildernessDispenser.bracket(brackets, player.streak)
        val clue = random.of(if (RING_OF_WEALTH_I in worn) CLUE_CHANCE_RING else CLUE_CHANCE) == 0
        val resource = WildernessDispenser.weighted(random, bracket.resources.map { it to it.t1 })
        val resourceCount = random.of(resource.t2, resource.t3)
        val armour = WildernessDispenser.weighted(random, bracket.armour.map { it to it.t1 })
        // UNVERIFIED: that the bag holds them noted; captures only show them leaving the inventory.
        val loot = listOf(noted(resource.t0, resourceCount), noted(armour.t0, 1))
        if (!LootingBag.add(player, loot)) {
            mesbox(BAG_FULL)
            return
        }
        if (ticket) {
            giveTicket()
        }
        player.rewardAvailable = false
        val awarded = mutableListOf<String>()
        if (clue) {
            // TODO(content): a scroll box instead once X Marks the Spot is done.
            invAddOrDrop(objRepo, CLUE)
            // UNVERIFIED: how the line names the clue; no capture rolled one.
            awarded += highlight("a clue scroll")
        }
        awarded += highlight("$resourceCount x ${resource.t0.name}")
        awarded += highlight("1 x ${armour.t0.name}")
        val extra =
            if (inv.freeSpace() > 0) {
                val supply = WildernessDispenser.weighted(random, supplies)
                invAddOrDropType(objRepo, supply)
                ", and an extra ${highlight(supply.name)}"
            } else {
                ""
            }
        mes("You have been awarded ${awarded.joinedWithAnd()}$extra from the Agility dispenser.")
    }

    private fun ProtectedAccess.giveTicket() {
        player.ticketAvailable = false
        invAddOrDrop(objRepo, TICKET)
        mes("You received ${highlight("a Wilderness agility ticket")} from the Agility dispenser.")
    }

    private fun ProtectedAccess.noted(obj: ItemServerType, count: Int): InvObj =
        InvObj(ocCert(obj.internalName), count)

    private suspend fun ProtectedAccess.pay() {
        if (player.feePaid) {
            mesbox("You have already paid 150,000 coins to receive a reward after each lap.")
            return
        }
        if (inv.freeSpace() == 0) {
            mesbox("You need at least 1 free inventory slot before depositing coins.")
            return
        }
        if (!player.paymentWarningOff) {
            val proceed =
                choice3(
                    "Yes.",
                    Proceed.Yes,
                    "Yes, and don't ask again.",
                    Proceed.YesAlways,
                    "No.",
                    Proceed.No,
                    title = PAYMENT_WARNING,
                )
            when (proceed) {
                Proceed.No -> return
                Proceed.YesAlways -> player.paymentWarningOff = true
                Proceed.Yes -> Unit
            }
        }
        if (invTotal(inv, COINS) < FEE || invDel(inv, COINS, FEE).failure) {
            mesbox("You need 150,000 coins to pay to receive additional rewards.")
            return
        }
        player.feePaid = true
        spam("You have paid 150,000 coins to receive a reward after each lap.")
        if (!LootingBag.owns(player)) {
            invAddOrDrop(objRepo, LootingBag.BAG)
            spam("You received ${highlight("a Looting bag")} from the Agility dispenser.")
        }
    }

    private suspend fun ProtectedAccess.redeem() {
        val tickets = invTotal(inv, TICKET)
        if (tickets == 0) {
            mesbox("You do not have any Wilderness agility tickets to redeem.")
            return
        }
        val perTicket = WildernessDispenser.ticketXpTenths(tickets, ticketRates)
        if (invDel(inv, TICKET, tickets).failure) {
            return
        }
        val xpTenths = tickets.toLong() * perTicket
        statAdvance(AGILITY, xpTenths / 10.0)
        soundSynth(REDEEM_SYNTH)
        val noun = if (tickets == 1) "ticket" else "tickets"
        mes(
            "You redeem ${tickets.formatted()} Wilderness agility $noun and have been awarded " +
                "${(xpTenths / 10).formatted()} Agility XP!"
        )
    }

    private fun highlight(text: String): String = "<col=ef1020>$text</col>"

    private fun Number.formatted(): String = String.format(Locale.US, "%,d", this)

    private fun List<String>.joinedWithAnd(): String =
        if (size <= 1) joinToString() else dropLast(1).joinToString() + " and " + last()

    private companion object {
        const val DISPENSER = "loc.wildy_agility_pillar"
        const val TICKET = "obj.wildy_agility_token"
        const val COINS = "obj.coins"
        const val CLUE = "obj.trail_medium_emote_exp1"
        const val RING_OF_WEALTH_I = "obj.ring_of_wealth_i"
        const val AGILITY = "stat.agility"
        const val REDEEM_SYNTH = "synth.found_gem"
        const val CLUE_CHANCE = 40
        const val CLUE_CHANCE_RING = 20
        const val NO_LAP =
            "You need to complete one full lap to receive a ticket and pay 150,000 coins to " +
                "receive a reward after each lap."
        const val NO_OPEN_BAG =
            "You must have an open looting bag in your inventory before trying to claim loot."
        const val BAG_FULL =
            "You must have free space in your looting bag before trying to claim loot."
        const val PAYMENT_WARNING =
            "Do you want to pay 150,000 coins to receive a reward after each lap?<br>You'll " +
                "lose your payment if you leave the course."
    }
}
