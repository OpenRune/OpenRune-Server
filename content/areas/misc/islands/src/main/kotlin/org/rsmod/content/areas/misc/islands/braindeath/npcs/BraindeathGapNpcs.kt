package org.rsmod.content.areas.misc.islands.braindeath.npcs

import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:rum_deal] the island opens with Rum Deal, so the after-quest dialogue
// is the default
class BraindeathGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.deal_50luke") { startDialogue(it.npc) { fiftyPercentLuke() } }
        onOpNpc1("npc.deal_captian_donnie") { startDialogue(it.npc) { captainDonnie() } }
        onOpNpc1("npc.deal_davey") { startDialogue(it.npc) { davey() } }
    }

    private val Dialogue.ladOrLass: String
        get() = if (player.appearance.bodyType == Constants.bodytype_a) "lad" else "lass"

    // https://oldschool.runescape.wiki/w/Transcript:50%25_Luke
    // TODO(content): [system:cutscene] the tinderbox animation and the "One 500 hit combo
    // later..." fade, and using a tinderbox on Luke
    private suspend fun Dialogue.fiftyPercentLuke() {
        chatPlayer(worried, "Are you all right?")
        chatNpc(quiz, "Who goes there?")
        chatNpc(angry, "Arr! A landlubber!")
        chatNpc(
            angry,
            "Begone afore I take my cutlass to ye! I've been charged with guardin' this gate and " +
                "no noodle-armed landlubbers will make it past me alive!",
        )
        chatPlayer(neutral, "That's not a cutlass.")
        chatPlayer(neutral, "I think it's a twig.")
        chatNpc(angry, "Ye cheeky begger! I was wavin' my finger at ye!")
        chatPlayer(neutral, "All right...")
        chatNpc(
            sad,
            "Arrr... just 'cos me body happens to be 50% wood does not mean I'm heartless.",
        )
        chatNpc(happy, "I got a bag of 'em here. Wanna see?")
        chatPlayer(worried, "I think I'll pass.")
        val option =
            choice3(
                "What happened to you?",
                1,
                "How flammable are you?",
                2,
                "So what is going on here anyway?",
                3,
            )
        when (option) {
            1 -> lukesTale()
            2 -> howFlammable()
            3 -> whatIsGoingOn()
        }
    }

    private suspend fun Dialogue.lukesTale() {
        chatPlayer(quiz, "What happened to you?")
        if (RING_OF_CHAROS.none { it in player.worn }) {
            chatNpc(
                sad,
                "That be a tale so heart-wrenchin' that it has so far wrenched the hearts from " +
                    "over a dozen sturdier men than thee!",
            )
            chatNpc(
                sad,
                "A tale of such woe that none but the stoniest hearts can hear it without " +
                    "sheddin' a single, poignant tear...",
            )
            chatNpc(
                angry,
                "And I'll never tell ye, not even if ye wore an item, such as a ring, with " +
                    "powers of mind control!",
            )
            chatPlayer(quiz, "What if I said please?")
            chatNpc(neutral, "Hmmmm...well alright...")
            chatNpc(angry, "Wait, no!")
            chatNpc(angry, "Get goin' landlubber before I take my splintery foot to yer behind!")
            return
        }
        chatNpc(
            neutral,
            "Well ye seem like a $ladOrLass that can handle such a tale...so I'll tell ye...",
        )
        chatNpc(neutral, "Well, it all starts with this albatross...")
        chatNpc(worried, "Wait, never mind, I'll skip forward a bit.")
        chatPlayer(quiz, "Are you sure?")
        chatNpc(
            worried,
            "I have to $ladOrLass, Cap'n Donnie will flay what little is left of me if I told ye.",
        )
        chatNpc(
            neutral,
            "But anyway, I got recruited to the zombie pirates along with the rest of the crew " +
                "in an unspecified incident involvin this albatross.",
        )
        chatNpc(
            happy,
            "We was sailin' along happily, and I was partakin' of a little 'rum' in the crows " +
                "nest.",
        )
        chatNpc(neutral, "Well, we hit either a really rough wave or some rocks.")
        chatNpc(laugh, "Twas kind of hard for me to tell which, as I was well out of it by then!")
        chatNpc(neutral, "Regardless I toppled from the crows nest into the water.")
        chatPlayer(quiz, "Is that how you got so badly injured?")
        chatNpc(neutral, "No $ladOrLass!")
        chatNpc(
            neutral,
            "What happened next was that I discovered a new, previously uncharted reef of hard, " +
                "spiky coral.",
        )
        chatNpc(
            neutral,
            "I made a mental note of its location, and to this day it is still marked on our " +
                "fleet's charts as Lukes Reef.",
        )
        chatNpc(
            neutral,
            "I managed to grab a hold of our ship, the Inebriated, as it passed overhead.",
        )
        chatNpc(
            sad,
            "And then I discovered another, taller, spikier reef of even sharper and more " +
                "painful coral.",
        )
        chatNpc(
            neutral,
            "To this day it is still marked on our fleet's charts as The Other 50% Reef.",
        )
        chatPlayer(worried, "Owwwwwwwww...")
        chatNpc(sad, "It gets worse...")
        chatNpc(
            sad,
            "When they hauled what was left of me on deck, they dropped me onto the floor while " +
                "they decided what to do with me.",
        )
        chatNpc(
            sad,
            "Bear in mind this would be on a ghost ship, the planks of which sweat a thick " +
                "mixture of stagnant water...and pure salt crystals.",
        )
        chatPlayer(shocked, "Oh...my...god...")
        chatNpc(
            happy,
            "But on the good side, all my thrashin' and pained squealin' settled the matter in " +
                "the Captain's mind, and he had the shipwright carve me half a body out of his " +
                "Witchwood Planks.",
        )
        chatPlayer(quiz, "Witchwood? What's that?")
        chatNpc(neutral, "Tis a special, magical wood from a now extinct tree.")
        chatNpc(
            happy,
            "Once they nailed it all in place the stuff moves like it is part of me body.",
        )
        chatNpc(happy, "The stuff will also grow back if it breaks, which is dead handy!")
        chatPlayer(happy, "Wow, that stuff must be very valuable!")
        chatNpc(happy, "Arr! That it be!")
        chatNpc(neutral, "So, that be the tale of how I managed to lose precisely 50% of my body.")
        chatPlayer(sad, "There there.")
    }

    private suspend fun Dialogue.howFlammable() {
        chatPlayer(quiz, "So how flammable are you?")
        chatNpc(angry, "What kind of a question is that?")
        if (TINDERBOX !in player.inv) {
            chatPlayer(neutral, "A rather rude, personal one?")
            chatNpc(angry, "Ye've got that right!")
            return
        }
        chatPlayer(angry, "I'll soon have you out of the way! Burn!")
        chatNpc(angry, "Don't make me angry! Ye'll not like me when I'm angry!")
        chatNpc(angry, "Let that be a lesson to ye!")
        chatPlayer(sad, "My world is an ocean of paaaain!")
    }

    private suspend fun Dialogue.whatIsGoingOn() {
        chatPlayer(quiz, "So what is going on here anyway?")
        chatNpc(shifty, "Ye expect me to talk?")
        chatPlayer(angry, "No Mr. Luke, I expect you to die!")
        chatNpc(laugh, "Hah! I'm one step ahead of ye!")
        chatPlayer(confused, "Egad, outsmarted by the man with the wooden brain.")
        chatPlayer(quiz, "But seriously, what is going on here?")
        chatNpc(neutral, "I can't tell ye $ladOrLass.")
        chatNpc(worried, "The Cap'n would have me whittled down to toothpicks if I did.")
        chatPlayer(
            quiz,
            "Well if you can't tell me, perhaps you could show me through the medium of " +
                "Interpretive Dance?",
        )
        chatNpc(angry, "No. Just...no.")
        chatPlayer(quiz, "Mime?")
        chatNpc(
            angry,
            "Look, $ladOrLass I'm not tellin' ye a thing! So clear out while ye still can!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Donnie
    private suspend fun Dialogue.captainDonnie() {
        chatNpc(angry, "Hey! You!")
        chatPlayer(confused, "Who? Me?")
        chatNpc(angry, "Aye! Ye!")
        chatNpc(quiz, "Got any more rum?")
        chatPlayer(quiz, "What happens if I don't?")
        chatNpc(angry, "Then I'll clap ye in Runes!")
        chatPlayer(confused, "Don't you mean clap me in irons?")
        chatNpc(neutral, "No lad, not irons, Runes!")
        chatNpc(happy, "We upgraded last week.")
        chatPlayer(
            happy,
            "Ok, well it's a good job that I have some over here isn't it?",
        )
        chatNpc(happy, "Arr, yer a good lad...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Davey
    // TODO(content): [system:lost_item_reclaim] the holy wrench when it isn't in the inventory
    // or bank
    private suspend fun Dialogue.davey() {
        chatNpc(happy, "Ello again guv. How's things?")
        chatPlayer(happy, "Good! Everything seems to have worked out ok.")
        chatNpc(shifty, "I'm sure it'll stay that way so long as you don't know it's missing.")
        chatPlayer(confused, "What?")
        chatNpc(happy, "Nothin' guv, you just have a nice day.")
    }

    private companion object {
        const val TINDERBOX = "obj.tinderbox"
        val RING_OF_CHAROS = listOf("obj.ring_of_charos", "obj.ring_of_charos_unlocked")
    }
}
