package org.rsmod.content.areas.city.lumbridge.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.countCheckTeleport by boolVarBit("varbit.count_check_stronghold_teleport")
private var Player.adventurePaths by intVarBit("varbit.adventurepath_player_participating")

/** The Doomsayer, Count Check, Nigel and Adventurer Jon, all north of Lumbridge Castle. */
class LumbridgeAdvisors : PluginScript() {
    override fun ScriptContext.startup() {
        // Adventurer Jon is only visible to players the varbit marks as taking part.
        onPlayerLogin {
            if (player.adventurePaths == ADVENTURE_PATHS_UNSET) {
                player.adventurePaths = ADVENTURE_PATHS_PARTICIPATING
            }
        }
        for (doomsayer in listOf("npc.cws_doomsayer", "npc.doomsayer_normal")) {
            onOpNpc1(doomsayer) { startDialogue(it.npc) { doomsayer() } }
        }
        onOpNpc1("npc.count_check") { startDialogue(it.npc) { countCheck() } }
        for (nigel in listOf("npc.deadman_nigel", "npc.deadman_nigel_regular")) {
            onOpNpc1(nigel) { startDialogue(it.npc) { nigel() } }
        }
        for (jon in listOf("npc.ap_guide_parent", "npc.ap_guide_active", "npc.ap_guide_opt_out")) {
            onOpNpc1(jon) { startDialogue(it.npc) { adventurerJon() } }
        }
    }

    private suspend fun Dialogue.doomsayer() {
        chatNpc(scared, "Dooooom!")
        chatPlayer(scared, "Where?")
        chatNpc(neutral, "All around us! I can feel it in the air, hear it on the wind, smell it... also in the air!")
        chatPlayer(scared, "Is there anything we can do about this doom?")
        chatNpc(
            neutral,
            "There is nothing you need to do my friend! I am the Doomsayer, although my real title " +
                "could be something like the Danger Tutor.",
        )
        chatPlayer(quiz, "Danger Tutor?")
        chatNpc(happy, "Yes! I roam the world sensing danger.")
        chatNpc(
            neutral,
            "If I find a dangerous area, then I put up warning signs that will tell you what is so " +
                "dangerous about that area.",
        )
        chatNpc(
            neutral,
            "If you see the signs often enough, then you can turn them off; by that time you likely " +
                "know what the area has in store for you.",
        )
        chatPlayer(quiz, "But what if I want to see the warnings again?")
        chatNpc(happy, "That's why I'm waiting here!")
        chatNpc(neutral, "If you want to see the warning messages again, I can turn them back on for you.")
        chatNpc(quiz, "Do you need to turn on any warnings right now?")
        if (menu("Yes, I do." to true, "Not right now." to false)) {
            chatPlayer(neutral, "Yes, I do.")
            return
        }
        chatPlayer(neutral, "Not right now.")
        chatNpc(neutral, "Ok, keep an eye out for the messages though!")
        chatPlayer(neutral, "I will.")
    }

    private val Dialogue.scared
        get() = mesanim("mesanim.scared")

