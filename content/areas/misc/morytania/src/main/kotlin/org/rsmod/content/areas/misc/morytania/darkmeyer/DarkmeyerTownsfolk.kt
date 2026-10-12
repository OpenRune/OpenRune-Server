package org.rsmod.content.areas.misc.morytania.darkmeyer

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:vyre_disguise] the disguised dialogue before The Blood Moon Rises;
//  without it every vampyre sends the player back to the slave pens
class DarkmeyerTownsfolk @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.lady_shadum") { startDialogue(it.npc) { ladyShadum() } }
        onOpNpc1("npc.lord_jovkai") { startDialogue(it.npc) { lordJovkai() } }
        onOpNpc1("npc.lord_myrmel") { startDialogue(it.npc) { lordMyrmel() } }
        onOpNpc1("npc.mordan_nikazsi") { startDialogue(it.npc) { mordan() } }
        onOpNpc1("npc.desmodus_lasiurus") { startDialogue(it.npc) { desmodus() } }
        onOpNpc1("npc.myq5_prisoner_vis") { startDialogue(it.npc) { prisoner() } }
        onOpNpc1("npc.darkm_werewolf_1") { startDialogue(it.npc) { meatShopWerewolf() } }
        onOpNpc1("npc.darkm_werewolf_2") { startDialogue(it.npc) { cryptWerewolf() } }
    }

    private fun Dialogue.freedFromTithes(): Boolean =
        QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES)

    private suspend fun Dialogue.slavePens() {
        chatNpc(angry, "Human! Get back to the slave pens before I call the guards!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lady_Nadezhda_Shadum
    // TODO(content): [quest:sins_of_the_father] the disguised greeting needs the player's vampyre
    //  name
    private suspend fun Dialogue.ladyShadum() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatNpc(angry, "Who are you? Why are you in my home?")
        chatPlayer(neutral, "I'm ${player.displayName}. I was just looking around.")
        chatNpc(
            angry,
            "You humans are becoming too arrogant for your own good. You're lucky you have our " +
                "new leader to protect you, but that doesn't mean you can just go wandering " +
                "around my house. Now get out!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lord_Alexei_Jovkai
    private suspend fun Dialogue.lordJovkai() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatNpc(neutral, "Do you want something?")
        chatPlayer(neutral, "Not really.")
        chatNpc(
            neutral,
            "Then be on your way. I have a meeting with Lord Vitur soon and I don't need any " +
                "distractions.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lord_Mischa_Myrmel
    private suspend fun Dialogue.lordMyrmel() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatNpc(
            angry,
            "The tithes are growing weak. That werewolf needs to do a better job of keeping them " +
                "fed.",
        )
        chatPlayer(quiz, "What do you feed them?")
        chatNpc(
            neutral,
            "There's no shortage of corpses around here. The dead can still have some uses.",
        )
        chatPlayer(
            shocked,
            "You're feeding them corpses? No wonder they're becoming weak. That meat must have " +
                "all sorts of problems.",
        )
        chatNpc(
            angry,
            "What else would you have me feed them? If I give them nicer food, I'm wasting more " +
                "money than if I were to just kill them and buy some more.",
        )
        chatPlayer(worried, "I... I suppose.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mordan_Nikazsi
    private suspend fun Dialogue.mordan() {
        if (!freedFromTithes()) {
            return slavePens()
        }
        chatNpc(happy, "Fancy a leg?")
        chatPlayer(confused, "What?")
        chatNpc(
            neutral,
            "A leg. I'm not allowed to have them any more, so maybe you want one instead.",
        )
        if (!choice2("Go on then.", true, "I think I'll pass.", false)) {
            chatPlayer(neutral, "I think I'll pass.")
            chatNpc(neutral, "Suit yourself.")
            return
        }
        chatPlayer(neutral, "Go on then.")
        player.invAddOrDrop(objRepo, SEVERED_LEG)
        objbox(SEVERED_LEG, "The vampyre hands you a severed leg.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Desmodus_Lasiurus
    private suspend fun Dialogue.desmodus() {
        chatPlayer(quiz, "Can I go in the Arboretum?")
        chatNpc(angry, "Yes. Now leave me alone.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Prisoner_(Darkmeyer)
    private suspend fun Dialogue.prisoner() {
        chatNpc(happy, "Hello there!")
        chatPlayer(quiz, "How are you doing in there?")
        chatNpc(happy, "Oh I'm great. Lord Jovkai takes good care of us.")
        chatPlayer(quiz, "So you don't want to escape?")
        chatNpc(confused, "Why would I want to do that? I'm very happy as I am.")
        chatPlayer(shifty, "If you say so.")
    }

    private suspend fun Dialogue.werewolfSlave() {
        chatNpc(
            angry,
            "You shouldn't be here, slave. Go back to your master before you get yourself killed.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Werewolf_(Darkmeyer) (east of the meat shop)
    private suspend fun Dialogue.meatShopWerewolf() {
        if (!freedFromTithes()) {
            return werewolfSlave()
        }
        chatPlayer(happy, "Hey!")
        chatNpc(angry, "What?")
        chatPlayer(quiz, "What are you doing?")
        chatNpc(neutral, "Waiting.")
        chatPlayer(quiz, "For what?")
        chatNpc(angry, "For you to go away.")
        chatPlayer(sad, "Oh... okay.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Werewolf_(Darkmeyer) (The Crypt)
    private suspend fun Dialogue.cryptWerewolf() {
        if (!freedFromTithes()) {
            return werewolfSlave()
        }
        chatNpc(angry, "There's nothing good to drink here.")
        chatPlayer(quiz, "What's wrong with blood?")
        chatNpc(neutral, "Blood's only good if there's meat to go with it.")
        chatPlayer(quiz, "Why even come here if there's nothing for you to drink?")
        chatNpc(neutral, "I'm here on business.")
        chatPlayer(quiz, "What business?")
        chatNpc(
            angry,
            "My business is none of your business. Go poke your nose somewhere else.",
        )
    }

    private companion object {
        const val BLOOD_MOON_RISES = "quest_bloodmoonrises"
        const val SEVERED_LEG = "obj.severed_leg"
    }
}
