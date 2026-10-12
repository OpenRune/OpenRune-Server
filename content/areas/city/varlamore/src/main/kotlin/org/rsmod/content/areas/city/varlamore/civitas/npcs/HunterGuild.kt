package org.rsmod.content.areas.city.varlamore.civitas.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class HunterGuild : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hg_apatura") { startDialogue(it.npc) { apatura() } }
        onOpNpc1("npc.hg_verity") { startDialogue(it.npc) { verity() } }
        onOpNpc1("npc.hunter_guild_witchy") { startDialogue(it.npc) { orsaga() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guildmaster_Apatura
    // TODO(content): [system:met_flags] the "Nilsal, hunter." greeting on later visits
    // TODO(content): [skill:hunters_rumours] the guild history note at 50 and 100 rumours and
    //  Apatura's key at 150
    // TODO(content): [system:skillcape_teleport] enhancing a Hunter cape with a guild teleport
    private suspend fun Dialogue.apatura() {
        if (!hasGuildHunterLevel()) {
            chatNpc(
                neutral,
                "Sorry, but you need to be a better hunter before we can help you out.",
            )
            mesbox("You need 46 Hunter to access Hunter Guild amenities.")
            return
        }
        chatNpc(happy, "Oh, you're a new face around here, aren't you?")
        chatPlayer(happy, "I am indeed. Just exploring the savannah.")
        chatNpc(happy, "Well, I'm Apatura. Let me welcome you to our small corner of Varlamore.")
        when (choice2("What do you do here?", 1, "I'd better be off.", 2)) {
            1 -> {
                chatPlayer(quiz, "What do you do here?")
                chatNpc(
                    neutral,
                    "We're mostly here to keep an eye on the local animal populations, and " +
                        "provide animal parts to the city.",
                )
                chatNpc(neutral, "As for me personally, I'm the current master of this guild.")
                chatPlayer(quiz, "Current master?")
                chatNpc(
                    neutral,
                    "A hunter's work is dangerous. We tend to go through members faster than " +
                        "other guilds. That said, I'm usually too busy with politics and " +
                        "paperwork to go out hunting these days.",
                )
            }
            2 -> chatPlayer(neutral, "I'd better be off.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guild_Scribe_Verity (first time talking)
    // TODO(content): [system:met_flags] the standard dialogue on later visits
    // TODO(content): [skill:hunters_rumours] the completed rumour count, trading in spare hunter
    //  gear and the back-to-back rumour setting
    private suspend fun Dialogue.verity() {
        chatNpc(
            happy,
            "Oh, hello there newcomer. I'm Guild Scribe Verity. Do you need me to explain the " +
                "work we do here?",
        )
        chatPlayer(happy, "Yes please.")
        chatNpc(
            happy,
            "So it's nice and simple. Just chat to any of the hunters in here to help them out. " +
                "They're all following reports that I've split up amongst them.",
        )
        chatPlayer(quiz, "Reports?")
        chatNpc(
            neutral,
            "Indeed. There are all sorts of interesting creatures to hunt out there. Sometimes " +
                "we get reports of a particularly unique example. When that happens, one of the " +
                "hunters will set out to find it.",
        )
        chatNpc(
            neutral,
            "If you do decide to take a job from any of the hunters, they'll tell you what type " +
                "of creature they're looking into. Hunt that creature until you encounter the " +
                "unique specimen.",
        )
        chatNpc(
            neutral,
            "Once you've caught it, bring some proof back to the hunter that sent you out in the " +
                "first place. I'm sure they'll reward you for your time.",
        )
        chatPlayer(happy, "I see. Thanks for the information.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Orsaga
    private suspend fun Dialogue.orsaga() {
        chatNpc(happy, "Hi there, friend!")
        chatPlayer(quiz, "Hello! You're not one of the hunters too, are you?")
        chatNpc(
            happy,
            "Oh no, but as someone that makes small soft versions of creatures, this is the " +
                "place to be.",
        )
        chatNpc(
            happy,
            "These books and the hunters' reports all contain lovely details about my subject " +
                "matter. I might never leave!",
        )
        chatPlayer(
            happy,
            "I'd imagine this is the place to be for materials too. These hunters seem to find " +
                "everything!",
        )
        chatNpc(
            laugh,
            "The cat is a great way to stress test some of the stitching too. He loves having " +
                "something to play with!",
        )
    }
}

// https://oldschool.runescape.wiki/w/Hunter_Guild: 46 Hunter for the guild's amenities.
internal fun Dialogue.hasGuildHunterLevel(): Boolean = access.hasGuildHunterLevel()

internal fun ProtectedAccess.hasGuildHunterLevel(): Boolean =
    player.statBase("stat.hunter") >= GUILD_HUNTER_LEVEL

internal suspend fun Dialogue.refuseUnqualifiedHunter() {
    chatNpc(
        neutral,
        "Sorry, but I can only provide services to qualified members of the Hunter Guild.",
    )
    mesbox("You need 46 Hunter to access Hunter Guild amenities.")
}

private const val GUILD_HUNTER_LEVEL = 46
