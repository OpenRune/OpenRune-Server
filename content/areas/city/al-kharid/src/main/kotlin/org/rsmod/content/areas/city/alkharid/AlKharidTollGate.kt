@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.areas.city.alkharid

import jakarta.inject.Inject
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.city.alkharid.quests.PrinceAliRescue
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Al-Kharid toll gate — Open / Pay-toll(10gp) on the Lumbridge↔Al-Kharid gate pair.
 *
 * Free passage is a Prince Ali Rescue completion reward, read through the quest engine
 * ([PrinceAliRescue.quest]) rather than off the raw `varp.princequest` value.
 */
class AlKharidTollGate @Inject constructor(private val princeAliRescue: PrinceAliRescue) :
    PluginScript() {
    override fun ScriptContext.startup() {
        // Default gate (from map data): Open=op1, Pay-toll(10gp)=op4.
        onOpLoc1(GATE_DEFAULT_LEFT) { openGate() }
        onOpLoc1(GATE_DEFAULT_RIGHT) { openGate() }
        onOpLoc4(GATE_DEFAULT_LEFT) { payToll() }
        onOpLoc4(GATE_DEFAULT_RIGHT) { payToll() }

        // 2op variant: Open=op1, Pay-toll(10gp)=op2.
        onOpLoc1(GATE_LEFT_2OP) { openGate() }
        onOpLoc1(GATE_RIGHT_2OP) { openGate() }

        // 1op variant (after quest): Open=op1.
        onOpLoc1(GATE_LEFT_1OP) { openGateAfterQuest() }
        onOpLoc1(GATE_RIGHT_1OP) { openGateAfterQuest() }

        // Border guard dialogue.
        onOpNpc1(BORDER_GUARD_1) { talkToGuard(it.npc) }
        onOpNpc1(BORDER_GUARD_2) { talkToGuard(it.npc) }
    }

    private fun ProtectedAccess.isQuestComplete(): Boolean =
        princeAliRescue.quest.isQuestCompleted(player)

    private fun ProtectedAccess.getDestination(): CoordGrid =
        if (player.coords.x >= GATE_PIVOT_X) EAST_SIDE_LANDING else WEST_SIDE_LANDING

    private suspend fun ProtectedAccess.openGate() {
        if (isQuestComplete()) {
            openGateAfterQuest()
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
        if (isQuestComplete()) {
            openGateAfterQuest()
            return
        }
        if (!player.invTakeFee(fee = TOLL_FEE)) {
            mes("You do not have enough coins to pay the toll.")
            return
        }
        mes("You pay the guard 10 gold.")
        telejump(getDestination())
    }

    private fun ProtectedAccess.openGateAfterQuest() {
        telejump(getDestination())
    }

    private suspend fun ProtectedAccess.talkToGuard(npc: Npc) {
        if (isQuestComplete()) {
            startDialogue(npc) {
                chatNpc(happy, "You are a friend of Al Kharid. Pass through freely.")
            }
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
                        access.telejump(access.getDestination())
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
        /** OSRS canon: the Al-Kharid gate toll is 10gp. */
        public const val TOLL_FEE: Int = 10

        public const val GATE_DEFAULT_LEFT: String = "loc.kharidmetalgateclosedl"
        public const val GATE_DEFAULT_RIGHT: String = "loc.kharidmetalgateclosedr"
        public const val GATE_LEFT_2OP: String = "loc.kharidmetalgateclosedl_2op"
        public const val GATE_RIGHT_2OP: String = "loc.kharidmetalgateclosedr_2op"
        public const val GATE_LEFT_1OP: String = "loc.kharidmetalgateclosedl_1op"
        public const val GATE_RIGHT_1OP: String = "loc.kharidmetalgateclosedr_1op"
        public const val BORDER_GUARD_1: String = "npc.borderguard1"
        public const val BORDER_GUARD_2: String = "npc.borderguard2"

        /** Absolute x of the gate line: east of it means the player is on the Al-Kharid side. */
        private const val GATE_PIVOT_X: Int = 3268

        private val WEST_SIDE_LANDING: CoordGrid = CoordGrid(0, 51, 50, 5, 28)
        private val EAST_SIDE_LANDING: CoordGrid = CoordGrid(0, 51, 50, 2, 28)

        private const val OPT_PAY: Int = 1
        private const val OPT_DECLINE: Int = 2
    }
}
