package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KeldagrimCitizens : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.aluft_gnome_inventor") { startDialogue(it.npc) { manglethorp() } }
        onOpNpc1("npc.dwarf_city_drunken_dwarf") { startDialogue(it.npc) { drunkenDwarf() } }
        onOpNpc1("npc.dwarfrock_engineer1") { startDialogue(it.npc) { dwarvenEngineer() } }
        onOpNpc1("npc.vc_the_beast") { startDialogue(it.npc) { theBeast() } }
        onOpNpc1("npc.vc_smokin_joe") { startDialogue(it.npc) { smokinJoe() } }
        onOpNpc1("npc.dwarf_city_boatman_city") { startDialogue(it.npc) { cityBoatman() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Professor_Manglethorp
    // TODO(content): [system:gnome_restaurant] delivery dialogue
    private suspend fun Dialogue.manglethorp() {
        val title = if (access.isBodyTypeA()) "man" else "woman"
        chatNpc(
            neutral,
            "Fascinating, fascinating. It's remarkable what these young dwarves have managed " +
                "to create. Oh, hello young $title.",
        )
        chatPlayer(quiz, "Hello, what's a gnome like you doing in a place like this?")
        chatNpc(
            happy,
            "Ah, well I have been sent by King to talk with the dwarves here and find out more " +
                "about their recent advances in technology.",
        )
        chatPlayer(quiz, "Found anything interesting?")
        chatNpc(
            happy,
            "The rideable carts are a very handy way to get to mining sites, and the Blast " +
                "Furnace is a magnificent beast of a machine.",
        )
        chatPlayer(happy, "I'll have a look, thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Drunken_Dwarf_(Keldagrim)
    // TODO(content): [system:met_flags] later visits open with "What are you doing here anyway?"
    // TODO(content): [system:treasure_trails] hard clue puzzle box dialogue
    private suspend fun Dialogue.drunkenDwarf() {
        chatPlayer(quiz, "Hello there! Are you alright?")
        chatNpc(drunk, "Of courshe! Why why why *hic* why shouldn't I be?")
        chatPlayer(
            confused,
            "Hey, you look vaguely familiar... haven't I seen you somewhere before?",
        )
        chatNpc(drunk, "That'sh not likely... I've been here for eh...")
        chatNpc(drunk, "I've been here for...")
        chatNpc(drunk, "Yearsh!")
        chatNpc(
            drunk,
            "I've been looking after thish houshe for my... my coushin? Or wash it my brother? " +
                "A family member anyway.",
        )
        chatPlayer(quiz, "And where is this... family member of yours?")
        chatNpc(
            drunk,
            "All over, all over... he shaish he went off to wander all over Gielinor. Don't " +
                "remember why.",
        )
        chatPlayer(neutral, "Hmm... I wonder, I may have met him...")
        chatNpc(drunk, "He comesh back sometimesh... saysh he needsh more kebabsh.")
        chatNpc(
            drunk,
            "Or ish that jusht a dream? Sometimesh I dream kebabsh invade Keldi... Keldu... " +
                "Keldashomething.",
        )
        chatNpc(
            drunk,
            "When you eatsh them, they take over your mind, they do! An army of mindlesh kebab " +
                "eating dwarvesh!",
        )
        chatPlayer(worried, "Er, yes, well, I think I should be off now.")
        chatNpc(drunk, "THE KEBABSH WILL COME FOR YOU!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dwarven_Engineer
    private suspend fun Dialogue.dwarvenEngineer() {
        chatNpc(angry, "I wish you'd just leave me alone.")
        chatPlayer(quiz, "Why, what's wrong?")
        chatNpc(sad, "I was a great engineer once. Still am actually.")
        chatNpc(sad, "They just don't let me do the work I do best anymore.")
        chatPlayer(quiz, "What do you do then?")
        chatNpc(sad, "I'm an assistant in this shop. I get all the lousy jobs too.")
        chatPlayer(sad, "Well, I'm sorry to hear that.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Beast
    private suspend fun Dialogue.theBeast() {
        chatPlayer(quiz, "Hello, The Beast! Why do they call you that?")
        chatNpc(neutral, "...")
        chatPlayer(worried, "Oh, I see.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Smokin%27_Joe (before Ratcatchers)
    // TODO(content): [quest:ratcatchers] after-quest directions and rat pits advice
    private suspend fun Dialogue.smokinJoe() {
        chatPlayer(happy, "Hello!")
        chatPlayer(quiz, "Hello?")
        chatPlayer(angry, "Hey!")
        mesbox("Smokin' Joe ignores you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dwarven_Boatman (Keldagrim)
    private suspend fun Dialogue.cityBoatman() {
        chatNpc(quiz, "Want me to take you back to the mines?")
        val choice = choice2("Yes, please take me.", 1, "What, on your ship! No thanks!", 2)
        if (choice == 1) {
            chatPlayer(happy, "Yes, please take me.")
            // TODO(content): [system:keldagrim_boat_travel] sail to the Keldagrim entrance mines
            return
        }
        chatPlayer(
            shocked,
            "What, on your ship? No thanks! I don't want to crash into any more things!",
        )
        chatNpc(neutral, "Hey now, it was only a slight accident!")
        // TODO(content): [quest:the_giant_dwarf] "Thanks to you I have to rebuild this statue..."
        chatPlayer(
            angry,
            "A slight accident?? Have you any idea how much time I spent rebuilding that statue?",
        )
        chatNpc(neutral, "Calm down, calm down! You got what you paid for the trip, didn't you?")
    }
}
