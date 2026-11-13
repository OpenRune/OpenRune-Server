package org.rsmod.content.areas.wilderness.locs

import dtx.core.ArgMap
import jakarta.inject.Inject
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.toDropList
import org.rsmod.api.player.events.LootSource
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.drops.LootDelivery
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val MUDDY_KEY = "obj.muddy_key"

private const val CHEST_CLOSED = "loc.muddy_chestclosed"
private const val CHEST_OPEN = "loc.muddy_chestopen"
private const val CHEST_OPEN_TICKS = 2
private const val VARP_KC = "varp.kc_muddy_chest"

class MuddyChestScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val loot: LootDelivery,
    private val dropRegistry: DropTableRegistry,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(CHEST_CLOSED) { openChest(it.loc) }
    }

    private suspend fun ProtectedAccess.openChest(loc: BoundLocInfo) {
        arriveDelay()

        if (invTotal(inv, MUDDY_KEY) == 0) {
            mes("The chest is locked. You need a muddy key to open it.")
            return
        }

        invDel(inv, MUDDY_KEY)
        anim("seq.human_openchest")
        locRepo.change(loc, CHEST_OPEN, CHEST_OPEN_TICKS)
        mes("You unlock the chest with your key.")

        VarPlayerIntMapSetter.set(player, VARP_KC, player.vars[VARP_KC] + 1)

        val table = dropRegistry.forLoc(CHEST_CLOSED) ?: return
        loot.give(player, LootSource.Loc(CHEST_CLOSED), table.roll(player, ArgMap()).toDropList())
        delay(1)
    }
}
