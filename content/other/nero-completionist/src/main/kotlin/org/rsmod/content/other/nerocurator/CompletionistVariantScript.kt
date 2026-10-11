package org.rsmod.content.other.nerocurator

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.config.refs.params
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.*
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getOrNull
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CompletionistVariantScript @Inject constructor(
    private val teleports: PlayerTeleportValidator,
    private val areas: AreaChecker,
    private val random: GameRandom,
) : PluginScript(), PlayerPostTickHook {
    override fun ScriptContext.startup() {
        onOpHeld3(ARDY) { travel(ARDY, MONASTERY) }
        onOpHeld4(ARDY) { travel(ARDY, FARM) }
        onOpWorn2(ARDY) { travel(ARDY, MONASTERY) }
        onOpWorn3(ARDY) { travel(ARDY, FARM) }
        onOpHeld3(MYTHICAL) { travel(MYTHICAL, MYTHS) }
        onOpWorn2(MYTHICAL) { travel(MYTHICAL, MYTHS) }
        for (cape in AVA) {
            onOpHeld4(cape) { commune(cape) }
            onOpWorn2(cape) { commune(cape) }
        }
    }
    private fun ProtectedAccess.commune(symbol: String) {
        if (!active(symbol)) return
        player.attr[JUNK_DISABLED] = !(player.attr[JUNK_DISABLED] ?: false)
        mes("Your cape will ${if (player.attr[JUNK_DISABLED] == true) "no longer" else "now"} collect miscellaneous metal items while worn.")
    }
    private suspend fun ProtectedAccess.travel(symbol: String, destination: CoordGrid) {
        if (!active(symbol)) return
        teleports.validate(player, TeleportType.Standard, areas)?.let { mes(it); return }
        val landing = mapFindSquareLineOfWalk(destination, minRadius = 0, maxRadius = 3)
        if (landing == null) { mes("That destination is temporarily unavailable."); return }
        clearPendingAction()
        telejump(landing)
        if (coords == landing) anim("seq.human_castteleport_reverse")
    }
    private fun ProtectedAccess.active(symbol: String) = player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut && (symbol in inv || symbol in worn)
    override fun onPostTick(player: Player) {
        if (player.hitpoints <= 0 || player.loggingOut || player.pendingLogout || player.isAccessProtected || player.attr[JUNK_DISABLED] == true || AVA.none { player.worn[Wearpos.Back.slot]?.id == it.asRSCM() }) return
        val next = player.attr[NEXT_COLLECTION] ?: (player.currentMapClock + 100)
        player.attr[NEXT_COLLECTION] = next
        if (player.currentMapClock < next) return
        player.attr[NEXT_COLLECTION] = player.currentMapClock + 100
        if (getOrNull(player.worn[Wearpos.Torso.slot])?.param(params.metallic_interference) == true) return
        val item = when (random.of(2000)) {
            in 0..1974 -> "obj.steel_arrow"
            in 1975..1979 -> "obj.steel_dart"
            in 1980..1984 -> "obj.steel_knife"
            in 1985..1989 -> "obj.iron_ore"
            in 1990..1994 -> "obj.nails"
            else -> "obj.steel_bar"
        }
        player.invAdd(player.inv, item)
    }
    companion object {
        const val ARDY = "obj.nero_completionist_ardougne"
        const val MYTHICAL = "obj.nero_completionist_mythical"
        val AVA = listOf("accumulator", "assembler", "masori_assembler", "assembler_locked", "masori_assembler_locked").map { "obj.nero_completionist_$it" }
        val MONASTERY = CoordGrid(2606, 3221)
        val FARM = CoordGrid(2674, 3374)
        val MYTHS = CoordGrid(2456, 2849)
        val JUNK_DISABLED = AttributeKey<Boolean>(persistenceKey = "completionist_ava_junk_disabled")
        val NEXT_COLLECTION = AttributeKey<Int>()
    }
}
