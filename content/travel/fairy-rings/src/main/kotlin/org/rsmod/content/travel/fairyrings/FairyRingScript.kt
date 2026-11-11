package org.rsmod.content.travel.fairyrings

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.table.FairyringRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FairyRingScript @Inject constructor() : PluginScript() {
    private val rings: Map<Int, FairyringRow> = FairyringRow.all().associateBy { it.id }

    override fun ScriptContext.startup() {
        onOpLoc1(MINOR_HUB) { travel(rings.getValue(ZANARIS_RING)) }
        for (hub in HUBS) {
            onOpLoc2(hub) { configure() }
            onOpLoc3(hub) { lastDestination() }
        }
        DIAL_BUTTONS.forEachIndexed { index, (clockwise, anticlockwise) ->
            onIfModalButton(clockwise) { rotate(index, CLOCKWISE_STEP) }
            onIfModalButton(anticlockwise) { rotate(index, ANTICLOCKWISE_STEP) }
        }
        onIfModalButton(CONFIRM) { confirm() }
        for (ring in rings.values) {
            val button = RSCM.getReverseMapping(RSCMType.COMPONENT, ring.textComponent.packed)
            onIfModalButton(button) { useCode(ring.id) }
        }
    }

    private fun ProtectedAccess.canUse(): Boolean {
        val staff = player.worn[Wearpos.RightHand.slot]
        val wieldsStaff = STAFFS.any { staff.isType(it) }
        val atStafflessRing =
            rings.values.any {
                it.noStaffReturn == true &&
                    player.coords.chebyshevDistance(it.destCoord) <= RING_RADIUS
            }
        val permitted =
            QuestRequirements.hasCompleted(player, FAIRYTALE_1) &&
                (QuestRequirements.isOnQuest(player, FAIRYTALE_2) ||
                    QuestRequirements.hasCompleted(player, FAIRYTALE_2))
        if ((wieldsStaff || atStafflessRing) && permitted) {
            return true
        }
        mes(NO_FAIRY_MAGIC)
        return false
    }

    private fun ProtectedAccess.configure() {
        if (!canUse()) {
            return
        }
        for (dial in DIAL_VARBITS) {
            vars[dial] = 0
        }
        for (ring in rings.values) {
            val text = RSCM.getReverseMapping(RSCMType.COMPONENT, ring.textComponent.packed)
            ifSetText(text, logText(ring))
        }
        ifOpenMainSidePair(
            MAIN,
            SIDE,
            colour = BACKGROUND_COLOUR,
            transparency = BACKGROUND_TRANSPARENCY,
        )
    }

    private fun ProtectedAccess.logText(ring: FairyringRow): String {
        val visited = VISITED_VARBITS[ring.code]
        if (visited != null && vars[visited] == 0) {
            return ""
        }
        return ring.desc ?: ""
    }

    private fun ProtectedAccess.rotate(dial: Int, step: Int) {
        val varbit = DIAL_VARBITS[dial]
        vars[varbit] = (vars[varbit] + step) % DIAL_POSITIONS
        soundSynth(DIAL_SOUND)
    }

    private fun ProtectedAccess.useCode(id: Int) {
        vars[DIAL_VARBITS[0]] = id / 100
        vars[DIAL_VARBITS[1]] = id / 10 % 10
        vars[DIAL_VARBITS[2]] = id % 10
        soundSynth(DIAL_SOUND)
    }

    private suspend fun ProtectedAccess.confirm() {
        if (!canUse()) {
            ifClose()
            return
        }
        val first = vars[DIAL_VARBITS[0]]
        val second = vars[DIAL_VARBITS[1]]
        val third = vars[DIAL_VARBITS[2]]
        val id = first * 100 + second * 10 + third
        vars[DESTINATION_VARP] = id
        vars[LAST_LOCATION] = first * 16 + second * 4 + third
        vars[LAST_LOCATION_SET] = 1
        ifClose()
        delay(1)
        travel(rings.getValue(id))
    }

    private suspend fun ProtectedAccess.lastDestination() {
        if (vars[LAST_LOCATION_SET] == 0 || !canUse()) {
            return
        }
        val last = vars[LAST_LOCATION]
        val id = last / 16 * 100 + last / 4 % 4 * 10 + last % 4
        travel(rings.getValue(id))
    }

    private suspend fun ProtectedAccess.travel(ring: FairyringRow) {
        anim(VANISH_ANIM, delay = VANISH_ANIM_DELAY)
        spotanim(VANISH_SPOTANIM)
        soundSynth(VANISH_SOUND)
        delay(TELEPORT_DELAY)
        val destination = ring.destCoord
        val desc = ring.desc
        val unreachable =
            when {
                destination.x < MIN_VALID_COORD -> NO_SUITABLE_RING
                desc != null &&
                    desc.startsWith(VARLAMORE_PREFIX) &&
                    vars[VARLAMORE_VISITED] == 0 -> VARLAMORE_LOCKED
                else -> null
            }
        if (unreachable != null) {
            telejump(player.coords)
            anim(APPEAR_ANIM)
            mesbox(unreachable)
            return
        }
        telejump(destination)
        anim(APPEAR_ANIM)
        VISITED_VARBITS[ring.code]?.let { vars[it] = 1 }
    }

    private companion object {
        private const val MINOR_HUB = "loc.fairyring_minorhub"
        private val HUBS = listOf(MINOR_HUB, "loc.fairyring_homehub")

        private const val MAIN = "interface.fairyrings"
        private const val SIDE = "interface.fairyrings_log"
        private const val CONFIRM = "component.fairyrings:confirm"
        private const val BACKGROUND_COLOUR = 4212288
        private const val BACKGROUND_TRANSPARENCY = 50

        private val DIAL_BUTTONS =
            listOf(
                "component.fairyrings:1_clockwise" to "component.fairyrings:1_anticlockwise",
                "component.fairyrings:2_clockwise" to "component.fairyrings:2_anticlockwise",
                "component.fairyrings:3_clockwise" to "component.fairyrings:3_anticlockwise",
            )
        private val DIAL_VARBITS =
            listOf("varbit.fairyring_1", "varbit.fairyring_2", "varbit.fairyring_3")
        private const val DIAL_POSITIONS = 4
        private const val CLOCKWISE_STEP = 1
        private const val ANTICLOCKWISE_STEP = 3
        private const val DIAL_SOUND = "synth.ft_fairyrings"

        private const val DESTINATION_VARP = "varp.fairyring_destination1"
        private const val LAST_LOCATION = "varbit.fairyring_lastloc"
        private const val LAST_LOCATION_SET = "varbit.fairyring_lastloc_set"
        private const val VARLAMORE_VISITED = "varbit.varlamore_visited"

        private const val FAIRYTALE_1 = "quest_fairytale1"
        private const val FAIRYTALE_2 = "quest_fairytale2"
        private val STAFFS = listOf("obj.dramen_staff", "obj.lunar_moonclan_liminal_staff")
        private const val RING_RADIUS = 10
        private const val ZANARIS_RING = 321

        private const val VANISH_ANIM = "seq.human_fairy_vanish"
        private const val VANISH_ANIM_DELAY = 30
        private const val APPEAR_ANIM = "seq.human_fairy_appear"
        private const val VANISH_SPOTANIM = "spotanim.fairy_flower_ring"
        private const val VANISH_SOUND = "synth.ft_fairy_teleport"
        private const val TELEPORT_DELAY = 3
        private const val MIN_VALID_COORD = 64
        private const val VARLAMORE_PREFIX = "<br>Varlamore:"

        private const val NO_FAIRY_MAGIC =
            "The fairy ring only works for those who wield fairy magic."
        private const val NO_SUITABLE_RING = "No suitable ring could be found at the destination."
        private const val VARLAMORE_LOCKED =
            "You can't use the fairy ring to go there until you have visited Varlamore."

        private val VISITED_VARBITS: Map<String, String> =
            mapOf(
                "A I Q" to "varbit.fairyrings_log_aiq",
                "A I R" to "varbit.fairyrings_log_air",
                "A I S" to "varbit.fairyrings_log_ais",
                "A J P" to "varbit.fairyrings_log_ajp",
                "A J Q" to "varbit.fairyrings_log_ajq",
                "A J R" to "varbit.fairyrings_log_ajr",
                "A J S" to "varbit.fairyrings_log_ajs",
                "A K P" to "varbit.fairyrings_log_akp",
                "A K Q" to "varbit.fairyrings_log_akq",
                "A K R" to "varbit.fairyrings_log_akr",
                "A K S" to "varbit.fairyrings_log_aks",
                "A L P" to "varbit.fairyrings_log_alp",
                "A L Q" to "varbit.fairyrings_log_alq",
                "A L R" to "varbit.fairyrings_log_alr",
                "A L S" to "varbit.fairyrings_log_als",
                "B I P" to "varbit.fairyrings_log_bip",
                "B I Q" to "varbit.fairyrings_log_biq",
                "B I S" to "varbit.fairyrings_log_bis",
                "B J P" to "varbit.fairyrings_log_bjp",
                "B J R" to "varbit.fairyrings_log_bjr",
                "B J S" to "varbit.fairyrings_log_bjs",
                "B K P" to "varbit.fairyrings_log_bkp",
                "B K Q" to "varbit.fairyrings_log_bkq",
                "B K R" to "varbit.fairyrings_log_bkr",
                "B K S" to "varbit.fairyrings_log_bks",
                "B L P" to "varbit.fairyrings_log_blp",
                "B L Q" to "varbit.fairyrings_log_blq",
                "B L R" to "varbit.fairyrings_log_blr",
                "B L S" to "varbit.fairyrings_log_bls",
                "C I P" to "varbit.fairyrings_log_cip",
                "C I Q" to "varbit.fairyrings_log_ciq",
                "C I R" to "varbit.fairyrings_log_cir",
                "C I S" to "varbit.fairyrings_log_cis",
                "C J Q" to "varbit.fairyrings_log_cjq",
                "C J R" to "varbit.fairyrings_log_cjr",
                "C K P" to "varbit.fairyrings_log_ckp",
                "C K Q" to "varbit.fairyrings_log_ckq",
                "C K R" to "varbit.fairyrings_log_ckr",
                "C K S" to "varbit.fairyrings_log_cks",
                "C L P" to "varbit.fairyrings_log_clp",
                "C L R" to "varbit.fairyrings_log_clr",
                "C L S" to "varbit.fairyrings_log_cls",
                "D I P" to "varbit.fairyrings_log_dip",
                "D I Q" to "varbit.fairyrings_log_diq",
                "D I R" to "varbit.fairyrings_log_dir",
                "D I S" to "varbit.fairyrings_log_dis",
                "D J P" to "varbit.fairyrings_log_djp",
                "D J R" to "varbit.fairyrings_log_djr",
                "D K P" to "varbit.fairyrings_log_dkp",
                "D K R" to "varbit.fairyrings_log_dkr",
                "D K S" to "varbit.fairyrings_log_dks",
                "D L P" to "varbit.fairyrings_log_dlp",
                "D L Q" to "varbit.fairyrings_log_dlq",
                "D L R" to "varbit.fairyrings_log_dlr",
                "D L S" to "varbit.fairyrings_log_dls",
            )
    }
}
