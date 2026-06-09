package org.rsmod.content.skills.runecrafting.essencepouch

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.runecrafting.cordeliaPouchRepairUnlocked
import org.rsmod.game.entity.Npc

object CordeliaDialogue {
    suspend fun ProtectedAccess.talkToCordelia(npc: Npc) {
        startDialogue(npc) {
            chatPlayer(neutral, "I was wondering if you could repair my pouches.")
            if (player.cordeliaPouchRepairUnlocked == 1) {
                unlockedRepairMenu()
            } else {
                unlockRepairContact()
            }
        }
    }

    private suspend fun Dialogue.unlockRepairContact() {
        chatNpc(
            neutral,
            "I need ${25} abyssal pearls to unlock the " +
                "contact with the mage who knows how to repair a pouch. After the contact has been " +
                "unlocked, it will cost you ${1} abyssal " +
                "pearl each time to repair the pouches.",
        )

        if (access.inv.count("obj.abyssal_pearl") < 25) {
            chatPlayer(neutral, "I don't have enough abyssal pearls for that, I'm afraid.")
            return
        }

        when (
            choice2(
                "Pay ${25} abyssal pearls to unlock the pouch repair?",
                1,
                "Cancel",
                2,
            )
        ) {
            1 -> confirmUnlock()
            2 -> Unit
        }
    }

    private suspend fun Dialogue.confirmUnlock() {
        chatPlayer(neutral, "Yes, please.")
        if (
            access.invDel(
                access.inv,
                "obj.abyssal_pearl",
                25,
            ).failure
        ) {
            chatPlayer(neutral, "I don't have enough abyssal pearls for that, I'm afraid.")
            return
        }

        chatNpc(neutral, "Now bear with me whilst I contact the mage.")
        access.anim("seq.human_casting")
        chatNpc(neutral, "Hello, can you hear me?")
        chatDarkMage(angry, "Quiet! Why do you all try to break my concentration?")
        chatNpc(happy, "OK. The contact has been set up.")
        access.player.cordeliaPouchRepairUnlocked = 1
        chatNpc(
            neutral,
            "It will cost you ${1} abyssal pearl to repair the pouches.",
        )
        when (
            choice2(
                "Yes, that sounds fair.",
                1,
                "No thanks.",
                2,
            )
        ) {
            1 -> performPearlRepair()
            2 -> Unit
        }
    }

    private suspend fun Dialogue.unlockedRepairMenu() {
        chatNpc(
            neutral,
            "It will cost you ${1} abyssal pearl to repair the pouches.",
        )

        if (access.inv.count("obj.abyssal_pearl") < 1) {
            chatPlayer(neutral, "I don't have enough abyssal pearls for that, I'm afraid.")
            return
        }

        when (
            choice2(
                "Yes, that sounds fair.",
                1,
                "No thanks.",
                2,
            )
        ) {
            1 -> performPearlRepair()
            2 -> Unit
        }
    }

    private suspend fun Dialogue.performPearlRepair() {
        chatPlayer(neutral, "Yes, that sounds fair.")
        if (!EssencePouch.hasPouchesNeedingRepair(access, includeColossal = true)) {
            chatNpc(neutral, "You don't seem to have any pouches in need of repair.")
            return
        }

        if (
            access.invDel(
                access.inv,
                "obj.abyssal_pearl",
                1,
            ).failure
        ) {
            chatPlayer(neutral, "I don't have enough abyssal pearls for that, I'm afraid.")
            return
        }

        chatNpc(neutral, "Bear with me whilst I make the contact for repairing the pouches.")
        access.anim("seq.human_casting")
        chatNpc(neutral, "Hi, it's me again.")
        chatDarkMage(silent, "...")
        chatNpc(neutral, "Hello, I know you can hear me.")
        chatDarkMage(silent, "...")
        chatNpc(neutral, "Ding Dong, Ding Dong. You can't keep me waiting.")
        chatDarkMage(angry, "Fine. What do you want?")
        chatNpc(neutral, "I got someone here in need of a pouch repair.")
        chatDarkMage(neutral, "OK...It's done.")
        EssencePouch.repairPouches(access, includeColossal = true)
        chatNpc(happy, "Your pouches have been repaired.")
    }

    private suspend fun Dialogue.chatDarkMage(mesanim: MesAnimType, text: String) {
        chatNpcSpecific(
            title = "Dark Mage",
            type = "npc.rcu_zammy_mage2",
            mesanim = mesanim,
            text = text,
        )
    }
}
