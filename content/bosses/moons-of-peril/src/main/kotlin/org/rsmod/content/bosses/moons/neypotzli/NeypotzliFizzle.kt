package org.rsmod.content.bosses.moons.neypotzli

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
class NeypotzliFizzler
@Inject
constructor(
    private val bounds: NeypotzliBounds,
    private val protectedAccess: ProtectedAccessLauncher,
) {
    private val lastSquare = IdentityHashMap<Player, Int>()

    fun onTick(player: Player) {
        val coords = player.coords
        val square = (coords.level shl 28) or ((coords.x shr 6) shl 14) or (coords.z shr 6)
        if (lastSquare.put(player, square) == square) return
        check(player)
    }

    fun check(player: Player) {
        if (bounds.contains(player)) return
        if (!carriesAny(player)) return
        protectedAccess.launch(player) { fizzle() }
    }

    fun forget(player: Player) {
        lastSquare -= player
    }

    private fun carriesAny(player: Player): Boolean =
        RESOURCES.any { player.inv.count(it) > 0 } || POTIONS.any { player.inv.count(it) > 0 }

    private fun ProtectedAccess.fizzle() {
        var changed = false
        for (obj in RESOURCES) {
            val count = inv.count(obj)
            if (count > 0 && invDel(inv, obj, count).success) changed = true
        }
        for (potion in POTIONS) {
            repeat(inv.count(potion)) {
                if (invReplace(inv, potion, 1, "obj.vial_water").success) changed = true
            }
        }
        if (changed) {
            mes("The resources from Neypotzli fizzle to dust as they are severed from its magic.")
        }
    }

    private companion object {
        private val RESOURCES =
            listOf(
                "obj.bream_fish_raw",
                "obj.bream_fish_cooked",
                "obj.raw_lizard",
                "obj.cooked_lizard",
                "obj.moonlight_grub",
                "obj.moonlight_grub_paste",
            )

        private val POTIONS =
            listOf(
                "obj.4dosemoonlightpotion",
                "obj.3dosemoonlightpotion",
                "obj.2dosemoonlightpotion",
                "obj.1dosemoonlightpotion",
            )
    }
}

class NeypotzliFizzleHook @Inject constructor(private val fizzler: NeypotzliFizzler) :
    PlayerPostTickHook {
    override fun onPostTick(player: Player) = fizzler.onTick(player)
}

class NeypotzliFizzle @Inject constructor(private val fizzler: NeypotzliFizzler) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin { fizzler.check(player) }
        onPlayerLogout { fizzler.forget(player) }
    }
}
