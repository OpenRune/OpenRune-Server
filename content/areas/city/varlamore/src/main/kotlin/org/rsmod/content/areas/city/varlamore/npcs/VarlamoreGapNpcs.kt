package org.rsmod.content.areas.city.varlamore.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class VarlamoreGapNpcs @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.stonecutter_liaison") { startDialogue(it.npc) { avinia() } }
        onOpNpc1(METLA) { startDialogue(it.npc) { metla(it.npc) } }
        onOpNpc3(METLA) { player.openShop(it.npc, METLA_SHOP) }
        onOpNpc1(ARCUANI) { startDialogue(it.npc) { arcuani(it.npc) } }
        onOpNpc3(ARCUANI) { player.openShop(it.npc, ARCUANI_SHOP) }
        onOpNpc1(XOCHITL) { startDialogue(it.npc) { xochitl(it.npc) } }
        onOpNpc3(XOCHITL) { player.openShop(it.npc, XOCHITL_SHOP) }
        onOpNpc1("npc.tal_teklan_farmer") { startDialogue(it.npc) { talTeklanFarmer() } }
        onOpNpc1(SAWMILL) { startDialogue(it.npc) { sawmillOperator(it.npc) } }
        onOpNpc4(SAWMILL) { player.openShop(it.npc, SAWMILL_SHOP) }
        onOpNpc1(WORKER) { startDialogue(it.npc) { constructionWorker() } }
        onOpNpc1("npc.varlamore_wyrm_agility_guide_3op") { startDialogue(it.npc) { wormTongue() } }
        onOpNpc1("npc.vmq4_captain_vibia_visible") { startDialogue(it.npc) { captainVibia() } }
        onOpNpc1("npc.tlati_lost_lover_fabia_vis") { startDialogue(it.npc) { fabia() } }
        onOpNpc1(DAVE) { startDialogue(it.npc) { daveAndJane() } }
        onOpNpc1(JANE) { startDialogue(it.npc) { daveAndJane() } }
        onOpNpc1(GARY) { startDialogue(it.npc) { garyAndSue() } }
        onOpNpc1(SUE) { startDialogue(it.npc) { garyAndSue() } }
        onOpNpc1("npc.huey_lobby_leader") { startDialogue(it.npc) { taala() } }
    }

    private fun Player.openShop(npc: Npc, shop: Pair<String, String>) {
        shops.open(this, npc, shop.first, shop.second)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Avinia
    private suspend fun Dialogue.avinia() {
        chatPlayer(happy, "Hey there.")
        chatNpc(neutral, "Need something, iknami?")
        chatPlayer(quiz, "Not really. Just wondering what brings you out here?")
        chatNpc(
            neutral,
            "I'm here to trade with the dwarves here on behalf of Fortis. Stonecutter Outpost is " +
                "the main supplier of the stone we use for building.",
        )
        chatPlayer(quiz, "And is the trading going well?")
        chatNpc(
            bored,
            "Well it's significantly better than dealing with the dwarves in Cam Torum, but " +
                "they're still not the easiest to work with.",
        )
        chatPlayer(neutral, "Ah, well in that case, I'd better leave you to it.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Metla
    // TODO(content): [system:treasure_trails] clue scroll dialogue
    private suspend fun Dialogue.metla(npc: Npc) {
        chatNpc(neutral, "Yeah? Need something?")
        while (true) {
            val option =
                choice3("What do you have?", 1, "What brings you here?", 2, "I'm good.", 3)
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What do you have?")
                    chatNpc(neutral, "Rock. Lots of it.")
                    player.openShop(npc, METLA_SHOP)
                    return
                }
                2 -> {
                    chatPlayer(quiz, "What brings you here?")
                    chatNpc(
                        neutral,
                        "Rock. This here is Stonecutter Outpost. We get rock, cut it, then sell " +
                            "it.",
                    )
                    chatPlayer(quiz, "So you're pretty far from home then?")
                    chatNpc(
                        confused,
                        "What? No. We go back to Cam Torum sometimes, but this is home.",
                    )
                    chatPlayer(neutral, "Fair enough.")
                    chatNpc(neutral, "Need something else?")
                }
                else -> {
                    chatPlayer(neutral, "I'm good.")
                    chatNpc(angry, "Then why you keep staring? Get gone!")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Arcuani
    private suspend fun Dialogue.arcuani(npc: Npc) {
        chatNpc(happy, "Hello there, iknami. Are you interested in some archery supplies?")
        if (choice2("Yes please.", true, "No thanks.", false)) {
            chatPlayer(happy, "Yes please.")
            player.openShop(npc, ARCUANI_SHOP)
            return
        }
        chatPlayer(neutral, "No thanks.")
        chatNpc(neutral, "Well, you know where I am if you change your mind, iknami.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Xochitl
    private suspend fun Dialogue.xochitl(npc: Npc) {
        chatNpc(happy, "Nilsal! Looking to buy some dyes?")
        if (choice2("I'll have a look.", true, "I'm good, thanks.", false)) {
            chatPlayer(happy, "I'll have a look.")
            player.openShop(npc, XOCHITL_SHOP)
            return
        }
        chatPlayer(neutral, "I'm good, thanks.")
        chatNpc(happy, "In that case, timoiva for now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Farmer_(Tal_Teklan)
    private suspend fun Dialogue.talTeklanFarmer() {
        chatPlayer(happy, "Hello there. How's it going?")
        when (access.random.of(5)) {
            0 -> chatNpc(happy, "Well, the work is going well, so I'm very good, thank you.")
            1 -> chatNpc(neutral, "Not too bad thanks.")
            2 -> {
                chatNpc(happy, "I'm fine, how are you?")
                chatPlayer(happy, "Very well, thank you.")
            }
            3 -> chatNpc(happy, "Nilsal! I'm pretty good, thank you.")
            else -> chatNpc(happy, "Hello there! Nice weather we've been having.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sawmill_Operator_(Auburnvale)
    // TODO(content): [system:plank_making] "Yes, please make me some planks." and the Buy-plank op
    private suspend fun Dialogue.sawmillOperator(npc: Npc) {
        chatNpc(
            happy,
            "Nilsal. Do you want me to make some planks for you? I can make planks from wood, " +
                "oak, teak and mahogany logs. Or would you like to buy some other housing " +
                "supplies?",
        )
        val option =
            choice3(
                "Yes, please make me some planks.",
                1,
                "Can I buy some housing supplies?",
                2,
                "I'm good, thanks",
                3,
            )
        when (option) {
            1 -> chatPlayer(happy, "Yes, please make me some planks.")
            2 -> {
                chatPlayer(quiz, "Can I buy some housing supplies?")
                chatNpc(happy, "Of course!")
                player.openShop(npc, SAWMILL_SHOP)
            }
            3 -> {
                chatPlayer(neutral, "I'm good, thanks.")
                chatNpc(
                    neutral,
                    "Well come back when you want some. You'll struggle to find quality planks " +
                        "anywhere but here!",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Construction_Worker
    private suspend fun Dialogue.constructionWorker() {
        chatNpc(bored, "Take it up with the anteater. I'm a bit busy right now.")
        chatPlayer(confused, "Well, that's not something you hear everyday...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Worm_Tongue (first conversation)
    // TODO(content): [system:met_flags] dialogue after the introduction (trade and lap counts)
    // TODO(content): [system:shops] no server inv data for Worm Tongue's Wares (Shop op)
    // TODO(content): [system:agility_course_stats] Stats op and lap counts
    private suspend fun Dialogue.wormTongue() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Hello there, my short-tongued friend. What can I do for you?")
        chatPlayer(quiz, "What are you doing here?")
        chatNpc(neutral, "What do you think I'm doing here?")
        chatPlayer(neutral, "I wouldn't like to guess.")
        chatNpc(
            neutral,
            "How disappointing. I enjoy a good guessing game! No matter. My colleague and I are " +
                "here are on a mission of preservation!",
        )
        chatPlayer(quiz, "Preservation of what?")
        chatNpc(
            neutral,
            "Preservation of these magnificent colossal wyrm remains, of course! You see, we " +
                "have a termite problem.",
        )
        chatPlayer(neutral, "I'm beginning to see your role in all of this.")
        chatNpc(
            neutral,
            "Indeed! And while my archaeological skills are certainly exemplary, I also bring a " +
                "certain set of more unique skills to this troublesome assignment.",
        )
        chatNpcSpecific(
            "Construction Worker",
            WORKER,
            angry,
            "Oh for goodness sake, he just eats the termites!",
        )
        chatNpc(shifty, "Ahem! I am able to dispose of the termites in ways others cannot.")
        chatPlayer(quiz, "I see. So can I help your preservation efforts?")
        chatNpc(
            happy,
            "Why yes! It seems our scaffolding has proven to be a useful place for adventurous " +
                "types to practice running and jumping in quick succession.",
        )
        chatNpc(
            neutral,
            "Should you also wish to practice these skills, you may come across the " +
                "aforementioned termites while you traverse the ropes. Simply scoop them up and " +
                "bring them to me.",
        )
        chatNpc(
            happy,
            "I'll happily exchange them for some goodies I've collected during my time here! You " +
                "can also keep any bone shards they may have unfortunately chipped off.",
        )
        chatPlayer(happy, "Sounds simple enough!")
        chatNpc(
            happy,
            "Wonderful! To get started, just use the ladder by the wyrm's skull to climb up onto " +
                "the scaffolding.",
        )
        chatNpc(
            neutral,
            "Initially the route is the same for everyone, but part way round you'll find a " +
                "loftier section of scaffolding. If you're skilled, that route might be more " +
                "appealing to you.",
        )
        chatNpc(
            neutral,
            "That said, don't go up there unless you know what you're doing. I have work to do " +
                "so I can't spend all my time patching people up when they hurt themselves!",
        )
        mesbox(
            "You'll need at least 50 Agility to tackle the basic course and 62 Agility to take " +
                "on the advanced course."
        )
        chatPlayer(happy, "Great, thanks!")
        chatNpc(happy, "Good luck!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Vibia
    // TODO(content): [quest:the_final_dawn] dialogue after the quest
    private suspend fun Dialogue.captainVibia() {
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "Do you need something?")
        chatPlayer(neutral, "Not that I know of.")
        chatNpc(bored, "Move along then. I have a lot of work to be doing.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fabia (before meeting Xilo)
    // TODO(content): [system:met_flags] reuniting Fabia with Xilo after talking to Xilo, and
    //  their dialogue once reunited
    private suspend fun Dialogue.fabia() {
        chatNpc(sad, "I can't belive Xilo hasn't shown up.")
        chatPlayer(quiz, "Were you expecting to meet someone here?")
        chatNpc(
            sad,
            "My... friend, Xilo. I told her to meet me by the magic trees of the rainforest. We " +
                "don't often have a chance to meet without a crowd of people around.",
        )
        chatPlayer(quiz, "Why? Is she not particularly local to you?")
        chatNpc(
            sad,
            "Unfortunately not. I'm from Aldarin, but she lives in Tal Teklan. It usually takes " +
                "a special event being hosted for us both to be in the same place.",
        )
        chatPlayer(quiz, "So you never really get any privacy?")
        chatNpc(angry, "Privacy? Are you insinuating something?")
        chatPlayer(worried, "Not at all. Sorry if it seemed like I was.")
        chatNpc(sad, "It's okay... Sorry... I just miss her.")
        chatPlayer(quiz, "Do you think she's just running a little late?")
        chatNpc(neutral, "It's possible.")
        chatPlayer(quiz, "Could she maybe have considered another tree magical?")
        chatNpc(
            sad,
            "I suppose she may be where we first met... but this is where we shared our first... " +
                "you know...",
        )
        chatPlayer(
            happy,
            "Well, I'll keep my fingers crossed for you. I'm sure you'll be able to find one " +
                "another soon!",
        )
        chatNpc(happy, "Thank you, iknami.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dave
    // https://oldschool.runescape.wiki/w/Transcript:Jane
    private suspend fun Dialogue.daveAndJane() {
        chatNpcSpecific("Dave", DAVE, happy, "Today is a good day to be a frog!")
        chatNpcSpecific(
            "Jane",
            JANE,
            happy,
            "It sure is. Now that we have peace, life is at an all time high!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gary
    // https://oldschool.runescape.wiki/w/Transcript:Sue
    private suspend fun Dialogue.garyAndSue() {
        chatNpcSpecific("Sue", SUE, happy, "Look who it is!")
        chatPlayer(happy, "Hello again. How are things going?")
        chatNpcSpecific(
            "Gary",
            GARY,
            happy,
            "Amazing! We get paid to eat all day, and our payment is more food. Life has never " +
                "been better!",
        )
        chatNpcSpecific(
            "Sue",
            SUE,
            happy,
            "Too right, Gary! This is much better than being on strike!",
        )
        chatPlayer(happy, "Glad to hear it.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Taala (initial dialogue)
    // TODO(content): [system:met_flags] subsequent dialogue and the chain warning before talking
    // TODO(content): [quest:perilous_moons] greeting for players who finished the quest
    private suspend fun Dialogue.taala() {
        chatPlayer(quiz, "Hello! Are you the leader of this expedition?")
        chatNpc(neutral, "Indeed I am, human! What's on your mind?")
        chatPlayer(quiz, "What's going on here?")
        chatNpc(neutral, "Well, the Hueycoatl has awoken.")
        chatPlayer(confused, "The Huey- what?")
        chatNpc(
            angry,
            "Huey-co-at... oh, I don't have time for this! The thing you need to know is that " +
                "it's dangerous and it's coiled throughout this entire mountain!",
        )
        chatPlayer(quiz, "Do you know where it came from?")
        chatNpc(
            neutral,
            "We're not sure. There are some records of our ancestors having dealings with it, " +
                "but the details have been lost, assuming they were ever recorded.",
        )
        chatPlayer(quiz, "So it's old?")
        chatNpc(angry, "Does it really matter how old it is?")
        chatPlayer(
            neutral,
            "I suppose not. I guess I just want to get a better understanding of what this thing " +
                "is.",
        )
        chatNpc(
            neutral,
            "We don't know how old it is. It could be extremely old, or this could be just one " +
                "in a lineage of them. Still, we haven't heard of any others so maybe it is some " +
                "sort of ancient creature.",
        )
        chatPlayer(quiz, "And given that you're here, I assume it poses some sort of threat?")
        chatNpc(
            neutral,
            "Now you're getting it! While it doesn't seem to be innately aggressive, our concern " +
                "is the potential risk of it tunnelling into Cam Torum.",
        )
        chatNpc(
            neutral,
            "Oh, and given that it does lash out from time to time, I suppose it does pose a " +
                "threat to your nearby human villages. A lesser priority for us, but maybe " +
                "something for you to care about.",
        )
        chatNpc(
            neutral,
            "Anyway, for now, our only option is to keep it in check while we figure out what to " +
                "do in the long term.",
        )
        chatNpc(
            bored,
            "Unfortunately, that means recruiting all the help we can get, even from you humans...",
        )
        chatPlayer(quiz, "How can I help?")
        chatNpc(
            neutral,
            "Well, despite what some of us may think of your kind, we need all the help we can " +
                "get.",
        )
        if (player.combatLevel < HUEYCOATL_COMBAT) {
            chatNpc(
                neutral,
                "However, I'm not sure you have enough combat experience to face the Hueycoatl.",
            )
            mesbox(
                "To face the Hueycoatl, it's recommended you have a combat level of 85 and a " +
                    "Prayer level of 43."
            )
            return
        }
        chatNpc(
            neutral,
            "We need to keep the Hueycoatl under control which, until we find a more permanent " +
                "solution, means wearing it out through combat.",
        )
        chatNpc(
            angry,
            "Don't go killing it, mind you! Not that I think you could, but such a magnificent " +
                "creature doesn't deserve destruction.",
        )
        chatPlayer(quiz, "Do you have any tips for surviving?")
        chatNpc(
            neutral,
            "We've found our prayers can protect us from its shards, so keep an eye out for those.",
        )
        chatNpc(
            neutral,
            "To reach the Hueycoatl's head, you'll need to get it to move its great body out of " +
                "the way. We've found we can persuade it by giving the exposed parts a helpful " +
                "nudge!",
        )
        chatNpc(
            neutral,
            "Once you reach its head, you'll need to work with your comrades and the seer to " +
                "balance your prayers and weaken the beast.",
        )
        chatNpc(happy, "But I'm sure you'll figure it out!")
        chatPlayer(
            happy,
            "Move the body, then attack the head while keeping prayers balanced. Got it!",
        )
    }

    private companion object {
        const val METLA = "npc.stonecutter_stonemason"
        const val ARCUANI = "npc.tal_teklan_archery_trader"
        const val XOCHITL = "npc.tal_teklan_dye_trader"
        const val SAWMILL = "npc.auburn_sawmill_operator"
        const val WORKER = "npc.varlamore_wyrm_agility_worker"
        const val DAVE = "npc.frog_quest_dave_named"
        const val JANE = "npc.frog_quest_jane_named"
        const val GARY = "npc.frog_quest_gary_named"
        const val SUE = "npc.frog_quest_sue_named"
        const val HUEYCOATL_COMBAT = 85

        val METLA_SHOP = "Stonecutter Supplies" to "inv.stonecutter_stonemason"
        val ARCUANI_SHOP = "Arcuani's Archery Supplies" to "inv.tal_teklan_archershop"
        val XOCHITL_SHOP = "Dyes to Die For" to "inv.tal_teklan_dyeshop"
        val SAWMILL_SHOP = "Construction supplies" to "inv.poh_sawmill_shop"
    }
}
