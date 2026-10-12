package org.rsmod.content.areas.wilderness.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MageArena @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.kolhuman") { startDialogue(it.npc) { kolodion() } }
        onOpNpc1("npc.magearena_guardian") { startDialogue(it.npc) { chamberGuardian(it.npc) } }
        onOpNpc3("npc.magearena_guardian") { player.openStaffShop(it.npc) }
        onOpNpc1("npc.magearena_runeshop") { startDialogue(it.npc) { lundail(it.npc) } }
        onOpNpc3("npc.magearena_runeshop") { player.openRuneShop(it.npc) }
        onOpNpc4("npc.magearena_runeshop") { startDialogue(it.npc) { claimFreeRunes() } }
        onOpNpc1("npc.magearena_banker") { startDialogue(it.npc) { gundai() } }
        onOpNpc3("npc.magearena_banker") { tryOpenBank() }
        onOpNpc4("npc.magearena_banker") { openCollectionBox() }
    }

    private fun Player.openStaffShop(npc: Npc) {
        shops.open(this, npc, "Mage Arena Staffs.", "inv.magearena_staffshop")
    }

    private fun Player.openRuneShop(npc: Npc) {
        shops.open(this, npc, "Lundail's Arena-side Rune Shop.", "inv.magearena_runeshop")
    }

    private fun ProtectedAccess.openCollectionBox() {
        ifOpenMainModal("interface.ge_collect")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mage_Arena_I
    // TODO(content): [miniquest:mage_arena_i] the duel against Kolodion on "Okay, let's fight.",
    //  the dialogue after the duel and the after-miniquest dialogue
    // TODO(content): [verify:in_game] whether "Can I fight here?" sits in a menu with other options
    private suspend fun Dialogue.kolodion() {
        chatPlayer(quiz, "Hello there. What is this place?")
        if (player.magicLvl < 60) {
            chatNpc(
                angry,
                "Do not waste my time with trivial questions. I am the Great Kolodion, master of " +
                    "battle magic. I have an arena to run.",
            )
            chatPlayer(quiz, "Can I enter?")
            chatNpc(laugh, "Hah! A wizard of your level? Don't be absurd.")
            return
        }
        chatNpc(
            happy,
            "I am the great Kolodion, master of battle magic, and this is my battle arena. Top " +
                "wizards travel from all over Gielinor to fight here.",
        )
        chatPlayer(quiz, "Can I fight here?")
        chatNpc(
            neutral,
            "My arena is open to any high level wizard, but this is no game. Many wizards fall " +
                "in this arena, never to rise again. The strongest mages have been destroyed.",
        )
        chatNpc(neutral, "If you're sure you want in?")
        while (true) {
            val option =
                choice4(
                    "Yes indeedy.",
                    1,
                    "No I don't.",
                    2,
                    "What's the point of that?",
                    3,
                    "That's barbaric!",
                    4,
                )
            when (option) {
                1 -> {
                    kolodionDuelOffer()
                    return
                }
                2 -> {
                    kolodionDecline()
                    return
                }
                3 -> {
                    chatPlayer(quiz, "What's the point of that?")
                    chatNpc(
                        neutral,
                        "We learn how to use our magic to its fullest and how to channel the " +
                            "forces of the cosmos into our world...",
                    )
                    chatNpc(laugh, "But mainly, I just like blasting people into dust.")
                }
                else -> {
                    chatPlayer(angry, "That's barbaric!")
                    chatNpc(
                        neutral,
                        "Nope, it's magic. But I know what you mean. So do you want to join us?",
                    )
                    when (choice2("Yes indeedy.", 1, "No I don't.", 2)) {
                        1 -> kolodionDuelOffer()
                        2 -> kolodionDecline()
                    }
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.kolodionDuelOffer() {
        chatPlayer(happy, "Yes indeedy.")
        chatNpc(happy, "Good, good. You have a healthy sense of competition.")
        chatNpc(
            neutral,
            "Remember, traveller - in my arena, hand-to-hand combat is useless. Your strength " +
                "will diminish as you enter the arena, but the spells you can learn are amongst " +
                "the most powerful in all of Gielinor.",
        )
        chatNpc(neutral, "Before I can accept you in, we must duel.")
        when (choice2("Okay, let's fight.", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "Okay, let's fight.")
                chatNpc(neutral, "I must first check that you are up to scratch.")
                chatPlayer(neutral, "You don't need to worry about that.")
                chatNpc(
                    neutral,
                    "Not just any magician can enter - only the most powerful and most feared. " +
                        "Before you can use the power of this arena, you must prove yourself " +
                        "against me.",
                )
            }
            2 -> chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun Dialogue.kolodionDecline() {
        chatPlayer(neutral, "No I don't.")
        chatNpc(neutral, "Your loss.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Chamber_guardian (post-miniquest)
    // TODO(content): [miniquest:mage_arena_i] the god cape and staff dialogue during the
    //  miniquest
    private suspend fun Dialogue.chamberGuardian(npc: Npc) {
        chatPlayer(happy, "Hello again.")
        chatNpc(neutral, "Hello adventurer, are you looking for another staff?")
        val option =
            choice3(
                "What do you have to offer?",
                1,
                "No thanks.",
                2,
                "Tell me what you know about the charge spell.",
                3,
            )
        when (option) {
            1 -> player.openStaffShop(npc)
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Well let me know if you need one.")
            }
            3 -> {
                chatPlayer(quiz, "Tell me what you know about the charge spell.")
                chatNpc(
                    neutral,
                    "We believe the spells are gifts from the gods. The charge spell draws even " +
                        "more power from the cosmos.",
                )
                chatNpc(
                    neutral,
                    "While wearing a matching cape and staff it will add 50% more damage to " +
                        "that already caused by battle mage spells for several minutes.",
                )
                chatPlayer(happy, "Good stuff.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lundail
    private suspend fun Dialogue.lundail(npc: Npc) {
        chatNpc(happy, "Hello there. How can I help you, brave adventurer?")
        val option =
            choice3(
                "What are you selling?",
                1,
                "What's that big old building above us?",
                2,
                "Claim free runes.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What are you selling?")
                chatNpc(
                    happy,
                    "I sell rune stones. I've got some good stuff, some really powerful little " +
                        "rocks. Take a look.",
                )
                player.openRuneShop(npc)
            }
            2 -> {
                chatPlayer(quiz, "What's that big old building above us?")
                chatNpc(
                    neutral,
                    "That, my friend, is the mage battle arena. Top mages come from all over " +
                        "Gielinor to compete in the arena.",
                )
                chatPlayer(shocked, "Wow.")
                chatNpc(laugh, "Few return, most get fried, hence the smell.")
                chatPlayer(confused, "Hmmm.. I did notice.")
            }
            3 -> claimFreeRunes()
        }
    }

    // TODO(content): [diary:wilderness] daily free runes once the easy diary is complete
    private suspend fun Dialogue.claimFreeRunes() {
        chatNpc(
            neutral,
            "You'll need to have completed the Easy Wilderness Achievement Diary to qualify for " +
                "free runes.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gundai
    private suspend fun Dialogue.gundai() {
        chatPlayer(quiz, "Hello, what are you doing out here?")
        chatNpc(neutral, "I'm a banker, the only one around these dangerous parts.")
        val option =
            choice4(
                "Cool, I'd like to access my bank account please.",
                1,
                "Right, so can I check my PIN settings?",
                2,
                "I'd like to collect items.",
                3,
                "Well, now I know.",
                4,
            )
        when (option) {
            1 -> access.tryOpenBank()
            2 -> access.ifOpenMainModal("interface.bankpin_settings")
            3 -> access.openCollectionBox()
            4 -> {
                chatPlayer(neutral, "Well, now I know.")
                chatNpc(happy, "Knowledge is power my friend.")
            }
        }
    }
}
