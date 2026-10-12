package org.rsmod.content.areas.city.burthorpe.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class TrollCountryGapNpcs @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
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

    // https://oldschool.runescape.wiki/w/Transcript:Tenzing
    private suspend fun Dialogue.tenzing() {
        if (!QuestRequirements.hasCompleted(player, DEATH_PLATEAU)) {
            mesbox("You knock on the door.")
            chatNpc(neutral, "No milk today! Thank you!")
            return
        }
        while (true) {
            val option =
                choice5(
                    "Can I buy some Climbing boots?",
                    1,
                    "What does a Sherpa do?",
                    2,
                    "How did you find out about the secret way?",
                    3,
                    "Nice place you have here.",
                    4,
                    "Nothing, thanks!",
                    5,
                )
            when (option) {
                1 -> tenzingClimbingBoots()
                2 -> {
                    chatPlayer(quiz, "What does a Sherpa do?")
                    chatNpc(
                        neutral,
                        "We are expert guides that take adventurers, such as yourself, on " +
                            "mountaineering expeditions.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "How did you find out about the secret way?")
                    chatNpc(
                        neutral,
                        "I used to take adventurers up Death Plateau and further north before " +
                            "the trolls came. I know these mountains well.",
                    )
                }
                4 -> {
                    chatPlayer(happy, "Nice place you have here.")
                    chatNpc(
                        happy,
                        "Thanks, I built it myself! I'm usually self sufficient but I can't earn " +
                            "any money with the trolls camped on Death Plateau.",
                    )
                    chatPlayer(
                        happy,
                        "Now that the Imperial Guard know about the secret way they will be " +
                            "able to destroy the trolls!",
                    )
                    chatNpc(happy, "Let us hope so traveller!")
                }
                5 -> {
                    chatPlayer(happy, "Nothing, thanks!")
                    return
                }
            }
            chatNpc(quiz, "Was there anything else?")
        }
    }

    // TODO(content): [verify:in_game] the wiki gives "Tenzing has given you some Climbing boots."
    //  twice and says nothing about a full inventory; this shows it once and drops the boots
    private suspend fun Dialogue.tenzingClimbingBoots() {
        chatPlayer(quiz, "Can I buy some Climbing boots?")
        chatNpc(neutral, "Sure, I'll sell you some in your size for 12 gold.")
        if (!choice2("OK, sounds good.", true, "No, thanks.", false)) {
            chatPlayer(neutral, "No, thanks.")
            return
        }
        chatPlayer(happy, "OK, sounds good.")
        if (!player.invTakeFee(CLIMBING_BOOTS_PRICE)) {
            chatPlayer(sad, "I don't have the money on me.")
            return
        }
        player.invAddOrDrop(objRepo, CLIMBING_BOOTS)
        mesbox("Tenzing has given you some Climbing boots.")
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

    // https://oldschool.runescape.wiki/w/Transcript:Aga
    private suspend fun Dialogue.aga() {
        if (QuestRequirements.hasCompleted(player, TROLL_ROMANCE)) {
            chatNpc(
                happy,
                "Thanks for getting Aga and Ug happy. Without you Ug too shy, Aga so in love " +
                    "with Ug!",
            )
            return
        }
        chatNpc(angry, "What man-thing want?")
        chatPlayer(worried, "Errr... I've got to go.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Arrg
    private suspend fun Dialogue.arrg() {
        if (QuestRequirements.hasCompleted(player, TROLL_ROMANCE)) {
            chatNpc(worried, "Oh mighty man-warrior, please no hurt Arrg.")
            return
        }
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

    private companion object {
        const val DEATH_PLATEAU = "quest_deathplateau"
        const val TROLL_ROMANCE = "quest_trollromance"

        // https://oldschool.runescape.wiki/w/Transcript:Tenzing: 12 gold
        const val CLIMBING_BOOTS_PRICE = 12
        const val CLIMBING_BOOTS = "obj.death_climbingboots"
    }
}
