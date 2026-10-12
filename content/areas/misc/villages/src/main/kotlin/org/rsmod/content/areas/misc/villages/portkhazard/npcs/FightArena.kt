package org.rsmod.content.areas.misc.villages.portkhazard.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:fight_arena] the Head Guard's disguise and Khali brew stages and the
//  prison guards
class FightArena @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.khazard_barman") { startDialogue(it.npc) { barman() } }
        onOpNpc1("npc.arena_guard_door_1") { startDialogue(it.npc) { doorGuard() } }
        onOpNpc1("npc.arena_guard_door_2") { startDialogue(it.npc) { doorGuard() } }
        for (guard in KHAZARD_GUARDS) {
            onOpNpc1(guard) { startDialogue(it.npc) { khazardGuard() } }
        }
        onOpNpc1("npc.arena_guard3") { startDialogue(it.npc) { offDutyGuard() } }
        onOpNpc1("npc.arena_guard2") { startDialogue(it.npc) { headGuard() } }
        onOpNpc1("npc.hengrad") { startDialogue(it.npc) { hengrad() } }
        onOpNpc1("npc.arena_spectator") { startDialogue(it.npc) { spectator() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Khazard_Barman
    // TODO(content): [quest:fight_arena] buying a Khali brew
    private suspend fun Dialogue.barman() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Hi, what can I get you? We have a range of quality brews.")
        while (true) {
            val option =
                choice3(
                    "I'll have a beer please.",
                    1,
                    "Got any news?",
                    2,
                    "I'm good, thanks.",
                    3,
                )
            when (option) {
                1 -> {
                    buyBeer()
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Got any news?")
                    chatNpc(
                        neutral,
                        "Not really, but if you want to see some action, keep an eye on the " +
                            "arena. I've seen some grand battles in my time. Ogres, goblins and " +
                            "even dragons have fought there.",
                    )
                    chatNpc(happy, "Now, can I get you something?")
                }
                3 -> {
                    chatPlayer(happy, "I'm good, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.buyBeer() {
        chatPlayer(happy, "I'll have a beer please.")
        chatNpc(happy, "That'll be two coins.")
        if (player.inv.count(COINS) < BEER_PRICE) {
            chatPlayer(sad, "Oh, I don't have enough money with me.")
            return
        }
        if (player.inv.isFull()) {
            chatPlayer(sad, "Oh, I don't have enough room for one.")
            return
        }
        if (!player.invTakeFee(BEER_PRICE)) {
            return
        }
        player.invAddOrDrop(objRepo, BEER)
        objbox(BEER, "The barman gives you a beer in exchange for two coins.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Door_Guard
    private suspend fun Dialogue.doorGuard() {
        chatPlayer(happy, "Hello.")
        if (QuestRequirements.hasCompleted(player, FIGHT_ARENA)) {
            val pronoun = if (access.isBodyTypeA()) "him" else "her"
            chatNpc(
                angry,
                "You're $pronoun! The one who murdered the General's pet. Leave this place at " +
                    "once!",
            )
            return
        }
        if (isDisguised()) {
            chatNpc(quiz, "Are you new? I've never seen you around here before.")
            chatPlayer(happy, "You could say that. Long live General Khazard!")
            chatNpc(confused, "Er... yeah...")
            return
        }
        chatNpc(angry, "What do you want?")
        chatPlayer(quiz, "Can I go in there?")
        chatNpc(
            angry,
            "Of course not! You have to be one of us, and you're clearly not. Now get out of here.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Khazard_Guard
    // TODO(content): [verify:in_game] which guards are prison guards ("Leave at once!")
    // TODO(content): [verify:in_game] whether the guard attacks after "Die you monster!"
    private suspend fun Dialogue.khazardGuard() {
        chatPlayer(happy, "Hello.")
        if (QuestRequirements.hasCompleted(player, FIGHT_ARENA)) {
            chatNpc(angry, "You! You're the one who killed Bouncer.")
            chatNpc(angry, "Die you monster!")
            return
        }
        if (isDisguised()) {
            when (access.random.of(4)) {
                0 -> chatNpc(happy, "There should be some good fights today.")
                1 -> chatNpc(neutral, "I think I'll head to the bar on my next break.")
                2 -> chatNpc(neutral, "Keep an eye out for anything suspicious.")
                else -> chatNpc(angry, "Don't bother me while I'm on duty.")
            }
            return
        }
        chatNpc(angry, "I don't know you, stranger. Get off our land.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Khazard_Guard_(Off_duty)
    private suspend fun Dialogue.offDutyGuard() {
        chatPlayer(happy, "Hello.")
        chatNpc(neutral, "Can I help you?")
        chatPlayer(confused, "I'm not sure.")
        chatNpc(
            angry,
            "Well I'm off-duty right now, so why don't you go and bother someone else.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Head_Guard_(Fight_Arena) (no disguise)
    private suspend fun Dialogue.headGuard() {
        chatNpc(angry, "This area is restricted. Leave now!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hengrad
    // TODO(content): [quest:fight_arena] "So Khazard got his hands on you too?" while captured
    private suspend fun Dialogue.hengrad() {
        if (QuestRequirements.hasCompleted(player, FIGHT_ARENA)) {
            chatPlayer(happy, "Hello again.")
            chatNpc(
                neutral,
                "I don't know how you managed to escape, but you may not be as lucky next time. " +
                    "Get out of here while you can.",
            )
            return
        }
        chatPlayer(happy, "Hello there.")
        if (isDisguised()) {
            chatNpc(quiz, "Hmm... You don't seem like a real guard to me. Who are you?")
            chatPlayer(neutral, "No one important.")
            return
        }
        chatNpc(quiz, "You're not a guard. What are you doing in here?")
        chatPlayer(neutral, "Nothing important.")
        chatNpc(neutral, "If you say so.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Spectator_(Fight_Arena)
    private suspend fun Dialogue.spectator() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Hello, stranger. Are you here for the fight arena? Apparently, there're some rich " +
                "folk fighting tomorrow. Should be entertaining.",
        )
    }

    // TODO(content): [verify:in_game] which Khazard armour pieces count as a disguise
    private fun Dialogue.isDisguised(): Boolean =
        KHAZARD_HELMET in player.worn && KHAZARD_PLATEMAIL in player.worn

    private companion object {
        const val FIGHT_ARENA = "quest_fightarena"
        const val KHAZARD_HELMET = "obj.khazard_helmet"
        const val KHAZARD_PLATEMAIL = "obj.khazard_platemail"
        const val COINS = "obj.coins"
        const val BEER = "obj.beer"
        const val BEER_PRICE = 2
        val KHAZARD_GUARDS =
            listOf(
                "npc.arena_guard1",
                "npc.arena_guard4",
                "npc.arena_guard5",
                "npc.arena_guard6",
                "npc.arena_guard7",
            )
    }
}
