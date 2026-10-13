package org.rsmod.content.areas.misc.deathsoffice

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.types.aconverted.interf.IfSubType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.ironman.isAnyIronman
import org.rsmod.api.player.ironman.isUltimateIronman
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onDropTrigger
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.DeathOfficeEntrancesRow
import org.rsmod.content.other.gravestones.DeathFeeSource
import org.rsmod.content.other.gravestones.GraveFees
import org.rsmod.content.other.gravestones.Gravestones
import org.rsmod.content.other.gravestones.deathCoffer
import org.rsmod.content.other.gravestones.deathOffice
import org.rsmod.content.other.gravestones.grave
import org.rsmod.content.other.gravestones.graveCoords
import org.rsmod.content.other.gravestones.graveType
import org.rsmod.content.other.gravestones.inDeathTutorial
import org.rsmod.content.other.gravestones.payDeathFee
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.officeEntrance by intVarp("varp.death_office_entrance")
private var Player.cofferShown by intVarp("varp.if1")
private var Player.selectedSlot by intVarp("varp.if2")
private var Player.selectedFee by intVarp("varp.if3")

/**
 * Death's Office (https://oldschool.runescape.wiki/w/Death%27s_Office): Death, the item retrieval
 * interface for expired graves, and the Death's Domain entrances near the respawn points.
 */
