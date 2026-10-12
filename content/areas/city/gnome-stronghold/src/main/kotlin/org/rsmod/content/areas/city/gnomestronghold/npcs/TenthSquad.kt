package org.rsmod.content.areas.city.gnomestronghold.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class TenthSquad @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.mm2_garkor_postquest") { startDialogue(it.npc) { garkor() } }
        onOpNpc1("npc.mm2_lumo_breach_multi") { startDialogue(it.npc) { lumo() } }
        onOpNpc1("npc.mm2_bunkdo_postquest") { startDialogue(it.npc) { bunkdo() } }
        onOpNpc1("npc.mm2_carado_postquest") { startDialogue(it.npc) { carado() } }
        onOpNpc1("npc.mm2_bunkwicket_postquest") { startDialogue(it.npc) { bunkwicket() } }
        onOpNpc1("npc.mm2_waymottin_postquest") { startDialogue(it.npc) { waymottin() } }
        onOpNpc1("npc.mm2_zooknock_postquest") { startDialogue(it.npc) { zooknock() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Garkor (after Monkey Madness II)
    // TODO(content): [system:gnome_restaurant] delivery hand-in dialogue
    private suspend fun Dialogue.garkor() {
        chatNpc(happy, "Greetings, ${player.displayName}.")
        chatPlayer(happy, "Hey Garkor, how's it going?")
        chatNpc(
            happy,
            "Very well. Since the 10th squad were recalled from Ape Atoll, King Narnode has named " +
                "me new Grand Tree Guardian.",
        )
        chatNpc(
            happy,
            "I believe I speak on behalf of everyone in the Stronghold when I say thank you for " +
                "stopping Glough. Who knows what would've happened if you weren't there to foil " +
                "his schemes.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lumo
    private suspend fun Dialogue.lumo() {
        chatPlayer(quiz, "What's going on here?")
        chatNpc(
            neutral,
            "We've secured the crash site, you've proven that you can handle yourself against " +
                "these demonic gorillas, so feel free to head down to the cavern whenever you " +
                "like.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bunkdo
    private suspend fun Dialogue.bunkdo() {
        chatPlayer(quiz, "How are things, Bunkdo?")
        chatNpc(happy, "It's great to be back home, ${player.displayName}.")
        chatNpc(
            happy,
            "We'd spent so long out there on Ape Atoll I was starting to doubt I'd ever see the " +
                "Grand Tree again!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Carado
    private suspend fun Dialogue.carado() {
        chatPlayer(happy, "Good day, Carado.")
        chatPlayer(quiz, "What are you doing here?")
        chatNpc(shifty, "Well, I figured that with Glough being gone and all...")
        chatPlayer(confused, "...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bunkwicket
    private suspend fun Dialogue.bunkwicket() {
        chatPlayer(quiz, "How does it feel to be back home?")
        chatNpc(
            drunk,
            "Ayyy ... *hiccup* ... Ish been sho long sinsh I've ... *hiccup* ... ad a deshent " +
                "cocktail!",
        )
        chatPlayer(confused, "Glad to see you enjoying yourself... I guess.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Waymottin
    private suspend fun Dialogue.waymottin() {
        chatPlayer(quiz, "Hi Waymottin, what are you up to?")
        chatNpc(neutral, "Someone has to keep an eye on Bunkwicket!")
        chatPlayer(happy, "Well, try to enjoy yourself.")
        chatNpc(laugh, "Ha, no chance!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zooknock (after Monkey Madness II)
    private suspend fun Dialogue.zooknock() {
        chatNpc(happy, "Good day, ${player.displayName}. What can I help you with?")
        while (true) {
            val option =
                choice4(
                    "Monkey talisman.",
                    1,
                    "Monkeyspeak amulet.",
                    2,
                    "10th squad sigil.",
                    3,
                    "Just passing by.",
                    4,
                )
            val handedOut =
                when (option) {
                    1 -> talisman()
                    2 -> {
                        chatPlayer(quiz, "I'd like a monkeyspeak amulet...")
                        handOut(MONKEYSPEAK_AMULET, "Zooknock hands you a new monkeyspeak amulet.")
                    }
                    3 -> {
                        chatPlayer(quiz, "I'd like a 10th squad sigil...")
                        handOut(SIGIL, "Zooknock hands you a new 10th squad sigil.")
                    }
                    else -> {
                        chatPlayer(neutral, "I was just passing by.")
                        chatNpc(happy, "Okay. Farewell, ${player.displayName}.")
                        return
                    }
                }
            if (!handedOut) {
                return
            }
            chatNpc(happy, "Is there anything else I can help you with, ${player.displayName}?")
        }
    }

    private suspend fun Dialogue.talisman(): Boolean {
        chatPlayer(quiz, "I'd like a new monkey talisman...")
        chatNpc(
            happy,
            "Not a problem, Awowogei has given us access to all the resources we need to create " +
                "new talismans.",
        )
        chatNpc(quiz, "What type of talisman are you after then?")
        var page = 0
        while (true) {
            val talisman =
                when (page) {
                    0 ->
                        choice4(
                            "Small ninja.",
                            SMALL_NINJA,
                            "Medium ninja.",
                            MEDIUM_NINJA,
                            "Large ninja.",
                            LARGE_NINJA,
                            "Next.",
                            NEXT,
                        )
                    1 ->
                        choice4(
                            "Small zombie.",
                            SMALL_ZOMBIE,
                            "Large zombie.",
                            LARGE_ZOMBIE,
                            "Previous.",
                            PREVIOUS,
                            "Next.",
                            NEXT,
                        )
                    else ->
                        choice4(
                            "Gorilla.",
                            GORILLA,
                            "Bearded gorilla.",
                            BEARDED_GORILLA,
                            "Karamjan monkey.",
                            KARAMJAN_MONKEY,
                            "Previous.",
                            PREVIOUS,
                        )
                }
            when (talisman) {
                NEXT -> page++
                PREVIOUS -> page--
                else -> {
                    chatPlayer(quiz, "I'd like a ${TALISMAN_NAMES.getValue(talisman)}, please.")
                    return handOut(talisman, "Zooknock hands you the talisman.")
                }
            }
        }
    }

    private suspend fun Dialogue.handOut(obj: String, text: String): Boolean {
        if (access.playerContainsObj(obj)) {
            chatNpc(neutral, "You already have one of those, ${player.displayName}.")
            return false
        }
        player.invAddOrDrop(objRepo, obj)
        objbox(obj, text)
        return true
    }

    private companion object {
        const val NEXT = "next"
        const val PREVIOUS = "previous"
        const val SMALL_NINJA = "obj.mm_monkey_greegree_for_small_ninja_monkey"
        const val MEDIUM_NINJA = "obj.mm_monkey_greegree_for_medium_ninja_monkey"
        const val LARGE_NINJA = "obj.mm2_kruk_greegree"
        const val SMALL_ZOMBIE = "obj.mm_monkey_greegree_for_small_zombie_monkey"
        const val LARGE_ZOMBIE = "obj.mm_monkey_greegree_for_large_zombie_monkey"
        const val GORILLA = "obj.mm_monkey_greegree_for_normal_gorilla"
        const val BEARDED_GORILLA = "obj.mm_monkey_greegree_for_bearded_gorilla"
        const val KARAMJAN_MONKEY = "obj.mm_monkey_greegree_for_normal_monkey"
        const val MONKEYSPEAK_AMULET = "obj.mm_amulet_of_monkey_speak"
        const val SIGIL = "obj.mm_sigil"
        val TALISMAN_NAMES =
            mapOf(
                SMALL_NINJA to "small ninja talisman",
                MEDIUM_NINJA to "medium ninja talisman",
                LARGE_NINJA to "large ninja talisman",
                SMALL_ZOMBIE to "small zombie talisman",
                LARGE_ZOMBIE to "large zombie talisman",
                GORILLA to "gorilla talisman",
                BEARDED_GORILLA to "bearded gorilla talisman",
                KARAMJAN_MONKEY to "karamjan monkey talisman",
            )
    }
}
