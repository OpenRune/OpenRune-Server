package org.rsmod.content.raids.toa.raid.loot

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import dtx.rs.RSDropTable
import dtx.rs.locs
import jakarta.inject.Inject
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val RUNE_CACHE = "obj.toa_rune_cache"

@field:RegisterDropTable
@JvmField
val toaRuneCacheTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Cache of runes",
        locs = locs(RUNE_CACHE),
        mainTable =
            rsPlayerWeightedTable(total = 30) {
                name("Cache of runes")
                6 weight "obj.deathrune" count 500..1000
                6 weight "obj.bloodrune" count 500..1000
                6 weight "obj.soulrune" count 500..1000
                3 weight "obj.deathrune" count 1000..1500
                3 weight "obj.bloodrune" count 1000..1500
                3 weight "obj.soulrune" count 1000..1500
                1 weight "obj.deathrune" count 3000
                1 weight "obj.bloodrune" count 3000
                1 weight "obj.soulrune" count 3000
            },
    )

class ToaRuneCacheScript
@Inject
constructor(private val registry: DropTableRegistry, private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(RUNE_CACHE) { open() }
    }

    private fun ProtectedAccess.open() {
        val table = registry.forLoc(RUNE_CACHE) ?: return
        val drop =
            (table.roll(player, ArgMap()).flatten() as? RollResult.Single<DropRollItem>)?.result
                ?: return
        if (invDel(inv, RUNE_CACHE).failure) return
        val count = drop.rollCount(random)
        invAddOrDrop(objRepo, drop.obj, count)
        mes("You open the cache and find runes inside.", ChatType.Spam)
    }
}
