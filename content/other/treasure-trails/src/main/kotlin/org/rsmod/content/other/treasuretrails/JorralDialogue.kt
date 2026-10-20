package org.rsmod.content.other.treasuretrails

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc

object JorralDialogue {
    private const val MAKING_HISTORY = "quest_makinghistory"

    suspend fun ProtectedAccess.masterDevice(npc: Npc) {
        val device = TrailTiers.deviceObj(TrailTiers.MASTER)
        if (!QuestRequirements.hasCompleted(player, MAKING_HISTORY)) {
            startDialogue(npc) {
                chatPlayer(neutral, "Hello there, I have this master clue he...")
                chatNpc(neutral, "Stop! I can't help you unless you help me out first.")
                chatPlayer(neutral, "Okay then, moving on...")
            }
            return
        }
        if (inv.count(device) > 0) {
            startDialogue(npc) {
                chatPlayer(
                    neutral,
                    "Hey Jorral, I have one of those clue scrolls again, do you have any advice?",
                )
                chatNpc(
                    neutral,
                    "That strange device will guide you in the same way the key did. Feel it, " +
                        "but be careful as it is very powerful. Good luck.",
                )
            }
            return
        }
        startDialogue(npc) {
            chatPlayer(neutral, "Hi Jorral, I have come to ask you to help with something.")
            chatNpc(neutral, "What can I do for you ${player.displayName}?")
            mesbox("You show Jorral your clue scroll.")
            chatNpc(neutral, "Ah! Remember that enchanted key you had to use previously?")
            if (choice2("Yes, I do.", true, "No, I don't.", false)) {
                chatPlayer(neutral, "Yes, I do.")
            } else {
                chatPlayer(neutral, "No, I don't.")
                chatNpc(
                    neutral,
                    "Ah, well the enchanted key can lead you to treasure. The magical nature of " +
                        "the key heats it up or cools it down, depending on your proximity to " +
                        "the treasure.",
                )
            }
            chatNpc(
                neutral,
                "Well this should function much the same, the deeper magic of the key itself " +
                    "is stored within this strange device.",
            )
            chatNpc(neutral, "You should feel it, and it will guide you to where you need to go.")
        }
        if (invAdd(inv, device, 1).failure) {
            startDialogue(npc) {
                chatNpc(neutral, "You should make some room for it before I can give you it.")
            }
            return
        }
        startDialogue(npc) {
            objbox(device, "Jorral hands you the strange device.")
            chatNpc(
                neutral,
                "Be careful with it, the raw magic can damage you unexpectedly when you feel it.",
            )
        }
    }
}