class DeathsOfficeScript
@Inject
constructor(
    private val graves: Gravestones,
    private val offices: DeathsOffices,
    private val held: HeldInteractions,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(DEATH) { talk(it.npc) }
        // live capture, rev 241: Collect opens the retrieval
        // interface, even with nothing stored.
        onOpNpc3(DEATH) { collect(it.npc) }
        onPlayerQueue(DeathTutorial.QUEUE) { tutorialIntroduction() }
        onPlayerQueue(RETURN_QUEUE) { returnToOffice() }
        onPlayerQueueWithArgs<Int>(DROP_QUEUE) { dropWarning(it.args) }
        onDropTrigger(DeathsOffices.DROP_TRIGGER) { warnBeforeDrop(player, dropSlot) }
        onPlayerLogin { if (player.officeCoord != 0) player.queue(RETURN_QUEUE, 1) }
        onPlayerLogout { offices.logout(player) }
        for (row in DeathOfficeEntrancesRow.all()) {
            onOpLoc1(row.entrance) { enter(row) }
        }
        onOpLoc1(EXIT_PORTAL) { leave() }
        onIfModalButton(ITEMS) { selectOrExamine(it) }
        onIfModalButton(TAKE_1) { takeSelected(1) }
        onIfModalButton(TAKE_5) { takeSelected(5) }
        // live capture, rev 241.
        onIfModalButton(TAKE_X) { takeSelected(countDialog("How many would you like to take?")) }
        onIfModalButton(TAKE_ALL) { takeSelected(Int.MAX_VALUE) }
        onIfModalButton(TAKE_EVERYTHING) { takeEverything() }
        onIfClose(INTERFACE) { player.stopInvTransmit(player.deathOffice) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        if (player.inDeathTutorial) {
            startDialogue(npc) { with(DeathTutorial) { talk() } }
            return
        }
        startDialogue(npc) { death() }
    }

    /** Live capture, rev 240, during the first-death tutorial. */
    private suspend fun ProtectedAccess.collect(npc: Npc) {
        if (player.inDeathTutorial) {
            startDialogue(npc) { chatNpc(bored, "Perhaps we should just talk instead.") }
            return
        }
        openOffice()
    }

    private suspend fun ProtectedAccess.tutorialIntroduction() {
        val death = offices.death(player) ?: return
        startDialogue(death) { with(DeathTutorial) { introduction() } }
    }

    /**
     * Live capture, rev 241, also during the first-death tutorial: logging in inside the office
     * rebuilds a new copy, and Death speaks two ticks later. UNVERIFIED: live moves the player on
     * the login tick; this waits a tick, as the client has no map to rebuild from until then.
     */
    private suspend fun ProtectedAccess.returnToOffice() {
        if (!offices.enter(player, CoordGrid(player.officeCoord))) {
            player.officeCoord = 0
            return
        }
        delay(2)
        if (player.inDeathTutorial) {
            val death = offices.death(player) ?: return
            startDialogue(death) { with(DeathTutorial) { resume() } }
            return
        }
        greet()
    }

    private suspend fun ProtectedAccess.greet() {
        val death = offices.death(player) ?: return
        startDialogue(death) { death() }
    }

    /**
     * Live capture, rev 241: dropping something in the office asks
     * first; once the player puts something down, later drops in that visit don't.
     */
    private fun warnBeforeDrop(player: Player, slot: Int) {
        player.dropTrigger(DeathsOffices.DROP_TRIGGER)
        player.queue(DROP_QUEUE, 1, slot)
    }

    private suspend fun ProtectedAccess.dropWarning(slot: Int) {
        mesbox("Are you sure you want to put that down here?<br>If you leave, it'll be lost.")
        if (!choice2("Put it down.", true, "Don't put it down.", false)) {
            return
        }
        if (player.dropTrigger == DeathsOffices.DROP_TRIGGER) {
            player.clearDropTrigger(DeathsOffices.DROP_TRIGGER)
        }
        held.drop(this, inv, slot)
    }

    /**
     * Live capture, rev 241: the portal overlay, the teleport two
     * ticks later, the overlay closing a tick after that, and Death's greeting two ticks later.
     */
    private suspend fun ProtectedAccess.enter(row: DeathOfficeEntrancesRow) {
        // Walking in doesn't count as meeting Death (live capture, rev 241: a new account's
        // `steam_visited_death` stays 0).
        player.officeEntrance = row.rowId
        ifOpenFullOverlay(PORTAL_OVERLAY)
        delay(2)
        val entered = offices.enter(player, ARRIVAL)
        delay(1)
        ifCloseSub(PORTAL_OVERLAY)
        if (!entered) {
            return
        }
        delay(2)
        greet()
    }

    /** Live capture, rev 241 (from Ferox Enclave). */
    private suspend fun ProtectedAccess.leave() {
        if (player.inDeathTutorial) {
            // live capture, rev 241: after the tutorial the portal
            // leads to the respawn point, and the grave's timer starts on the teleport's tick.
            val respawn = mapFindSquareLineOfWalk(RESPAWN, minRadius = 0, maxRadius = 2) ?: RESPAWN
            portal(respawn) { player.inDeathTutorial = false }
        } else {
            val row = player.officeEntrance.takeIf { it != 0 }?.let(DeathOfficeEntrancesRow::getRow)
            portal(row?.exit ?: LUMBRIDGE_EXIT)
        }
        offices.exit(player)
    }

    private suspend fun ProtectedAccess.portal(dest: CoordGrid, onArrive: () -> Unit = {}) {
        ifOpenFullOverlay(PORTAL_OVERLAY)
        delay(2)
        telejump(dest)
        onArrive()
        delay(1)
        ifCloseSub(PORTAL_OVERLAY)
    }

    private suspend fun Dialogue.death() {
        if (player.isUltimateIronman) {
            // https://oldschool.runescape.wiki/w/Transcript:Death_(NPC)
            // TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
            chatNpc(
                bored,
                "Hello mortal. If you're looking for something you lost when you died, I'm " +
                    "afraid I don't cater to Ultimate Ironmen. Your items would have fallen on " +
                    "the ground where you died.",
            )
            // UNVERIFIED: a one-option menu, as the wiki lists only "Can I change how I die?".
            if (choice2("Can I change how I die?", true, "Nothing, thanks.", false)) {
                changeDeath()
            }
            return
        }
        if (player.graveCoords != null) {
            graveGreeting()
            return
        }
        // live capture, rev 241.
        chatNpc(bored, "Hello mortal. Have you come to retrieve something you lost when you died?")
        mainOptions()
    }

    /**
     * Live capture, rev 240 and rev 241.
     */
    private suspend fun Dialogue.graveGreeting() {
        chatNpc(
            bored,
            "Hello mortal. I see you have a gravestone, somewhere in the world. Did you want to ask " +
                "me about it?",
        )
        val collect =
            choice2(
                "Can I collect the items from that gravestone now?",
                true,
                "No, I want to talk about something else.",
                false,
            )
        if (!collect) {
            // UNVERIFIED: that this leads to the usual options.
            mainOptions()
            return
        }
        chatPlayer(quiz, "Can I collect the items from that gravestone now?")
        // Since 10 April 2024 Death states the cost (https://oldschool.runescape.wiki/w/Death).
        // live capture, rev 241: the office's fee for everything.
        val fee = officeFee(player.grave.filterNotNull { true })
        val cost =
            if (fee == 0L) {
                // UNVERIFIED: the wording for a free reclaim (wiki, Death).
                "I however expect it to be free for this reclaim."
            } else {
                "I expect this fee to be about ${"%,d".format(fee)} coins for this reclaim."
            }
        chatNpc(
            quiz,
            "If you wish. However, I do charge for returning items here - you would likely pay " +
                "less if you collect them from the gravestone directly. $cost",
        )
        val bring =
            choice2(
                "Bring my items here now; I'll pay your fee.",
                true,
                "Actually I'll go and collect them at the gravestone.",
                false,
            )
        if (!bring) {
            // UNVERIFIED: the mesanim; no capture picked this option.
            chatPlayer(neutral, "Actually I'll go and collect them at the gravestone.")
            return
        }
        chatPlayer(angry, "Bring my items here now; I'll pay your fee.")
        graves.expire(player)
        access.openOffice()
    }

    /** Live capture, rev 241. */
    private suspend fun Dialogue.mainOptions() {
        when (
            choice4(
                "How does that work?",
                1,
                "What is this place?",
                2,
                "Yes, have you got anything for me?",
                3,
                "More options...",
                4,
            )
        ) {
            1 -> howDoesThatWork()
            2 -> whatIsThisPlace()
            3 -> anythingForMe()
            4 -> moreOptions()
        }
    }

    private suspend fun Dialogue.howDoesThatWork() {
        chatPlayer(quiz, "How does that work?")
        chatNpc(
            bored,
            "When you die, you often drop things you were carrying. In most cases, a gravestone " +
                "appears near where you died, and your items can be retrieved there.",
        )
        chatNpc(
            shifty,
            "However, gravestones do not last for long. When your gravestone collapses, your " +
                "items come here, to me. I can return them to you - though I charge a fee for my " +
                "services.",
        )
        chatNpc(
            happy,
            "Aside from that, if you bring me your unwanted possessions, and sacrifice them into " +
                "my Coffer here, I'll count their value against any future fees I charge you.",
        )
        chatNpc(
            shifty,
            "I don't accept just any junk, but I'll try to give you a good price for anything " +
                "decent.",
        )
        chatNpc(bored, "So do you need to retrieve something you lost when you died?")
        when (
            choice4(
                "What is this place?",
                1,
                "Yes, have you got anything for me?",
                2,
                "Can I choose a different-looking gravestone?",
                3,
                "Not just now, thanks.",
                4,
            )
        ) {
            1 -> whatIsThisPlace()
            2 -> anythingForMe()
            3 -> chooseGravestone()
            4 -> notNow()
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Death_(NPC)
    // TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
    private suspend fun Dialogue.whatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            neutral,
            "As you should be able to see, I am Death. The Grim Reaper. The Inexorable One. The " +
                "Inescapable Grasp of Mortal Destiny. And this is my little office. Nice, isn't it?",
        )
        val lovely = choice2("It's lovely.", true, "Your decorations leave a bit to be desired.", false)
        if (lovely) {
            chatPlayer(happy, "It's lovely.")
            chatNpc(
                happy,
                "Thank you. I feel the colour scheme suits my style. Now, can I help you retrieve " +
                    "something you lost when you died?",
            )
        } else {
            chatPlayer(neutral, "Your decorations leave a bit to be desired.")
            chatNpc(
                sad,
                "Sometimes it's hard to care for beauty when one's very existence is just seen as " +
                    "a curse upon the world. Now, do you actually want to retrieve something from " +
                    "me?",
            )
        }
        mainOptions()
    }

    /** Live capture, rev 241. */
    private suspend fun Dialogue.anythingForMe() {
        chatPlayer(happy, "Yes, have you got anything for me?")
        access.openOffice()
    }

    private suspend fun Dialogue.moreOptions() {
        when (
            choice4(
                "Can I choose a different-looking gravestone?",
                1,
                "Can I change how I die?",
                2,
                "Not just now, thanks.",
                3,
                "Previous options...",
                4,
            )
        ) {
            1 -> chooseGravestone()
            2 -> changeDeath()
            3 -> notNow()
            4 -> mainOptions()
        }
    }

    /** Live capture, rev 241. */
    private suspend fun Dialogue.changeDeath() {
        chatNpc(neutral, "Er... I suppose so.")
        chatNpc(quiz, "How would you like to... die?")
        // TODO: Live opens the death animation picker (interface animation_picker, varp
        // death_anim_toggles) here.
    }

    // https://oldschool.runescape.wiki/w/Transcript:Death_(NPC)
    // UNVERIFIED: the mesanims.
    private suspend fun Dialogue.notNow() {
        chatPlayer(neutral, "Not just now, thanks.")
        chatNpc(bored, "Really? There isn't much else for you to do here. But you know your business.")
    }

    /**
     * Live capture, rev 241, without the coins and buying the angel with 200,000 coins from the
     * inventory.
     */
    private suspend fun Dialogue.chooseGravestone() {
        chatPlayer(quiz, "Can I choose a different-looking gravestone?")
        val angel = access.gravestoneChoice(player.deathCoffer)
        if (!angel) {
            // https://oldschool.runescape.wiki/w/Transcript:Death_(NPC)
            // UNVERIFIED: the mesanims.
            if (player.graveType == 0) {
                chatNpc(bored, "You've already got the basic gravestone.")
                return
            }
            chatNpc(
                quiz,
                "Do you really want to switch to the basic gravestone? I'm not offering to give " +
                    "back the fee you paid earlier.",
            )
            // UNVERIFIED: the second option; the wiki gives only the first.
            if (choice2("Yes, I want the basic gravestone.", true, "No, I'll keep the angel.", false)) {
                setGraveType(0)
                objbox(BASIC_OBJ, "Your gravestone is now the basic type.")
            }
            return
        }
        if (player.graveType == 1) {
            // UNVERIFIED: the wiki gives no line for picking the angel again.
            chatNpc(bored, "You've already got the Angel of Death.")
            return
        }
        // "I can take coins from your inventory, or from my Coffer." UNVERIFIED: which goes first
        // when both could pay (a live capture, rev 241, took the inventory's with 13,351 in the
        // coffer).
        val fromInv = access.inv.count(COINS) >= ANGEL_PRICE
        if (!fromInv && player.deathCoffer < ANGEL_PRICE) {
            chatNpc(
                bored,
                "You haven't got 200,000 coins to pay for that. I can take coins from your " +
                    "inventory, or from my Coffer.",
            )
            if (choice2("Can I choose a different-looking gravestone?", true, "Actually, never mind.", false)) {
                chooseGravestone()
            } else {
                chatPlayer(sad, "Actually, never mind.")
            }
            return
        }
        chatNpc(
            quiz,
            "Most people won't see your grave, by the way. It appears to you, but it aims to be " +
                "invisible to everyone else. Are you sure you want to buy a cosmetic override for it?",
        )
        val buy =
            choice2(
                "Yes, I understand other people won't often see it.",
                true,
                "No thanks, it's not worth it.",
                false,
            )
        if (buy) {
            chatPlayer(happy, "Yes, I understand other people won't often see it.")
            if (fromInv) {
                if (player.invDel(access.inv, COINS, ANGEL_PRICE).failure) {
                    return
                }
            } else {
                player.deathCoffer -= ANGEL_PRICE
            }
            setGraveType(1)
            objbox(ANGEL_OBJ, "Your gravestone is now the Angel of Death.")
        } else {
            // UNVERIFIED: the mesanim; the closing line is the same (wiki transcript).
            chatPlayer(neutral, "No thanks, it's not worth it.")
        }
        chatNpc(bored, "It is not for me to judge how people spend their money.")
    }

    private fun Dialogue.setGraveType(type: Int) {
        player.graveType = type
        if (player.graveCoords != null) {
            graves.spawn(player)
        }
    }

    /**
     * The `graphical_multi` chatbox (live capture, rev 241): the
     * Coffer's balance as the title, then the basic and angel gravestones. True picks the angel.
     */
    private suspend fun ProtectedAccess.gravestoneChoice(coffer: Int): Boolean {
        ifSetText(MULTI_TITLE, "Death's Coffer: ${"%,d".format(coffer)} coins")
        ifSetObj(MULTI_PIC_A, BASIC_OBJ, 0)
        ifSetObj(MULTI_PIC_B, ANGEL_OBJ, 0)
        ifSetText(MULTI_A, "<br><br><br><br>Basic: Free")
        ifSetText(MULTI_B, "<br><br><br><br>Angel: 200k coins")
        ifOpenSub(GRAPHICAL_MULTI, CHATBOX_MODAL, IfSubType.Modal)
        val input = pauseButton()
        return input.isComponentType(MULTI_B)
    }

    /** Live capture, rev 241 and rev 240. */
    private fun ProtectedAccess.openOffice() {
        player.selectedSlot = -1
        player.cofferShown = player.deathCoffer
        invTransmit(player.deathOffice)
        ifOpenMainModal(INTERFACE)
        ifSetEvents(ITEMS, ITEM_SLOTS, IfEvent.Op1, IfEvent.Op10)
    }

    /**
     * Live capture, rev 240: Select on a free stack took all of it at
     * once. A paid obj is selected instead, so the client shows its fee (`death_office_redraw`
     * reads the slot from `if2` and the fee each from `if3`) for the quantity buttons.
     */
    private fun ProtectedAccess.selectOrExamine(button: IfModalButton) {
        val slot = button.comsub
        val obj = player.deathOffice.objOrNull(slot) ?: return
        if (button.op == IfButtonOp.Op10) {
            mes(getInvObj(obj).examine)
            return
        }
        if (button.op != IfButtonOp.Op1) {
            return
        }
        val fee = feeEach(slot)
        if (fee == 0) {
            take(slot, obj.count, 0)
            return
        }
        player.selectedSlot = slot
        player.selectedFee = fee
    }

    /**
     * Live capture, rev 241: 1, 5, X and All take that many of the
     * selected obj at once and charge for them, with no confirmation.
     */
    private fun ProtectedAccess.takeSelected(count: Int) {
        val slot = player.selectedSlot
        val obj = player.deathOffice.objOrNull(slot) ?: return
        take(slot, count.coerceIn(0, obj.count), feeEach(slot))
    }

    /**
     * Live capture, rev 241 (rev 240 for the empty office): Take-All closes the office and asks to
     * pay the fee for every paid obj stored. Paying takes what fits, in slot order, charges only
     * for the paid objs taken and reopens the office; Cancel leaves it closed.
     */
    private suspend fun ProtectedAccess.takeEverything() {
        val office = player.deathOffice
        if (office.isEmpty()) {
            mes("There's nothing here to take.")
            return
        }
        val quoted = office.indices.sumOf { slot -> feeEach(slot).toLong() * (office[slot]?.count ?: 0) }
        // UNVERIFIED: that free objs alone are taken without asking (every capture had paid objs).
        if (quoted > 0) {
            ifCloseSub(INTERFACE)
            val amount = "%,d".format(quoted)
            val pay = choice2("Pay Death's fee.", true, "Cancel", false, "Death will charge $amount coins.")
            if (!pay) {
                return
            }
        }
        val plan = planTakeAll()
        if (plan.isEmpty()) {
            mesbox("You don't have enough inventory space.")
            openOffice()
            return
        }
        val fee = plan.sumOf { (slot, count) -> feeEach(slot).toLong() * count }
        if (charge(fee)) {
            for ((slot, count) in plan) {
                invMoveFromSlot(from = office, into = inv, fromSlot = slot, count = count)
            }
        }
        clearSelection()
        openOffice()
    }

    /** The slots and counts Take-All moves: each stored obj in turn, as far as the inv fits. */
    private fun ProtectedAccess.planTakeAll(): List<Pair<Int, Int>> {
        val office = player.deathOffice
        var free = inv.freeSpace()
        val stacked = mutableSetOf<String>()
        val plan = mutableListOf<Pair<Int, Int>>()
        for (slot in office.indices) {
            val obj = office[slot] ?: continue
            val type = getInvObj(obj)
            if (type.isStackable) {
                val name = type.internalName
                if (name !in stacked && inv.count(name) == 0) {
                    if (free == 0) continue
                    free--
                }
                stacked += name
                plan += slot to obj.count
                continue
            }
            val taking = obj.count.coerceAtMost(free)
            if (taking > 0) {
                free -= taking
                plan += slot to taking
            }
        }
        return plan
    }

    /** Takes [count] of the obj in [slot], paying [feeEach] for each. */
    private fun ProtectedAccess.take(slot: Int, count: Int, feeEach: Int) {
        if (count <= 0) {
            return
        }
        val taking = count.coerceAtMost(spaceFor(slot, count))
        if (taking <= 0) {
            // UNVERIFIED: the message here; Take-All shows it in a mesbox.
            mes("You don't have enough inventory space.")
            return
        }
        if (!charge(feeEach.toLong() * taking)) {
            return
        }
        invMoveFromSlot(from = player.deathOffice, into = inv, fromSlot = slot, count = taking)
        if (player.deathOffice[slot] == null) {
            clearSelection()
        }
    }

    /** How many of the obj in [slot] the inventory has room for, up to [count]. */
    private fun ProtectedAccess.spaceFor(slot: Int, count: Int): Int {
        val obj = player.deathOffice[slot] ?: return 0
        val type = getInvObj(obj)
        if (type.isStackable) {
            return if (inv.count(type.internalName) > 0 || inv.freeSpace() > 0) count else 0
        }
        return inv.freeSpace().coerceAtMost(count)
    }

    // UNVERIFIED: the message when nothing covers the fee.
    private fun ProtectedAccess.charge(fee: Long): Boolean {
        if (fee <= 0) {
            return true
        }
        val amount = fee.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val source = if (fee > Int.MAX_VALUE) null else payDeathFee(amount)
        if (source == null) {
            mes("You don't have enough coins with you or in your bank to pay the fee.")
            return false
        }
        mes(officeFeeMessage(source, amount))
        player.cofferShown = player.deathCoffer
        return true
    }

    /**
     * Unlike a grave's ("x Coins"), the office's message ends in "coins" (live capture, rev 241).
     */
    private fun officeFeeMessage(source: DeathFeeSource, fee: Int): String {
        val amount = "%,d".format(fee)
        return when (source) {
            DeathFeeSource.Bank -> "Payment has been taken from your bank: $amount coins"
            // UNVERIFIED: the coffer and inventory messages (the bank one, reworded).
            DeathFeeSource.Inventory -> "Payment has been taken from your inventory: $amount coins"
            DeathFeeSource.Coffer -> "Payment has been taken from Death's Coffer: $amount coins"
        }
    }

    private fun Inventory.objOrNull(slot: Int): InvObj? =
        if (slot in indices) this[slot] else null

    private fun ProtectedAccess.clearSelection() {
        player.selectedSlot = -1
        player.selectedFee = 0
    }

    private fun ProtectedAccess.feeEach(slot: Int): Int {
        val obj = player.deathOffice[slot] ?: return 0
        return GraveFees.officeFee(graves.value(obj), player.isAnyIronman)
    }

    private fun Dialogue.officeFee(items: List<InvObj>): Long =
        items.sumOf { GraveFees.officeFee(graves.value(it), player.isAnyIronman).toLong() * it.count }

    private companion object {
        const val DEATH = DeathsOffices.DEATH
        const val EXIT_PORTAL = "loc.death_office_exitportal"
        const val PORTAL_OVERLAY = "interface.overlay_portal"
        const val INTERFACE = "interface.death_office"
        const val ITEMS = "component.death_office:items"
        const val TAKE_1 = "component.death_office:1"
        const val TAKE_5 = "component.death_office:5"
        const val TAKE_X = "component.death_office:x"
        const val TAKE_ALL = "component.death_office:all"
        const val TAKE_EVERYTHING = "component.death_office:takeall"
        val ITEM_SLOTS = 0 until 120

        const val CHATBOX_MODAL = "component.chatbox:chatmodal"
        const val GRAPHICAL_MULTI = "interface.graphical_multi"
        const val MULTI_TITLE = "component.graphical_multi:graphical_multi_2title"
        const val MULTI_A = "component.graphical_multi:graphical_multi_2a"
        const val MULTI_B = "component.graphical_multi:graphical_multi_2b"
        const val MULTI_PIC_A = "component.graphical_multi:graphical_multi_pic2a"
        const val MULTI_PIC_B = "component.graphical_multi:graphical_multi_pic2b"
        const val BASIC_OBJ = "obj.sirens_tome"
        const val ANGEL_OBJ = "obj.osb7_comb"

        // https://oldschool.runescape.wiki/w/Grave: the angel costs 200,000 coins.
        const val ANGEL_PRICE = 200_000
        const val COINS = "obj.coins"

        // live capture, rev 241.
        val ARRIVAL = CoordGrid(3171, 5726)

        const val RETURN_QUEUE = "queue.death_office_return"
        const val DROP_QUEUE = "queue.death_office_drop"

        // live capture, rev 241.
        val LUMBRIDGE_EXIT = CoordGrid(3238, 3194)

        // The default respawn point (api/death PlayerDeath), as no respawn points can be chosen.
        // TODO: The player's chosen respawn point
        // (https://oldschool.runescape.wiki/w/Respawn_point).
        val RESPAWN = CoordGrid(0, 50, 50, 21, 18)
    }
}
