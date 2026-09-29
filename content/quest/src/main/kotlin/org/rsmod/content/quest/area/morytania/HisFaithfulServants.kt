package org.rsmod.content.quest.area.morytania

import kotlin.random.Random
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class HisFaithfulServants :
    QuestScript(
        questKey = "miniquest_hisfaithfulservants",
        questVarp = "varp.hfs_primary",
        rewards = rewards {},
        completedQuestItemDisplay = ItemRewardDisplay("obj.hfs_reward_lamp"),
        questVarbit = "varbit.hfs",
    ) {

    private var Player.lampClaimed by boolVarBit("varbit.hfs_reward_lamp")
    private var Player.cryptMapStudied by boolVarBit("varbit.barrows_map")
    private var Player.warningsCondensed by boolVarBit("varbit.barrows_warning_toggle")
    private var Player.miniquestsCompleted by intVarBit("varbit.miniquests_completed_count")
    private val Player.barrowsChests by intVarp("varp.total_barrows_chests")

    override fun ScriptContext.init() {
        onOpNpc1("npc.barrows_oldman") { startDialogue(it.npc) { oldManDialogue() } }
        onOpHeld1("obj.hfs_reward_lamp") { rubLamp() }
        onOpHeld1("obj.barrows_map") { studyMap() }
    }

    override fun subTitle(): String =
        "talking to the <col=800000>Strange Old Man</col> at the <col=800000>Barrows</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            description(
                "The <red>Strange Old Man</red> at the <red>Barrows</red> wants me to bring him " +
                    "an <red>icon</red> that the <red>Barrows brothers</red> have been guarding."
            )

            objective(
                "I need to slay the six <red>Barrows brothers</red> and loot the " +
                    "<red>Barrows chest</red> to find the <red>icon</red>."
            ) {
                visibleWhen { !hasIcon(access.player) && stage(access.player) < STAGE_ICON_GIVEN }
            }

            objective("I have the <red>icon</red>. I should take it to the <red>Strange Old Man</red>.") {
                visibleWhen { hasIcon(access.player) && stage(access.player) < STAGE_ICON_GIVEN }
            }

            objective("I gave the <red>icon</red> to the <red>Strange Old Man</red>. I should talk to him again.") {
                visibleWhen { stage(access.player) >= STAGE_ICON_GIVEN }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line("The Strange Old Man asked me to slay the Barrows brothers and bring him an icon from the Barrows chest.")
            line("I brought him the icon and he destroyed it, saying that there was a new plan.")
            line("As a reward he gave me a lamp and a map of the Barrows crypt.")
        }

    private fun stage(player: Player): Int = quest.getQuestStage(player)

    private fun hasIcon(player: Player): Boolean = "obj.barrows_icon" in player.inv

    private suspend fun Dialogue.oldManDialogue() {
        val stage = stage(player)
        when {
            stage >= STAGE_COMPLETE && hasUnclaimedRewards() -> reclaimRewards()
            stage >= STAGE_COMPLETE -> standardDialogue()
            stage == STAGE_ICON_DESTROYED -> {
                chatPlayer(confused, "Why did you destroy the icon? I though you needed it!")
                afterIconDestroyed()
            }
            stage == STAGE_ICON_GIVEN -> afterIconGiven()
            stage >= STAGE_STARTED && hasIcon(player) -> turnInIcon()
            stage >= STAGE_STARTED -> askForIcon()
            QuestRequirements.hasCompleted(player, "quest_priestinperil") -> startQuest()
            else -> standardDialogue()
        }
    }

    private suspend fun Dialogue.startQuest() {
        chatNpc(happy, "It's time.")
        chatPlayer(confused, "Time for what?")
        chatNpc(happy, "Time to dig. To dig, dig, dig. But no normal dig. We now have new purpose!")
        chatPlayer(confused, "What do you mean? What purpose?")
        chatNpc(happy, "The brothers six! Dutiful servants of Saradomin... At least, that's what they'd tell you! Ha! Not so. Not so.")
        chatNpc(happy, "They made a pact you see! Strength in life for servitude in death.")
        chatPlayer(confused, "Servitude?")
        chatNpc(happy, "Yes! They guard it for him! That was their purpose, but that time is up now. He wants it back! You can get it! You can dig!")
        chatPlayer(confused, "You want me to bring you something?")
        chatNpc(happy, "The icon! He needs it now! You must dig, dig, dig. Slay the brothers six! It's okay, it won't hurt them... much. The icon will be in the chest!")
        chatPlayer(confused, "Right... So you want me to go and kill six brothers and bring you back an icon?")
        chatNpc(happy, "Yes! Do this! Do this! Maybe little prize for you if you do. But first you must dig! Dig, dig, dig!")
        quest.setQuestStage(access, STAGE_STARTED)
        chatPlayer(confused, "Okay...")
        questMenu("I'll be back soon.")
    }

    private suspend fun Dialogue.askForIcon() {
        chatNpc(quiz, "Icon?")
        chatPlayer(neutral, "I don't have it yet.")
        chatNpc(happy, "Slow! No slow! Only fast! Fast dig! Dig, dig, dig!")
        questMenu("Okay, I'll be back soon.")
    }

    private suspend fun Dialogue.questMenu(leave: String) {
        val option =
            if (player.barrowsChests > 0) {
                choice3(
                    "Can you tell me more about the brothers?", MENU_BROTHERS,
                    "I wanted to ask you about warnings.", MENU_WARNINGS,
                    leave, MENU_LEAVE,
                )
            } else {
                choice2("Can you tell me more about the brothers?", MENU_BROTHERS, leave, MENU_LEAVE)
            }
        when (option) {
            MENU_BROTHERS -> askAboutBrothers()
            MENU_WARNINGS -> askAboutWarnings()
            else -> chatPlayer(confused, leave)
        }
    }

    private suspend fun Dialogue.askAboutBrothers() {
        chatPlayer(quiz, "Can you tell me more about the brothers?")
        if ("obj.barrows_book_history" in player.inv) {
            chatNpc(happy, "The brothers? His faithful servants! Book tell you more! Read it! Read it!")
            return
        }
        chatNpc(happy, "The brothers? His faithful servants! Book tell you more! Take it! I don't even want it!")
        if (access.invAdd(access.inv, "obj.barrows_book_history").success) {
            objbox("obj.barrows_book_history", "The Strange Old Man gives you a book.")
        }
    }

    private suspend fun Dialogue.askAboutWarnings() {
        chatPlayer(neutral, "I wanted to ask you about warnings.")
        chatNpc(quiz, "Yes?")
        if (player.warningsCondensed) {
            chatPlayer(neutral, "Thanks for having those mental warnings I was having condensed into a single one, but I miss having them seperated.")
            chatPlayer(quiz, "Is there any chance you'd be willing to change it back for me?")
        } else {
            chatPlayer(neutral, "Currently when I'm trying to go plundering the catacombs here, my mind is plagued by several inconvenient warnings about the dangers below.")
            chatPlayer(quiz, "I was wondering, do you know of a way to condense these warnings into a single one?")
        }
        chatNpc(scared, "AAAAAAAAARRRRRRGGGGGHHHHHHHH!")
        player.warningsCondensed = !player.warningsCondensed
        chatPlayer(confused, "I'm guessing that was a yes? In that case, thanks.")
    }

    private suspend fun Dialogue.turnInIcon() {
        chatNpc(quiz, "Icon?")
        chatPlayer(neutral, "Yes, I have it here.")
        chatNpc(happy, "At last! Quick! Give it!")
        if (access.invDel(access.inv, "obj.barrows_icon").failure) return
        quest.setQuestStage(access, STAGE_ICON_GIVEN)
        objbox("obj.barrows_icon", "You give the icon to the Strange Old Man.")
        afterIconGiven()
    }

    private suspend fun Dialogue.afterIconGiven() {
        chatNpc(happy, "You did it! Now for me.")
        quest.setQuestStage(access, STAGE_ICON_DESTROYED)
        access.soundSynth("synth.hfs_icon_disintegrate")
        access.soundSynth("synth.hfs_icon_disintegrate_echo", delay = ICON_ECHO_DELAY)
        objbox("obj.barrows_icon_dust", "Before you can react, the Strange Old Man casts a spell. The icon disintegrates in his hands.")
        chatNpc(happy, "It's all little bits now.")
        chatPlayer(shocked, "What? But why? I thought you needed it!")
        afterIconDestroyed()
    }

    private suspend fun Dialogue.afterIconDestroyed() {
        chatNpc(happy, "Yes! Needed it for him, but not to keep! His friend asked him to protect it, but he made a new plan, a better plan!")
        chatPlayer(confused, "What are you on about?")
        chatNpc(happy, "The old plan is gone. We're not doing it that way. His friend won't be happy, but it was for the best!")
        chatNpc(shifty, "Now we're doing it his way...")
        chatNpc(happy, "Anyway, job well done! Still more digging though. Always dig. Dig, dig, dig!")
        chatNpc(happy, "Oh, and don't forget your prize! He made it very clear you had to have a prize!")
        completeMiniquest()
        access.invAdd(access.inv, "obj.hfs_reward_lamp")
        access.invAdd(access.inv, "obj.barrows_map")
        player.midiJingle(COMPLETE_JINGLE)
        doubleobjbox("obj.hfs_reward_lamp", "obj.barrows_map", "The Strange Old Man gives you a lamp and a map.")
        chatPlayer(confused, "Thanks... I guess.")
    }

    private fun Dialogue.completeMiniquest() {
        VarPlayerIntMapSetter.set(player, "varbit.hfs", STAGE_COMPLETE)
        player.miniquestsCompleted++
    }

    private fun Dialogue.lampMissing(): Boolean =
        !player.lampClaimed && !ownsObj("obj.hfs_reward_lamp")

    private fun Dialogue.mapMissing(): Boolean =
        !player.cryptMapStudied && !ownsObj("obj.barrows_map")

    private fun Dialogue.ownsObj(obj: String): Boolean = obj in access.inv || obj in access.bank

    private fun Dialogue.hasUnclaimedRewards(): Boolean = lampMissing() || mapMissing()

    private suspend fun Dialogue.reclaimRewards() {
        val lamp = lampMissing()
        val map = mapMissing()
        val needed = listOf(lamp, map).count { it }
        chatNpc(happy, "This is for you. Take it! Take it!")
        if (access.inv.freeSpace() < needed) {
            mesbox("You don't have enough inventory space.")
            return
        }
        if (lamp) access.invAdd(access.inv, "obj.hfs_reward_lamp")
        if (map) access.invAdd(access.inv, "obj.barrows_map")
        when {
            lamp && map ->
                doubleobjbox("obj.hfs_reward_lamp", "obj.barrows_map", "The Strange Old Man has given you a lamp and a map.")
            lamp -> objbox("obj.hfs_reward_lamp", "The Strange Old Man has given you a lamp.")
            else -> objbox("obj.barrows_map", "The Strange Old Man has given you a map.")
        }
        chatPlayer(happy, "Thanks.")
        questMenu("I'll leave you to it then...")
    }

    private suspend fun Dialogue.standardDialogue() {
        when (Random.nextInt(5)) {
            0 -> secretDialogue()
            1 -> knockKnockDialogue()
            2 -> screamDialogue()
            3 -> bookDialogue()
            else -> {
                chatNpc(happy, "Dig, dig, dig.")
                when (Random.nextInt(4)) {
                    0 -> secretDialogue()
                    1 -> knockKnockDialogue()
                    2 -> screamDialogue()
                    else -> bookDialogue()
                }
            }
        }
    }

    private suspend fun Dialogue.secretDialogue() {
        chatNpc(shifty, "Pst, wanna hear a secret?")
        when (choice3("Sure!", 1, "No thanks.", 2, "Ask about tunnel warnings.", 3)) {
            1 -> {
                chatPlayer(happy, "Sure!")
                chatNpc(happy, "They're not normal!")
            }
            2 -> chatPlayer(neutral, "No thanks.")
            3 -> tunnelWarnings()
        }
    }

    private suspend fun Dialogue.knockKnockDialogue() {
        chatNpc(happy, "Knock knock.")
        chatPlayer(quiz, "Who's there?")
        chatNpc(laugh, "A big scary monster HAHAHAHAHAHAHAHAHAHA!")
        chatPlayer(confused, "Okay...")
        standardMenu()
    }

    private suspend fun Dialogue.screamDialogue() {
        chatNpc(scared, "AAAAAAAAARRRRRRGGGGGHHHHHHHH!")
        while (true) {
            when (choice3("What's wrong?", 1, "I'll leave you to it then...", 2, "Ask about tunnel warnings.", 3)) {
                1 -> {
                    chatPlayer(quiz, "What's wrong?")
                    chatNpc(scared, "AAAAAAAAARRRRRRGGGGGHHHHHHHH!")
                }
                2 -> return chatPlayer(neutral, "I'll leave you to it then...")
                else -> return tunnelWarnings()
            }
        }
    }

    private suspend fun Dialogue.bookDialogue() {
        chatNpc(angry, "What? I didn't ask for a book!")
        access.invAdd(access.inv, "obj.barrows_book_history")
        standardMenu()
    }

    private suspend fun Dialogue.standardMenu() {
        when (choice2("Ask about tunnel warnings.", 1, "I'll leave you to it then...", 2)) {
            1 -> tunnelWarnings()
            2 -> chatPlayer(neutral, "I'll leave you to it then...")
        }
    }

    private suspend fun Dialogue.tunnelWarnings() {
        if (player.warningsCondensed) {
            chatPlayer(neutral, "Strange Old Man, thanks for having those mental warnings I was having condensed into a single one, but I miss having them seperated.")
            chatPlayer(quiz, "Is there any chance you'd be willing to change it back for me?")
        } else {
            chatPlayer(neutral, "Strange Old Man, I was wondering if I could ask a favour of you.")
            chatPlayer(neutral, "Currently when I'm trying to go plundering the catacombs here, my mind is plagued by several inconvenient warnings about the dangers below.")
            chatPlayer(quiz, "I was wondering, do you know of a way to condense these warnings into a single condensed one?")
        }
        chatNpc(scared, "AAAAAAAAARRRRRRGGGGGHHHHHHHH?")
        player.warningsCondensed = !player.warningsCondensed
        chatPlayer(happy, "Wow, you'll have that arranged for me? Thanks a lot, Strange Old Man!")
    }

    private val Dialogue.scared
        get() = mesanim("mesanim.scared")

    private suspend fun ProtectedAccess.rubLamp() {
        startDialogue {
            val xp = (LAMP_XP * player.xpRate * player.globalXpRate).toInt()
            val claim = choice2("Yes.", true, "No.", false, title = "Use the lamp to receive ${"%,d".format(xp)} Prayer experience?")
            if (!claim || access.invDel(access.inv, "obj.hfs_reward_lamp").failure) return@startDialogue
            player.lampClaimed = true
            val gained = access.statAdvance("stat.prayer", LAMP_XP)
            access.soundSynth("synth.found_gem")
            mesbox("<col=0000ff>Your wish has been granted!</col> You have been awarded ${"%,d".format(gained)} Prayer experience!")
        }
    }

    private suspend fun ProtectedAccess.studyMap() {
        if (invDel(inv, "obj.barrows_map").failure) return
        player.cryptMapStudied = true
        startDialogue {
            objbox("obj.barrows_map", "It's a map of the Barrows Crypt. You carefully study it and memorise all the details. You then discard it.")
        }
    }

    private companion object {
        const val STAGE_STARTED = 2
        const val STAGE_ICON_GIVEN = 6
        const val STAGE_ICON_DESTROYED = 8
        const val STAGE_COMPLETE = 10
        const val MENU_BROTHERS = 1
        const val MENU_WARNINGS = 2
        const val MENU_LEAVE = 3
        const val LAMP_XP = 20_000.0
        const val COMPLETE_JINGLE = 283
        const val ICON_ECHO_DELAY = 30
    }
}
