package org.rsmod.content.areas.misc.guilds.firemaking

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.misc.guilds.SKILLCAPE_LEVEL
import org.rsmod.content.areas.misc.guilds.sellSkillcape
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ignatius_Vulcan
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] Ignatius's fire and the player's "Yeeouch!" overhead, and
// whether a cape in the bank also unlocks the hood option
class IgnatiusVulcan @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.skillcapes_firemaster") { startDialogue(it.npc) { ignatius() } }
    }

    private suspend fun Dialogue.ignatius() {
        chatNpc(quiz, "Can I help you at all?")
        if (player.statBase("stat.firemaking") < SKILLCAPE_LEVEL) {
            val option =
                choice3("Who are you?", 1, "What is that cape you're wearing?", 2, "No, thanks.", 3)
            when (option) {
                1 -> whoAreYou()
                2 -> {
                    chatPlayer(quiz, "What is that cape you're wearing?")
                    chatNpc(
                        happy,
                        "This is a Skillcape of Firemaking, so bright that wearing it lights up " +
                            "caves! I was given it in recognition of my skill as the greatest " +
                            "firemaker in the lands! I AM the Master of Flame!",
                    )
                    chatPlayer(
                        neutral,
                        "Hmm, I'll be going now. Keep a sharp look out for those men with their " +
                            "white jackets!",
                    )
                }
                3 -> noThanks()
            }
            return
        }
        val option =
            choice3("Who are you?", 1, "Can I buy a Skillcape of Firemaking?", 2, "No, thanks.", 3)
        when (option) {
            1 -> whoAreYou()
            2 -> if (hasCapeWithoutHood()) capeOrHood() else buyCape()
            3 -> noThanks()
        }
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            happy,
            "My name is Ignatius Vulcan. Once I was - like you - an adventurer, but that was " +
                "before I realised the beauty and power of flame! Just look at this...",
        )
        chatNpc(
            happy,
            "Stare into the flame and witness the purity and power of fire! As my attraction to " +
                "flame grew, so did my skills at firelighting. I began to neglect my combat " +
                "skills, my Mining skills and my questing. Who needs such",
        )
        chatNpc(
            happy,
            "mundane skills when one can harness the power of fire? After years of practice I am " +
                "now the acknowledged Master of Flame! Everything must be purified by fire!",
        )
        chatPlayer(worried, "Okaaaay! Err, I'll be going now. Umm, get better soon.")
    }

    private suspend fun Dialogue.noThanks() {
        chatPlayer(neutral, "No, thanks.")
        chatNpc(neutral, "Okay, keep the fire burning.")
    }

    private fun Dialogue.hasCapeWithoutHood(): Boolean {
        val owns = { obj: String -> player.inv.count(obj) > 0 || player.worn.count(obj) > 0 }
        return CAPES.any(owns) && !owns(HOOD)
    }

    private suspend fun Dialogue.capeOrHood() {
        when (choice2("Skillcape", 1, "Hood", 2)) {
            1 -> buyCape()
            2 -> {
                chatPlayer(quiz, "May I have another hood for my cape, please?")
                if (player.inv.freeSpace() < 1) {
                    chatNpc(
                        neutral,
                        "You don't appear to have enough space in your inventory, come back and " +
                            "see me when you do.",
                    )
                    return
                }
                chatNpc(happy, "Most certainly, and free of charge!")
                player.invAddOrDrop(objRepo, HOOD)
            }
        }
    }

    private suspend fun Dialogue.buyCape() {
        chatPlayer(quiz, "Can I buy a Skillcape of Firemaking?")
        chatNpc(
            happy,
            "Of course you can. You are already a master of fire, you just need the cape to " +
                "prove it. The cape is so bright that wearing it will light up dark caves. " +
                "That'll be 99000 coins, please.",
        )
        val option = choice2("No thanks, that's too much.", 1, "Okay, that seems reasonable.", 2)
        if (option == 1) {
            chatPlayer(neutral, "No thanks, that's too much.")
            chatNpc(neutral, "Sorry to hear that, I'll be here if you change your mind.")
            return
        }
        chatPlayer(happy, "Okay, that seems reasonable.")
        if (sellSkillcape(objRepo, CAPE, HOOD)) {
            chatNpc(happy, "I'm sure you'll look hot in that cape.")
        }
    }

    private companion object {
        const val CAPE = "obj.skillcape_firemaking"
        const val HOOD = "obj.skillcape_firemaking_hood"
        val CAPES = listOf(CAPE, "obj.skillcape_firemaking_trimmed")
    }
}
