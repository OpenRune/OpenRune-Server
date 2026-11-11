package org.rsmod.content.travel.fairyrings

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.enums.NamedEnums.fairyring_visited_varbits
import org.rsmod.api.player.output.ClientScripts.mesLayerClose
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.table.FairyringRequirementsRow
import org.rsmod.api.table.FairyringRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FairyRingScript @Inject constructor() : PluginScript() {
    private val rings: Map<Int, FairyringRow> = FairyringRow.all().associateBy { it.id }
    private val requirements: Map<Int, FairyringRequirementsRow> =
        FairyringRequirementsRow.all().associateBy { it.ring }
    private val textButtons: Map<FairyringRow, String> =
        rings.values.associateWith {
            RSCM.getReverseMapping(RSCMType.COMPONENT, it.textComponent.packed)
        }

    override fun ScriptContext.startup() {
        onOpLoc1(MINOR_HUB) { zanaris() }
        for (hub in HUBS) {
            onOpLoc2(hub) { configure() }
            onOpLoc3(hub) { lastDestination() }
        }
        DIAL_BUTTONS.forEachIndexed { index, (clockwise, anticlockwise) ->
            onIfModalButton(clockwise) { rotate(index, CLOCKWISE_STEP) }
            onIfModalButton(anticlockwise) { rotate(index, ANTICLOCKWISE_STEP) }
        }
        onIfModalButton("component.fairyrings:confirm") { confirm() }
        onIfClose(MAIN) { mesLayerClose(player, SEARCH_LAYER_MODE) }
        for ((ring, button) in textButtons) {
            onIfModalButton(button) {
                if (it.op == IfButtonOp.Op2) addFavourite(ring.id) else useCode(ring.id)
            }
            val icon = RSCM.getReverseMapping(RSCMType.COMPONENT, ring.faveIconComponent.packed)
            onIfModalButton(icon) { addFavourite(ring.id) }
        }
        FAVOURITE_TEXTS.forEachIndexed { slot, button ->
            onIfModalButton(button) {
                val id = favourites().getOrNull(slot) ?: return@onIfModalButton
                if (it.op == IfButtonOp.Op2) removeFavourite(id) else useCode(id)
            }
        }
    }

    private fun ProtectedAccess.favourites(): List<Int> =
        FAVOURITE_VARBITS.map { vars[it] }
            .filter { it >= FAVOURITE_SET }
            .map { indexToId(it - FAVOURITE_SET) }

    private fun ProtectedAccess.storeFavourites(ids: List<Int>) {
        FAVOURITE_VARBITS.forEachIndexed { slot, varbit ->
            val id = ids.getOrNull(slot)
            vars[varbit] = if (id == null) 0 else FAVOURITE_SET + idToIndex(id)
        }
        refreshFavourites(ids)
    }

    private fun ProtectedAccess.refreshFavourites(ids: List<Int> = favourites()) {
        FAVOURITE_TEXTS.forEachIndexed { slot, text ->
            val ring = ids.getOrNull(slot)?.let { rings[it] }
            ifSetText(text, ring?.desc ?: "")
            ifSetText(FAVOURITE_CODES[slot], ring?.code ?: "")
        }
    }

    private fun ProtectedAccess.addFavourite(id: Int) {
        val current = favourites()
        if (id in current) {
            return
        }
        if (current.size >= FAVOURITE_VARBITS.size) {
            mes("Your favourites list is full.")
            return
        }
        storeFavourites(current + id)
    }

    private fun ProtectedAccess.removeFavourite(id: Int) {
        storeFavourites(favourites() - id)
    }

    private fun dials(id: Int): List<Int> = listOf(id / 100, id / 10 % 10, id % 10)

    private fun dialsToId(dials: List<Int>): Int = dials[0] * 100 + dials[1] * 10 + dials[2]

    private fun idToIndex(id: Int): Int = dials(id).let { (a, b, c) -> a * 16 + b * 4 + c }

    private fun indexToId(index: Int): Int = dialsToId(listOf(index / 16, index / 4 % 4, index % 4))

    private fun ProtectedAccess.canUse(): Boolean {
        if (!hasPermission()) {
            // UNVERIFIED: the real message for using a ring without the Godfather's permission.
            mes("You don't have permission to use the fairy rings.")
            return false
        }
        if (!hasFairyMagic()) {
            mes("The fairy ring only works for those who wield fairy magic.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.hasPermission(): Boolean {
        if (vars["varbit.fairyring_use"] >= 1 && vars["varbit.fairyring_permission"] >= 2) {
            return true
        }
        return QuestRequirements.hasCompleted(player, "quest_fairytale1") &&
            (QuestRequirements.isOnQuest(player, FAIRYTALE_2) ||
                QuestRequirements.hasCompleted(player, FAIRYTALE_2))
    }

    private fun ProtectedAccess.hasFairyMagic(): Boolean {
        val staff = player.worn[Wearpos.RightHand.slot]
        return vars["varbit.lumbridge_diary_elite_complete"] != 0 ||
            STAFFS.any { staff.isType(it) } ||
            rings.values.any {
                it.noStaffReturn == true &&
                    player.coords.chebyshevDistance(it.destCoord) <= RING_RADIUS
            }
    }

    private suspend fun ProtectedAccess.zanaris() {
        if (canUse()) {
            travel(rings.getValue(ZANARIS_RING))
        }
    }

    private fun ProtectedAccess.configure() {
        if (!canUse()) {
            return
        }
        for (dial in DIAL_VARBITS) {
            vars[dial] = 0
        }
        for ((ring, button) in textButtons) {
            ifSetText(button, if (visited(ring)) ring.desc ?: "" else "")
        }
        refreshFavourites()
        ifOpenMainSidePair(
            MAIN,
            "interface.fairyrings_log",
            colour = BACKGROUND_COLOUR,
            transparency = BACKGROUND_TRANSPARENCY,
        )
    }

    private fun ProtectedAccess.visited(ring: FairyringRow): Boolean {
        val varbit = visitedVarbit(ring) ?: return true
        return player.vars[varbit] != 0
    }

    private fun visitedVarbit(ring: FairyringRow) =
        fairyring_visited_varbits[ring.id]?.let { ServerCacheManager.getVarbit(it) }

    private fun ProtectedAccess.rotate(dial: Int, step: Int) {
        val varbit = DIAL_VARBITS[dial]
        vars[varbit] = (vars[varbit] + step) % DIAL_POSITIONS
        soundSynth(DIAL_SOUND)
    }

    private fun ProtectedAccess.useCode(id: Int) {
        dials(id).forEachIndexed { index, position -> vars[DIAL_VARBITS[index]] = position }
        soundSynth(DIAL_SOUND)
    }

    private suspend fun ProtectedAccess.confirm() {
        if (!canUse()) {
            ifClose()
            return
        }
        val id = dialsToId(DIAL_VARBITS.map { vars[it] })
        vars["varp.fairyring_destination1"] = id
        vars[LAST_LOCATION] = idToIndex(id)
        vars[LAST_LOCATION_SET] = 1
        ifClose()
        delay(1)
        travel(rings[id])
    }

    private suspend fun ProtectedAccess.lastDestination() {
        if (vars[LAST_LOCATION_SET] == 0 || !canUse()) {
            return
        }
        travel(rings[indexToId(vars[LAST_LOCATION])])
    }

    private suspend fun ProtectedAccess.travel(ring: FairyringRow?) {
        val here =
            rings.values.firstOrNull {
                player.coords.chebyshevDistance(it.destCoord) <= STANDING_RADIUS
            }
        anim("seq.human_fairy_vanish", delay = VANISH_ANIM_DELAY)
        spotanim("spotanim.fairy_flower_ring")
        soundSynth("synth.ft_fairy_teleport")
        delay(TELEPORT_DELAY)
        val reachable = ring != null && ring != here && ring.destCoord.x >= MIN_VALID_COORD
        val locked = ring?.let { lockedMessage(it) }
        if (!reachable || locked != null) {
            telejump(here?.destCoord ?: player.coords)
            anim(APPEAR_ANIM)
            delay(1)
            if (locked != null && reachable) {
                mesbox(locked)
            } else {
                startDialogue {
                    chatPlayer(neutral, "Wow, fairy magic sure is useful, I hardly moved at all!")
                }
            }
            return
        }
        telejump(ring.destCoord)
        anim(APPEAR_ANIM)
        visitedVarbit(ring)?.let { vars[it] = 1 }
    }

    private fun ProtectedAccess.lockedMessage(ring: FairyringRow): String? {
        val requirement = requirements[ring.id] ?: return null
        val quest = requirement.quest
        if (quest != null) {
            val done =
                QuestRequirements.hasCompleted(player, quest) ||
                    (requirement.questStarted == true && QuestRequirements.isOnQuest(player, quest))
            if (!done) {
                return requirement.message
            }
        }
        val varbit = requirement.unlockVarbit?.let { ServerCacheManager.getVarbit(it) }
        if (varbit != null && player.vars[varbit] == 0) {
            return requirement.message
        }
        return null
    }

    private companion object {
        private const val MINOR_HUB = "loc.fairyring_minorhub"
        private val HUBS = listOf(MINOR_HUB, "loc.fairyring_homehub")

        private const val MAIN = "interface.fairyrings"
        private const val BACKGROUND_COLOUR = 4212288
        private const val SEARCH_LAYER_MODE = 28
        private const val BACKGROUND_TRANSPARENCY = 50

        private val DIAL_BUTTONS =
            listOf(
                "component.fairyrings:1_clockwise" to "component.fairyrings:1_anticlockwise",
                "component.fairyrings:2_clockwise" to "component.fairyrings:2_anticlockwise",
                "component.fairyrings:3_clockwise" to "component.fairyrings:3_anticlockwise",
            )
        private val DIAL_VARBITS =
            listOf("varbit.fairyring_1", "varbit.fairyring_2", "varbit.fairyring_3")
        private val FAVOURITE_VARBITS =
            listOf(
                "varbit.fairyring_fave_1",
                "varbit.fairyring_fave_2",
                "varbit.fairyring_fave_3",
                "varbit.fairyring_fave_4",
                "varbit.fairyring_fave_5",
                "varbit.fairyring_fave_6",
                "varbit.fairyring_fave_7",
                "varbit.fairyring_fave_8",
                "varbit.fairyring_fave_9",
                "varbit.fairyring_fave_10",
            )
        private val FAVOURITE_TEXTS =
            listOf(
                "component.fairyrings_log:fave_1",
                "component.fairyrings_log:fave_2",
                "component.fairyrings_log:fave_3",
                "component.fairyrings_log:fave_4",
                "component.fairyrings_log:fave_5",
                "component.fairyrings_log:fave_6",
                "component.fairyrings_log:fave_7",
                "component.fairyrings_log:fave_8",
                "component.fairyrings_log:fave_9",
                "component.fairyrings_log:fave_10",
            )
        private val FAVOURITE_CODES =
            listOf(
                "component.fairyrings_log:fave_code_1",
                "component.fairyrings_log:fave_code_2",
                "component.fairyrings_log:fave_code_3",
                "component.fairyrings_log:fave_code_4",
                "component.fairyrings_log:fave_code_5",
                "component.fairyrings_log:fave_code_6",
                "component.fairyrings_log:fave_code_7",
                "component.fairyrings_log:fave_code_8",
                "component.fairyrings_log:fave_code_9",
                "component.fairyrings_log:fave_code_10",
            )
        private const val FAVOURITE_SET = 64
        private const val DIAL_POSITIONS = 4
        private const val CLOCKWISE_STEP = 1
        private const val ANTICLOCKWISE_STEP = 3
        private const val DIAL_SOUND = "synth.ft_fairyrings"

        private const val LAST_LOCATION = "varbit.fairyring_lastloc"
        private const val LAST_LOCATION_SET = "varbit.fairyring_lastloc_set"

        private const val FAIRYTALE_2 = "quest_fairytale2"
        private val STAFFS = listOf("obj.dramen_staff", "obj.lunar_moonclan_liminal_staff")
        private const val RING_RADIUS = 10
        private const val STANDING_RADIUS = 2
        private const val ZANARIS_RING = 321

        private const val VANISH_ANIM_DELAY = 30
        private const val APPEAR_ANIM = "seq.human_fairy_appear"
        private const val TELEPORT_DELAY = 3
        private const val MIN_VALID_COORD = 64
    }
}
