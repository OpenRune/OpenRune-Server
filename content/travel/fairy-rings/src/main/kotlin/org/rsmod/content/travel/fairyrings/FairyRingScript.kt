package org.rsmod.content.travel.fairyrings

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Fairy rings: the mushroom circles that teleport a player wielding a dramen or lunar staff to
 * whichever ring the three-letter code on the dials names.
 *
 * What the cache provides and this script leans on:
 * - `dbtable.fairyring` lists every code with its landing tile, its travel log entry and its
 *   `multiloc_state` index; see [FairyRings].
 * - The ring interface (`interface.fairyrings`) rotates its dials client-side by writing
 *   `varbit.fairyring_1..3` and sends each rotation as a button press, so the server mirrors the
 *   rotation into the same varbits and reads them back when `Confirm` arrives.
 * - The travel log (`interface.fairyrings_log`) draws only the entries whose text is non-empty,
 *   so the visited codes and the favourites are written into their components before the pair is
 *   opened, and again (with the side panel re-opened) whenever a favourite changes.
 * - Favourite slots are `varbit.fairyring_fave_<slot>_1..3` (the dial values) plus
 *   `varbit.fairyring_fave_set_<slot>`; the last destination is `varbit.fairyring_lastloc`.
 *
 * Codes the cache points at its placeholder tile, or that match the tile the player stands on,
 * only earn the "hardly moved at all" line, as they do officially.
 */
