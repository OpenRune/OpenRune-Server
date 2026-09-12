@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.areas.city.alkharid

import jakarta.inject.Inject
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.city.alkharid.quests.PrinceAliRescue
import org.rsmod.content.generic.locs.gate.GateTranslations.leftGateOpen
import org.rsmod.content.generic.locs.gate.GateTranslations.leftGateRightPair
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AlKharidTollGate
@Inject
constructor(
    private val princeAliRescue: PrinceAliRescue,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(GATE_DEFAULT_LEFT) { useGate() }
        onOpLoc1(GATE_DEFAULT_RIGHT) { useGate() }
        onOpLoc4(GATE_DEFAULT_LEFT) { payToll() }
        onOpLoc4(GATE_DEFAULT_RIGHT) { payToll() }

        onOpLoc1(GATE_LEFT_2OP) { useGate() }
        onOpLoc1(GATE_RIGHT_2OP) { useGate() }

        onOpLoc1(GATE_LEFT_1OP) { useGate() }
        onOpLoc1(GATE_RIGHT_1OP) { useGate() }

        onOpNpc1(BORDER_GUARD_1) { talkToGuard(it.npc) }
        onOpNpc1(BORDER_GUARD_2) { talkToGuard(it.npc) }
    }

    private fun ProtectedAccess.isQuestComplete(): Boolean =
        princeAliRescue.quest.isQuestCompleted(player)

    private fun ProtectedAccess.onAlKharidSide(): Boolean = coords.x >= AL_KHARID_BORDER_X

    private fun ProtectedAccess.isFreeToPass(): Boolean = onAlKharidSide() || isQuestComplete()

    private suspend fun ProtectedAccess.useGate() {
        if (isFreeToPass()) {
            passThroughGate()
            return
        }
        mes("You need to pay 10 gold to pass through the gate.")
        val option = choice2("Pay 10 gold.", OPT_PAY, "No thanks.", OPT_DECLINE)
        when (option) {
            OPT_PAY -> payToll()
            OPT_DECLINE -> mes("You decide not to pass through the gate.")
        }
    }

    private suspend fun ProtectedAccess.payToll() {
        if (isFreeToPass()) {
            passThroughGate()
            return
        }
        if (!player.invTakeFee(fee = TOLL_FEE)) {
            mes("You do not have enough coins to pay the toll.")
            return
        }
        mes("You pay the guard 10 gold.")
        passThroughGate()
    }

    private suspend fun ProtectedAccess.passThroughGate() {
        val panels = findGatePanels()
        if (panels == null) {
            telejump(fallbackDestination())
            return
        }
        val crossing = crossing(panels)

        if (coords != crossing.approach) {
            playerWalk(crossing.approach)
            if (!awaitArrival(crossing.approach)) {
                return
            }
        }

        val (left, right) = panels
        val leaves =
            listOf(
                openPanel(left, LEFT_SWING_ROTATIONS),
                openPanel(right, RIGHT_SWING_ROTATIONS),
            )
        soundSynth(SOUND_OPEN)

        playerWalkThroughCollision(crossing.destination)
        awaitArrival(crossing.destination)
        delay(CLOSE_DELAY)

        for (leaf in leaves) {
            worldRepo.locHide(leaf.coords, leaf.shape, leaf.angle)
        }
        worldRepo.locShow(left.loc)
        worldRepo.locShow(right.loc)
        soundSynth(SOUND_CLOSE)
    }

    private suspend fun ProtectedAccess.awaitArrival(dest: CoordGrid): Boolean {
        for (cycle in 0 until ARRIVAL_TIMEOUT) {
            if (coords == dest) {
                return true
            }
            delay(1)
        }
        return coords == dest
    }

    private fun ProtectedAccess.crossing(panels: Pair<GatePanel, GatePanel>): Crossing {
        val nearest =
            listOf(panels.first, panels.second).minBy { coords.chebyshevDistance(it.loc.coords) }.loc
        val gateside = nearest.coords
        val across = gateside + leftGateOpen(nearest.shape, nearest.angle)

        val offsetX = (coords.x - gateside.x) * (across.x - gateside.x)
        val offsetZ = (coords.z - gateside.z) * (across.z - gateside.z)
        val onAcrossSide = offsetX + offsetZ > 0

        return if (onAcrossSide) {
            Crossing(approach = across, destination = gateside)
        } else {
            Crossing(approach = gateside, destination = across)
        }
    }

    private fun openPanel(panel: GatePanel, rotations: Int): OpenLeaf {
        val loc = panel.loc
        val leaf =
            OpenLeaf(
                coords = loc.coords + leftGateOpen(loc.shape, loc.angle),
                shape = loc.shape,
                angle = loc.turnAngle(rotations = rotations),
            )
        worldRepo.locHide(loc)
        worldRepo.locShow(leaf.coords, panel.openLoc, leaf.shape, leaf.angle)
        return leaf
    }

    private fun ProtectedAccess.fallbackDestination(): CoordGrid {
        val exitX = if (onAlKharidSide()) WEST_EXIT_X else EAST_EXIT_X
        val exitZ = coords.z.coerceIn(GATE_SOUTH_Z, GATE_NORTH_Z)
        return CoordGrid(exitX, exitZ, coords.level)
    }

    private fun ProtectedAccess.findGatePanels(): Pair<GatePanel, GatePanel>? {
        var first: GatePanel? = null
        var second: GatePanel? = null
        for (deltaX in -SEARCH_RADIUS..SEARCH_RADIUS) {
            for (deltaZ in -SEARCH_RADIUS..SEARCH_RADIUS) {
                val search = coords.translate(deltaX, deltaZ)
                val loc = locRepo.findExact(search, LocShape.WallStraight) ?: continue
                val panel = gatePanelAt(search, loc) ?: continue
                if (first == null) {
                    first = panel
                } else if (second == null) {
                    second = panel
                }
            }
        }
        val a = first ?: return null
        val b = second ?: return null
        val pair = leftGateRightPair(a.loc.shape, a.loc.angle)
        return if (a.loc.coords + pair == b.loc.coords) a to b else b to a
    }

    private fun gatePanelAt(coords: CoordGrid, loc: LocInfo): GatePanel? {
        val openLoc =
            GATE_PANELS.entries.firstOrNull { locRepo.findLoc(coords, it.key) }?.value ?: return null
        return GatePanel(loc, openLoc)
    }

    private data class GatePanel(val loc: LocInfo, val openLoc: String)

    private data class OpenLeaf(val coords: CoordGrid, val shape: LocShape, val angle: LocAngle)

    private data class Crossing(val approach: CoordGrid, val destination: CoordGrid)

    private suspend fun ProtectedAccess.talkToGuard(npc: Npc) {
        if (isQuestComplete()) {
            startDialogue(npc) {
                chatNpc(happy, "You are a friend of Al Kharid. Pass through freely.")
            }
            passThroughGate()
            return
        }
        if (onAlKharidSide()) {
            startDialogue(npc) { chatNpc(neutral, "You may pass.") }
            passThroughGate()
            return
        }
        startDialogue(npc) {
            chatNpc(
                neutral,
                "Halt! You must pay a toll of 10 gold coins to pass through this gate.",
            )
            val option =
                choice2(
                    "Okay, I will pay.",
                    OPT_PAY,
                    "No thanks, I will find another way.",
                    OPT_DECLINE,
                )
            when (option) {
                OPT_PAY -> {
                    if (!access.player.invTakeFee(fee = TOLL_FEE)) {
                        chatPlayer(sad, "Oh dear, I do not seem to have enough money.")
                    } else {
                        chatPlayer(happy, "Here you go.")
                        chatNpc(happy, "Thank you, you may pass.")
                        access.passThroughGate()
                    }
                }
                OPT_DECLINE -> {
                    chatPlayer(neutral, "No thanks, I will find another way.")
                    chatNpc(neutral, "Suit yourself.")
                }
            }
        }
    }

    public companion object {
        public const val TOLL_FEE: Int = 10

        public const val GATE_DEFAULT_LEFT: String = "loc.kharidmetalgateclosedl"
        public const val GATE_DEFAULT_RIGHT: String = "loc.kharidmetalgateclosedr"
        public const val GATE_LEFT_2OP: String = "loc.kharidmetalgateclosedl_2op"
        public const val GATE_RIGHT_2OP: String = "loc.kharidmetalgateclosedr_2op"
        public const val GATE_LEFT_1OP: String = "loc.kharidmetalgateclosedl_1op"
        public const val GATE_RIGHT_1OP: String = "loc.kharidmetalgateclosedr_1op"
        public const val BORDER_GUARD_1: String = "npc.borderguard1"
        public const val BORDER_GUARD_2: String = "npc.borderguard2"

        private const val GATE_OPEN_LEFT: String = "loc.inacmetalgateopenl"
        private const val GATE_OPEN_RIGHT: String = "loc.inacmetalgateopenr"
        private const val SOUND_OPEN: String = "synth.door_open"
        private const val SOUND_CLOSE: String = "synth.door_close"

        private val GATE_PANELS: Map<String, String> =
            mapOf(
                GATE_DEFAULT_LEFT to GATE_OPEN_LEFT,
                GATE_LEFT_1OP to GATE_OPEN_LEFT,
                GATE_LEFT_2OP to GATE_OPEN_LEFT,
                GATE_DEFAULT_RIGHT to GATE_OPEN_RIGHT,
                GATE_RIGHT_1OP to GATE_OPEN_RIGHT,
                GATE_RIGHT_2OP to GATE_OPEN_RIGHT,
            )

        private const val AL_KHARID_BORDER_X: Int = 3268
        private const val WEST_EXIT_X: Int = 3266
        private const val EAST_EXIT_X: Int = 3269
        private const val GATE_SOUTH_Z: Int = 3227
        private const val GATE_NORTH_Z: Int = 3228

        private const val LEFT_SWING_ROTATIONS: Int = 3
        private const val RIGHT_SWING_ROTATIONS: Int = 1

        private const val SEARCH_RADIUS: Int = 3
        private const val ARRIVAL_TIMEOUT: Int = 10
        private const val CLOSE_DELAY: Int = 1

        private const val OPT_PAY: Int = 1
        private const val OPT_DECLINE: Int = 2
    }
}
