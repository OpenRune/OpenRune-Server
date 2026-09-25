package org.rsmod.content.bosses.whisperer

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.annotations.InternalApi
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

internal const val SANITY_TIMER = "timer.whisperer_sanity"
internal const val INSANITY_TIMER = "timer.whisperer_insanity"
internal const val FADE_TIMER = "timer.whisperer_fade"
internal const val SANITY_MAX = 100
internal const val FRAGMENT_OBJ = "obj.blackstone_fragment"
internal const val FRAGMENT_ACTIVE_OBJ = "obj.blackstone_fragment_active"

private const val INSANITY_DEATH_DELAY = 10
private const val SHADOW_SPLIT_OFFSET_X = 128
private val SHADOW_PROBE = CoordGrid(2400, 6384, 0)

internal const val REALM_OFFSET_X = 256

internal var Player.sanity by intVarBit("varbit.sanity")
internal var Player.inShadowRealm by boolVarBit("varbit.whisperer_shadow_realm_visited")
internal var Player.canSwitchRealms by boolVarBit("varbit.whisperer_allowed_to_switch_realms")

internal fun Player.changeSanity(delta: Int) {
    val before = sanity
    sanity = (before + delta).coerceIn(0, SANITY_MAX)
    if (before > 0 && sanity == 0 && inShadowRealm) softTimer(INSANITY_TIMER, INSANITY_DEATH_DELAY)
}

internal fun InstanceManager.isShadowRealm(session: InstanceSession, coords: CoordGrid): Boolean {
    val shadowBase = resolveCoord(session, SHADOW_PROBE) ?: return false
    return coords.x < shadowBase.x + SHADOW_SPLIT_OFFSET_X
}

/**
 * Moves the boss to the other realm along with its spawn point. Retreat, wander and interaction
 * range checks are all measured from [Npc.spawnCoords], so leaving it behind makes her flee.
 */
@OptIn(InternalApi::class)
internal fun Npc.telejumpRealm(collision: CollisionFlagMap, dx: Int) {
    telejump(collision, coords.translate(dx, 0))
    spawnCoords = spawnCoords.translate(dx, 0)
}

internal fun Player.setFragmentActive(active: Boolean) {
    val from = if (active) FRAGMENT_OBJ else FRAGMENT_ACTIVE_OBJ
    val to = if (active) FRAGMENT_ACTIVE_OBJ else FRAGMENT_OBJ
    if (from !in inv) return
    invDel(inv, from, 1)
    invAdd(inv, to, 1)
}

private const val REALM_FADE_DURATION = 20
private const val DISABLE_FADE_DELAY = 3
private const val SANITY_FADE_SCRIPT = "clientscript.[clientscript,sanity_fade]"
private const val SANITY_DISABLE_FADE_SCRIPT = "clientscript.[clientscript,sanity_disable_fade]"

internal fun ProtectedAccess.sanityFade(a: Int, b: Int, c: Int, d: Int) {
    runClientScript(SANITY_FADE_SCRIPT.asRSCM(RSCMType.CLIENTSCRIPT), a, b, c, d, REALM_FADE_DURATION)
}

internal suspend fun ProtectedAccess.fadeTeleport(dest: CoordGrid, whileBlack: () -> Unit = {}) {
    player.clearSoftTimer(FADE_TIMER)
    sanityFade(0, 255, 0, 0)
    delay(1)
    telejump(dest)
    whileBlack()
    sanityFade(0, 0, 0, 255)
    player.softTimer(FADE_TIMER, DISABLE_FADE_DELAY)
}

internal fun Player.disableSanityFade() {
    clearSoftTimer(FADE_TIMER)
    runClientScript(SANITY_DISABLE_FADE_SCRIPT.asRSCM(RSCMType.CLIENTSCRIPT))
}
