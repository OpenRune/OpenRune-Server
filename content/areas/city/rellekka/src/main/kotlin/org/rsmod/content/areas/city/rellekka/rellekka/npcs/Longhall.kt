package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_trials] after-quest dialogue and the hecklers' overhead
//  lines during the bard's trial
class Longhall : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_reveller") { startDialogue(it.npc) { drunkReveller() } }
        onOpNpc1("npc.viking_reveller_2") { startDialogue(it.npc) { drunkReveller() } }
        onOpNpc1("npc.viking_heckler") { startDialogue(it.npc) { waitingForShow() } }
        onOpNpc1("npc.viking_heckler_2") { startDialogue(it.npc) { waitingForShow() } }
        onOpNpc1("npc.viking_heckler_4") { startDialogue(it.npc) { waitingForShow() } }
        onOpNpc1("npc.viking_ant_hambling") { startDialogue(it.npc) { hambling() } }
        onOpNpc1("npc.viking_longhall_bouncer") { startDialogue(it.npc) { bouncer() } }
        onOpNpc1("npc.viking_longhall_barkeep") { startDialogue(it.npc) { thora() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bjorn
    // https://oldschool.runescape.wiki/w/Transcript:Eldgrim
    private suspend fun Dialogue.drunkReveller() {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            drunk,
            "Hey! scheck it out! Itsh an outerlandub! Yer shud go shpeak to the chieftain!",
        )
        chatPlayer(confused, "The who?")
        chatNpc(drunk, "That guy over there by that stuff! (hic) Yeh, abshoultely! He's da bosh!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ospak
    // https://oldschool.runescape.wiki/w/Transcript:Styrmir
    // https://oldschool.runescape.wiki/w/Transcript:Fridgeir
    private suspend fun Dialogue.waitingForShow() {
        chatNpc(angry, "Shhh! I'm waiting for the show!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hambling
    private suspend fun Dialogue.hambling() {
        chatNpc(happy, "I can't wait for the show to start - our bards are always brilliant!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Longhall_Bouncer
    // TODO(content): [quest:the_fremennik_trials] the same line on opening the backstage door,
    //  and the after-bard's-trial reply
    private suspend fun Dialogue.bouncer() {
        chatNpc(angry, "Hey, outerlander. You can't go through there. Talent only, backstage.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Thora_the_Barkeep
    // TODO(content): [quest:the_fremennik_trials] Trade (Rellekka Longhall Bar) is for Fremennik
    //  only; the refusal for outerlanders isn't on the wiki
    private suspend fun Dialogue.thora() {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            neutral,
            "Hello yourself, outerlander. A little out of the way up here, aren't you?",
        )
        chatNpc(
            neutral,
            "I would love to stop and chat with you, but unfortunately we have a custom that no " +
                "Fremennik may speak with the outerlanders without the permission of our " +
                "chieftain.",
        )
        chatNpc(
            neutral,
            "Don't take it personally. The Chieftain is stood just over there, his name is " +
                "Brundt. I suggest you go and introduce yourself.",
        )
        chatNpc(
            quiz,
            "You wouldn't want to go making any enemies because you weren't aware of our " +
                "customs now, would you?",
        )
        chatPlayer(neutral, "No, I guess not. Thanks anyway.")
        chatNpc(
            happy,
            "Not a problem, outerlander. It's always nice to see a new face in the Longhall; it " +
                "happens so rarely.",
        )
    }
}
