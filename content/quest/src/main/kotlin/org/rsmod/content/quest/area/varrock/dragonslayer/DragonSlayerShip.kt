package org.rsmod.content.quest.area.varrock.dragonslayer

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.CRANDOR_MAP
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.LOZAR_PIECE
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.MELZAR_PIECE
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.STAGE_COMPLETE
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.STAGE_CRANDOR
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.STAGE_NED_HIRED
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.STAGE_SHIP_BOUGHT
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.STAGE_SHIP_REPAIRED
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.STAGE_TASK
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.THALZAR_PIECE
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.askedNed
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.foundSecretDoor
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.nedKnowsShipFixed
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.shipFullyFixed
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.shipOneThird
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.shipTwoThirds
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The Lady Lumbridge: buying her, patching her hull, her crew and her captain. */
class DragonSlayerShip
@Inject
constructor(
    private val quest: DragonSlayerQuest,
    private val voyage: DragonSlayerVoyage,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(GANGPLANK_ON) { boardShip() }
        onOpLoc1(GANGPLANK_OFF) { leaveShip() }
        onOpLoc1(HULL_HOLE) { repairHull() }
        onOpLocU(HULL_HOLE, PLANK) { repairHull() }
        onOpNpc1(JENKINS) { startDialogue(it.npc) { talkToJenkins() } }
        onOpNpc1(NED_ON_SHIP) { startDialogue(it.npc) { nedOnShip() } }
        onOpNpc1(NED_ON_CRANDOR) { startDialogue(it.npc) { nedOnCrandor() } }
    }

    fun ownsShip(player: Player): Boolean =
        quest.stage(player) in STAGE_SHIP_BOUGHT until STAGE_COMPLETE

    private suspend fun ProtectedAccess.boardShip() {
        arriveDelay()
        if (!ownsShip(player)) {
            startDialogue {
                chatNpcSpecific(KLARENSE_NAME, KLARENSE, angry, "Hey, stay off my ship! That's private property!")
            }
            return
        }
        telejump(DECK)
    }

    private suspend fun ProtectedAccess.leaveShip() {
        arriveDelay()
        telejump(DOCK)
    }

    private suspend fun Dialogue.talkToJenkins() {
        if (quest.stage(player) == STAGE_NED_HIRED) crewBanter() else jenkinsMenu()
    }

    private suspend fun Dialogue.jenkinsMenu() {
        chatNpc(happy, "Ahoy! What d'ye think of yer ship then?")
        when (
            menu(
                "I'm ready to go back to shore." to JenkinsTopic.Shore,
                "I'd like to inspect her some more." to JenkinsTopic.Inspect,
                "Can you sail this ship to Crandor?" to JenkinsTopic.Sail,
            )
        ) {
            JenkinsTopic.Shore -> {
                chatPlayer(neutral, "I'm ready to go back on shore.")
                access.telejump(DOCK)
                mesbox("You disembark from the ship.")
            }
            JenkinsTopic.Inspect -> {
                chatPlayer(neutral, "I'd like to inspect her some more.")
                chatNpc(neutral, "Aye.")
            }
            JenkinsTopic.Sail -> {
                chatPlayer(quiz, "Can you sail this ship to Crandor?")
                chatNpc(
                    shocked,
                    "Not me, sir! I'm just an 'umble cabin boy. You'll need a proper cap'n.",
                )
                chatPlayer(quiz, "Where can I find a captain?")
                chatNpc(
                    neutral,
                    "The cap'ns round 'ere seem to be a mite scared of Crandor. I ask 'em why and " +
                        "they just say it was afore my time,",
                )
                chatNpc(
                    neutral,
                    "but there is one cap'n I reckon might 'elp. I 'eard there's a retired 'un " +
                        "who lives in Draynor Village who's so desperate to sail again 'e'd take " +
                        "any job.",
                )
                chatNpc(
                    neutral,
                    "I can't remember 'is name, but 'e lives in Draynor Village an' makes rope.",
                )
            }
        }
    }

    private suspend fun Dialogue.crewBanter() {
        val (ned, jenkins) = CREW_BANTER[access.random.of(CREW_BANTER.size)]
        chatNpcSpecific(CAPTAIN_NED, NED_ON_SHIP, happy, ned)
        chatNpc(happy, jenkins)
        if (ned == "Arr, Jim lad!") {
            chatNpcSpecific(CAPTAIN_NED, NED_ON_SHIP, happy, "Arr, Jenkins lad!")
        }
    }

    private suspend fun ProtectedAccess.repairHull() {
        arriveDelay()
        if (!ownsShip(player) || player.shipFullyFixed == 1) {
            mes("Nothing interesting happens.")
            return
        }
        if (player.foundSecretDoor == 1) {
            mes(
                "You don't need to mess about with broken ships now that you have found the " +
                    "secret passage from Karamja"
            )
            return
        }
        if (PLANK !in inv) {
            startDialogue { mesbox("You'll need to use wooden planks on this hole to patch it up.") }
            return
        }
        if (inv.count(NAILS) < NAILS_PER_PLANK) {
            startDialogue { mesbox("You need $NAILS_PER_PLANK steel nails to attach the plank with.") }
            return
        }
        if (HAMMER !in inv) {
            startDialogue { mesbox("You need a hammer to force the nails in with.") }
            return
        }
        if (!player.exchange(take = listOf(PLANK to 1, NAILS to NAILS_PER_PLANK))) {
            return
        }
        anim(HAMMER_SEQ)
        soundSynth(HAMMER_SOUND)
        val text =
            when {
                player.shipOneThird == 0 -> {
                    player.shipOneThird = 1
                    "You nail a plank over the hole, but you still need more planks to close the " +
                        "hole completely."
                }
                player.shipTwoThirds == 0 -> {
                    player.shipTwoThirds = 1
                    "You nail a plank over the hole, but you still need one more plank to close " +
                        "the hole completely."
                }
                else -> {
                    player.shipFullyFixed = 1
                    if (quest.stage(player) == STAGE_SHIP_BOUGHT) {
                        quest.setStage(this, STAGE_SHIP_REPAIRED)
                    }
                    "You nail a final plank over the hole. You have successfully patched the hole " +
                        "in the ship."
                }
            }
        startDialogue { mesbox(text) }
    }

    /** Klarense before the quest is over; Port Sarim keeps his post-quest lines. */
    suspend fun Dialogue.klarense() {
        val stage = quest.stage(player)
        when {
            stage >= STAGE_CRANDOR -> {
                chatNpc(
                    happy,
                    "Wow! You sure are lucky! Seems the Lady Lumbridge just washed right up into " +
                        "the dock by herself!",
                )
                chatNpc(neutral, "She's pretty badly damaged, though...")
                klarenseCaptainMenu(includeSail = false)
            }
            stage >= STAGE_SHIP_BOUGHT -> {
                chatNpc(
                    happy,
                    "Hello, captain! Here to inspect your new ship? Just a little work and she'll " +
                        "be seaworthy again.",
                )
                klarenseCaptainMenu(includeSail = true)
            }
            else -> klarenseSale(canBuy = stage == STAGE_TASK)
        }
    }

    private suspend fun Dialogue.klarenseSale(canBuy: Boolean) {
        chatNpc(
            happy,
            "Hello there! Interested in buying this fine ship? I'm sure I can offer her to you for " +
                "a much better price than any of those shipwrights ask for!",
        )
        chatPlayer(quiz, "A much better price? What's the catch?")
        chatNpc(neutral, "Well, she's not quite seaworthy right now...")
        chatPlayer(confused, "Not seaworthy? Why would I want to buy her then?")
        chatNpc(
            happy,
            "Because this isn't just any ship! This is the Lady Lumbridge, one of the last " +
                "Crandorian ships! Despite her size, she's one of the most agile ship around!",
        )
        chatPlayer(neutral, "Except she's not seaworthy...")
        chatNpc(happy, "No, but a bit of work will quickly sort that out!")
        while (true) {
            val options = buildList {
                if (canBuy) add("I'd like to buy her." to SaleTopic.Buy)
                add("Why is she damaged?" to SaleTopic.Damaged)
                add("I think I'll pass." to SaleTopic.Pass)
            }
            when (menu(options)) {
                SaleTopic.Buy -> {
                    buyShip()
                    return
                }
                SaleTopic.Damaged -> {
                    chatPlayer(quiz, "Why is she damaged?")
                    chatNpc(
                        neutral,
                        "Oh, there was no particular accident, just years of wear and tear. It's " +
                            "been a long time since Crandor was destroyed and she's had several " +
                            "owners since then. Unfortunately, not all of them looked after her.",
                    )
                }
                SaleTopic.Pass -> {
                    chatPlayer(neutral, "I think I'll pass.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.buyShip() {
        chatPlayer(happy, "I'd like to buy her.")
        chatNpc(
            happy,
            "Of course! I'm sure the work needed to do on it wouldn't be too expensive.",
        )
        chatNpc(
            happy,
            "How does 2,000 gold sound? I'll even throw in my cabin boy, Jenkins, for free! He'll " +
                "swab the decks and splice the mainsails for you!",
        )
        val accept =
            menu(
                "Yep, sounds good." to true,
                "I'm not paying that much for a broken boat!" to false,
            )
        if (!accept) {
            chatPlayer(angry, "I'm not paying that much for a broken boat!")
            chatNpc(neutral, "That's fair enough, I suppose.")
            return
        }
        chatPlayer(happy, "Yep, sounds good.")
        if (quest.stage(player) != STAGE_TASK) {
            return
        }
        if (!player.exchange(take = listOf(COINS to SHIP_PRICE))) {
            chatPlayer(sad, "...except I don't have that kind of money on me...")
            return
        }
        quest.setStage(access, STAGE_SHIP_BOUGHT)
        chatNpc(happy, "Okey-dokey! She's all yours!")
    }

    private suspend fun Dialogue.klarenseCaptainMenu(includeSail: Boolean) {
        while (true) {
            val options = buildList {
                add("So, what needs fixing on the ship?" to CaptainTopic.Fixing)
                if (includeSail) add("Would you sail this ship to Crandor for me?" to CaptainTopic.Sail)
                add("What are you going to do now you don't have a ship?" to CaptainTopic.Future)
                add("Can I board the ship now?" to CaptainTopic.Board)
            }
            when (menu(options)) {
                CaptainTopic.Fixing -> klarenseFixing()
                CaptainTopic.Sail -> klarenseSail()
                CaptainTopic.Future -> {
                    chatPlayer(quiz, "What are you going to do now you don't have a ship?")
                    chatNpc(happy, "Oh, I'll be fine. I've got work as Port Sarim's first lifeguard!")
                }
                CaptainTopic.Board -> {
                    chatPlayer(quiz, "Can I board the ship now?")
                    chatNpc(happy, "Sure thing, she's all yours.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.klarenseFixing() {
        if (player.shipFullyFixed == 0) {
            chatPlayer(quiz, "So, what needs fixing on this ship?")
            chatNpc(
                neutral,
                "Well, the big gaping hole in the hold is the problem. You'll need a few wooden " +
                    "planks hammered in with steel nails to fix it.",
            )
            return
        }
        chatNpc(neutral, "I dunno. It looks fine to me.")
        chatPlayer(confused, "But you said it needed fix-")
        chatNpc(happy, "Yes, but that was before you fixed it!")
    }

    private suspend fun Dialogue.klarenseSail() {
        chatPlayer(quiz, "So would you like to sail this ship to Crandor for me?")
        chatNpc(shocked, "Crandor? You're joking, right?")
        val joking = menu("Yes. Ha ha ha!" to true, "No. I want to go to Crandor." to false)
        if (joking) {
            chatPlayer(laugh, "Yes. Ha ha ha!")
            chatNpc(
                angry,
                "Crandor's not something we sailors joke about. You can't sail from here to " +
                    "Catherby, or Entrana, or Ardougne without going past that accursed island.",
            )
            chatNpc(neutral, "You can't get close to it because of the reefs, but you can always see it.")
            chatNpc(
                worried,
                "Sometimes you can see a dark shape in the sky, circling the island. That's when " +
                    "you have to sail on as quick as you can and pray it's not hungry. Every year, " +
                    "more ships are lost to that dragon.",
            )
            return
        }
        chatPlayer(neutral, "No. I want to go to Crandor.")
        chatNpc(shocked, "Then you must be crazy.")
        chatNpc(
            worried,
            "That island is surrounded by reefs that would rip this ship to shreds. Even if you " +
                "found a map, you'd need an experienced captain to stand a chance of getting through",
        )
        chatNpc(
            worried,
            "and, even if I could get to it, there's no way I'm going any closer to that dragon " +
                "than I have to. They say it can destroy whole ships with one bite.",
        )
    }

    /** Ned in Draynor while the quest is under way; Draynor keeps the rope shop and the rest. */
    fun nedHasQuestTopic(player: Player): Boolean =
        quest.stage(player) in STAGE_TASK..STAGE_NED_HIRED

    suspend fun Dialogue.nedQuestTopic() {
        if (quest.stage(player) == STAGE_NED_HIRED) {
            chatPlayer(quiz, "Will you take me to Crandor now, then?")
            chatNpc(
                happy,
                "I said I would and old Ned is a man of his word! I'll meet you on board the Lady " +
                    "Lumbridge in Port Sarim.",
            )
            return
        }
        if (player.askedNed == 0) {
            chatPlayer(quiz, "You're a sailor? Could you take me to the island of Crandor?")
            chatNpc(
                sad,
                "Well, I was a sailor. I've not been able to get work at sea these days, though. " +
                    "They say I'm too old.",
            )
            chatNpc(quiz, "Sorry, where was it you said you wanted to go?")
            chatPlayer(neutral, "To the island of Crandor.")
            chatNpc(shocked, "Crandor?")
            chatNpc(
                neutral,
                "But... It would be a chance to sail a ship once more. I'd sail anywhere if it was " +
                    "a chance to sail again.",
            )
            chatNpc(worried, "Then again, no captain in his right mind would sail to that island...")
            chatNpc(happy, "Ah, you only live once! I'll do it!")
            player.askedNed = 1
        } else {
            chatPlayer(quiz, "Will you take me to Crandor now, then?")
            chatNpc(happy, "I said I would and old Ned is a man of his word!")
        }
        chatNpc(quiz, "So, where's your ship?")
        if (quest.stage(player) < STAGE_SHIP_BOUGHT) {
            chatPlayer(sad, "Well, I don't actually have a ship yet.")
            chatNpc(neutral, "Ah, well. Let me know when you do. The place to look would be Port Sarim.")
            return
        }
        chatPlayer(neutral, "It's the Lady Lumbridge, in Port Sarim.")
        chatNpc(confused, "That old pile of junk? Last I heard, she wasn't seaworthy.")
        if (player.shipFullyFixed == 0) {
            chatPlayer(neutral, "Yes, good point. I'd better go and fix her up.")
            return
        }
        chatPlayer(happy, "I fixed her up!")
        player.nedKnowsShipFixed = 1
        chatNpc(happy, "You did? Excellent!")
        chatNpc(happy, "Just show me the map and we can get ready to go!")
        handOverMap()
    }

    private suspend fun Dialogue.handOverMap() {
        val hasMap = CRANDOR_MAP in player.inv
        val hasParts = listOf(MELZAR_PIECE, THALZAR_PIECE, LOZAR_PIECE).all { it in player.inv }
        if (!hasMap && !hasParts) {
            chatPlayer(worried, "Uh...yeah...about that. I don't actually have a map on me.")
            chatNpc(
                angry,
                "You'd better go find it, then. I'm not going to go sailing off without a map to " +
                    "show me where to head!",
            )
            return
        }
        chatPlayer(happy, "Here you go.")
        val handed =
            if (hasMap) {
                player.exchange(take = listOf(CRANDOR_MAP to 1))
            } else {
                player.exchange(take = listOf(MELZAR_PIECE to 1, THALZAR_PIECE to 1, LOZAR_PIECE to 1))
            }
        if (!handed) {
            return
        }
        quest.setStage(access, STAGE_NED_HIRED)
        objbox(CRANDOR_MAP, if (hasMap) "You hand the map to Ned." else "You hand the map parts to Ned.")
        chatNpc(happy, "Excellent! I'll meet you at the ship, then.")
    }

    private suspend fun Dialogue.nedOnShip() {
        val stage = quest.stage(player)
        chatNpc(
            happy,
            "Ah, it's good to be on board a ship again! No matter how long I live on land, a ship " +
                "will always seem better. Are you ready to depart?",
        )
        if (stage != STAGE_NED_HIRED && !(stage == STAGE_CRANDOR && player.shipFullyFixed == 1)) {
            return
        }
        val go = menu("Yes, let's go!" to true, "No, I'm not quite ready yet." to false)
        if (!go) {
            chatPlayer(neutral, "No, I'm not quite ready yet.")
            chatNpc(
                neutral,
                "Well, you go do whatever you need to do. I'll wait here for you until you're ready.",
            )
            return
        }
        chatPlayer(happy, "Yes, let's go!")
        voyage.sail(access)
    }

    private suspend fun Dialogue.nedOnCrandor() {
        chatNpc(
            sad,
            "Now I see why all the other captains said I'd be mad to go near Crandor. It looks " +
                "like we're stranded.",
        )
        if (player.foundSecretDoor == 0) {
            return
        }
        chatPlayer(
            happy,
            "Don't worry about that. I found a secret passage in the dragon's lair that leads " +
                "off the island!",
        )
        chatNpc(happy, "Oh, that's good. I'll be able to get back to Draynor somehow,")
        chatNpc(worried, "if I can sneak past all the skeletons and demons on this island!")
    }

    private enum class JenkinsTopic {
        Shore,
        Inspect,
        Sail,
    }

    private enum class SaleTopic {
        Buy,
        Damaged,
        Pass,
    }

    private enum class CaptainTopic {
        Fixing,
        Sail,
        Future,
        Board,
    }

    companion object {
        const val GANGPLANK_ON = "loc.dragonshipgangplank_on"
        const val GANGPLANK_OFF = "loc.dragonshipgangplank_off"
        const val HULL_HOLE = "loc.dragonslayer_shiphole"
        const val JENKINS = "npc.dragonslayer_jenkins_there"
        const val NED_ON_SHIP = "npc.dragonslayer_ned_on_ship"
        const val NED_ON_CRANDOR = "npc.dragonslayer_ned_on_crandor"
        const val KLARENSE = "npc.klarense"
        const val KLARENSE_NAME = "Klarense"
        const val CAPTAIN_NED = "Captain Ned"

        const val PLANK = "obj.woodplank"
        const val NAILS = "obj.nails"
        const val HAMMER = "obj.hammer"
        const val LOGS = "obj.logs"
        const val COINS = "obj.coins"
        const val NAILS_PER_PLANK = 30
        const val SHIP_PRICE = 2_000
        const val MAX_PLANKS = 9
        const val PLANK_PRICE = 100

        const val HAMMER_SEQ = "seq.human_hammer_hit"
        const val HAMMER_SOUND = "synth.hammering"
        val DECK = CoordGrid(3047, 3207, 1)
        val DOCK = CoordGrid(3047, 3204, 0)

        val CREW_BANTER =
            listOf(
                "Arr, Jim lad!" to "It's Jenkins, cap'n!",
                "Shiver me timbers!" to "Aye aye, cap'n!",
                "Splice the mainsail!" to "Aye aye, cap'n!",
                "Swab the deck!" to "Aye aye, cap'n!",
                "Weigh the anchor!" to "It's too heavy, cap'n!",
            )
    }
}
