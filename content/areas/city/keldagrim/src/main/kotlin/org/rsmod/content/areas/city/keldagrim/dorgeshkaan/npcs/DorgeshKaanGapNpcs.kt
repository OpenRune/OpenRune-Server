package org.rsmod.content.areas.city.keldagrim.dorgeshkaan.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class DorgeshKaanGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(MOVARIO) { startDialogue(it.npc) { movarioAndDarve() } }
        onOpNpc1(DARVE) { startDialogue(it.npc) { movarioAndDarve() } }
        onOpNpc1("npc.dorgesh_male_engineer") { startDialogue(it.npc) { turgall() } }
        onOpNpc1("npc.slice_cave_goblin_inspector") { startDialogue(it.npc) { ticketGoblin() } }
        onOpNpc1("npc.slice_goblin_archaeologist") { startDialogue(it.npc) { tegdak() } }
    }

    private suspend fun Dialogue.movario(mesanim: MesAnimType, text: String) {
        chatNpcSpecific("Movario", MOVARIO, mesanim, text)
    }

    private suspend fun Dialogue.darve(mesanim: MesAnimType, text: String) {
        chatNpcSpecific("Darve", DARVE, mesanim, text)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Movario
    // https://oldschool.runescape.wiki/w/Transcript:Darve
    private suspend fun Dialogue.movarioAndDarve() {
        darve(quiz, "What's the device saying now?")
        movario(confused, "I don't know. It's giving strange readings...")
        movario(confused, "I'm not even sure it's working any more.")
        darve(
            angry,
            "This is a wild goose chase! We trek halfway across the world and there's nothing " +
                "down here but this bunch of useless goblins!",
        )
        movario(
            neutral,
            "I am sure my triangulation calculations indicated that the source of power was in " +
                "this rough area...",
        )
        movario(
            neutral,
            "I find it encouraging that it's pointed to somewhere where there actually are caves.",
        )
        darve(
            angry,
            "This part of the world is riddled with caves! It's hard not to hit a cave around " +
                "here! Are you sure you're not just picking up these goblin lanterns or " +
                "something stupid?",
        )
        movario(
            angry,
            "You should have more faith in my skills as a mage! I've been working on this for " +
                "years-",
        )
        darve(
            angry,
            "Gah! We have an eavesdropper! Buzz off, adventurer - go talk to the goblins or " +
                "something.",
        )
        val option =
            if (PENDANT_OF_LUCIEN in player.worn) {
                choice3(
                    "Okay, I won't bother you.",
                    1,
                    "What are you looking for?",
                    2,
                    "Hey, I'm one of you guys! See this pendant?",
                    3,
                )
            } else {
                choice2("Okay, I won't bother you.", 1, "What are you looking for?", 2)
            }
        when (option) {
            1 -> {
                chatPlayer(neutral, "Okay, I won't bother you.")
                movario(
                    happy,
                    "That's what I like to see: a polite adventurer! Now, where was I...",
                )
            }
            2 -> {
                chatPlayer(quiz, "What are you looking for?")
                darve(angry, "None of your business!")
            }
            3 -> pendant()
        }
    }

    private suspend fun Dialogue.pendant() {
        chatPlayer(happy, "Hey, I'm one of you guys! See this pendant?")
        darve(
            angry,
            "Where did you come from? Who sent you? I'll have you know I'm the mage's bodyguard " +
                "for this job. We don't need anyone else!",
        )
        val option =
            if (RING_OF_CHAROS_A in player.worn) {
                choice3(
                    "No one sent me, I'm just nosy.",
                    1,
                    "Lucien sent me.",
                    2,
                    "(Charm) I'm sure you're a much better bodyguard than me...",
                    3,
                )
            } else {
                choice2("No one sent me, I'm just nosy.", 1, "Lucien sent me.", 2)
            }
        when (option) {
            1 -> {
                chatPlayer(neutral, "No one sent me, I'm just nosy.")
                darve(angry, "Well stick your nose somewhere else!")
                movario(
                    neutral,
                    "I'm sorry, but pendant or no pendant, this is a secret. I'm sure you " +
                        "understand.",
                )
            }
            2 -> {
                chatPlayer(neutral, "Lucien sent me.")
                movario(angry, "He did not! If he had, I would be aware of it!")
                darve(angry, "Don't try to lie to us! Now go away!")
            }
            3 -> charm()
        }
    }

    private suspend fun Dialogue.charm() {
        chatPlayer(
            happy,
            "I'm sure you're a much better bodyguard than me, but perhaps I can help in some " +
                "non-bodyguarding way?",
        )
        movario(
            neutral,
            "Well, we don't know how big this thing is. If we find it, I suppose we might need " +
                "someone else to help us carry it.",
        )
        chatPlayer(quiz, "So, what are we looking for?")
        movario(angry, "You're not very well briefed, are you?")
        darve(confused, "To be honest I'm still a bit hazy on that point too.")
        movario(sad, "*sigh*")
        movario(
            neutral,
            "Lucien has asked us to search for artefacts that give off a very specific type of " +
                "energy. After many years of research, I think I'm finally drawing near.",
        )
        darve(neutral, "You don't sound very sure.")
        movario(angry, "Bah! My detection device is totally shot. I don't understand it.")
        movario(
            neutral,
            "I wouldn't hang around for now, adventurer. It could be a long while before I can " +
                "get a clear reading.",
        )
        chatPlayer(neutral, "Okay, I'll be on my way.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Turgall
    // TODO(content): [system:dorgesh_kaan_power_station] the parts Turgall asks for, hand-ins and
    // the Agility/Ranged tips
    private suspend fun Dialogue.turgall() {
        chatNpc(worried, "Oh! Hello, surface-dweller. Do you think you could help me?")
        val option =
            choice4(
                "What help?",
                1,
                "What is this place?",
                2,
                "Why are there cables all over the cavern?",
                3,
                "Goodbye.",
                4,
            )
        when (option) {
            1 -> turgallHelp()
            2 -> turgallPowerStation()
            3 -> turgallCables()
            4 -> turgallGoodbye()
        }
    }

    private suspend fun Dialogue.turgallHelp() {
        chatPlayer(quiz, "What help?")
        chatNpc(
            worried,
            "This power station keeps breaking. I can fix it easily with parts from the old " +
                "power generator on the other side of the cavern, but that's not easy to get to.",
        )
        chatNpc(quiz, "Do you think you could get something from there for me?")
        if (choice2("Okay", true, "Get it yourself!", false)) {
            chatPlayer(happy, "Okay!")
        } else {
            chatPlayer(angry, "Get it youself!")
            chatNpc(sad, "Alright, I'm sure someone else will help me.")
        }
    }

    private suspend fun Dialogue.turgallPowerStation() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            neutral,
            "Oh, this is, um, the power station. It converts heat from the lava into magical " +
                "energy, which powers the lights in the city.",
        )
        var asked = 0
        while (true) {
            val options =
                listOf(
                    "How does it work?" to 1,
                    "Why is it outside the city?" to 2,
                    "Why are there cables all over the cavern?" to 3,
                    "Goodbye." to 4,
                ).filter { it.second != asked }
            val option =
                if (options.size == 4) {
                    choice4(
                        options[0].first,
                        options[0].second,
                        options[1].first,
                        options[1].second,
                        options[2].first,
                        options[2].second,
                        options[3].first,
                        options[3].second,
                    )
                } else {
                    choice3(
                        options[0].first,
                        options[0].second,
                        options[1].first,
                        options[1].second,
                        options[2].first,
                        options[2].second,
                    )
                }
            asked = option
            when (option) {
                1 -> {
                    chatPlayer(quiz, "How does it work?")
                    chatNpc(happy, "By magic!")
                    chatNpc(
                        neutral,
                        "The lava heats water into steam, which drives a turbine around. The " +
                            "turbine contains magical stones.",
                    )
                    chatNpc(
                        neutral,
                        "As the magical stones rotate they generate a magical current in the " +
                            "conductor cables, and that powers the light!",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Why is it outside the city?")
                    chatNpc(
                        neutral,
                        "We're hardly going to let flowing lava inside the city, are we?",
                    )
                    chatNpc(
                        neutral,
                        "But don't worry, it's quite safe. The creatures on the cavern floor " +
                            "can't get up here.",
                    )
                }
                3 -> {
                    turgallCables()
                    return
                }
                4 -> {
                    turgallGoodbye()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.turgallCables() {
        chatPlayer(quiz, "Why are there cables all over the cavern?")
        chatNpc(neutral, "Oh...they deliver power from the old power station.")
        chatNpc(
            sad,
            "There was an accident with that power station and it stopped working, so we set " +
                "this one up instead.",
        )
        if (choice2("What is this place?", true, "Goodbye.", false)) {
            turgallPowerStation()
        } else {
            turgallGoodbye()
        }
    }

    private suspend fun Dialogue.turgallGoodbye() {
        chatPlayer(neutral, "Goodbye.")
        chatNpc(neutral, "Oh, er, bye.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ticket_goblin
    // TODO(content): [quest:another_slice_of_ham] the train opens with the quest
    private suspend fun Dialogue.ticketGoblin() {
        chatPlayer(quiz, "Can I buy a ticket?")
        chatNpc(
            happy,
            "You don't need to buy a ticket! You're ${player.displayName}! Without you, this " +
                "train wouldn't exist!",
        )
        chatNpc(happy, "Just board the train and wait for it to travel to Keldagrim.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tegdak
    // TODO(content): [quest:another_slice_of_ham] Tegdak stands here after the quest
    // TODO(content): [system:lost_item_reclaim] "I lost the ancient goblin mace" (1,000 coins);
    // until then the other option, "What have you learned about the artefacts we dug up?", is
    // answered straight away
    private suspend fun Dialogue.tegdak() {
        chatNpc(happy, "Hello ${player.displayName}! Thanks again for your help with the dig.")
        when (access.random.of(5)) {
            0 -> {
                chatNpc(
                    neutral,
                    "Bartak used the design of the shield you extracted to recreate a shield " +
                        "like it. Even though the original shield was poorly made, the design " +
                        "itself was a good one.",
                )
                chatNpc(
                    neutral,
                    "Perhaps there is some important knowledge that we have lost since the " +
                        "early, more war-like days of Dorgesh-Kaan.",
                )
            }
            1 -> {
                chatNpc(
                    happy,
                    "The helmet you helped to unearth has come in useful. I used it as a visual " +
                        "aid when I went to talk to the children in the nursery about our history.",
                )
                chatNpc(
                    laugh,
                    "It's amazing that the ancient goblins' eyes were even smaller than a child's!",
                )
            }
            2 -> {
                chatNpc(
                    neutral,
                    "Oldak examined the axe blade you dug up and found that it had a faint " +
                        "magical aura. It looks like the mace wasn't the only magical weapon our " +
                        "ancestors brought from the surface. ",
                )
                chatNpc(
                    neutral,
                    "In fact, it could be that a lot of their equipment was magical. This means " +
                        "that either the ancient goblins were skilled magical craftsmen, or some " +
                        "other power was supplying them with magical equipment.",
                )
            }
            3 -> {
                chatNpc(
                    neutral,
                    "We used the armour fragment you unearthed to reconstruct the shape of the " +
                        "original armour, and with it the shape of the goblin who wore the armour.",
                )
                chatNpc(
                    neutral,
                    "This told us a lot about how the goblin body shape changed from its surface " +
                        "form to that of the cave goblins.",
                )
            }
            else -> {
                chatNpc(
                    neutral,
                    "When we examined the sword you dug up, we found that the blade is covered " +
                        "in tiny symbols.",
                )
                chatNpc(
                    neutral,
                    "The scribe thinks he has enough of them to begin understanding the ancient " +
                        "goblin language, but it will take a while for him to decipher them.",
                )
            }
        }
    }

    private companion object {
        const val MOVARIO = "npc.dorgesh_dark_wizard_there"
        const val DARVE = "npc.dorgesh_khazard_bodyguard_there"
        const val PENDANT_OF_LUCIEN = "obj.ikov_pendantoflucien"
        const val RING_OF_CHAROS_A = "obj.ring_of_charos_unlocked"
    }
}
