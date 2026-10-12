package org.rsmod.content.areas.city.burthorpe.npcs.warriorsguild

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.baseAttackLvl
import org.rsmod.api.player.stat.baseStrengthLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:combat_achievements] "I think I've completed a combat tier!" rewards,
//  hilt reclaims and Slayer helmet customisation
// TODO(content): [system:treasure_trails] master clue torn scroll part
class GuildGuards : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.warguild_ghommal_npc") { startDialogue(it.npc) { ghommal() } }
        onOpNpc1("npc.warguild_laidee") { startDialogue(it.npc) { laidee() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ghommal
    private suspend fun Dialogue.ghommal() {
        chatNpc(neutral, "Can I help you?")
        val option =
            choice3(
                "What are you doing out here?",
                1,
                "I'm here to talk about Combat Achievements.",
                2,
                "No, I'm alright thanks.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What are you doing out here?")
                chatNpc(
                    neutral,
                    "This here is the Warrior's Guild, a place where the strong may train. I'm " +
                        "the one who decides if you are allowed in or not.",
                )
                chatPlayer(quiz, "Am I qualified to enter?")
                if (canEnterGuild()) {
                    chatNpc(happy, "Yes you are. Welcome to the Warrior's Guild!")
                    chatPlayer(happy, "Thank you!")
                    return
                }
                chatNpc(neutral, "You may not enter the Warrior's Guild as you are right now.")
                chatPlayer(angry, "What? But I'm a warrior!")
                chatNpc(neutral, "While you may consider yourself a warrior, I do not.")
                chatPlayer(
                    shifty,
                    "Go on, let me in, you know you want to. I could... make it worth your " +
                        "while...",
                )
                chatNpc(angry, "Go away and train small one. I do not take bribes.")
                chatPlayer(quiz, "Why not?")
                chatNpc(
                    neutral,
                    "The Warrior's Guild has a code of honour, a code that I respect. Move along.",
                )
            }
            2 -> combatAchievements()
            3 -> chatPlayer(neutral, "No, I'm alright thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Laidee_Gnonock
    private suspend fun Dialogue.laidee() {
        chatNpc(neutral, "Hello, I'm Laidee. Welcome to the Warriors' Guild.")
        val option =
            choice3(
                "What are you doing out here?",
                1,
                "I'm here to talk about Combat Achievements.",
                2,
                "I'd better get going.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What are you doing out here?")
                chatNpc(
                    neutral,
                    "I'm here to guard the Warrior's Guild and determine who's qualified to enter.",
                )
                chatPlayer(quiz, "So you're Ghommal's replacement?")
                chatNpc(
                    sad,
                    "That I am. Ghommal and I used to adventure together before we joined the " +
                        "Guild. It felt only right that I take over from him. It is... strange " +
                        "not having him around.",
                )
                chatPlayer(sad, "I'm sorry for your loss. He was a great warrior.")
                chatNpc(sad, "And an even greater friend.")
            }
            2 -> combatAchievements()
            3 -> chatPlayer(neutral, "I'd better get going.")
        }
    }

    // TODO(content): [verify:in_game] the three info boxes show the summary panel and Combat
    //  Achievements sprites; these send plain message boxes
    private suspend fun Dialogue.combatAchievements() {
        chatPlayer(neutral, "I'm here to talk about Combat Achievements.")
        if (!choice2("Could you explain what they are?", true, "I have to go.", false)) {
            chatPlayer(neutral, "I have to go.")
            return
        }
        chatPlayer(quiz, "Could you explain what they are?")
        chatNpc(
            neutral,
            "Combat Achievements are a set of tasks for all adventurers to try and accomplish. " +
                "They range from simple missions to kill various creatures, to challenges that " +
                "will push you to your limits.",
        )
        chatNpc(
            neutral,
            "The purpose of these tasks is for you to grow and improve, experience all that " +
                "Gielinor has to offer, and become stronger in the process.",
        )
        chatNpc(quiz, "Is this something that sounds interesting to you?")
        val tellMe =
            choice2(
                "Yes, tell me about Combat Achievements.",
                true,
                "Not at the moment, no.",
                false,
            )
        if (!tellMe) {
            chatPlayer(neutral, "Not at the moment, no.")
            return
        }
        chatPlayer(happy, "Yes, tell me about Combat Achievements.")
        chatNpc(
            neutral,
            "Combat Achievements are broken down into six tiers: Easy, Medium, Hard, Elite, " +
                "Master and Grandmaster.",
        )
        chatNpc(
            neutral,
            "As you complete tasks, you'll earn points. Tasks in higher tiers give more points " +
                "than easier tasks. Your rank depends on how many points you've earned.",
        )
        chatNpc(
            neutral,
            "Whenever new tasks are added to the list, there are more points available to earn, " +
                "so the threshold needed for each rank will increase. Keep completing tasks to " +
                "maintain your rank!",
        )
        mesbox(
            "This is the icon for the summary panel. Within it there is a section for Combat " +
                "Achievements."
        )
        mesbox(
            "Clicking on the section will open the Combat Achievements interface, where you can " +
                "browse tasks, bosses and rewards."
        )
        mesbox(
            "Some tasks will require numerous criteria to be completed, so make sure you read " +
                "them carefully."
        )
        chatNpc(
            neutral,
            "I'd recommend you go kill some monsters. Come back to me when you've completed a " +
                "tier or two. I'll reward you for your efforts.",
        )
        chatPlayer(happy, "Thanks a lot, I'll get going.")
    }

    // https://oldschool.runescape.wiki/w/Warriors%27_Guild
    private fun Dialogue.canEnterGuild(): Boolean {
        val attack = player.baseAttackLvl
        val strength = player.baseStrengthLvl
        return attack + strength >= ENTRY_TOTAL || attack >= MAX_LEVEL || strength >= MAX_LEVEL
    }

    private companion object {
        const val ENTRY_TOTAL = 130
        const val MAX_LEVEL = 99
    }
}
