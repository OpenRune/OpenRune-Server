package org.rsmod.content.areas.city.rellekka.miscellania.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MiscellaniaCastle : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.misc_king_vargas") { startDialogue(it.npc) { kingVargas() } }
        onOpNpc1("npc.misc_advisor_ghrim") { startDialogue(it.npc) { advisorGhrim() } }
        onOpNpc1("npc.misc_prince_brand") { startDialogue(it.npc) { royalChild() } }
        onOpNpc1("npc.misc_princess_astrid") { startDialogue(it.npc) { royalChild() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Throne_of_Miscellania (Talking to King
    // Vargas)
    // TODO(content): [quest:throne_of_miscellania] "Start the Throne of Miscellania quest?",
    // in-quest dialogue and the dialogue between the quest and Royal Trouble
    private suspend fun Dialogue.kingVargas() {
        if (QuestRequirements.hasCompleted(player, ROYAL_TROUBLE)) {
            kingVargasAfterRoyalTrouble()
            return
        }
        chatNpc(
            neutral,
            "Who are you? Who seeks audience with the King of this fair land of Miscellania?",
        )
        chatPlayer(
            happy,
            "I am known in these lands as ${fremennikName()}, Your Majesty. I come in search " +
                "of a quest.",
        )
        chatNpc(happy, "Ah, it pleases me to see such enthusiasm!")
        chatNpc(
            happy,
            "I was once an adventurer such as yourself; even while fulfilling my duties as King " +
                "of Miscellania, I would roam the lands in search of adventure, and strike " +
                "down mighty foes with a single blow!",
        )
        chatNpc(
            sad,
            "Unfortunately, I was a little too daring, and attempted to brave the icelands, " +
                "where I finally met my match.",
        )
        chatNpc(
            sad,
            "I survived, barely, but I was cursed for the rest of my life to stay in this form. " +
                "Which leaves me incapable of performing my duties as ruler.",
        )
        chatNpc(
            neutral,
            "This is why I am looking for an exceptional soul to be regent of this fair kingdom " +
                "of Miscellania.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:King_Vargas (after Royal Trouble)
    // TODO(content): [quest:throne_of_miscellania] the "Select an Option" menu after this:
    //  recommending the consort's sibling as heir (depends on the player's consort) or "Never
    //  mind."
    private suspend fun Dialogue.kingVargasAfterRoyalTrouble() {
        chatNpc(
            happy,
            "Thank you for your work in bringing peace back to these islands, ${fremennikName()}.",
        )
        chatNpc(
            happy,
            "I hear that the Etceterians have also pledged to give you some of their resources, " +
                "as thanks for preventing war once again.",
        )
        chatNpc(quiz, "How is the kingdom faring?")
        chatPlayer(happy, "Everything is fine, your Majesty.")
        chatNpc(neutral, "Remember, speak to Ghrim if you require assistance.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Advisor_Ghrim
    // TODO(content): [quest:throne_of_miscellania] in-quest dialogue
    // TODO(content): [quest:royal_trouble] start offer before the quest
    // TODO(content): [system:managing_miscellania] kingdom management, coffers and collecting
    // resources after Throne of Miscellania, and the Collect op of npc.misc_advisor_ghrim_2ops
    private suspend fun Dialogue.advisorGhrim() {
        val title = if (access.isBodyTypeA()) "Sir" else "Madam"
        chatNpc(neutral, "Greetings, $title.")
        chatNpc(
            neutral,
            "The King needs someone to take over the rule of the Kingdom. If I were you, I " +
                "should consider the position.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Throne_of_Miscellania (Talking to Prince
    // Brand / Princess Astrid before the quest)
    // TODO(content): [quest:throne_of_miscellania] approval, in-quest and after-quest dialogue
    // TODO(content): [quest:royal_trouble] after-quest dialogue
    // (https://oldschool.runescape.wiki/w/Transcript:Prince_Brand,
    // https://oldschool.runescape.wiki/w/Transcript:Princess_Astrid)
    private suspend fun Dialogue.royalChild() {
        val title = if (access.isBodyTypeA()) "sir" else "madam"
        chatNpc(happy, "Good day, $title.")
        chatPlayer(happy, "Good day, Your Royal Highness.")
    }

    private companion object {
        const val ROYAL_TROUBLE = "quest_royaltrouble"
    }
}
