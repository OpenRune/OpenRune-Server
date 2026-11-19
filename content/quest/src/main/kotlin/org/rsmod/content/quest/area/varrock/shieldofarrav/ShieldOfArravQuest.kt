package org.rsmod.content.quest.area.varrock.shieldofarrav

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.ironman.isAnyIronman
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.chooseLampSkill
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.ScriptContext

class ShieldOfArravQuest @Inject constructor() :
    QuestScript(
        "quest_shieldofarrav",
        "varp.phoenixgang",
        rewards {
            item(COINS, REWARD_COINS, label = "600 Coins")
            item(LAMP, label = "An antique lamp")
        },
        ItemRewardDisplay(CERTIFICATE, zoom = 250),
        questVarbit = "varbit.shieldofarrav",
    ) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == SOA_COMPLETE) {
            "Shield of Arrav end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $SOA_COMPLETE."
        }
        onOpLoc1("loc.questbookcase") { searchBookcase() }
        onOpHeld1(BOOK) {
            markBookRead(player)
            ShieldOfArravBook.read(this)
        }
        onOpHeld1(PHOENIX_HALF) { inspectHalf(PHOENIX_HALF) }
        onOpHeld1(BLACKARM_HALF) { inspectHalf(BLACKARM_HALF) }
        onOpHeldU(PHOENIX_HALF, BLACKARM_HALF) { combineHalves() }
        onOpHeld4(WEAPON_STORE_KEY) { inspectTradeable(KEY_INSPECT, KEY_INSPECT_IRONMAN) }
        onOpHeld4(CROSSBOW) { inspectTradeable(CROSSBOW_INSPECT, CROSSBOW_INSPECT_IRONMAN) }
        onOpHeld1(LAMP) { rubLamp() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Reldo<col=000080> in the " +
            "<col=800000>Varrock Palace Library<col=000080>."

    override fun questLog(player: ProtectedAccess): String =
        questJournal(player) { ShieldOfArravJournal.write(this, player.player) }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) { ShieldOfArravJournal.writeComplete(this, player.player) }

    fun reldoTopic(player: Player): String? =
        when {
            player.isQuestComplete -> null
            player.isOnQuest && (player.holds(PHOENIX_SHIELD) || player.holds(BLACKARM_SHIELD)) ->
                "I've found half of the Shield of Arrav!"
            player.phoenixGang == PHOENIX_STARTED && player.holds(BOOK) -> "I found that book."
            player.phoenixGang == PHOENIX_STARTED -> "About that book... Where is it again?"
            player.phoenixGang == PHOENIX_BOOK_READ ->
                "Okay, I've read that book about the Shield of Arrav."
            else -> null
        }

    fun canStartFromReldo(player: Player): Boolean = player.soaState == 0

    suspend fun Dialogue.reldoQuestTopic() {
        val topic = reldoTopic(player) ?: return
        chatPlayer(neutral, topic)
        when {
            topic.startsWith("I've found half") -> reldoShieldHalf()
            topic == "I found that book." ->
                chatNpc(happy, "Very good. I suggest you give it a read.")
            topic.startsWith("About that book") ->
                chatNpc(
                    neutral,
                    "I'm not sure exactly... but I'm sure it's around here somewhere.",
                )
            else -> reldoAfterBook()
        }
    }

    /** Continues Reldo's "I'm in search of a quest." branch after his "if you're definitely interested?" line. */
    suspend fun Dialogue.reldoStartOffer() {
        if (player.combatLevel < RECOMMENDED_COMBAT) {
            mesbox(
                "Before starting this quest, be aware that you combat level is lower than the " +
                    "recommended level of $RECOMMENDED_COMBAT."
            )
        }
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "Actually, I don't think I am. Goodbye.")
            return
        }
        chatPlayer(happy, "Of course.")
        chatNpc(
            neutral,
            "Well, if you look around for book called The Shield of Arrav, you'll find a quest " +
                "in there.",
        )
        player.phoenixGang = PHOENIX_STARTED
        quest.setQuestStage(access, SOA_STARTED)
        chatNpc(
            neutral,
            "I'm not sure where the book is mind you... but I'm sure it's around here somewhere.",
        )
        chatPlayer(happy, "Great! Thank you.")
    }

    private suspend fun Dialogue.reldoAfterBook() {
        chatNpc(neutral, "Then perhaps you now have your quest?")
        chatPlayer(
            quiz,
            "I think I do. Do you know where I can find the Phoenix Gang or the Black Arm Gang?",
        )
        chatNpc(
            neutral,
            "No, I don't. However, I hear Baraek, the fur trader in the market, has had " +
                "connections with the Phoenix Gang.",
        )
        chatPlayer(quiz, "And the Black Arm Gang?")
        player.blackArmGang = BLACKARM_KNOWN
        player.phoenixGang = PHOENIX_GANGS_KNOWN
        chatNpc(
            neutral,
            "There are rumours that they are based in the south west corner of the city. " +
                "Charlie the Tramp might be able to tell you more. He knows that part of Varrock " +
                "well.",
        )
        chatPlayer(happy, "Thanks! I'll get to it!")
        chatNpc(happy, "Good luck.")
    }

    private suspend fun Dialogue.reldoShieldHalf() {
        val phoenix = player.holds(PHOENIX_SHIELD)
        chatNpc(shocked, "Half?")
        chatPlayer(neutral, "Yes. It seems it was split in two.")
        chatNpc(quiz, "I see... Where did you find this half?")
        if (phoenix) {
            chatPlayer(
                neutral,
                "The Phoenix Gang had it. I managed to infiltrate their gang and steal it from " +
                    "them.",
            )
        } else {
            chatPlayer(
                neutral,
                "The Black Arm Gang had it. I managed to infiltrate their gang and steal it from " +
                    "them.",
            )
        }
        chatNpc(
            neutral,
            "A split gang and a split shield. My guess would be that the other gang has the " +
                "other half.",
        )
        if (phoenix) {
            chatPlayer(
                neutral,
                "Well, I don't think I'll have much luck infiltrating the Black Arm Gang now " +
                    "that I've joined their rivals, but maybe someone else will be able to.",
            )
        } else {
            chatPlayer(
                neutral,
                "Well, I did have a member of the Phoenix Gang help me... Maybe they can find the " +
                    "other half.",
            )
        }
        chatNpc(
            neutral,
            "Let's hope so. In the meantime, you should probably head over to the museum so " +
                "they can verify the authenticity of this half.",
        )
    }

    fun hasBaraekTopic(player: Player): Boolean =
        player.isOnQuest && player.phoenixGang == PHOENIX_GANGS_KNOWN

    suspend fun Dialogue.baraekPhoenixTopic() {
        chatPlayer(quiz, "Can you tell me where I can find the Phoenix Gang?")
        chatNpc(
            worried,
            "Shh, shh, shh! Not so loud! You don't want to get me in trouble, do you?",
        )
        chatPlayer(neutral, "So I take it that you do know where they are.")
        chatNpc(
            shifty,
            "I may do... but I don't want to get into trouble for revealing their hideout. Of " +
                "course, if I was, say 20 coins richer, I may happen to be more inclined to take " +
                "that sort of risk...",
        )
        val choice =
            menu(
                "Okay. Have 20 gold coins." to 0,
                "No, I don't like things like bribery." to 1,
                "Yes. I'd like to be 20 coins richer too." to 2,
            )
        when (choice) {
            0 -> {
                chatPlayer(neutral, "Okay. Have 20 gold coins.")
                if (!ShieldOfArravExchange.exchange(player, take = listOf(COINS to BARAEK_FEE))) {
                    chatPlayer(sad, "Uh... oops. I don't actually have 20 coins. Silly me.")
                    return
                }
                player.phoenixGang = PHOENIX_BARAEK_PAID
                objbox(InvObj(COINS, BARAEK_FEE), "You give 20 coins to Baraek.")
                chatNpc(
                    neutral,
                    "Okay, to get to the gang hideout, head to the southern gates of the city and " +
                        "then head down the alleyway past the Blue Moon Inn.",
                )
                chatNpc(
                    neutral,
                    "Down there you'll find a building owned by the VTAM corporation. That's " +
                        "where you'll find the Phoenix Gang. Just be careful. They ain't the types " +
                        "to be messed about.",
                )
                chatPlayer(happy, "Thanks!")
            }
            1 -> {
                chatPlayer(neutral, "No, I don't do things like bribery.")
                chatNpc(
                    neutral,
                    "If you wanna deal with the Phoenix Gang, they're involved in much worse " +
                        "than a bit of bribery.",
                )
            }
            else -> chatPlayer(laugh, "Yes, I'd like to be 20 coins richer too.")
        }
    }

    private suspend fun ProtectedAccess.searchBookcase() {
        arriveDelay()
        if (!player.isOnQuest || player.holds(BOOK)) {
            mes("You search the bookcase but find nothing of interest.")
            return
        }
        if (!ShieldOfArravExchange.exchange(player, give = listOf(BOOK to 1))) {
            mes(Constants.dm_invspace)
            return
        }
        startDialogue {
            objbox(BOOK, "You search the bookcase and find a book titled The Shield of Arrav.")
            chatPlayer(happy, "Aha! Exactly what I was looking for.")
        }
    }

    private fun markBookRead(player: Player) {
        if (player.phoenixGang == PHOENIX_STARTED) {
            player.phoenixGang = PHOENIX_BOOK_READ
        }
    }

    private suspend fun ProtectedAccess.inspectHalf(half: String) {
        val other = if (half == PHOENIX_HALF) BLACKARM_HALF else PHOENIX_HALF
        if (player.holds(other)) {
            var combine = false
            startDialogue {
                combine =
                    choice2("Yes.", true, "No.", false, title = "Combine the two half-certificates?")
            }
            if (combine) combineHalves()
            return
        }
        inspectTradeable(HALF_INSPECT, HALF_INSPECT_IRONMAN, half)
    }

    private suspend fun ProtectedAccess.combineHalves() {
        val combined =
            ShieldOfArravExchange.exchange(
                player,
                take = listOf(PHOENIX_HALF to 1, BLACKARM_HALF to 1),
                give = listOf(CERTIFICATE to 1),
            )
        if (!combined) {
            mes(Constants.dm_invspace)
            return
        }
        startDialogue {
            objbox(CERTIFICATE, "You combine the two halves to make a complete certificate.")
        }
    }

    private suspend fun ProtectedAccess.inspectTradeable(
        text: String,
        ironmanText: String,
        obj: String? = null,
    ) {
        val message = if (player.isRestrictedFromTrading()) ironmanText else text
        startDialogue { if (obj != null) objbox(obj, message) else mesbox(message) }
    }

    private fun Player.isRestrictedFromTrading(): Boolean = isAnyIronman

    private suspend fun ProtectedAccess.rubLamp() {
        val stat = chooseLampSkill(LAMP_TITLE) ?: return
        if (invDel(inv, LAMP).failure) {
            return
        }
        player.lampUsed = 1
        statAdvance(stat, LAMP_XP)
        mes("Your wish has been granted!")
    }

    suspend fun Dialogue.curatorShieldTopic(): Boolean {
        val half =
            when {
                player.holds(PHOENIX_SHIELD) -> PHOENIX_SHIELD
                player.holds(BLACKARM_SHIELD) -> BLACKARM_SHIELD
                else -> null
            }
        if (half == null && (player.shieldGiven == 0 || player.isQuestComplete)) {
            return false
        }
        chatPlayer(neutral, "Hello there. I'm here about the Shield of Arrav.")
        if (player.shieldGiven != 0 || half == null) {
            curatorMoreHalves()
            return true
        }
        val phoenix = half == PHOENIX_SHIELD
        chatNpc(
            happy,
            "The Shield of Arrav? The Museum has been searching for that for years! The late " +
                "King Roald II offered a generous reward for its return!",
        )
        chatPlayer(neutral, "Well, I'm here to claim it.")
        chatNpc(shocked, "You've found the shield? Let's have a look!")
        objbox(half!!, "You show the shield half to the curator.")
        chatNpc(shocked, "This is incredible! But where's the other half?")
        val (mine, theirs) =
            if (phoenix) "Phoenix Gang" to "Black Arm Gang" else "Black Arm Gang" to "Phoenix Gang"
        chatPlayer(
            neutral,
            "I obtained this half from the $mine. The $theirs might have the other half.",
        )
        chatNpc(
            neutral,
            "That does sound plausible. We will have to hope that another adventurer can " +
                "recover that half.",
        )
        chatPlayer(quiz, "So will I be rewarded for recovering half of the shield?")
        chatNpc(
            neutral,
            "I'm afraid the reward is for the recovery of the full shield. However, what I'll do " +
                "is write you out two half- certificates. If another adventurer returns the " +
                "other shield half, I'll do the same for them.",
        )
        chatNpc(
            neutral,
            "The two of you can then swap one of the half- certificates to make a full one " +
                "each. Take that to the King and he will grant you your reward.",
        )
        val certificate = if (phoenix) PHOENIX_HALF else BLACKARM_HALF
        val swapped =
            ShieldOfArravExchange.exchange(
                player,
                take = listOf(half to 1),
                give = listOf(certificate to 2),
            )
        if (!swapped) {
            mesbox("You don't have enough space in your inventory.")
            return true
        }
        player.shieldGiven = if (phoenix) SHIELD_GIVEN_PHOENIX else SHIELD_GIVEN_BLACKARM
        doubleobjbox(
            half,
            certificate,
            "You give the shield half to the curator and he gives you two half-certificates.",
        )
        return true
    }

    private suspend fun Dialogue.curatorMoreHalves() {
        chatNpc(
            neutral,
            "I gave you two half-certificates for the half you returned. If another adventurer " +
                "returns the other shield half, I'll do the same for them.",
        )
        chatNpc(
            neutral,
            "The two of you can then swap one of the half-certificates to make a full one each. " +
                "Take that to the King and he will grant you your reward.",
        )
        if (player.isQuestComplete) {
            return
        }
        chatPlayer(quiz, "Would it be okay to get more half-certificates?")
        chatNpc(
            neutral,
            "I suppose so. It's not like you can do anything with them without the other half.",
        )
        val certificate =
            if (player.shieldGiven == SHIELD_GIVEN_PHOENIX) PHOENIX_HALF else BLACKARM_HALF
        if (!ShieldOfArravExchange.exchange(player, give = listOf(certificate to 2))) {
            mesbox("You don't have enough space in your inventory.")
            return
        }
        doubleobjbox(certificate, certificate, "The curator gives you two half-certificates.")
    }

    fun hasRoaldTopic(player: Player): Boolean =
        player.isOnQuest &&
            (player.holds(CERTIFICATE) ||
                player.holds(PHOENIX_HALF) ||
                player.holds(BLACKARM_HALF) ||
                player.holds(PHOENIX_SHIELD) ||
                player.holds(BLACKARM_SHIELD))

    suspend fun Dialogue.roaldShieldTopic() {
        chatPlayer(neutral, "I'm here about the Shield of Arrav.")
        when {
            player.holds(CERTIFICATE) -> roaldReward()
            player.holds(PHOENIX_HALF) || player.holds(BLACKARM_HALF) -> {
                val half = if (player.holds(PHOENIX_HALF)) PHOENIX_HALF else BLACKARM_HALF
                objbox(half, "You show the certificate half to the king.")
                chatNpc(
                    neutral,
                    "I'm afraid that's only half the reward certificate. You'll have to get the " +
                        "other half and join them together if you want to claim the reward.",
                )
            }
            else -> {
                chatNpc(
                    neutral,
                    "The Shield of Arrav? Yes, I do recall my father put a reward out for that.",
                )
                chatPlayer(happy, "Well, I've managed to find half the shield!")
                chatNpc(
                    neutral,
                    "You have? Well, before we go any further, you'll need to get the " +
                        "authenticity of the shield verified by the curator at the museum.",
                )
                chatPlayer(neutral, "Okay. I'll head over there.")
            }
        }
    }

    private suspend fun Dialogue.roaldReward() {
        objbox(CERTIFICATE, "You show the certificate to the king.")
        chatNpc(
            shocked,
            "My goodness! This is for the reward offered by my father many years ago! I never " +
                "thought I would live to see the day when someone came forward to claim it!",
        )
        chatPlayer(
            neutral,
            "It was quite the undertaking. I wouldn't have been able to do it without help from " +
                "a fellow adventurer.",
        )
        chatNpc(
            happy,
            "Well, in that case, it's only right that you both be rewarded. I will grant half of " +
                "the reward to you and half to your companion. That means 600 coins for you. " +
                "Enjoy it! You've very much earned it!",
        )
        if (!ShieldOfArravExchange.exchange(player, take = listOf(CERTIFICATE to 1))) {
            return
        }
        val phoenix = player.isPhoenixMember
        val previous = if (phoenix) player.phoenixGang else player.blackArmGang
        setGang(phoenix, if (phoenix) PHOENIX_COMPLETE else BLACKARM_COMPLETE)
        if (!quest.completeQuest(access)) {
            setGang(phoenix, previous)
            ShieldOfArravExchange.exchange(player, give = listOf(CERTIFICATE to 1))
            mesbox("You don't have enough space in your inventory.")
        }
    }

    private fun Dialogue.setGang(phoenix: Boolean, value: Int) {
        if (phoenix) player.phoenixGang = value else player.blackArmGang = value
    }

    fun canReclaimLamp(player: Player): Boolean =
        player.isQuestComplete && player.lampUsed == 0 && !player.holds(LAMP)

    suspend fun Dialogue.roaldLamp() {
        chatNpc(neutral, "Here, citizen. I have this lamp for you.")
        if (!ShieldOfArravExchange.exchange(player, give = listOf(LAMP to 1))) {
            mesbox("You don't have enough space in your inventory.")
            return
        }
        chatPlayer(happy, "Thanks!")
    }

    companion object {
        const val REWARD_COINS = 600
        const val BARAEK_FEE = 20
        const val RECOMMENDED_COMBAT = 10
        const val LAMP_XP = 500.0
        const val LAMP_TITLE = "Choose the stat you wish to be advanced!"

        const val KEY_INSPECT =
            "It's the key to the Phoenix Gang's weapons store. If you're working with a player " +
                "who's restricted from trading, you can still give them this item by " +
                "<col=7f0000>using</col> it on them."
        const val KEY_INSPECT_IRONMAN =
            "It's the key to the Phoenix Gang's weapons store. Although your account is " +
                "restricted from trading, you can still give them this item by " +
                "<col=7f0000>using</col> it on them."
        const val CROSSBOW_INSPECT =
            "If you need to give this item to a player who's restricted from trading, you can " +
                "give them the item by using it on them."
        const val CROSSBOW_INSPECT_IRONMAN =
            "It's a crossbow used by members of the Phoenix Gang. Although your account is " +
                "restricted from trading, you can still give this item to another player by " +
                "<col=7f0000>using</col> it on them."
        const val HALF_INSPECT =
            "It's half of a certificate for claiming a reward from King Roald. If you're " +
                "working with a player who's restricted from trading, you can still give them " +
                "this item by <col=7f0000>using</col> it on them."
        const val HALF_INSPECT_IRONMAN =
            "It's half of a certificate for claiming a reward from King Roald. Although your " +
                "account is restricted from trading, you can still give them this item by " +
                "<col=7f0000>using</col> it on them."
    }
}
