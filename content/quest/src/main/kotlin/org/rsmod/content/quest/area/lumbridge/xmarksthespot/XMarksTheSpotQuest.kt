package org.rsmod.content.quest.area.lumbridge.xmarksthespot

import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestJournalBuilder
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.chooseLampSkill
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class XMarksTheSpotQuest @Inject constructor(private val objRepo: ObjRepository) :
    QuestScript(
        "quest_xmarksthespot",
        "varp.cluequest_main",
        rewards {
            item("obj.coins", COIN_REWARD, label = "$COIN_REWARD Coins")
            item(LAMP, label = "An antique lamp")
            extra("A beginner scroll box")
        },
        ItemRewardDisplay(CASKET, zoom = 400),
        questVarbit = "varbit.cluequest",
    ),
    SpadeDigHook {

    override fun ScriptContext.init() {
        check(quest.maxSteps == STAGE_COMPLETE) {
            "X Marks the Spot end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $STAGE_COMPLETE."
        }

        onPlayerLogin { syncVeos(player) }

        onOpHeld1(BOBS_SCROLL) { readClue(BOBS_CLUE_TEXT) }
        onOpHeld1(MAP_SCROLL) { ifOpenMainModal("interface.cluequest_map") }
        onOpHeld1(ORB) { feelOrb() }
        onOpHeld1(CIPHER_SCROLL) { readClue(CIPHER_CLUE_TEXT) }
        onOpHeld1(CASKET) { startDialogue { chatPlayer(neutral, CASKET_WARNING) } }
        onOpHeld1(LAMP) { rubLamp() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Veos</col> in <col=800000>The Sheared Ram</col> pub in " +
            "<col=800000>Lumbridge</col>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            description(
                "<red>Veos</red>, a treasure hunter from the Kingdom of <red>Great Kourend</red>, " +
                    "has asked me to help him follow a treasure scroll he found."
            )
            objective(
                "I need to make some room in my inventory so that <red>Veos</red> can give me " +
                    "his treasure scroll."
            ) {
                visibleWhen { quest.getQuestStage(access.player) == STAGE_ACCEPTED }
            }
            step(
                STAGE_BOBS_CLUE,
                "The scroll describes a walk taken by a man named <red>Bob</red> in " +
                    "<red>Lumbridge</red>. I should retrace his steps and dig where he buried " +
                    "his treasure.",
            )
            step(
                STAGE_MAP_CLUE,
                "I dug up a scroll with a <red>map</red> on it. I should dig where the " +
                    "<red>X</red> marks the spot.",
            )
            step(
                STAGE_ORB_CLUE,
                "I dug up a <red>mysterious orb</red>. It grows hotter the closer I get to the " +
                    "next place to dig.",
            )
            step(
                STAGE_CIPHER_CLUE,
                "I dug up a scroll written in a strange <red>cipher</red>. Shifting the letters " +
                    "might reveal where to dig.",
            )
            step(
                STAGE_CASKET,
                "I dug up an <red>ancient casket</red>. I should take it to <red>Veos</red> at " +
                    "his ship on the northernmost pier in <red>Port Sarim</red>.",
            )
            objective("I gave Veos the casket. I should speak to him to collect my reward.") {
                visibleWhen { quest.getQuestStage(access.player) == STAGE_DELIVERED }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(
                "I helped Veos, a treasure hunter from Great Kourend, follow a treasure scroll " +
                    "from Lumbridge to Draynor Village."
            )
            line(
                "At the end of the trail I dug up an ancient casket and took it to Veos in Port " +
                    "Sarim, who rewarded me for my help."
            )
        }

    private fun QuestJournalBuilder.step(from: Int, text: String) =
        objective(text) {
            visibleWhen { quest.getQuestStage(access.player) >= from }
            stageAtLeast(from + 1, text).strike()
        }

    fun stage(player: Player): Int = quest.getQuestStage(player)

    fun syncVeos(player: Player) {
        val stage = stage(player)
        player.veosLumbridgeVis = if (stage >= STAGE_COMPLETE) 0 else VEOS_LUMBRIDGE_VISIBLE
        player.veosSarimVis =
            when {
                QuestRequirements.hasCompleted(player, QUEST_KEY) -> VEOS_SARIM_TRAVEL
                stage >= STAGE_ACCEPTED -> VEOS_SARIM_VISIBLE
                else -> 0
            }
    }

    fun ProtectedAccess.setStage(stage: Int) {
        quest.setQuestStage(this, stage)
        syncVeos(player)
    }

    override fun claims(player: Player): Boolean {
        val coords = player.coords
        return when (stage(player)) {
            STAGE_BOBS_CLUE -> coords == BOBS_SPOT
            STAGE_MAP_CLUE -> coords == CASTLE_SPOT
            STAGE_ORB_CLUE -> coords.level == 0 && orbDistance(coords) <= ORB_DIG_RADIUS
            STAGE_CIPHER_CLUE -> coords == PIG_PEN_SPOT
            STAGE_CASKET -> coords == PIG_PEN_SPOT && !player.owns(CASKET)
            else -> false
        }
    }

    override suspend fun ProtectedAccess.dig() {
        when (stage(player)) {
            STAGE_BOBS_CLUE ->
                findClue(BOBS_SCROLL, MAP_SCROLL, STAGE_MAP_CLUE, "You dig up another treasure scroll.")
            STAGE_MAP_CLUE ->
                findClue(MAP_SCROLL, ORB, STAGE_ORB_CLUE, "You dig up a mysterious orb.")
            STAGE_ORB_CLUE ->
                findClue(
                    ORB,
                    CIPHER_SCROLL,
                    STAGE_CIPHER_CLUE,
                    "You dig up another treasure scroll.",
                )
            STAGE_CIPHER_CLUE -> unearthCasket()
            STAGE_CASKET -> {
                invAddOrDrop(objRepo, CASKET)
                startDialogue { objbox(CASKET, "You dig up the ancient casket again.") }
            }
        }
    }

    private suspend fun ProtectedAccess.findClue(old: String, new: String, next: Int, text: String) {
        if (inv.count(old) > 0) {
            invDel(inv, old)
        }
        invAddOrDrop(objRepo, new)
        setStage(next)
        startDialogue { objbox(new, text) }
    }

    private suspend fun ProtectedAccess.unearthCasket() {
        if (inv.count(CIPHER_SCROLL) > 0) {
            invDel(inv, CIPHER_SCROLL)
        }
        invAddOrDrop(objRepo, CASKET)
        setStage(STAGE_CASKET)
        startDialogue {
            objbox(
                CASKET,
                "You dig up an ancient casket. As you do, you hear a faint whispering, but you " +
                    "can't make out the words...",
            )
            chatPlayer(confused, "Hmmmm... Must have been the wind.")
            chatPlayer(
                neutral,
                "Anyway, this must be the treasure Veos is after. I should take it to him. If I " +
                    "remember right, his ship is docked at the northernmost pier in Port Sarim.",
            )
        }
    }

    private fun ProtectedAccess.readClue(text: String) {
        ifOpenMainModal("interface.trail_cluetext")
        ifSetText("component.trail_cluetext:text", text)
    }

    private fun ProtectedAccess.feelOrb() {
        if (stage(player) != STAGE_ORB_CLUE) {
            mes("The orb is cold and lifeless.")
            return
        }
        val distance = if (player.coords.level == 0) orbDistance(player.coords) else ICE_COLD
        val temperature = ORB_TEMPERATURES.first { distance >= it.first }.second
        val previous = player.orbPreviousDistance - 1
        player.orbPreviousDistance = min(distance, MAX_STORED_DISTANCE - 1) + 1
        val comparison =
            when {
                distance <= ORB_DIG_RADIUS || previous < 0 -> ""
                distance < previous -> ", and warmer than last time"
                distance > previous -> ", and colder than last time"
                else -> ", and the same temperature as last time"
            }
        mes("The orb is $temperature$comparison.")
    }

    private suspend fun ProtectedAccess.rubLamp() {
        val stat = chooseLampSkill("Choose the stat you wish to be advanced!") ?: return
        if (invDel(inv, LAMP).failure) {
            return
        }
        player.lampUsed = true
        statAdvance(stat, LAMP_XP)
        mes("Your wish has been granted!")
    }

    private fun orbDistance(coords: CoordGrid): Int =
        max(abs(coords.x - ORB_SPOT.x), abs(coords.z - ORB_SPOT.z))

    companion object {
        const val STAGE_NOT_STARTED = 0
        const val STAGE_ACCEPTED = 1
        const val STAGE_BOBS_CLUE = 2
        const val STAGE_MAP_CLUE = 3
        const val STAGE_ORB_CLUE = 4
        const val STAGE_CIPHER_CLUE = 5
        const val STAGE_CASKET = 6
        const val STAGE_DELIVERED = 7
        const val STAGE_COMPLETE = 8

        const val BOBS_SCROLL = "obj.cluequest_clue1"
        const val MAP_SCROLL = "obj.cluequest_clue2"
        const val ORB = "obj.cluequest_clue3"
        const val CIPHER_SCROLL = "obj.cluequest_clue4"
        const val CASKET = "obj.cluequest_casket"
        const val LAMP = "obj.cluequest_lamp"
        const val SCROLL_BOX = "obj.league_clue_box_beginner"

        val CLUE_ITEMS =
            mapOf(
                STAGE_BOBS_CLUE to BOBS_SCROLL,
                STAGE_MAP_CLUE to MAP_SCROLL,
                STAGE_ORB_CLUE to ORB,
                STAGE_CIPHER_CLUE to CIPHER_SCROLL,
            )

        private const val QUEST_KEY = "quest_xmarksthespot"

        private const val COIN_REWARD = 200
        private const val LAMP_XP = 300.0

        private const val VEOS_LUMBRIDGE_VISIBLE = 2
        private const val VEOS_SARIM_VISIBLE = 3
        private const val VEOS_SARIM_TRAVEL = 4

        private val BOBS_SPOT = CoordGrid(3230, 3209, 0)
        private val CASTLE_SPOT = CoordGrid(3203, 3212, 0)
        private val ORB_SPOT = CoordGrid(3109, 3264, 0)
        private val PIG_PEN_SPOT = CoordGrid(3078, 3259, 0)

        private const val ORB_DIG_RADIUS = 3
        private const val ICE_COLD = 500
        private const val MAX_STORED_DISTANCE = 8191

        private val ORB_TEMPERATURES =
            listOf(
                ICE_COLD to "ice cold",
                200 to "very cold",
                150 to "cold",
                100 to "warm",
                70 to "hot",
                30 to "very hot",
                ORB_DIG_RADIUS + 1 to "incredibly hot",
                0 to "visibly shaking",
            )

        private const val BOBS_CLUE_TEXT =
            "A man named Bob lives in the town of Lumbridge. Leaving his house by the door, he " +
                "walks 1 step east, 7 steps north, 5 steps west and 1 step south. There he digs " +
                "a hole and buries his treasure."

        private const val CIPHER_CLUE_TEXT = "ESBZOPS QJH QFO"

        private const val CASKET_WARNING =
            "I don't think Veos would want me opening his treasure. I should take it to him. If " +
                "I remember right, his ship is docked at the northernmost pier in Port Sarim."
    }
}
