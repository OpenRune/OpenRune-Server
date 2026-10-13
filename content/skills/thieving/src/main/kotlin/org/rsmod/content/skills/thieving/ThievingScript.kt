package org.rsmod.content.skills.thieving

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import dtx.rs.RSDropTable
import jakarta.inject.Inject
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.hands
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.table.thieving.ThievingPickpocketRow
import org.rsmod.api.table.thieving.ThievingStallRow
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.content.skills.thieving.ThievingEquipment.DODGY_NECKLACE
import org.rsmod.content.skills.thieving.ThievingEquipment.GLOVES_OF_SILENCE
import org.rsmod.content.skills.thieving.ThievingEquipment.degradeGlovesOfSilence
import org.rsmod.content.skills.thieving.ThievingEquipment.tryDodgyNecklace
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private typealias ThievingDropTable = RSDropTable<Player, DropRollItem>

class ThievingScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val dropRegistry: DropTableRegistry,
    private val petRewards: PetRewards,
    private val invisibleLevels: InvisibleLevels,
) : PluginScript() {
    private val restockingUntil = HashMap<CoordGrid, Int>()
    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        for (stall in ThievingStallRow.all()) {
            val loc = stall.loc.internalName
            val table =
                dropRegistry.forLoc(loc) ?: error("No stall drop table registered for '$loc'.")
            onOpLoc2(loc) { stealFromStall(it.loc, stall, table) }
        }
        onOpLoc2(KELDAGRIM_CLOTHES_STALL) {
            mes("You don't really see anything you'd want to steal from this stall.")
        }
        val bound = HashSet<Int>()
        for (target in ThievingPickpocketRow.all()) {
            val table = target.dropTable()
            for (type in target.npcTypes()) {
                val slot = type.pickpocketSlot() ?: continue
                if (bound.add(type.id)) {
                    onPickpocketOp(type, slot, target, table)
                }
            }
        }
        warnUnboundPickpockets(bound)
        onOpHeld3(GLOVES_OF_SILENCE) { mes(ThievingEquipment.glovesCondition(player)) }
        onOpWorn2(GLOVES_OF_SILENCE) { mes(ThievingEquipment.glovesCondition(player)) }
        onOpHeld3(DODGY_NECKLACE) { mes(ThievingEquipment.dodgyCondition(player)) }
        onOpWorn2(DODGY_NECKLACE) { mes(ThievingEquipment.dodgyCondition(player)) }
        for (target in ThievingPickpocketRow.all().distinctBy { it.pouchObj }) {
            val pouch = target.pouchObj ?: continue
            onOpHeld1(pouch) { openPouches(pouch, target.pouchCoins, all = true) }
            onOpHeld2(pouch) { openPouches(pouch, target.pouchCoins, all = false) }
        }
    }

    private fun ThievingPickpocketRow.dropTable(): ThievingDropTable =
        dropRegistry.forPickpocket(lootTable)
            ?: error("No pickpocket drop table registered for '$lootTable'.")

    private fun warnUnboundPickpockets(bound: Set<Int>) {
        val unbound =
            ServerCacheManager.getNpcs().values.filter {
                it.id !in bound && it.pickpocketSlot() != null
            }
        if (unbound.isEmpty()) return
        val names = unbound.map { it.name }.distinct().sorted()
        logger.warn {
            "${unbound.size} pickpocketable npc type(s) have no thieving target: " +
                names.joinToString()
        }
    }

    private fun ScriptContext.onPickpocketOp(
        type: NpcServerType,
        slot: Int,
        target: ThievingPickpocketRow,
        table: ThievingDropTable,
    ) {
        when (slot) {
            1 -> onProtectedEvent<NpcEvents.Op1>(type.id) { pickpocket(it.npc, target, table) }
            2 -> onProtectedEvent<NpcEvents.Op2>(type.id) { pickpocket(it.npc, target, table) }
            3 -> onProtectedEvent<NpcEvents.Op3>(type.id) { pickpocket(it.npc, target, table) }
            4 -> onProtectedEvent<NpcEvents.Op4>(type.id) { pickpocket(it.npc, target, table) }
            5 -> onProtectedEvent<NpcEvents.Op5>(type.id) { pickpocket(it.npc, target, table) }
        }
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
        val loot = rollDrops(table).map { (drop, obj) -> LootDrop(obj, drop.rollCount(random)) }
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
            CollectionLog.grant(player, drop.obj, drop.count)
            mes("You steal ${describe(drop.obj, drop.count)}.")
        }
        statAdvance(THIEVING, stall.experience)
        restock(loc, stall)
    }

    private fun ProtectedAccess.rollDrops(
        table: ThievingDropTable,
    ): List<Pair<DropRollItem, String>> {
        val drops =
            when (val result = table.roll(player, ArgMap()).flatten()) {
                is RollResult.Nothing -> emptyList()
                is RollResult.Single -> listOf(result.result)
                is RollResult.ListOf -> result.results
            }
        return drops
            .flatMap { it.withBonusDrops() }
            .filter { !it.isNothing && it.condition(player) }
            .map { it to (it.transformObj(player) ?: it.obj) }
    }

    private fun DropRollItem.withBonusDrops(): List<DropRollItem> =
        listOf(this) + bonusDrops.flatMap { it.withBonusDrops() }

    private fun ProtectedAccess.findSpotter(stall: ThievingStallRow): Npc? {
        val owners = stall.owners.mapTo(HashSet()) { it.id }
        val guards = stall.guards.mapTo(HashSet()) { it.id }
        return npcRepo
            .findAll(ZoneKey.from(player.coords), zoneRadius = 1)
            .filter { it.coords.level == player.coords.level }
            .filter { it.coords.chebyshevDistance(player.coords) <= SPOT_RANGE }
            .filter { npc ->
                when (npc.type.id) {
                    in owners -> lineOfSight(npc.coords, player.coords)
                    in guards -> lineOfWalk(player.coords, npc.coords)
                    else -> false
                }
            }
            .minByOrNull { it.coords.chebyshevDistance(player.coords) }
    }

    private fun ProtectedAccess.caughtAtStall(spotter: Npc, stall: ThievingStallRow) {
        spotter.say(CAUGHT_SHOUT)
        val guards = stall.guards.mapTo(HashSet()) { it.id }
        val guard =
            if (spotter.type.id in guards) {
                spotter
            } else {
                npcRepo
                    .findAll(ZoneKey.from(player.coords), zoneRadius = 1)
                    .filter { it.type.id in guards }
                    .minByOrNull { it.coords.chebyshevDistance(player.coords) }
            }
        guard?.opPlayer2(player, aiPlayerInteractions)
    }

    private fun ProtectedAccess.restock(loc: BoundLocInfo, stall: ThievingStallRow) {
        val emptyLoc = stall.emptyLoc
        if (emptyLoc != null) {
            locRepo.change(loc, emptyLoc.internalName, stall.restockTicks)
        } else {
            restockingUntil[loc.coords] = mapClock + stall.restockTicks
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
        val pouch = target.pouchObj
        if (pouch != null && inv.count(pouch) >= ThievingEquipment.pouchLimit(player)) {
            mes("You need to empty your coin pouches before you can continue pickpocketing.")
            return
        }
        val double = random.of(100) < ThievingEquipment.rogueDoubleChance(player)
        val loot = rollPickpocketLoot(table, target, double)
        if (!player.addLoot(inv, loot, commit = false)) {
            mes("You don't have enough inventory space.")
            return
        }
        faceEntitySquare(npc)
        mes("You attempt to pick $owner's pocket.", ChatType.Spam)
        delay(1)
        val rates = ThievingEquipment.rates(player, target.successLow, target.successHigh)
        if (!statRandom(THIEVING, rates.low, rates.high, invisibleLevels)) {
            if (rates.glovesApplied) {
                degradeGlovesOfSilence()
            }
            failPickpocket(npc, target, owner)
            return
        }
        if (!player.addLoot(inv, loot)) {
            mes("You don't have enough inventory space.")
            return
        }
        loot.forEach { CollectionLog.grant(player, it.obj, it.count) }
        mes("You pick $owner's pocket.", ChatType.Spam)
        anim(PICKPOCKET_SEQ)
        soundSynth(PICK_SYNTH)
        if (double) {
            mes("Your rogue clothing allows you to steal twice as much loot!", ChatType.Spam)
        }
        statAdvance(THIEVING, target.experience)
        if (target.hotPocketDamage > 0 && player.hands?.isType(ICE_GLOVES) != true) {
            queueHit(delay = 1, type = HitType.Typeless, damage = target.hotPocketDamage)
        }
        petRewards.rollSkillingPet(player, ROCKY, THIEVING, target.rockyChance)
    }

    private fun ProtectedAccess.rollPickpocketLoot(
        table: ThievingDropTable,
        target: ThievingPickpocketRow,
        double: Boolean,
    ): List<LootDrop> {
        val loot = mutableListOf<LootDrop>()
        for ((drop, obj) in rollDrops(table)) {
            val rolled = LootDrop(obj, drop.rollCount(random))
            loot +=
                if (double && !drop.obj.startsWith(CLUE_PREFIX)) {
                    rolled.doubled(target.pouchObj) { random.of(target.pouchCoins) }
                } else {
                    listOf(rolled)
                }
        }
        return loot
    }

    private fun ProtectedAccess.openPouches(pouch: String, coins: IntRange, all: Boolean) {
        val count = if (all) inv.count(pouch) else 1
        if (count == 0) return
        val total = (1..count).sumOf { random.of(coins).toLong() }
        if (!player.exchangePouches(inv, pouch, count, total)) {
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
        if (tryDodgyNecklace()) return
        npc.say(target.caughtShout)
        npc.facePlayer(player)
        try {
            stun(target.stunTicks)
            delay(1)
            spotanim(STUN_SPOTANIM, height = STUN_SPOTANIM_HEIGHT)
            anim(STUN_BLOCK_SEQ)
            soundSynth(STUN_SYNTH)
            queueHit(delay = 1, type = HitType.Typeless, damage = random.of(target.stunDamage))
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
        // The freeze starts a tick before the stun spotanim; stun times count from the spotanim.
        player.timer(FREEZE_TIMER, ticks + 1)
    }

    private fun pocketOwner(npc: Npc, target: ThievingPickpocketRow): String =
        pocketOwnerName(npc.visType.name, target.lowercaseName, target.properName)

    private fun describe(obj: String, count: Int): String {
        val name = ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ))?.name ?: obj
        val lower = name.replaceFirstChar { it.lowercase() }
        if (count > 1) return "$count x $lower"
        val article = if (lower.first() in "aeiou") "an" else "a"
        return "$article $lower"
    }

    private companion object {
        const val THIEVING = "stat.thieving"
        const val SPOT_RANGE = 5
        const val KELDAGRIM_CLOTHES_STALL = "loc.dwarf_market_clothes"
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
        const val CLUE_PREFIX = "obj.trail_"
        const val ROCKY = "obj.skillpetthieving"
        const val ICE_GLOVES = "obj.ice_gloves"
    }
}
