package org.rsmod.content.skills.hunter

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onApNpc3
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.table.hunter.HunterRumourHuntersRow
import org.rsmod.api.table.hunter.HunterRumoursRow
import org.rsmod.content.skills.hunter.HunterRumours.Companion.BACK_TO_BACK
import org.rsmod.content.skills.hunter.HunterRumours.Companion.BACK_TO_BACK_OFF
import org.rsmod.content.skills.hunter.HunterRumours.Companion.COMPLETED
import org.rsmod.content.skills.hunter.HunterRumours.Companion.UNLOCKED
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HunterGuildScript @Inject constructor(private val rumours: HunterRumours, private val random: GameRandom) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (hunter in rumours.hunters) {
            onNpc(hunter.npc.internalName, { talk(hunter) }, { rumourTalk(hunter, quick = true) })
        }
        onNpc(VERITY, { verity() }, { veritySettings() })
    }

    private fun ScriptContext.onNpc(
        npc: String,
        op1: suspend Dialogue.() -> Unit,
        op3: suspend Dialogue.() -> Unit,
    ) {
        onOpNpc1(npc) { startDialogue(it.npc) { op1() } }
        onOpNpc3(npc) { startDialogue(it.npc) { op3() } }
        if (npc in BEHIND_BAR) {
            onApNpc1(npc) { acrossBar(it.npc, op1) }
            onApNpc3(npc) { acrossBar(it.npc, op3) }
        }
    }

    private suspend fun ProtectedAccess.acrossBar(npc: Npc, dialogue: suspend Dialogue.() -> Unit) {
        if (isWithinDistance(npc, BAR_RANGE)) startDialogue(npc) { dialogue() } else apRange(BAR_RANGE)
    }

    private val HunterRumourHuntersRow.lines: HunterLines
        get() = LINES.getValue(name)

    private suspend fun Dialogue.talk(hunter: HunterRumourHuntersRow) {
        val lines = hunter.lines
        if (hunter.name == "Wolf" && vars[UNLOCKED] == 0) {
            chatNpc(neutral, "My apologies, but I don't have time to chat right now.")
            return
        }
        for (line in lines.greeting) say(line)
        options(hunter)
    }

    private suspend fun Dialogue.options(hunter: HunterRumourHuntersRow) {
        val lines = hunter.lines
        if (hunter.name == "Gilman") return gilmanMenu(hunter)
        val choice = choice3(lines.chatOption, 1, lines.rumourOption, 2, lines.leaveOption, 3)
        when (choice) {
            1 -> {
                for (line in lines.chat) say(line)
                if (choice2(lines.rumourOption, true, "See you later.", false)) {
                    chatPlayer(neutral, lines.rumourOption)
                    rumourTalk(hunter, quick = false)
                } else {
                    chatPlayer(neutral, "See you later.")
                }
            }
            2 -> {
                chatPlayer(neutral, lines.rumourOption)
                rumourTalk(hunter, quick = false)
            }
            else -> chatPlayer(neutral, lines.leaveLine ?: lines.leaveOption)
        }
    }

    private suspend fun Dialogue.gilmanMenu(gilman: HunterRumourHuntersRow) {
        val choice =
            choice3(
                "What kinds of food and drink do you serve?", 1,
                "Can we talk about rumours?", 2,
                "I'm good, thanks.", 3,
            )
        when (choice) {
            1 -> gilmanFood()
            2 -> {
                chatPlayer(neutral, "Can we talk about rumours?")
                if (vars[UNLOCKED] == 0) {
                    chatNpc(neutral, gilman.lines.notVerity)
                    return
                }
                chatNpc(neutral, "Yes, yes. How can I help you on that front?")
                rumourTalk(gilman, quick = false, asked = true)
            }
            else -> chatPlayer(neutral, "I'm good, thanks.")
        }
    }

    private suspend fun Dialogue.gilmanFood() {
        chatPlayer(quiz, "What kinds of food and drink do you serve?")
        chatNpc(neutral, "In terms of drinks, we've got a Trapper's Tipple, and for food, a simple hearty stew.")
        if (player.inv.freeSpace() == 0) {
            chatNpc(neutral, "But you don't have room to hold either of them, so you'll have to come back later.")
            chatPlayer(quiz, "Couldn't you just leave it on the ground for me?")
            chatNpc(neutral, "We don't tend to just... leave things like that around here.")
            return
        }
        val choice = choice3("I'd love a Trapper's Tipple!", 1, "Could I grab a stew?", 2, "I'll pass, thanks.", 3)
        when (choice) {
            1 -> {
                chatPlayer(happy, "I'd love a Trapper's Tipple!")
                chatNpc(neutral, "That'll be three coins then.")
                if (access.buy(TIPPLE, TIPPLE_PRICE)) mesbox("You buy a pint of Trapper's Tipple.")
                else chatPlayer(sad, "Oh, I can't afford one right now.")
            }
            2 -> {
                chatPlayer(quiz, "Could I grab a stew?")
                chatNpc(happy, "Absolutely! They're 20 coins these days.")
                if (access.buy(STEW, STEW_PRICE)) mesbox("You buy some stew.")
                else chatPlayer(sad, "Oh, I can't afford that right now.")
            }
            else -> chatPlayer(neutral, "I'll pass, thanks.")
        }
    }

    fun ProtectedAccess.buy(obj: String, price: Int): Boolean {
        if (inv.count(COINS) < price) return false
        if (inv.count(COINS) > price && inv.freeSpace() == 0) return false
        if (invDel(inv, COINS, price).failure) return false
        if (invAdd(inv, obj).failure) {
            invAdd(inv, COINS, price)
            return false
        }
        return true
    }

    private suspend fun Dialogue.rumourTalk(hunter: HunterRumourHuntersRow, quick: Boolean, asked: Boolean = false) {
        val lines = hunter.lines
        if (vars[UNLOCKED] == 0) {
            if (hunter.name == "Wolf") chatNpc(neutral, "My apologies, but I don't have time to chat right now.")
            else chatNpc(neutral, lines.notVerity)
            return
        }
        if (player.statBase(HunterRumours.HUNTER) < hunter.level) {
            if (hunter.name == "Wolf") {
                chatNpc(neutral, "There's an extra seat at this table. I suppose you'd be interested in claiming it one day?")
            }
            mesbox("You need ${hunter.level} Hunter to take on tier ${tier(hunter)} Hunters' Rumours.")
            return
        }
        if (rumours.hasPart(player, hunter)) return handIn(hunter, quick)
        val own = rumours.rumourOf(player, hunter)
        val current = rumours.currentHunter(player)
        val currentRumour = current?.let { rumours.rumourOf(player, it) }
        when {
            own != null && (current == hunter || currentRumour == null) -> {
                rumours.resume(player, hunter)
                if (hunter.name == "Gilman") {
                    chatPlayer(quiz, "Can you remind me which rumour I'm chasing right now?")
                }
                chatNpc(
                    neutral,
                    "You're hunting for me at the moment. This rumour was about ${a(own)}. You'll need to bring " +
                        "back ${parts(own)}.",
                )
            }
            current != null && current != hunter && currentRumour != null ->
                otherHuntersRumour(hunter, current, currentRumour, own)
            else -> {
                if (asked) chatPlayer(quiz, "Do you have any new ones for me?")
                newRumour(hunter)
            }
        }
    }

    private suspend fun Dialogue.otherHuntersRumour(
        hunter: HunterRumourHuntersRow,
        other: HunterRumourHuntersRow,
        otherRumour: HunterRumoursRow,
        own: HunterRumoursRow?,
    ) {
        val lines = hunter.lines
        val title = "Switch to ${npcName(hunter)}'s rumour?"
        if (hunter.name == "Gilman") {
            chatNpc(
                neutral,
                "I can see you're following ${other.name}'s rumour looking for ${a(otherRumour)} at the moment. " +
                    "You'll need to bring back ${parts(otherRumour)}.",
            )
            if (own == null) {
                if (choice2("Yes.", true, "No.", false, title)) newRumour(hunter)
                return
            }
            chatNpc(
                neutral,
                "I seem to remember sending you off to hunt ${a(own)} and bring back one of its " +
                    "${own.partText}. Would you prefer that one, or a new one entirely?",
            )
            val choice = choice3("Yes, go back to his old one.", 1, "Yes, but with a brand new one.", 2, "No.", 3, title)
            when (choice) {
                1 -> {
                    rumours.resume(player, hunter)
                    chatNpc(neutral, lines.remember.format(a(own), own.partText))
                }
                2 -> newRumour(hunter)
            }
            return
        }
        chatNpc(
            neutral,
            "I hear you're hunting for ${other.name}. Aren't you looking for ${a(otherRumour)} at the moment? " +
                "You'll need to bring back ${parts(otherRumour)}.",
        )
        chatNpc(neutral, "Unless you fancy hunting for me instead?")
        if (!choice2("Yes.", true, "No.", false, title)) return
        if (own != null) {
            rumours.resume(player, hunter)
            chatNpc(neutral, lines.remember.format(a(own), own.partText))
        } else {
            newRumour(hunter)
        }
    }

    private suspend fun Dialogue.newRumour(hunter: HunterRumourHuntersRow) {
        val rumour = rumours.assign(player, hunter)
        if (rumour == null) {
            chatNpc(neutral, "I don't have any rumours for you at the moment.")
            return
        }
        val offers = ASSIGN_LINES + hunter.lines.extraAssign
        chatNpc(neutral, offers[random.of(offers.size)].format(rumour.name, rumour.partText))
    }

    private suspend fun Dialogue.handIn(hunter: HunterRumourHuntersRow, quick: Boolean) {
        if (!rumours.turnIn(player, hunter)) return
        if (quick) {
            chatNpc(neutral, "Thanks for that. I'll mark off that report for you. Would you like another rumour?")
        } else {
            chatNpc(happy, hunter.lines.done)
            chatNpc(quiz, hunter.lines.another)
        }
        if (choice2("Yes.", true, "No.", false, "Get another rumour?")) newRumour(hunter) else options(hunter)
    }

    private suspend fun Dialogue.verity() {
        if (vars[UNLOCKED] == 0) {
            chatNpc(
                neutral,
                "Oh, hello there newcomer. I'm Guild Scribe Verity. Do you need me to explain the work we do here?",
            )
            chatPlayer(neutral, "Yes please.")
            chatNpc(
                neutral,
                "So it's nice and simple. Just chat to any of the hunters in here to help them out. They're all " +
                    "following reports that I've split up amongst them.",
            )
            chatPlayer(quiz, "Reports?")
            chatNpc(
                neutral,
                "Indeed. There are all sorts of interesting creatures to hunt out there. Sometimes we get reports of " +
                    "a particularly unique example. When that happens, one of the hunters will set out to find it.",
            )
            chatNpc(
                neutral,
                "If you do decide to take a job from any of the hunters, they'll tell you what type of creature " +
                    "they're looking into. Hunt that creature until you encounter the unique specimen.",
            )
            chatNpc(
                neutral,
                "Once you've caught it, bring some proof back to the hunter that sent you out in the first place. " +
                    "I'm sure they'll reward you for your time.",
            )
            VarPlayerIntMapSetter.set(player, UNLOCKED, 1)
            chatPlayer(neutral, "I see. Thanks for the information.")
            return
        }
        chatNpc(neutral, "Nilsal, hunter. Anything to report?")
        chatPlayer(neutral, "Nothing abnormal today.")
        verityMenu()
    }

    private suspend fun Dialogue.verityMenu() {
        val choice =
            choice3("What does a scribe do?", 1, "How many rumours have I completed?", 2, "I have to go.", 3)
        when (choice) {
            1 -> {
                chatPlayer(quiz, "What does a scribe do?")
                chatNpc(
                    neutral,
                    "I'm the primary record-keeper here. I organise all of our reports and write up anything " +
                        "important to be sent back to the city.",
                )
                chatPlayer(quiz, "Do you go out on any hunts with the others?")
                chatNpc(
                    neutral,
                    "I wouldn't know what to do with myself out there, so I like to stay back here with my scrolls.",
                )
                verityMenu()
            }
            2 -> {
                chatPlayer(quiz, "How many rumours have I completed?")
                chatNpc(
                    happy,
                    "Our records suggest you've managed to complete ${vars[COMPLETED]} rumours for us!",
                )
            }
            else -> chatPlayer(neutral, "I have to go.")
        }
    }

    private suspend fun Dialogue.veritySettings() {
        val on = vars[BACK_TO_BACK] != BACK_TO_BACK_OFF
        val amount = if (on) "more" else "less"
        chatNpc(
            quiz,
            "Would you like me to filter the rumour information a little $amount than I am right now?",
        )
        val title = if (on) "Disable back-to-back rumours?" else "Enable back-to-back rumours?"
        if (!choice2("Yes", true, "No", false, title)) return
        VarPlayerIntMapSetter.set(player, BACK_TO_BACK, if (on) BACK_TO_BACK_OFF else 0)
        mesbox(if (on) "Repeat rumours will no longer be offered." else "Repeat rumours will now be offered.")
    }

    private suspend fun Dialogue.say(line: Line) {
        if (line.player) chatPlayer(neutral, line.text.replace("[player name]", player.displayName))
        else chatNpc(neutral, line.text)
    }

    private fun npcName(hunter: HunterRumourHuntersRow): String = hunter.npc.name

    private fun tier(hunter: HunterRumourHuntersRow): Int =
        when {
            hunter.level >= 91 -> 4
            hunter.level >= 72 -> 3
            hunter.level >= 57 -> 2
            else -> 1
        }

    private fun a(rumour: HunterRumoursRow): String = "${HunterRumours.article(rumour.name)} ${rumour.name}"

    private fun parts(rumour: HunterRumoursRow): String = "some of its ${rumour.partText}"

    class Line(val player: Boolean, val text: String)

    class HunterLines(
        val greeting: List<Line>,
        val chatOption: String,
        val chat: List<Line>,
        val rumourOption: String,
        val leaveOption: String,
        val leaveLine: String? = null,
        val notVerity: String,
        val done: String,
        val another: String,
        val remember: String,
        val extraAssign: List<String> = emptyList(),
    )

    companion object {
        const val VERITY = "npc.hg_verity"
        const val BAR_RANGE = 3
        val BEHIND_BAR = setOf("npc.hg_gilman", VERITY)
        const val COINS = "obj.coins"
        const val TIPPLE = "obj.trappers_tipple"
        const val STEW = "obj.stew"
        const val TIPPLE_PRICE = 3
        const val STEW_PRICE = 20

        fun npc(text: String) = Line(false, text)

        fun player(text: String) = Line(true, text)

        const val NEW_HERE = "You're new here, aren't you? You might want to talk to Verity before you get started."

        val ASSIGN_LINES =
            listOf(
                "Could you check out the validity of this report for me? Someone thinks they've seen a %1\$s. Once " +
                    "you find it, bring back one of its %2\$s.",
                "There's been rumours of a rare %1\$s coming in. Once you find it, bring back one of its %2\$s.",
                "We've got reports of a slightly different %1\$s in the local population. Once you find it, bring " +
                    "back one of its %2\$s.",
                "Looks like we need some information on a unique specimen amongst the local %1\$s population. Once " +
                    "you find it, bring back some of its %2\$s.",
                "I've heard rumours of a strange %1\$s that you could check out for me. Once you find it, bring " +
                    "back one of its %2\$s.",
            )

        val LINES =
            mapOf(
                "Gilman" to
                    HunterLines(
                        greeting = listOf(npc("Greetings, fellow hunter. How can I be of help?")),
                        chatOption = "",
                        chat = emptyList(),
                        rumourOption = "Can we talk about rumours?",
                        leaveOption = "I'm good, thanks.",
                        notVerity =
                            "Looks like you could do with talking to Verity first, given you're new to this whole " +
                                "business.",
                        done = "Another one done? You're really doing a lot for the guild.",
                        another = "Are you up for another?",
                        remember = "That was it. I asked you to hunt %1\$s and bring back one of its %2\$s. Best of luck.",
                    ),
                "Cervus" to
                    HunterLines(
                        greeting =
                            listOf(npc("Good day, hunter."), player("Good day."), npc("What's brought you in here today?")),
                        chatOption = "Just looking for something to do.",
                        chat =
                            listOf(
                                player("Just looking for something to do."),
                                npc("Ah, that's the same thing that brought me here."),
                                player("Are you not a hunter by trade?"),
                                npc("Only by interest at the moment, although the gang here are trying their best to train me."),
                                player("Are you enjoying it?"),
                                npc("Well it's less boring than what I've done before, so I can't complain!"),
                                player("What were you doing before this then?"),
                                npc(
                                    "Well, my parents wanted me to go into the whole wine growing business, but farming " +
                                        "was a bit too slow-paced for me.",
                                ),
                            ),
                        rumourOption = "Can I get a rumour?",
                        leaveOption = "Never mind.",
                        notVerity = NEW_HERE,
                        done = "Excellent! Thanks for getting that done. Here's your reward.",
                        another = "Are you interested in taking on another one for me?",
                        remember = "Ah, yes. I asked you to hunt %1\$s and bring back some of its %2\$s. Hope it's not too boring for you!",
                    ),
                "Ornus" to
                    HunterLines(
                        greeting =
                            listOf(
                                npc("Welcome to the Burrow, hunter."),
                                player("Thanks! It's nice and homely in here, isn't it?"),
                                npc("Quite! It's a good break from the city."),
                            ),
                        chatOption = "How long have you been around here?",
                        chat =
                            listOf(
                                player("How long have you been around here?"),
                                npc("Not too long, in fact. Gilman invited me to join the guild a short while ago."),
                                player("What were you up to before that?"),
                                npc("Oh... A little bit of this and that."),
                                player("Nothing particularly interesting?"),
                                npc("You could say that... I'm definitely not proud of it."),
                            ),
                        rumourOption = "Can I get a rumour?",
                        leaveOption = "Never mind.",
                        notVerity = NEW_HERE,
                        done = "Verity will be glad to have another one ticked off. Thanks. Here's your reward.",
                        another = "Fancy taking on another?",
                        remember = "Oh, I remember! I asked you to hunt %1\$s and bring back its %2\$s. I hope it's going okay!",
                    ),
                "Aco" to
                    HunterLines(
                        greeting =
                            listOf(
                                npc("Hunter Aco reporting for duty!"),
                                player("[player name] reporting for... nothing in particular, really."),
                                npc("What can I do for you?"),
                            ),
                        chatOption = "What do you do here?",
                        chat =
                            listOf(
                                player("What do you do here?"),
                                npc("Hunting, mostly."),
                                player("Hmm, I probably could've guessed that."),
                                npc("Perhaps you confused me for Teco over there? He's more into plants and all that."),
                                player("What got you into hunting then?"),
                                npc("Don't tell too many people, but it's much nicer here than wearing a uniform and marching around!"),
                                npc(
                                    "I'm still not sure how I feel about having Teco around the whole time though. People " +
                                        "confuse us too often for my liking.",
                                ),
                            ),
                        rumourOption = "Can I get a rumour?",
                        leaveOption = "Never mind.",
                        notVerity = NEW_HERE,
                        done = "Thanks for doing that for me. Here's your reward.",
                        another = "Would you like another rumour?",
                        remember = "Now then... I asked you to hunt %1\$s for me and bring back one of its %2\$s.",
                    ),
                "Teco" to
                    HunterLines(
                        greeting =
                            listOf(
                                npc("Oh, what corner have you grown from then?"),
                                player("The upstairs corner!"),
                                npc("That would explain why I didn't see you coming."),
                            ),
                        chatOption = "What are you up to?",
                        chat =
                            listOf(
                                player("What are you up to?"),
                                npc("Same thing I'm always up to: studying the plants."),
                                player("That's an odd thing for a hunter to do, no?"),
                                npc("The plants make me happy! Not to mention, many of them are excellent for healing injuries."),
                                player("Yes! I happened to use a couple of them lately to help out Fox."),
                                npc("It's worth also being aware of which ones may be dangerous, but that lesson is best learned through practice."),
                                player("Practice?"),
                                npc("Yes, by frequent exposure! You start to recognise them very quickly after that."),
                            ),
                        rumourOption = "Can I get a rumour?",
                        leaveOption = "Never mind.",
                        notVerity = NEW_HERE,
                        done = "All done? That's great! I'll make sure the report is submitted. Here's your reward.",
                        another = "Dare to take on another rumour?",
                        remember = "I asked you to hunt %1\$s for me and bring back some of its %2\$s. Thanks!",
                        extraAssign =
                            listOf(
                                "We've got rumours circling about an odd %1\$s. You can follow up on that if you'd like? " +
                                    "Once you find it, bring back some of its %2\$s.",
                            ),
                    ),
                "Wolf" to
                    HunterLines(
                        greeting = listOf(npc("Nilsal, hunter."), player("Hello there."), npc("Can I help you out?")),
                        chatOption = "Can we talk about Fox?",
                        chat =
                            listOf(
                                player("Can we talk about Fox?"),
                                npc("I'm so glad you managed to find and help him. I've read his report and you feature quite heavily."),
                                player("Oh, he did most of it himself. I was just running errands, really!"),
                                npc("Oi - don't sell yourself short. More importantly, don't give Fox too much credit."),
                            ),
                        rumourOption = "Can I get a rumour?",
                        leaveOption = "Never mind.",
                        leaveLine = "See you later.",
                        notVerity = NEW_HERE,
                        done = "You're quite the talented hunter! I'll have to keep you around. Here's your reward.",
                        another = "Want to test your skills again?",
                        remember = "If I'm remembering correctly, I asked you to hunt %1\$s and bring back some of its %2\$s.",
                    ),
            )
    }
}
