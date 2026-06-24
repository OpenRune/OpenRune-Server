package org.rsmod.content.areas.wilderness.locs

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import jakarta.inject.Inject
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.player.front
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val ZOMBIE_PIRATE_KEY = "obj.zombie_pirate_wildy_key"
private const val SCROLL = "obj.wilderness_blip_blocking_scroll"
private const val VARBIT_SCROLL = "varbit.blip_blocking_scroll_used"

private const val LOCKER_CLOSED = "loc.wildy_pirate_boat_locker"
private const val LOCKER_OPEN = "loc.wildy_pirate_boat_locker_open"
private const val LOCKER_OPEN_TICKS = 2
private const val VARP_KC = "varp.kc_zombie_pirate_locker"

class ZombiePirateLockerScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val objRepo: ObjRepository,
    private val dropRegistry: DropTableRegistry,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(LOCKER_CLOSED) { openLocker(it.loc) }
        onOpHeld1(SCROLL) { unlockBlipScroll() }
    }

    private suspend fun ProtectedAccess.openLocker(loc: BoundLocInfo) {
        arriveDelay()

        if (invTotal(inv, ZOMBIE_PIRATE_KEY) == 0) {
            mes("The locker is locked. You need a zombie pirate key to open it.")
            return
        }

        invDel(inv, ZOMBIE_PIRATE_KEY)
        anim("seq.human_openchest")
        locRepo.change(loc, LOCKER_OPEN, LOCKER_OPEN_TICKS)
        mes("You unlock the locker with your key.")

        VarPlayerIntMapSetter.set(player, VARP_KC, player.vars[VARP_KC] + 1)

        val table = dropRegistry.forLoc(LOCKER_CLOSED) ?: return
        when (val result = table.roll(player, ArgMap()).flatten()) {
            is RollResult.Nothing -> Unit
            is RollResult.Single -> giveDrop(result.result)
            is RollResult.ListOf -> result.results.forEach { giveDrop(it) }
        }
        delay(1)
    }

    private fun ProtectedAccess.giveDrop(drop: DropRollItem) {
        if (drop.isNothing || !drop.condition(player)) return
        var obj = drop.transformObj(player) ?: drop.obj
        if (player.front.isType("obj.wild_cave_amulet")) {
            obj = ocCert(obj).internalName
        }
        invAddOrDrop(objRepo, obj, drop.rollCount(random))
    }

    private suspend fun ProtectedAccess.unlockBlipScroll() {
        if (player.vars[VARBIT_SCROLL] == 1) {
            objbox(SCROLL, "You can make out some faded words on the ancient parchment, but there's nothing more for you to learn.")
            return
        }

        objbox(
            SCROLL,
            "You can make out some faded words on the ancient parchment, revealing the secrets of " +
                "teleport negation. Would you like to absorb their power?",
        )

        val proceed =
            choice2(
                "Become immune to the Abyssal demon and <br>Elder Chaos druid teleportation attacks.",
                true,
                "Cancel",
                false,
                title = "This will consume the scroll."
            )
        if (!proceed) return

        invDel(inv, SCROLL)
        objbox(SCROLL, "You study the scroll and become immune to the Abyssal demon and Elder Chaos druid teleportation attacks.")
        VarPlayerIntMapSetter.set(player, VARBIT_SCROLL, 1)
    }

}
