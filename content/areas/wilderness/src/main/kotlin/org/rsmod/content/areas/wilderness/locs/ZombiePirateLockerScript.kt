package org.rsmod.content.areas.wilderness.locs

import dtx.core.ArgMap
import jakarta.inject.Inject
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.toDropList
import org.rsmod.api.player.events.LootSource
import org.rsmod.api.player.front
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.drops.LootDelivery
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
    private val loot: LootDelivery,
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
        val source = LootSource.Loc(LOCKER_CLOSED)
        val items = loot.resolve(player, source, table.roll(player, ArgMap()).toDropList())
        if (player.front.isType("obj.wild_cave_amulet")) {
            for (item in items) item.item = ocCert(item.item).internalName
        }
        loot.give(player, source, items)
        delay(1)
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