class FairyRingScript
@Inject
constructor(
    private val teleportValidator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    private lateinit var rings: FairyRings
    private val staffs: Set<Int> = STAFFS.map { it.asRSCM(RSCMType.OBJ) }.toSet()

    private var ProtectedAccess.lastDestination by intVarBit("varbit.fairyring_lastloc")

    override fun ScriptContext.startup() {
        rings = FairyRings.load()

        for (loc in RING_LOCS) {
            if (loc != "loc.fairyring_homehub") onOpLoc1(loc) { zanaris() }
            onOpLoc2(loc) { configure() }
            onOpLoc3(loc) { lastDestination() }
            onOpLoc4(loc) { favourites() }
        }

        for (loc in listOf("loc.poh_spirit_ring", "loc.league_5_poh_spirit_ring")) {
            onOpLoc2(loc) { zanaris() }
            onOpLoc3(loc) { configure() }
            onOpLoc4(loc) { lastDestination() }
        }
        onIfModalButton("component.fairyrings_log:hideout") { if (vars["varbit.fairyrings_log_hideout"] != 0) hideoutSequence() }

        for ((dial, varbit) in DIAL_VARBITS.withIndex()) {
            onIfModalButton("component.fairyrings:${dial + 1}_clockwise") { rotate(varbit, 1) }
            onIfModalButton("component.fairyrings:${dial + 1}_anticlockwise") {
                rotate(varbit, -1)
            }
        }
        onIfModalButton("component.fairyrings:confirm") { confirm() }

        for (ring in rings.all) {
            onIfModalButton(ring.logComponent) {
                when (it.op) {
                    IfButtonOp.Op1 -> useCode(ring)
                    IfButtonOp.Op2 -> addFavourite(ring)
                    else -> {}
                }
            }
            onIfModalButton(ring.favouriteIconComponent) { addFavourite(ring) }
        }
        for (slot in 1..FAVOURITE_SLOTS) {
            onIfModalButton("component.fairyrings_log:fave_$slot") {
                when (it.op) {
                    IfButtonOp.Op1 -> favourite(slot)?.let { ring -> useCode(ring) }
                    IfButtonOp.Op2 -> removeFavourite(slot)
                    else -> {}
                }
            }
            onIfModalButton("component.fairyrings_log:fave_icon_$slot") { removeFavourite(slot) }
        }
    }

    /* Ring ops */

    private suspend fun ProtectedAccess.zanaris() {
        val ring = rings.byCode(ZANARIS_CODE) ?: return
        travel(ring)
    }

    private fun ProtectedAccess.configure() {
        if (!hasFairyMagic()) {
            mes(NO_FAIRY_MAGIC)
            return
        }
        refreshLog()
        ifOpenMainSidePair(RING_INTERFACE, LOG_INTERFACE)
    }

    private suspend fun ProtectedAccess.lastDestination() {
        val ring = rings.byLogIndex(lastDestination)
        if (ring == null || ring.destination == null) {
            mes("You have not travelled anywhere by fairy ring yet.")
            return
        }
        travel(ring, if (player.attr[LAST_HIDEOUT] == true) HIDEOUT else null)
    }

    private suspend fun ProtectedAccess.favourites() {
        if (!hasFairyMagic()) {
            mes(NO_FAIRY_MAGIC)
            return
        }
        val favourites = (1..FAVOURITE_SLOTS).mapNotNull { favourite(it) }
        if (favourites.isEmpty()) {
            mes("You have no favourite fairy ring codes. Add some from the travel log.")
            return
        }
        val labels = favourites.map { ring -> "${ring.code} - ${ring.description ?: "Nowhere"}" }
        val picked = menu("Favourite fairy rings", hotkeys = true, choices = labels + "Cancel")
        val ring = favourites.getOrNull(picked) ?: return
        travel(ring)
    }

    /* Ring interface */

    private fun ProtectedAccess.rotate(varbit: String, steps: Int) {
        vars[varbit] = FairyRingDials.rotate(vars[varbit], steps)
    }

    private suspend fun ProtectedAccess.confirm() {
        val ring = rings.byDials(dials()) ?: return
        travel(ring)
    }

    private fun ProtectedAccess.dials(): List<Int> = DIAL_VARBITS.map { vars[it] }

    /** Spins the dials round to [ring]'s code; the client redraws them from the varbits. */
    private fun ProtectedAccess.useCode(ring: FairyRing) {
        for ((dial, varbit) in DIAL_VARBITS.withIndex()) {
            vars[varbit] = ring.dials[dial]
        }
    }

    /* Travel log */

    private fun ProtectedAccess.refreshLog() {
        for (ring in rings.all) {
            val text = if (hasVisited(ring)) ring.logText else ""
            ifSetText(ring.logComponent, text)
        }
        ifSetText("component.fairyrings_log:hideout", if (vars["varbit.fairyrings_log_hideout"] != 0) "Fairy Resistance Hideout" else "")
        for (slot in 1..FAVOURITE_SLOTS) {
            val ring = favourite(slot)
            // The favourites list draws the code from `fave_code_<slot>` (in its own colour) over
            // the entry, so the entry itself only carries the location on its second line.
            val description = ring?.description?.let { "<br>$it" }.orEmpty()
            ifSetText("component.fairyrings_log:fave_$slot", description)
            ifSetText("component.fairyrings_log:fave_code_$slot", ring?.spacedCode.orEmpty())
        }
    }

    private fun ProtectedAccess.reopenLog() {
        refreshLog()
        ifOpenSide(LOG_INTERFACE)
    }

    private fun ProtectedAccess.hasVisited(ring: FairyRing): Boolean {
        val varbit = ring.visitedVarBit ?: return false
        return vars[varbit] != 0
    }

    private fun ProtectedAccess.markVisited(ring: FairyRing) {
        val varbit = ring.visitedVarBit ?: return
        vars[varbit] = 1
    }

    /* Favourites */

    private fun ProtectedAccess.favourite(slot: Int): FairyRing? {
        if (vars["varbit.fairyring_fave_set_$slot"] == 0) {
            return null
        }
        val dials = (1..FairyRingDials.COUNT).map { vars["varbit.fairyring_fave_${slot}_$it"] }
        return rings.byDials(dials)
    }

    private fun ProtectedAccess.addFavourite(ring: FairyRing) {
        if ((1..FAVOURITE_SLOTS).any { favourite(it) == ring }) {
            mes("${ring.code} is already one of your favourite codes.")
            return
        }
        val slot = (1..FAVOURITE_SLOTS).firstOrNull { favourite(it) == null }
        if (slot == null) {
            mes("You can only have $FAVOURITE_SLOTS favourite fairy ring codes.")
            return
        }
        for ((dial, value) in ring.dials.withIndex()) {
            vars["varbit.fairyring_fave_${slot}_${dial + 1}"] = value
        }
        vars["varbit.fairyring_fave_set_$slot"] = 1
        reopenLog()
    }

    private fun ProtectedAccess.removeFavourite(slot: Int) {
        if (favourite(slot) == null) {
            return
        }
        vars["varbit.fairyring_fave_set_$slot"] = 0
        reopenLog()
    }

    /* Teleport */

    private fun ProtectedAccess.hasFairyMagic(): Boolean {
        if (player.vars["varbit.lumbridge_diary_elite_complete"] != 0) return true
        if (rings.all.any { it.noStaffReturn && it.destination?.let { tile -> tile.level == coords.level && tile.chebyshevDistance(coords) <= 3 } == true }) return true
        val weapon = player.worn[Wearpos.RightHand.slot] ?: return false
        return weapon.id in staffs
    }

    private suspend fun ProtectedAccess.hideoutSequence() {
        for (code in HIDEOUT_CODES) {
            val r = rings.byCode(code) ?: return
            if (!travel(r)) return
            delay(ACTION_DELAY)
            if (player.pendingLogout || player.loggingOut || player.hitpoints == 0) return
            val expected = if (code == "DJQ") rings.byCode("DLR")!!.destination else if (code == "AJS") HIDEOUT else r.destination
            if (coords != expected) return
        }
    }

    private fun ProtectedAccess.canEnterHideout(): Boolean =
        QuestRequirements.hasCompleted(player, "quest_fairytale2") || inv.contains("obj.fairytale2_nuffs_certificate")

    private suspend fun ProtectedAccess.travel(ring: FairyRing, override: CoordGrid? = null): Boolean {
        if (!hasFairyMagic()) {
            mes(NO_FAIRY_MAGIC)
            return false
        }
        if (actionDelay > mapClock) {
            return false
        }
        val denial = teleportValidator.validate(player, TeleportType.Standard, areaChecker)
        if (denial != null) {
            mes(denial, ChatType.Engine)
            return false
        }
        val origin = coords
        if (ring.code == "DIQ") { mes("You need a built fairy ring in your own house to use this code."); return false }
        val progress = player.attr[HIDEOUT_PROGRESS] ?: 0
        val hideout = override == HIDEOUT || (ring.code == "AJS" && progress == 3 && canEnterHideout())
        val destination = if (hideout) HIDEOUT else ring.destination
        if (destination != null && !validArrival(destination)) {
            mes("That fairy ring is currently unavailable.")
            return false
        }
        ifClose()
        beginTeleport()
        actionDelay = mapClock + ACTION_DELAY
        resetAnim()
        resetSpotanim()
        anim(VANISH_ANIM)
        spotanim(FLOWER_RING)
        soundSynth(TELEPORT_SOUND)
        var handedOff = false
        try {
            delay(VANISH_DELAY)
            if (player.pendingLogout || player.loggingOut || player.hitpoints == 0 || coords != origin || !hasFairyMagic()) return false
            val lateDenial = teleportValidator.validate(player, TeleportType.Standard, areaChecker)
            if (lateDenial != null) { mes(lateDenial); return false }
            resetAnim()
            resetSpotanim()
            if (destination == null || destination == coords) {
                anim(APPEAR_ANIM)
                mes(HARDLY_MOVED)
                advanceSequence(ring.code)
                return true
            }
            if (!validArrival(destination)) return false
            telejump(destination, TeleportType.Standard, onArrival = {
                resetAnim()
                resetSpotanim()
                anim(APPEAR_ANIM)
                markVisited(ring)
                lastDestination = ring.logIndex
                vars["varbit.fairyring_lastloc_set"] = 1
                player.attr[LAST_HIDEOUT] = hideout
                if (hideout) vars["varbit.fairyrings_log_hideout"] = 1
                advanceSequence(ring.code)
            })
            handedOff = true
            return true
        } finally {
            resetSpotanim()
            if (coords == origin && destination != origin) resetAnim()
            if (!handedOff) teleportValidator.cancel(player)
        }
    }

    private fun ProtectedAccess.advanceSequence(code: String) {
        val progress = player.attr[HIDEOUT_PROGRESS] ?: 0
        player.attr[HIDEOUT_PROGRESS] = when {
            HIDEOUT_CODES.getOrNull(progress) == code -> (progress + 1) % HIDEOUT_CODES.size
            code == HIDEOUT_CODES.first() -> 1
            else -> 0
        }
    }

    /** Native destinations are the blocked centre tile of the ring. Players must be able to exit. */
    private fun validArrival(tile: org.rsmod.map.CoordGrid): Boolean {
        if (!collision.isZoneValid(tile)) return false
        return listOf(tile.translate(1, 0), tile.translate(-1, 0), tile.translate(0, 1), tile.translate(0, -1))
            .any { collision.isZoneValid(it) && collision[it.x, it.z, it.level] and BLOCKED == 0 }
    }

    private companion object {
        private val HIDEOUT = CoordGrid(2328, 4426)
        private val HIDEOUT_CODES = listOf("AIR", "DLR", "DJQ", "AJS")
        private val HIDEOUT_PROGRESS = AttributeKey<Int>()
        private val LAST_HIDEOUT = AttributeKey<Boolean>(persistenceKey = "fairy_ring_last_hideout")
        private const val RING_INTERFACE = "interface.fairyrings"
        private const val LOG_INTERFACE = "interface.fairyrings_log"

        /** Every ring the player can operate; the Zanaris hub has no `Zanaris` op of its own. */
        private val RING_LOCS =
            listOf("loc.fairyring_minorhub", "loc.fairyring_homehub", "loc.poh_fairy_ring")

        private val STAFFS = listOf("obj.dramen_staff", "obj.lunar_moonclan_liminal_staff")

        private val DIAL_VARBITS =
            listOf("varbit.fairyring_1", "varbit.fairyring_2", "varbit.fairyring_3")

        private const val FAVOURITE_SLOTS = 10
        private const val ZANARIS_CODE = "BKS"

        private const val NO_FAIRY_MAGIC = "The fairy rings only work for those who wield fairy magic."
        private const val HARDLY_MOVED = "Wow, fairy magic sure is useful, I hardly moved at all!"

        private const val VANISH_ANIM = "seq.human_fairy_vanish"
        private const val APPEAR_ANIM = "seq.human_fairy_appear"

        /** The ring of flowers that springs up around a player as the fairy magic takes them. */
        private const val FLOWER_RING = "spotanim.fairy_flower_ring"
        private const val TELEPORT_SOUND = "synth.fairy_teleport"
        private const val BLOCKED = CollisionFlag.LOC or CollisionFlag.GROUND_DECOR or CollisionFlag.BLOCK_WALK
        private const val VANISH_DELAY = 2
        private const val ACTION_DELAY = 4
    }
}
