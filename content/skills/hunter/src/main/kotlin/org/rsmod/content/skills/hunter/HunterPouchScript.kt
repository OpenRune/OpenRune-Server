package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.enums.HunterEnums.hunter_pouch_contents
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.invtx.invTransfer
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.table.hunter.HunterPouchesRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

enum class HunterPouch(val noun: String, val plural: String, val inv: String) {
    Fur("fur", "furs", "inv.hunter_fur_pouch"),
    Meat("meat", "meats", "inv.hunter_meat_pouch");

    data class Size(val closed: String, val open: String, val capacity: Int)

    val sizes: List<Size> by lazy {
        HunterPouchesRow.all().filter { it.kind == ordinal }.map { Size(it.closed.internalName, it.open.internalName, it.capacity) }
    }

    val storable: List<String> by lazy {
        hunter_pouch_contents.filter { it.value == ordinal }.map { it.key.internalName }
    }

    fun store(player: Player): Inventory = player.invMap.getOrPut(inv)

    fun stored(player: Player): Int = store(player).let { s -> s.indices.sumOf { s[it]?.count ?: 0 } }

    fun held(player: Player): Size? = sizes.lastOrNull { it.closed in player.inv || it.open in player.inv }

    fun openHeld(player: Player): Size? = sizes.lastOrNull { it.open in player.inv }

    companion object {
        fun of(obj: String): HunterPouch? = entries.firstOrNull { obj in it.storable }

        fun ofPouch(obj: String): HunterPouch? = entries.firstOrNull { p -> p.sizes.any { obj == it.closed || obj == it.open } }

        fun ProtectedAccess.pouchHunted(obj: String, count: Int = 1): Boolean {
            val pouch = of(obj) ?: return false
            val size = pouch.openHeld(player) ?: return false
            if (pouch.stored(player) + count > size.capacity) return false
            return invAdd(pouch.store(player), obj, count).success
        }
    }
}

class HunterPouchScript
@Inject
constructor(private val locRepo: LocRepository, private val npcRepo: NpcRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        for (pouch in HunterPouch.entries) {
            for (size in pouch.sizes) {
                for (item in listOf(size.closed, size.open)) {
                    onOpHeld1(item) { fill(pouch) }
                    onOpHeld2(item) { empty(pouch) }
                    onOpHeld3(item) { check(pouch) }
                    onOpHeld5(item) { destroy(pouch, it) }
                    onEvent<HeldBanksideEvents.Type>(item.asRSCM(RSCMType.OBJ)) { emptyIntoBank(player, pouch) }
                }
                onOpHeld4(size.closed) { toggle(pouch, size.closed, size.open, "open") }
                onOpHeld4(size.open) { toggle(pouch, size.open, size.closed, "close") }
            }
        }
    }

    private fun ProtectedAccess.toggle(pouch: HunterPouch, from: String, into: String, verb: String) {
        if (invReplace(inv, from, 1, into).failure) return
        val name = ServerCacheManager.getItem(from.asRSCM(RSCMType.OBJ))?.name?.lowercase()?.removeSuffix(" (open)")
        mes("You $verb your $name.")
    }

    private fun ProtectedAccess.fill(pouch: HunterPouch) {
        val size = pouch.held(player) ?: return
        var room = size.capacity - pouch.stored(player)
        var moved = 0
        for (slot in inv.indices) {
            val obj = inv[slot] ?: continue
            if (room <= 0) break
            if (RSCM.getReverseMapping(RSCMType.OBJ, obj.id) !in pouch.storable) continue
            val count = minOf(obj.count, room)
            if (invMoveFromSlot(inv, pouch.store(player), slot, count).failure) continue
            room -= count
            moved += count
        }
        if (moved == 0) {
            mes("You don't have any ${pouch.plural} that can go into this pouch.")
        }
    }

    private fun ProtectedAccess.empty(pouch: HunterPouch) {
        val store = pouch.store(player)
        if (store.isEmpty()) {
            mes("Your ${pouch.noun} pouch is empty.")
            return
        }
        if (pouch != HunterPouch.Meat || !(nearBank(player.coords) || inGuildTopFloor(player.coords))) {
            mes("You can't empty your ${pouch.noun} pouch here.")
            return
        }
        for (slot in store.indices) {
            val obj = store[slot] ?: continue
            val count = minOf(obj.count, inv.freeSpace())
            if (count <= 0) break
            invMoveFromSlot(store, inv, slot, count)
        }
    }

    private fun emptyIntoBank(player: Player, pouch: HunterPouch) {
        val store = pouch.store(player)
        if (store.isEmpty()) {
            player.mes("Your ${pouch.noun} pouch is empty.")
            return
        }
        val bank = player.invMap.getOrPut("inv.bank")
        for (slot in store.indices) {
            val obj = store[slot] ?: continue
            player.invTransfer(store, slot, obj.count, bank)
        }
        player.mes("You deposit some ${pouch.noun} into your bank.", ChatType.Spam)
    }

    private fun ProtectedAccess.check(pouch: HunterPouch) {
        mes("Your ${pouch.noun} pouch is currently holding ${pouch.stored(player)} ${pouch.noun}.")
    }

    private suspend fun ProtectedAccess.destroy(pouch: HunterPouch, event: HeldObjEvents.Op5) {
        val item = RSCM.getReverseMapping(RSCMType.OBJ, event.type.id)
        val header = event.type.param(params.destroy_note_title)
        val text = event.type.param(params.destroy_note_desc)
        if (!confirmDestroy(item, event.obj.count, header, text)) return
        if (invDel(event.inventory, item, count = event.obj.count, slot = event.slot).failure) return
        val store = pouch.store(player)
        for (slot in store.indices) store[slot] = null
    }

    private fun nearBank(coords: CoordGrid): Boolean {
        val zone = ZoneKey.from(coords)
        if (npcRepo.findAll(zone, BANK_ZONE_RADIUS).any { it.type.isContentType(BANKER) }) return true
        for (dx in -BANK_ZONE_RADIUS..BANK_ZONE_RADIUS) {
            for (dz in -BANK_ZONE_RADIUS..BANK_ZONE_RADIUS) {
                val locs = locRepo.findAll(zone.translate(dx, dz))
                if (locs.any { loc -> ServerCacheManager.getObject(loc.id)?.let { t -> BANK_LOCS.any(t::isContentType) } == true }) {
                    return true
                }
            }
        }
        return false
    }

    private fun inGuildTopFloor(coords: CoordGrid): Boolean =
        coords.level == GUILD_TOP_LEVEL && coords.x in GUILD_X && coords.z in GUILD_Z

    private companion object {
        const val BANK_ZONE_RADIUS = 1
        const val BANKER = "content.banker"
        val BANK_LOCS = listOf("content.bank_booth", "content.bank_deposit_box")
        const val GUILD_TOP_LEVEL = 1
        val GUILD_X = 1546..1566
        val GUILD_Z = 3026..3046
    }
}