    private suspend fun Dialogue.countCheck() {
        chatNpc(
            happy,
            "Ahahahaha! I am Count Check, the renowned security expert. Would you like me to check " +
                "your account to see if it is secure?",
        )
        while (true) {
            when (
                menu(
                    "Check my account, Count Check!" to 1,
                    "Can you give me any advice?" to 2,
                    "Where can I learn more about security?" to 3,
                    "Who are you?" to 4,
                    "I'll see you another time." to 5,
                )
            ) {
                1 -> {
                    chatPlayer(happy, "Check my account, Count Check!")
                    chatNpc(
                        neutral,
                        "You do not have a Bank PIN, so you fail my checks! Talk to a banker to set a PIN " +
                            "before we meet again.",
                    )
                }
                2 -> countAdvice()
                3 -> if (strongholdOffer()) return
                4 -> whoAreYou()
                else -> {
                    chatPlayer(bored, "I'll see you another time.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.countAdvice() {
        chatPlayer(quiz, "Can you give me any advice?")
        chatNpc(happy, "I can! Count with the Count as I count the steps to securing your account:")
        chatNpc(happy, "ONE... unique password!")
        chatNpc(
            neutral,
            "Never use the same password for different accounts, or on different websites. If it got " +
                "leaked, all of your accounts would become vulnerable at once!",
        )
        chatNpc(happy, "TWO... step verification for your email account!")
        chatNpc(
            neutral,
            "Good email providers let you set up two-step verification, maybe by generating security " +
                "codes on a second device, or by sending codes to your phone. Make sure you've enabled " +
                "this.",
        )
        chatNpc(neutral, "THREE... Um... Use a Jagex Account. I can see you already do this.")
        if (!menu("Carry on." to true, "What's that got to do with THREE?" to false)) {
            chatPlayer(quiz, "What's that got to do with THREE?")
            chatNpc(
                angry,
                "Look, I'd got something for ONE, TWO and FOUR, and I like counting, okay? Don't " +
                    "interrupt my flow!",
            )
            chatPlayer(neutral, "...")
        } else {
            chatPlayer(neutral, "Carry on.")
        }
        chatNpc(happy, "FOUR... digit Bank PIN!")
        chatNpc(neutral, "Next time you visit a bank, talk to a banker and ask to check your PIN settings.")
        chatNpc(laugh, "Muhahahahhaa! Such fun it is to count!")
    }

    /** Returns true when the player takes the teleport, which ends the conversation. */
    private suspend fun Dialogue.strongholdOffer(): Boolean {
        chatPlayer(quiz, "Where can I learn more about security?")
        if (player.countCheckTeleport) {
            chatNpc(
                neutral,
                "The Stronghold of Security, in Barbarian Village. There is much to learn there. But " +
                    "while you are here, may I check your account for you?",
            )
            return false
        }
        chatNpc(
            neutral,
            "The Stronghold of Security, in Barbarian Village. I see you have not visited it. Would " +
                "you like to? I can send you straight there - but only once.",
        )
        val teleport =
            menu("Yes" to true, "No" to false, title = "Teleport to the Stronghold of Security?")
        if (!teleport) {
            chatNpc(neutral, "So may I check your account for you?")
            return false
        }
        player.countCheckTeleport = true
        access.telejump(STRONGHOLD)
        return true
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            neutral,
            "As you may see, I am a vampyre, from Morytania. I left there when I heard the terrible " +
                "news and now I choose to spend my days warning others of the perils that lay in wait " +
                "for them.",
        )
        chatPlayer(quiz, "The perils?")
        chatNpc(
            neutral,
            "Yes, be warned! The travellers of these lands face danger from outside. Accounts can be " +
                "stolen, and when that happens one can lose everything one has worked for.",
        )
        chatNpc(
            neutral,
            "I've left my vampyric ways behind me, and now I spend my days letting everyone know of " +
                "this terrible danger.",
        )
        chatNpc(
            neutral,
            "In return for my dedication, I have been blessed with the ability to check if adventurers " +
                "are ready to defend themselves against these account thieves.",
        )
    }

    private suspend fun Dialogue.nigel() {
        chatPlayer(neutral, "Hey, who are you?")
        chatNpc(neutral, "I'm Nigel. It is my job to tell you about Deadman Mode.")
        while (true) {
            when (
                menu(
                    "I have a question about Deadman Mode." to 1,
                    "Can I purchase some Deadman rewards?" to 2,
                    "Goodbye." to 3,
                )
            ) {
                1 -> {
                    chatPlayer(neutral, "I have a question about Deadman Mode.")
                    chatNpc(quiz, "What would you like to know?")
                    deadmanQuestions()
                }
                2 -> chatNpc(neutral, "Unfortunately the store is not currently available.")
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.deadmanQuestions() {
        while (true) {
            val topic =
                menu(
                    "What is Deadman Mode?" to 1,
                    "How do I play Deadman Mode?" to 2,
                    "What happens when I die in Deadman Mode?" to 3,
                    "What happens when I kill a player in Deadman Mode?" to 4,
                    "More options..." to 5,
                )
            when (topic) {
                1 -> {
                    chatPlayer(quiz, "What is Deadman Mode?")
                    chatNpc(
                        neutral,
                        "Deadman mode is an incredibly dangerous survival game mode. Anyone can attack " +
                            "anyone, anywhere. Death comes at a huge cost and killing other players can be " +
                            "extremely lucrative.",
                    )
                    chatPlayer(quiz, "Lucrative, you say?")
                    chatNpc(
                        neutral,
                        "Killing a player will earn you all of the items they had on them, plus the 10 most " +
                            "valuable items from their bank. The unfortunate soul will also lose half of " +
                            "their experience in most skills, or a quarter if they were not skulled. " +
                            "Regardless, it's quite the set-back.",
                    )
                    chatPlayer(shocked, "Wow, that is brutal!")
                    chatNpc(neutral, "It is, but it is no simple task to kill someone in Deadman mode.")
                    chatNpc(
                        neutral,
                        "Very high level guards patrol every major city and bank. They will kill anyone who " +
                            "has a skull. Getting yourself a kill is only half the battle.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "How do I play Deadman Mode?")
                    chatNpc(
                        neutral,
                        "You can log into Deadman mode by selecting a Deadman mode world from the world list " +
                            "before you log in.",
                    )
                    chatPlayer(quiz, "So I don't need a new account to play Deadman mode?")
                    chatNpc(
                        neutral,
                        "Nope! You can use your existing account on a Deadman world, providing you have " +
                            "membership.",
                    )
                    chatPlayer(quiz, "Do my stats and items transfer over to Deadman mode?")
                    chatNpc(
                        neutral,
                        "No, they do not. Your Deadman mode progress is completely separate from normal Old " +
                            "School worlds.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "What happens when I die in Deadman Mode?")
                    chatNpc(
                        neutral,
                        "If you die to a player you will lose the 10 most valuable stacks of items from your " +
                            "bank, all items that you have on your person and half of the experience you " +
                            "have gained in any unprotected skills.",
                    )
                    chatNpc(
                        neutral,
                        "If you die to a monster when you have a PK skull you will lose the 10 most valuable " +
                            "stacks of items from your bank, all items that you have on your person and half " +
                            "of the experience you have gained in any unprotected skills.",
                    )
                    chatNpc(
                        neutral,
                        "If you die to a monster when you do not have a PK skull you will lose all of the " +
                            "items on your person except for the 3 most valuable. You will not lose " +
                            "experience or any items from your bank.",
                    )
                }
                4 -> {
                    chatPlayer(quiz, "What happens when I kill a player in Deadman Mode?")
                    chatNpc(
                        neutral,
                        "When you kill a player you will receive any items they have on their person and you " +
                            "will also be given a key. You can use this key on Deadman chests found in " +
                            "various banks across the map. Within the chest you will find the 10 most " +
                            "valuable stacks of items from the bank of the player you struck down. From here " +
                            "you will be able to choose the items you would like to keep.",
                    )
                }
                else -> if (deadmanMoreOptions()) return
            }
        }
    }

    /** Returns true when the player backs out to Nigel's main menu. */
    private suspend fun Dialogue.deadmanMoreOptions(): Boolean {
        while (true) {
            when (
                menu(
                    "How can I protect my items in Deadman Mode?" to 1,
                    "What are safe zones in Deadman Mode?" to 2,
                    "Previous options..." to 3,
                )
            ) {
                1 -> {
                    chatPlayer(quiz, "How can I protect my items in Deadman Mode?")
                    chatNpc(
                        neutral,
                        "You can protect up to 10 items by placing them in your safe deposit box. Items in " +
                            "your safe deposit box will not be lost to a player who kills you, unlike those " +
                            "found in your bank. Remember that you can only place 10 single items into your " +
                            "safe deposit box, no stacks.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What are safe zones in Deadman Mode?")
                    chatNpc(
                        neutral,
                        "Safe zones are areas patrolled by very high level guards which can be found in " +
                            "various places across the map. The guards watch out for any player with a PK " +
                            "skull and will attack them if they're spotted. It is still possible to be " +
                            "attacked inside safe zones but the person doing the attacking will not stand a " +
                            "chance of making it out alive.",
                    )
                }
                else -> return false
            }
        }
    }

    private suspend fun Dialogue.adventurerJon() {
        if (player.adventurePaths == ADVENTURE_PATHS_OPTED_OUT) {
            chatNpc(neutral, "You are currently not participating in Adventure Paths. Would you like to join back in?")
            if (!menu("Yes please." to true, "No thanks." to false)) {
                chatPlayer(neutral, "No thanks.")
                return
            }
            chatPlayer(neutral, "Yes please.")
            mesbox(
                "Opting into Adventure Paths will allow you to see the interface and you will be able to " +
                    "receive task and path notifications. You can talk to Adventurer Jon to opt back out of " +
                    "Adventure Paths"
            )
            if (menu("Yes" to true, "No" to false, title = "Opt into Adventure Paths?")) {
                player.adventurePaths = ADVENTURE_PATHS_PARTICIPATING
                mesbox("You have opted back into Adventure Paths.")
            }
            return
        }
        chatNpc(neutral, "Hello ${player.displayName}, what can I do for you?")
        while (true) {
            when (
                menu(
                    "What are Adventure Paths?" to 1,
                    "Do you have any Adventure Path starter kits for me?" to 2,
                    "I'd like to claim my Adventure Path rewards." to 3,
                    "Can you show me some Adventure Paths?" to 4,
                    "More options." to 5,
                )
            ) {
                1 -> {
                    chatPlayer(quiz, "What are Adventure Paths?")
                    chatNpc(
                        happy,
                        "A long time ago, I was a newbie just like you who didn't know what to do and I " +
                            "would be more than happy to pass on my experience.",
                    )
                    chatNpc(
                        happy,
                        "I've created some Adventure Paths which I recommend you to start off with. An " +
                            "Adventure Path is a list of tasks which you can complete in any order.",
                    )
                    chatNpc(
                        happy,
                        "To help you get started on your Adventure Path, I'll also give you a starter kit " +
                            "with some useful items.",
                    )
                    chatNpc(
                        happy,
                        "Once you've completed an Adventure Path task, come back to me and I'll give you a " +
                            "reward.",
                    )
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Do you have any Adventure Path starter kits for me?")
                    chatNpc(neutral, "You seem to have already claimed all of your starter kits.")
                }
                3 -> {
                    chatPlayer(neutral, "I'd like to claim my Adventure Path rewards.")
                    chatNpc(neutral, "I have no rewards to give you at the moment.")
                }
                4 -> {
                    chatPlayer(quiz, "Can you show me some Adventure Paths?")
                    chatNpc(happy, "Of course, here you go.")
                    return
                }
                else -> {
                    if (!menu("Disable Adventure Paths." to true, "Previous options." to false)) continue
                    mesbox(
                        "Opting out of Adventure Paths will hide the interface and you will no longer " +
                            "receive task and path notifications. You can talk to Adventurer Jon to opt back " +
                            "into Adventure Paths"
                    )
                    if (menu("Yes" to true, "No" to false, title = "Opt out of Adventure Paths?")) {
                        player.adventurePaths = ADVENTURE_PATHS_OPTED_OUT
                        mesbox("You have now opted out of Adventure Paths.")
                    }
                    return
                }
            }
        }
    }

    private companion object {
        const val ADVENTURE_PATHS_UNSET = 0
        const val ADVENTURE_PATHS_PARTICIPATING = 1
        const val ADVENTURE_PATHS_OPTED_OUT = 2

        val STRONGHOLD = CoordGrid(3081, 3421, 0)
    }
}
