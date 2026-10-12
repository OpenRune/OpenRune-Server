package org.rsmod.content.areas.city.burthorpe.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class TrollCountryGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.death_white_knight") { startDialogue(it.npc) { whiteKnight() } }
        onOpNpc1("npc.death_sherpa") { startDialogue(it.npc) { tenzing() } }
        onOpNpc1("npc.troll_champion") { startDialogue(it.npc) { dad() } }
        onOpNpc1("npc.eadgar_troll_chief_cook") { startDialogue(it.npc) { burntmeat() } }
        onOpNpc1("npc.trollromance_aga") { startDialogue(it.npc) { aga() } }
        onOpNpc1("npc.trollromance_arrg") { startDialogue(it.npc) { arrg() } }
        onOpNpc1("npc.fd_troll_dad") { startDialogue(it.npc) { trollFather() } }
        onOpNpc1("npc.fd_troll_mum") { startDialogue(it.npc) { trollMother() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:White_Knight_(Burthorpe)
    private suspend fun Dialogue.whiteKnight() {
        chatPlayer(happy, "Hi!")
        chatNpc(happy, "Hi!")
        chatPlayer(quiz, "So, what you up to out here?")
        chatNpc(shifty, "Oh...er...nothing!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tenzing (before Death Plateau)
    // TODO(content): [quest:death_plateau] after-quest options and the climbing boots sale
    private suspend fun Dialogue.tenzing() {
        mesbox("You knock on the door.")
        chatNpc(neutral, "No milk today! Thank you!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dad (after Troll Stronghold)
    // TODO(content): [quest:troll_stronghold] before and during the quest
    private suspend fun Dialogue.dad() {
        chatPlayer(quiz, "Why are you called Dad?")
        chatNpc(neutral, "Troll named after first thing try to eat!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Burntmeat
    // TODO(content): [quest:making_friends_with_my_arm] Wolfbone dialogue after the fire notes
    private suspend fun Dialogue.burntmeat() {
        chatPlayer(worried, "Er, hi.")
        chatNpc(
            neutral,
            "Hmm? What human do in troll kitchen? Burntmeat tired of cooking goats. Human look " +
                "tasty.",
        )
        chatPlayer(
            shocked,
            "Oh, you don't want to eat me! I'm a tough, hardened adventurer, not tender or " +
                "tasty at all!",
        )
        chatNpc(neutral, "Hmm. Burntmeat think you probably right. What human doing here?")
        chatPlayer(worried, "Nothing. I was just leaving.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Aga (before Troll Romance)
    // TODO(content): [quest:troll_romance] after-quest dialogue
    private suspend fun Dialogue.aga() {
        chatNpc(angry, "What man-thing want?")
        chatPlayer(worried, "Errr... I've got to go.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Arrg (before Troll Romance)
    // TODO(content): [quest:troll_romance] after-quest dialogue
    private suspend fun Dialogue.arrg() {
        chatNpc(verymad, "Aaaarrrrrghhh!")
        chatPlayer(worried, "O-oh, he seems cheesed off...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Troll_father (after Desert Treasure I)
    // TODO(content): [quest:desert_treasure_i] only spawns once freed
    private suspend fun Dialogue.trollFather() {
        chatNpc(
            happy,
            "Thanks again for freeing me from that ice block! I might be a troll, but it was " +
                "real uncomfortable in there!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Troll_mother (after Desert Treasure I)
    // TODO(content): [quest:desert_treasure_i] only spawns once freed
    private suspend fun Dialogue.trollMother() {
        chatNpc(
            happy,
            "Thanks again for freeing me from that ice block! I don't know what my little " +
                "snookums would have done without us!",
        )
    }
}
