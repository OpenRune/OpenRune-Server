package org.rsmod.content.skills.runecrafting.essencepouch

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Npc

object DarkMageDialogue {
    suspend fun ProtectedAccess.talkToDarkMage(npc: Npc) {
        startDialogue(npc) {
            chatPlayer(neutral, "Hello there.")
            chatNpc(angry, "Quiet! You must not break my concentration!")
            mainMenu()
        }
    }

    suspend fun ProtectedAccess.repairPouchesDirectly(npc: Npc) {
        if (!EssencePouch.hasPouchesNeedingRepair(this, includeColossal = false)) {
            startDialogue(npc) {
                chatNpc(angry, "You don't seem to have any pouches in need of repair. Leave me alone!")
            }
            return
        }

        mes("The Dark mage repairs your essence pouches.")
        EssencePouch.repairPouches(this, includeColossal = false)
        startDialogue(npc) {
            chatNpc(angry, "There, I have repaired your pouches. Now leave me alone. I'm concentrating!")
        }
    }

    private suspend fun Dialogue.mainMenu() {
        when (
            choice4(
                "Why not?",
                1,
                "What are you doing here?",
                2,
                "I need your help with something.",
                3,
                "Sorry, I'll go.",
                4,
            )
        ) {
            1 -> explainConcentration()
            2 -> explainAbyssTask()
            3 -> helpMenu()
            4 -> dismissPlayer()
        }
    }

    private suspend fun Dialogue.explainConcentration() {
        chatPlayer(quiz, "Why not?")
        chatNpc(
            angry,
            "Well, if my concentration is broken while keeping this rift open, the results won't be pretty.",
        )
        chatPlayer(quiz, "In what way?")
        chatNpc(
            angry,
            "If we are lucky, the heads of anyone within the Abyss will suddenly explode, including us.",
        )
        chatPlayer(shocked, "Err... And if we're unlucky?")
        chatNpc(
            angry,
            "If we are unlucky, then the entire universe will begin to fold in upon itself, and all " +
                "reality as we know it will be annihilated in a single stroke.",
        )
        chatNpc(angry, "So leave me alone!")
        mainMenu()
    }

    private suspend fun Dialogue.explainAbyssTask() {
        chatPlayer(quiz, "What are you doing here?")
        chatNpc(
            neutral,
            "Do you mean what am I doing here in the Abyss? Or are you asking me what I consider my " +
                "ultimate role to be in this voyage that we call life?",
        )
        chatPlayer(quiz, "Err... The first one.")
        chatNpc(
            neutral,
            "By remaining here and holding this rift open, I am providing a permanent link between " +
                "normal space and this strange dimension.",
        )
        chatNpc(
            neutral,
            "As long as my spell remains in effect, we have the capability to teleport into the Abyss.",
        )
        chatNpc(angry, "Now leave me be! I can afford no distraction in my task!")
        mainMenu()
    }

    private suspend fun Dialogue.helpMenu() {
        chatPlayer(neutral, "I need your help with something.")
        chatNpc(neutral, "What? Oh... very well. What did you want?")
        when (
            choice4(
                "Can I have another Abyssal book?",
                1,
                "Can I have a new essence pouch?",
                2,
                "Can you repair my pouches?",
                3,
                "Actually, I don't need anything right now.",
                4,
            )
        ) {
            1 -> giveAbyssalBook()
            2 -> giveEssencePouch()
            3 -> repairPouchesFromDialogue()
            4 -> declineHelp()
        }
    }

    private suspend fun Dialogue.giveAbyssalBook() {
        chatPlayer(neutral, "Can I have another Abyssal book?")
        if (access.inv.freeSpace() < 1) {
            chatNpc(angry, "Don't waste my time if you don't have enough free space to take it.")
            return
        }

        if (access.invAdd(access.inv, "obj.rcu_instruction_book", 1).failure) {
            chatNpc(angry, "Don't waste my time if you don't have enough free space to take it.")
            return
        }

        chatNpc(neutral, "Here, take it. It is important to pool our research.")
        access.mes("You have been given a book.")
        helpMenu()
    }

    private suspend fun Dialogue.giveEssencePouch() {
        chatPlayer(neutral, "Can I have a new essence pouch?")
        if (EssencePouch.hasColossalPouch(access.player)) {
            chatNpc(
                angry,
                "You already have a Colossal Pouch. Are you aware of the dimensional turmoil you can " +
                    "cause by using too many pouches at the same time?",
            )
            chatNpc(angry, "Now stop wasting my time!")
            return
        }

        if (access.inv.freeSpace() < 1) {
            chatNpc(angry, "Don't waste my time if you don't have enough free space to take it.")
            return
        }

        if (access.invAdd(access.inv, EssencePouch.Tier.Small.intactItem, 1).failure) {
            chatNpc(angry, "Don't waste my time if you don't have enough free space to take it.")
            return
        }

        chatNpc(neutral, "Here. Be more careful with your belongings in future.")
        access.mes("You have been given a pouch.")
        helpMenu()
    }

    private suspend fun Dialogue.repairPouchesFromDialogue() {
        chatPlayer(neutral, "Can you repair my pouches?")
        if (!EssencePouch.hasPouchesNeedingRepair(access, includeColossal = false)) {
            chatNpc(angry, "You don't seem to have any pouches in need of repair. Leave me alone!")
            return
        }

        chatNpc(angry, "Fine. A simple transfiguration spell should resolve things for you.")
        EssencePouch.repairPouches(access, includeColossal = false)
        chatPlayer(happy, "Thanks.")
        chatNpc(angry, "Now can you leave me alone? I can't keep affording these distractions!")
        helpMenu()
    }

    private suspend fun Dialogue.declineHelp() {
        chatPlayer(neutral, "Actually, I don't need anything right now.")
        chatNpc(
            angry,
            "Then go away! Honestly, you have no idea of the pressure I am under. I can't afford any distractions!",
        )
        mainMenu()
    }

    private suspend fun Dialogue.dismissPlayer() {
        chatPlayer(neutral, "Sorry, I'll go.")
        chatNpc(
            angry,
            "Good. I'm attempting to subdue the elemental mechanisms of the universe to my will. " +
                "Inane chatter from random idiots is not helping me achieve this!",
        )
    }
}
