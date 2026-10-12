package org.rsmod.content.areas.city.canifis.burghderott

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:In_Aid_of_the_Myreque (speaking to citizens)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:in_aid_of_the_myreque] Florin at the gate, "Are there any 'out of the
//  way' places here?", the rebuilding stages and the after-quest "hero" dialogue
// TODO(content): [quest:in_aid_of_the_myreque] Aurel's Trade op and Cornelius's bank
class BurghDeRottCitizens : PluginScript() {
    override fun ScriptContext.startup() {
        for (citizen in SURVIVORS) {
            onOpNpc1(citizen) { startDialogue(it.npc) { citizen { survivorJob() } } }
        }
        for (citizen in RAT_HUNTERS) {
            onOpNpc1(citizen) { startDialogue(it.npc) { citizen { ratHunterJob() } } }
        }
        for (citizen in SITTERS) {
            onOpNpc1(citizen) {
                startDialogue(it.npc) {
                    citizen {
                        chatNpc(sad, "I'm ${npcVisType.name}. I'm just trying to stay warm.")
                    }
                }
            }
        }
        onOpNpc1("npc.burgh_vilager_leader") {
            startDialogue(it.npc) {
                citizen { chatNpc(neutral, "I'm Elisabeta, the village leader.") }
            }
        }
        onOpNpc1("npc.burgh_farmer") {
            startDialogue(it.npc) {
                citizen {
                    chatNpc(sad, "I'm Teodor. I'm a farmer. Not that there's much to grow here.")
                }
            }
        }
        onOpNpc1("npc.burgh_general_store_owner") { startDialogue(it.npc) { citizen { aurel() } } }
        onOpNpc1("npc.burgh_potential_bank_teller") {
            startDialogue(it.npc) {
                citizen { chatNpc(neutral, "I'm Cornelius. I don't really do anything here.") }
            }
        }
        onOpNpc1("npc.burgh_bed_man_wife") {
            startDialogue(it.npc) {
                citizen {
                    chatNpc(
                        worried,
                        "I'm Gabriela. I'm looking after my husband who isn't feeling too well!",
                    )
                }
            }
        }
        for (child in CHILDREN) {
            onOpNpc1(child) {
                startDialogue(it.npc) { chatNpc(worried, "Sorry. I shouldn't talk to strangers.") }
            }
        }
        onOpNpc1("npc.burgh_bed_man") {
            startDialogue(it.npc) { mesbox("The man is too ill to talk.") }
        }
    }

    private suspend fun Dialogue.citizen(job: suspend Dialogue.() -> Unit) {
        chatNpc(neutral, "Hey there. How's it going?")
        chatPlayer(neutral, "Not so bad.")
        while (true) {
            val option =
                choice4(
                    "How come you have no food?",
                    1,
                    "What is this place?",
                    2,
                    "What do you do here?",
                    3,
                    "Okay, thanks.",
                    4,
                )
            when (option) {
                1 -> noFood()
                2 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        neutral,
                        "This is our town! We's live here. It's called Burgh de Rott. It aint " +
                            "much to look at, but it's our home.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "What do you do here?")
                    job()
                }
                4 -> {
                    chatPlayer(neutral, "Okay, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.noFood() {
        chatPlayer(quiz, "How come you have no food?")
        when (access.random.of(3)) {
            0 ->
                chatNpc(
                    sad,
                    "Well obviously we just don't seem to get out to the shops like we used to!",
                )
            1 -> {
                chatNpc(
                    sad,
                    "We can only stay in the village and there's precious little food in here, " +
                        "just rats to live on! Not very appetising.",
                )
                chatNpc(
                    sad,
                    "There's also nowhere to store any food around here, the town is incredibly " +
                        "run down.",
                )
            }
            else -> {
                chatNpc(
                    worried,
                    "We dare not leave the town for fear that the vampyres might spot us and " +
                        "force us to pay a blood tithe.",
                )
                chatNpc(
                    worried,
                    "We're all very weak and would most likely die if they took more blood from " +
                        "us.",
                )
            }
        }
    }

    private suspend fun Dialogue.survivorJob() {
        val name = npcVisType.name
        when (access.random.of(3)) {
            0 ->
                chatNpc(
                    sad,
                    "My name's $name. I do a bit of this, a bit of that. Just try to survive " +
                        "really, there's nothing much else we can do.",
                )
            1 ->
                chatNpc(
                    neutral,
                    "Oh, my name's $name. I just help out around here, you know, gathering food " +
                        "and so on.",
                )
            else ->
                chatNpc(
                    sad,
                    "Not that it makes much difference but my name's $name. Like all the other " +
                        "survivors here, I just try to make do and get through each day at a time.",
                )
        }
    }

    private suspend fun Dialogue.ratHunterJob() {
        val name = npcVisType.name
        when (access.random.of(3)) {
            0 ->
                chatNpc(
                    neutral,
                    "Oh hi. I'm $name. I hunt rats mostly. Speaking of which, this rat tastes " +
                        "awful, but it's the only food we have!",
                )
            1 ->
                chatNpc(
                    happy,
                    "Huh? I'm $name. Ravaging after food is what I mostly do. Ocasionally I'll " +
                        "happen across a rat, and that's a good day, just like today!",
                )
            else ->
                chatNpc(
                    happy,
                    "Oh, hello. $name is the name and I help to keep the village clear of rat " +
                        "infestations. Clearly I love my work, as I bring it home with me... On " +
                        "a stick!",
                )
        }
    }

    private suspend fun Dialogue.aurel() {
        chatNpc(neutral, "I'm Aurel, the village store owner.")
        chatPlayer(quiz, "Can you open the shop for me please?")
        chatNpc(sad, "I'm sorry my friend, but I cannot open a store which has no stock.")
    }

    private companion object {
        val SURVIVORS =
            listOf("npc.burgh_villager_0") + (1..8).map { "npc.burgh_vilager_$it" }
        val RAT_HUNTERS = (1..3).map { "npc.burgh_vilager_rat_$it" }
        val SITTERS = (1..4).map { "npc.burgh_vilager_sit$it" }
        val CHILDREN = (1..4).map { "npc.burgh_villager_child$it" }
    }
}
