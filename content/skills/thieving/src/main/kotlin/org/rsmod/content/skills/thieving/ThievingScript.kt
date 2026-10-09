package org.rsmod.content.skills.thieving

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import jakarta.inject.Inject
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.table.thieving.ThievingCoinPouchRow
import org.rsmod.api.table.thieving.ThievingPickpocketRow
import org.rsmod.api.table.thieving.ThievingStallRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ThievingScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : PluginScript() {
    private val restockingUntil = HashMap<CoordGrid, Int>()

    override fun ScriptContext.startup() {
        val stallLoot = lootTables(ThievingDropTables.stalls, ThievingStallRow.all()) { it.rowId }
        stallLoot.forEach { (stall, loot) ->
            when (opSlot { stall.loc.actions.getOpOrNull(it) == STEAL_OP }) {
                1 -> onOpLoc1(stall.loc) { stealFromStall(it.loc, stall, loot) }
                2 -> onOpLoc2(stall.loc) { stealFromStall(it.loc, stall, loot) }
                3 -> onOpLoc3(stall.loc) { stealFromStall(it.loc, stall, loot) }
                4 -> onOpLoc4(stall.loc) { stealFromStall(it.loc, stall, loot) }
                5 -> onOpLoc5(stall.loc) { stealFromStall(it.loc, stall, loot) }
            }
        }
        bindPickpockets()
        ThievingCoinPouchRow.all().forEach { pouch ->
            onOpHeld1(pouch.obj) { openPouches(pouch, all = true) }
            onOpHeld2(pouch.obj) { openPouches(pouch, all = false) }
        }
    }

    /**
     * Binds every npc with a Pickpocket op to the target sharing its cache name, or to the target
     * whose symbol prefix it carries when its name is its own (Prifddinas elves, Darkmeyer vyres).
     */
    private fun ScriptContext.bindPickpockets() {
        val targets = ThievingPickpocketRow.all()
        val loot = lootTables(ThievingDropTables.pickpockets, targets) { it.rowId }
        val byName = targets.associateBy { it.name.lowercase() }
        val byPrefix = targets.flatMap { t -> t.symbolPrefixes.map { it to t } }
        for ((id, type) in ServerCacheManager.getNpcs()) {
            val slot = opSlot { type.actions.getOpOrNull(it) == PICKPOCKET_OP } ?: continue
            val symbol = runCatching { RSCM.getReverseMapping(RSCMType.NPC, id) }.getOrNull()
            if (symbol.isNullOrBlank()) continue
            val bare = symbol.removePrefix("npc.")
            val target =
                byName[type.name.lowercase()]
                    ?: byPrefix.firstOrNull { bare.startsWith(it.first) }?.second
                    ?: continue
            val table = loot.getValue(target)
            when (slot) {
                1 -> onOpNpc1(symbol) { pickpocket(it.npc, target, table) }
                2 -> onOpNpc2(symbol) { pickpocket(it.npc, target, table) }
                3 -> onOpNpc3(symbol) { pickpocket(it.npc, target, table) }
                4 -> onOpNpc4(symbol) { pickpocket(it.npc, target, table) }
                5 -> onOpNpc5(symbol) { pickpocket(it.npc, target, table) }
            }
        }
    }

    private fun <R> lootTables(
        tables: Map<String, ThievingDropTable>,
        rows: List<R>,
        rowId: (R) -> Int,
    ): Map<R, ThievingDropTable> {
        val byId = tables.mapKeys { (symbol, _) -> symbol.asRSCM(RSCMType.DBROW) and 0xFFFF }
        val orphaned = byId.keys - rows.map { rowId(it) and 0xFFFF }.toSet()
        check(orphaned.isEmpty()) { "Thieving loot keyed to rows outside its table: $orphaned" }
        return rows.associateWith { row ->
            byId[rowId(row) and 0xFFFF] ?: error("No thieving loot for dbrow ${rowId(row)}")
        }
    }

    private fun opSlot(matches: (Int) -> Boolean): Int? = (0 until 5).firstOrNull(matches)?.plus(1)

    private fun ProtectedAccess.rollLoot(table: ThievingDropTable): List<LootDrop> {
        val drops =
            when (val result = table.roll(player, ArgMap()).flatten()) {
                is RollResult.Nothing -> emptyList()
                is RollResult.Single -> listOf(result.result)
                is RollResult.ListOf -> result.results
            }
        return drops
            .filter { !it.isNothing && it.condition(player) }
            .map { LootDrop(it.transformObj(player) ?: it.obj, it.rollCount(random)) }
    }

    private suspend fun ProtectedAccess.stealFromStall(
        loc: BoundLocInfo,
        stall: ThievingStallRow,
        table: ThievingDropTable,
    ) {
        if (player.isFrozen) return
        arriveDelay()
        faceLoc(loc)
        if (stat(THIEVING) < stall.level) {
            mes("You need to be level ${stall.level} to steal from this stall.")
            return
        }
        if (restockingUntil.getOrDefault(loc.coords, 0) > mapClock) return
        val loot = rollLoot(table)
        if (!player.addLoot(inv, loot, commit = false)) {
            mes("You don't have enough inventory space.")
            return
        }
        stall.attemptMessage?.let { mes(it, ChatType.Spam) }
        val spotter = findSpotter(stall)
        if (spotter != null) {
            caughtAtStall(spotter, stall)
            return
        }
        anim(STALL_SEQ)
        delay(2)
        if (!player.addLoot(inv, loot)) {
            mes("You don't have enough inventory space.")
            return
        }
        for (drop in loot) {
            mes("You steal ${describe(drop.obj, drop.count)}.")
        }
        statAdvance(THIEVING, stall.xp / 10.0)
        restock(loc, stall)
    }

    private fun ProtectedAccess.findSpotter(stall: ThievingStallRow): Npc? {
        val watchers = stall.owners + stall.guards
        return npcRepo
            .findAll(ZoneKey.from(player.coords), zoneRadius = 1)
            .filter { npc -> watchers.any { it.id == npc.type.id } }
            .filter { it.coords.level == player.coords.level }
            .filter { it.coords.chebyshevDistance(player.coords) <= SPOT_RANGE }
            .filter { lineOfSight(it.coords, player.coords) }
            .minByOrNull { it.coords.chebyshevDistance(player.coords) }
    }

    private fun ProtectedAccess.caughtAtStall(spotter: Npc, stall: ThievingStallRow) {
        spotter.say(CAUGHT_SHOUT)
        val guard =
            if (stall.guards.any { it.id == spotter.type.id }) {
                spotter
            } else {
                npcRepo
                    .findAll(ZoneKey.from(player.coords), zoneRadius = 1)
                    .filter { npc -> stall.guards.any { it.id == npc.type.id } }
                    .minByOrNull { it.coords.chebyshevDistance(player.coords) }
            }
        guard?.opPlayer2(player, aiPlayerInteractions)
    }

    private fun ProtectedAccess.restock(loc: BoundLocInfo, stall: ThievingStallRow) {
        val empty = stall.empty
        if (empty != null) {
            locRepo.change(loc, empty, stall.respawn)
        } else {
            restockingUntil[loc.coords] = mapClock + stall.respawn
        }
    }

    private suspend fun ProtectedAccess.pickpocket(
        npc: Npc,
        target: ThievingPickpocketRow,
        table: ThievingDropTable,
    ) {
        if (player.isFrozen) return
        val owner = pocketOwner(npc, target)
        if (stat(THIEVING) < target.level) {
            mes("You need to be level ${target.level} to pickpocket $owner.")
            return
        }
        val pouch = target.pouch?.internalName
        if (pouch != null && inv.count(pouch) >= MAX_POUCHES) {
            mes("You need to empty your coin pouches before you can continue pickpocketing.")
            return
        }
        val loot = rollLoot(table)
        if (!player.addLoot(inv, loot, commit = false)) {
            mes("You don't have enough inventory space.")
            return
        }
        faceEntitySquare(npc)
        mes("You attempt to pick $owner's pocket.", ChatType.Spam)
        delay(1)
        if (!statRandom(THIEVING, target.low, target.high, invisibleBoost = 0)) {
            failPickpocket(npc, target, owner)
            return
        }
        if (!player.addLoot(inv, loot)) {
            mes("You don't have enough inventory space.")
            return
        }
        mes("You pick $owner's pocket.", ChatType.Spam)
        anim(PICKPOCKET_SEQ)
        soundSynth(PICK_SYNTH)
        for (drop in loot.filter { it.obj != pouch }) {
            mes("You steal ${describe(drop.obj, drop.count)}.", ChatType.Spam)
        }
        statAdvance(THIEVING, target.xp / 10.0)
    }

    private fun ProtectedAccess.openPouches(pouch: ThievingCoinPouchRow, all: Boolean) {
        val obj = pouch.obj.internalName
        val count = if (all) inv.count(obj) else 1
        if (count == 0) return
        val total = (1..count).sumOf { random.of(pouch.coinsMin, pouch.coinsMax).toLong() }
        if (!player.exchangePouches(inv, obj, count, total)) {
            mes("You don't have enough inventory space.")
            return
        }
        val message = if (count > 1) "You open all of the pouches." else "You open the coin pouch."
        mes(message, ChatType.Spam)
    }

    private suspend fun ProtectedAccess.failPickpocket(
        npc: Npc,
        target: ThievingPickpocketRow,
        owner: String,
    ) {
        mes("You fail to pick $owner's pocket.", ChatType.Spam)
        if (player.vars[SHADOW_VEIL_ACTIVE] == 1 && random.of(100) < SHADOW_VEIL_CHANCE) {
            mes("Your attempt to steal goes unnoticed.", ChatType.Spam)
            return
        }
        npc.say(target.caughtShout)
        npc.facePlayer(player)
        try {
            stun(target.stunTicks)
            delay(1)
            spotanim(STUN_SPOTANIM, height = STUN_SPOTANIM_HEIGHT)
            anim(STUN_BLOCK_SEQ)
            soundSynth(STUN_SYNTH)
            queueHit(delay = 1, type = HitType.Typeless, damage = target.stunDamage)
            delay(1)
            mes("You've been stunned!", ChatType.Spam)
        } finally {
            if (npc.faceEntity.playerSlot == player.slotId) {
                npc.resetFaceEntity()
            }
        }
    }

    private fun ProtectedAccess.stun(ticks: Int) {
        player.frozen = true
        player.routeDestination.clear()
        // The freeze starts a tick before the stun spotanim lands; the table counts from the spotanim.
        player.timer(FREEZE_TIMER, ticks + 1)
    }

    private fun pocketOwner(npc: Npc, target: ThievingPickpocketRow): String {
        val name = if (target.lowercaseName) npc.visType.name.lowercase() else npc.visType.name
        return if (name.contains(" the ")) name else "the $name"
    }

    private fun describe(obj: String, count: Int): String {
        val name = ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ))?.name ?: obj
        val lower = name.replaceFirstChar { it.lowercase() }
        if (count > 1) return "$count ${lower}s"
        val article = if (lower.first() in "aeiou") "an" else "a"
        return "$article $lower"
    }

    private companion object {
        const val THIEVING = "stat.thieving"
        const val SPOT_RANGE = 5
        const val SHADOW_VEIL_ACTIVE = "varbit.arceuus_shadow_veil_active"
        const val SHADOW_VEIL_CHANCE = 15
        const val CAUGHT_SHOUT = "Hey! Get your hands off there!"
        const val STALL_SEQ = "seq.human_pickuptable"
        const val PICKPOCKET_SEQ = "seq.human_pickpocket"
        const val PICK_SYNTH = "synth.pick"
        const val STUN_SPOTANIM = "spotanim.stunned_thieving"
        const val STUN_SPOTANIM_HEIGHT = 124
        const val STUN_BLOCK_SEQ = "seq.human_unarmedblock"
        const val STUN_SYNTH = "synth.thieving_stunned"
        const val FREEZE_TIMER = "timer.combat_freeze"
        const val MAX_POUCHES = 28
        const val STEAL_OP = "Steal-from"
        const val PICKPOCKET_OP = "Pickpocket"
    }
}
