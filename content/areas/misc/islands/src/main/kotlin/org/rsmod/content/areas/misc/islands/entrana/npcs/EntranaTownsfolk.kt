package org.rsmod.content.areas.misc.islands.entrana.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class EntranaTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.entrana_monk") { startDialogue(it.npc) { monk() } }
        onOpNpc1("npc.cave_monk") { startDialogue(it.npc) { caveMonk() } }
        onOpNpc1("npc.handsand_naziom") { startDialogue(it.npc) { mazion() } }
        onOpNpc1(HIGH_PRIEST) { startDialogue(it.npc) { highPriest() } }
        onOpNpcU(HIGH_PRIEST) { startDialogue(it.npc) { highPriestDonation() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Monk
    private suspend fun Dialogue.monk() {
        chatNpc(happy, "Greetings traveller.")
        val option =
            choice2(
                "Can you heal me? I'm injured.",
                1,
                "Isn't this place built a bit out of the way?",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(sad, "Can you heal me? I'm injured.")
                chatNpc(happy, "Ok.")
                player.statHeal(HITPOINTS, constant = 2, percent = 20)
            }
            2 -> {
                chatPlayer(quiz, "Isn't this place built a bit out of the way?")
                chatNpc(
                    happy,
                    "We like it that way actually! We get disturbed less. We still get rather a " +
                        "large amount of travellers looking for sanctuary and healing here as it " +
                        "is!",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cave_monk
    // TODO(content): [system:entrana_dungeon] the same warning before climbing down the ladder
    private suspend fun Dialogue.caveMonk() {
        chatNpc(
            worried,
            "Be careful going in there! You are unarmed, and there is much evilness lurking down " +
                "there! The evilness seems to block off our contact with our gods,",
        )
        chatNpc(
            worried,
            "so our prayers seem to have less effect down there. Oh, also, you won't be able to " +
                "come back this way - This ladder only goes one way!",
        )
        chatNpc(
            worried,
            "The only exit from the caves below is a portal which leads only to the deepest " +
                "wilderness!",
        )
        val option =
            choice2(
                "I don't think I'm strong enough to enter then.",
                1,
                "Well that is a risk I will have to take.",
                2,
            )
        when (option) {
            1 -> chatPlayer(worried, "I don't think I'm strong enough to enter then.")
            2 -> chatPlayer(neutral, "Well that is a risk I will have to take.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mazion
    private suspend fun Dialogue.mazion() {
        when (access.random.of(3)) {
            0 -> chatNpc(happy, "Nice weather we're having today!")
            1 -> chatNpc(happy, "Hello ${player.displayName}, fine day today!")
            else -> chatNpc(sad, "Please leave me alone, a parrot stole my banana.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:High_Priest_(Entrana)
    // TODO(content): [skill:sailing] the holy sheet and the "Ask for more paint." option
    // TODO(content): [system:wise_old_man_tasks] delivering the Wise Old Man's message
    // TODO(content): [quest:holy_grail] quest dialogue
    // TODO(content): [quest:devious_minds] quest dialogue
    // TODO(content): [quest:desert_treasure_i] quest dialogue
    private suspend fun Dialogue.highPriest() {
        chatNpc(happy, "Many greetings. Welcome to our fair island.")
        chatNpc(happy, "Enjoy your stay here. May it be spiritually uplifting!")
    }

    private suspend fun Dialogue.highPriestDonation() {
        chatNpc(neutral, "No thank you, I am not accepting donations for the church at this time.")
    }

    private companion object {
        const val HIGH_PRIEST = "npc.high_priest_of_entrana"
        const val HITPOINTS = "stat.hitpoints"
    }
}
