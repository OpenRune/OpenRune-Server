package org.rsmod.content.raids.toa.raid.encounter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.map.Direction
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

open class ToaBossEncounter(raid: ToaRaid, room: ToaRoom, region: Region, controllerId: Int) :
    ToaEncounter(raid, room, region, controllerId) {

    protected open val osmumtenDelay: Int = 0

    protected open val loot: ToaBossLoot? = null

    protected open val lootSource: Npc? = null

    override fun onComplete() {
        val path = room.path ?: return
        if (path !in raid.pathsCompleted) {
            raid.pathsCompleted += path
        }
        if (osmumtenDelay > 0) schedule(osmumtenDelay) { reward() } else reward()
    }

    private fun reward() {
        spawnOsmumten()
        dropLoot()
    }

    // TODO: play jingle 296 when Osmumten spawns.
    private fun spawnOsmumten() {
        val tile = room.osmumtenTile ?: return
        val challengeSpawn = room.challengeSpawn ?: return
        val npc = Npc(OSMUMTEN, coords(tile))
        npc.respawnDir = if (challengeSpawn.x > tile.x) Direction.East else Direction.West
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.noneMode()
        npc.anim(OSMUMTEN_SPAWN_ANIM)
    }

    private fun dropLoot() {
        val loot = loot ?: return
        val tile = coords(room.dropTile ?: return)
        val npcId = loot.trackerNpc.asRSCM(RSCMType.NPC)
        val heroObj = loot.heroObj
        val hero = lootSource?.findHero(deps.playerList)
        if (heroObj != null && hero != null && hero in players) {
            dropFor(hero, heroObj, tile, npcId)
        }
        val book = loot.book ?: return
        for (player in players) {
            if (!owns(player, book)) dropFor(player, book.obj, tile, npcId)
        }
    }

    private fun dropFor(player: Player, obj: String, tile: CoordGrid, npcId: Int) {
        val drop = deps.objRepo.add(obj, tile, DROP_DESPAWN, player, reveal = DROP_DESPAWN + 1)
        ClientScripts.lootTrackerAddLoot(player, npcId, deps.mapClock.cycle, drop.type, drop.count)
    }

    private fun owns(player: Player, book: ToaBook): Boolean {
        val varbit = ServerCacheManager.getVarbit(book.ownedVarbit.asRSCM(RSCMType.VARBIT))
        if (varbit != null && player.vars[varbit] == 1) return true
        return book.obj in player.inv || player.invMap[BANK]?.contains(book.obj) == true
    }

    companion object {
        const val OSMUMTEN = "npc.toa_osmumten_vis"

        private const val OSMUMTEN_SPAWN_ANIM = "seq.ghost_summon2_priority"
        private const val DROP_DESPAWN = 18_000
        private const val BANK = "inv.bank"
    }
}
