package org.rsmod.content.quest.area.asgarnia.icemountain

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.interfaces.emotes.PlayEmote
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.ScriptContext

class BelowIceMountain
@Inject
constructor(
    private val launcher: ProtectedAccessLauncher,
    private val cutscene: BimCutscene,
    private val guardian: BimGuardian,
    private val workout: BimWorkout,
    private val npcList: NpcList,
    private val rps: BimRps,
) :
    QuestScript(
        questKey = "quest_belowicemountain",
        questVarp = "varp.bim_main",
        rewards =
            rewards {
                extra("2,000 Coins")
                extra("Access to the Ruins of")
                extra("Camdozaal")
                extra("1,200 Mining XP")
            },
        completedQuestItemDisplay = ItemRewardDisplay("obj.bim_steak_sandwich", zoom = 400),
        questVarbit = "varbit.bim",
    ) {

    private var Player.questPoints by intVarp("varp.qp")

    override fun ScriptContext.init() {
        check(quest.maxSteps == BimStage.COMPLETE) {
            "Below Ice Mountain end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at ${BimStage.COMPLETE}."
        }
        onEvent<SessionStateEvent.PrepareLogin> { recover(player) }

        val quest = this@BelowIceMountain
        onOpNpc1("npc.bim_willow_outside") {
            startDialogue(it.npc) { willowOutside(quest) }
        }
        onOpNpc1("npc.bim_willow_outside_entrace") {
            startDialogue(it.npc) { willowEntrance(quest) }
            if (player.bimStage == BimStage.ENTRANCE_CUTSCENE) {
                cutscene.playEntrance(this, quest)
            }
        }
        onOpNpc1("npc.bim_willow_postquest") {
            startDialogue(it.npc) { willowPostquest(quest) }
        }
        onOpNpc1("npc.bim_checkal_barb") {
            startDialogue(it.npc) { checkalBarbarian(quest) }
        }
        onOpNpc1("npc.bim_checkal_temple_outside") {
            startDialogue(it.npc) { checkalEntrance(quest) }
        }
        onOpNpc1("npc.bim_checkal_postquest") {
            startDialogue(it.npc) { checkalPostquest(quest) }
        }
        onOpNpc1("npc.bim_atlas") {
            startDialogue(it.npc) { atlas(quest, workout) }
        }
        onOpNpc1("npc.bim_burntof_pub") {
            startDialogue(it.npc) { burntof(quest, rps) }
        }
        onOpNpc1("npc.bim_burntof_temple_outside") {
            startDialogue(it.npc) { burntofEntrance(quest) }
        }
        onOpNpc1("npc.bim_burntof_postquest") {
            startDialogue(it.npc) { burntofPostquest(quest) }
        }
        onOpNpc1("npc.bim_marley_edge") {
            startDialogue(it.npc) { marleyEdgeville(quest) }
        }
        onOpNpc1("npc.bim_marley_temple_outside") {
            startDialogue(it.npc) { marleyEntrance(quest) }
        }
        onOpNpc1("npc.bim_marley_postquest") {
            startDialogue(it.npc) { marleyPostquest(quest) }
        }
        onOpNpc1("npc.tramppg") {
            startDialogue(it.npc) { charlieTramp(quest) }
        }
        onOpNpc1("npc.fai_varrock_bluemoon_chef") {
            startDialogue(it.npc) { blueMoonCook(quest) }
        }
        onOpNpc1("npc.camdozaal_ramarno_entrance_multi") {
            startDialogue(it.npc) { ramarnoEntrance(quest) }
        }
        onOpNpc1("npc.camdozaal_ramarno_multi") {
            startDialogue(it.npc) { ramarnoWorkshop(quest) }
        }
        onOpNpcU("npc.camdozaal_ramarno_entrance_multi") {
            startDialogue { ramarnoItem() }
        }
        onOpNpcU("npc.camdozaal_ramarno_multi") {
            startDialogue { ramarnoItem() }
        }
        onEvent<PlayEmote> {
            if (
                player.bimCheckal == BimCheckal.FLEX_PROMPT &&
                    seq.id == FLEX_SEQ_ID &&
                    player.coords.chebyshevDistance(CHECKAL_TILE) <= 10
            ) {
                player.softTimer(BIM_FLEX_TIMER, cycles = 3)
            }
        }
        onPlayerSoftTimer(BIM_FLEX_TIMER) {
            if (player.bimCheckal != BimCheckal.FLEX_PROMPT) {
                return@onPlayerSoftTimer
            }
            val checkal =
                npcList.firstOrNull {
                    (it.isType(CHECKAL_BARB) || it.isType(CHECKAL)) &&
                        it.coords.chebyshevDistance(player.coords) <= 5
                } ?: return@onPlayerSoftTimer
            launcher.launch(player) {
                startDialogue(checkal) { checkalPostFlex(quest) }
            }
        }
    }

    override fun subTitle(): String =
        "helping <col=800000>Willow</col> and her crew gain entry to the " +
            "<col=800000>Ruins of Camdozaal</col>."

    override fun questLog(player: ProtectedAccess): String {
        val stage = stage(player.player)
        return buildString {
            if (stage >= BimStage.ALL_RECRUITED) {
                appendStruck(INTRO_LINES)
                appendLine(
                    "<col=000080>I spoke with <col=800000>Burntof<col=000080>, " +
                        "<col=800000>Checkal<col=000080> and <col=800000>Marley<col=000080>. " +
                        "It took some"
                )
                appendLine(
                    "<col=000080>convincing, but they've all agreed to work with " +
                        "<col=800000>Willow<col=000080> again. I"
                )
                appendLine(
                    "<col=000080>should meet up with them all at the entrance to the " +
                        "<col=800000>Ancient"
                )
                appendLine(
                    "<col=800000>Ruins<col=000080> on the west side of " +
                        "<col=800000>Ice Mountain<col=000080>."
                )
                return@buildString
            }

            appendBody(INTRO_LINES)
            appendLine()
            appendLine(burntofObjective(player.player))
            appendLine()
            appendLine(checkalObjective(player.player))
            appendLine()
            appendLines(marleyObjective(player.player))
        }
    }

    override fun completedLog(player: ProtectedAccess): String =
        buildString {
            appendStruck(
                listOf(
                    "I spoke to an archaeologist called Willow near Ice Mountain.",
                    "She asked me to help her find a way into some Ancient Ruins",
                    "beneath the mountain. To do this, she wanted me to gather",
                    "her old crew together.",
                    "I spoke with Burntof, Checkal and Marley. It took some",
                    "convincing, but they all agreed to work with Willow again.",
                    "With the help of her crew, Willow managed to gain entry into",
                    "the Ancient Ruins.",
                    "Inside the Ancient Ruins, Willow revealed herself to be a",
                    "graverobber rather than an archaeologist. She tried to kill me,",
                    "but was interrupted by an Ancient Guardian activating. Willow",
                    "and her crew fled, leaving me with the Ancient Guardian.",
                    "I managed to defeat the Ancient Guardian. With Willow gone, I",
                    "claimed my reward from a bag she left behind. Now that no",
                    "one else is trying to kill me, I can explore the Ancient Ruins at",
                    "my leisure.",
                )
            )
            appendLine()
            appendLine("<col=ff0000>QUEST COMPLETE!")
        }

    internal fun stage(player: Player): Int = quest.getQuestStage(player)

    internal fun hasStarted(player: Player): Boolean = stage(player) > BimStage.NOT_STARTED

    internal fun canRecruit(player: Player): Boolean =
        stage(player) in BimStage.RECRUITING until BimStage.ALL_RECRUITED

    internal fun hasAllRecruits(player: Player): Boolean =
        player.bimCheckal >= BimCheckal.RECRUITED &&
            player.bimMarley >= BimMarley.RECRUITED &&
            player.bimBurntof >= BimBurntof.RECRUITED

    internal fun markOneRecruited(player: Player) {
        if (player.bimStage == BimStage.RECRUITING) {
            player.bimStage = BimStage.ONE_RECRUITED
        }
    }

    internal fun markAllRecruited(player: Player) {
        if (player.bimStage == BimStage.RECRUITING || player.bimStage == BimStage.ONE_RECRUITED) {
            player.bimStage = BimStage.ALL_RECRUITED
        }
    }

    internal fun setStage(access: ProtectedAccess, stage: Int) {
        quest.setQuestStage(access, stage)
    }

    internal fun complete(access: ProtectedAccess, onCompleted: ProtectedAccess.() -> Unit = {}) {
        quest.completeQuest(
            access,
            onCompleted = {
                statAdvance("stat.mining", MINING_XP)
                onCompleted()
            },
        )
    }

    internal fun questPoints(player: Player): Int = player.questPoints

    private fun recover(player: Player) {
        val inInstance = isInInstance(player)
        cutscene.release(player)
        if (inInstance && player.bimStage >= BimStage.GUARDIAN_FIGHT) {
            guardian.releaseScene()
        }
        if (
            player.bimCheckal == BimCheckal.TRAINING &&
                player.bimStage < BimStage.ENTRANCE_CUTSCENE
        ) {
            player.bimCheckal = BimCheckal.SENT_TO_ATLAS
            player.coords = ATLAS_TILE
            return
        }
        // The workout uses a private copied region. A logout there leaves the player on
        // instance coordinates after the region has been released, so send them back to Atlas
        // and let them restart the training. The same guard covers any other interrupted scene.
        if (inInstance && player.bimStage < BimStage.ENTRANCE_CUTSCENE) {
            player.bimCheckal = BimCheckal.SENT_TO_ATLAS
            player.coords = ATLAS_TILE
            return
        }
        when (player.bimStage) {
            BimStage.ENTRANCE_CUTSCENE -> {
                player.bimStage = BimStage.ALL_RECRUITED
                player.coords = ENTRANCE_TILE
            }
            BimStage.INSIDE_CUTSCENE -> {
                player.bimStage = BimStage.GUARDIAN_FIGHT
                player.coords = GUARDIAN_ROOM_TILE
            }
            BimStage.GUARDIAN_FIGHT,
            BimStage.GUARDIAN_DEAD,
            -> if (inInstance) {
                player.coords = GUARDIAN_ROOM_TILE
            }
        }
    }

    private fun isInInstance(player: Player): Boolean =
        player.coords.x >= org.rsmod.api.registry.region.RegionRegistry.INSTANCE_MIN_X

    private fun StringBuilder.appendBody(lines: List<String>) {
        for (line in lines) {
            appendLine("<col=000080>$line")
        }
    }

    private fun StringBuilder.appendLines(lines: List<String>) {
        for (line in lines) {
            appendLine(line)
        }
    }

    private fun StringBuilder.appendStruck(lines: List<String>) {
        for (line in lines) {
            appendLine("<str>$line")
        }
    }

    private fun burntofObjective(player: Player): String =
        when {
            player.bimBurntof >= BimBurntof.RECRUITED -> "<str>I have recruited Burntof."
            player.bimBurntof >= BimBurntof.DRINK_REQUESTED ->
                "<col=000080>I need to recruit <col=800000>Burntof<col=000080> from the " +
                    "<col=800000>Rising Sun Inn<col=000080> in <col=800000>Falador<col=000080>.\n" +
                    "<col=000080>However, he wants me to bring him a " +
                    "<col=800000>Beer<col=000080> before he speaks to\n" +
                    "<col=000080>me."
            else ->
                "<col=000080>I need to recruit <col=800000>Burntof<col=000080>. I can find " +
                    "him in the <col=800000>Rising Sun Inn<col=000080> in\n" +
                    "<col=000080><col=800000>Falador<col=000080>."
        }

    private fun checkalObjective(player: Player): String =
        if (player.bimCheckal >= BimCheckal.RECRUITED) {
            "<str>I have recruited Checkal."
        } else {
            "<col=000080>I need to recruit <col=800000>Checkal<col=000080>. I can find him " +
                "in <col=800000>Barbarian Village<col=000080>."
        }

    private fun marleyObjective(player: Player): List<String> =
        when {
            player.bimMarley >= BimMarley.RECRUITED -> listOf("<str>I have recruited Marley.")
            player.bimMarley >= BimMarley.SANDWICH_REQUESTED ->
                listOf(
                    "<col=000080>I need to recruit <col=800000>Marley<col=000080> from the " +
                        "<col=800000>Edgeville Ruins<col=000080>. However, he",
                    "<col=000080>wants me to bring him a <col=800000>Steak Sandwich" +
                        "<col=000080> before he agrees to",
                    "<col=000080>help <col=800000>Willow<col=000080>. To make a " +
                        "<col=800000>Steak Sandwich<col=000080>, I'll need a " +
                        "<col=800000>Knife<col=000080>, some",
                    "<col=000080><col=800000>Bread<col=000080> and some " +
                        "<col=800000>Cooked Meat<col=000080>.",
                )
            else ->
                listOf(
                    "<col=000080>I need to recruit <col=800000>Marley<col=000080>. " +
                        "<col=800000>Willow<col=000080> doesn't know where he is but",
                    "<col=000080>suggested I ask <col=800000>Charlie<col=000080>, who can be " +
                        "found near the <col=800000>Blue Moon",
                    "<col=800000>Inn<col=000080> in <col=800000>Varrock<col=000080>.",
                )
        }

    private companion object {
        const val MINING_XP = 1_200.0
        val ATLAS_TILE = org.rsmod.map.CoordGrid(3075, 3439)
        val ENTRANCE_TILE = org.rsmod.map.CoordGrid(2996, 3496)
        val GUARDIAN_ROOM_TILE = org.rsmod.map.CoordGrid(2953, 5769)
        val CHECKAL_TILE = org.rsmod.map.CoordGrid(3087, 3415)
        val FLEX_SEQ_ID = "seq.emote_flex".asRSCM(RSCMType.SEQ)
        const val BIM_FLEX_TIMER = "timer.bim_flex"
        const val CHECKAL_BARB = "npc.bim_checkal_barb"
        const val CHECKAL = "npc.bim_checkal"

        val INTRO_LINES =
            listOf(
                "I spoke to an archaeologist called <col=800000>Willow<col=000080> near " +
                    "<col=800000>Ice Mountain<col=000080>.",
                "She asked me to help her find a way into some <col=800000>Ancient Ruins" +
                    "<col=000080>",
                "beneath the mountain. To do this, she wants me to gather her",
                "old crew together.",
            )
    }
}
