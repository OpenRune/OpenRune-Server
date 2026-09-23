package org.rsmod.content.skills.thieving

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpNpc3
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
        for (stall in ThievingTables.stalls) {
            onOpLoc2(stall.loc) { stealFromStall(it.loc, stall) }
        }
        for (target in ThievingTables.pickpockets) {
            for (npc in target.npcs) {
                onOpNpc3(npc) { pickpocket(it.npc, target) }
            }
            val pouch = target.pouch ?: continue
            onOpHeld1(pouch.obj) { openPouches(pouch, all = true) }
            onOpHeld2(pouch.obj) { openPouches(pouch, all = false) }
        }
    }

    private suspend fun ProtectedAccess.stealFromStall(loc: BoundLocInfo, stall: Stall) {
        if (player.isFrozen) return
        arriveDelay()
        faceLoc(loc)
        if (stat(THIEVING) < stall.level) {
            mes("You need to be level ${stall.level} to steal from this stall.")
            return
        }
        if (restockingUntil.getOrDefault(loc.coords, 0) > mapClock) return
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        mes(stall.attemptMessage, ChatType.Spam)
        val spotter = findSpotter(stall)
        if (spotter != null) {
            caughtAtStall(spotter, stall)
            return
        }
        anim(STALL_SEQ)
        delay(2)
        val loot = stall.loot.roll(random)
        val count = random.of(loot.min, loot.max)
        invAdd(inv, loot.obj, count)
        mes("You steal ${describe(loot.obj, count)}.")
        statAdvance(THIEVING, stall.xp)
        restock(loc, stall)
    }

    private fun ProtectedAccess.findSpotter(stall: Stall): Npc? {
        val watchers = stall.owners + stall.guards
        return npcRepo
            .findAll(ZoneKey.from(player.coords), zoneRadius = 1)
            .filter { npc -> watchers.any { npc.type.isType(it) } }
            .filter { it.coords.level == player.coords.level }
            .filter { it.coords.chebyshevDistance(player.coords) <= SPOT_RANGE }
            .filter { lineOfSight(it.coords, player.coords) }
            .minByOrNull { it.coords.chebyshevDistance(player.coords) }
    }

    private fun ProtectedAccess.caughtAtStall(spotter: Npc, stall: Stall) {
        spotter.say(CAUGHT_SHOUT)
        val guard =
            if (stall.guards.any { spotter.type.isType(it) }) {
                spotter
            } else {
                npcRepo
                    .findAll(ZoneKey.from(player.coords), zoneRadius = 1)
                    .filter { npc -> stall.guards.any { npc.type.isType(it) } }
                    .minByOrNull { it.coords.chebyshevDistance(player.coords) }
            }
        guard?.opPlayer2(player, aiPlayerInteractions)
    }

    private fun ProtectedAccess.restock(loc: BoundLocInfo, stall: Stall) {
        if (stall.emptyLoc != null) {
            locRepo.change(loc, stall.emptyLoc, stall.restockTicks)
        } else {
            restockingUntil[loc.coords] = mapClock + stall.restockTicks
        }
    }

    private suspend fun ProtectedAccess.pickpocket(npc: Npc, target: Pickpocket) {
        if (player.isFrozen) return
        val owner = pocketOwner(npc, target)
        if (stat(THIEVING) < target.level) {
            mes("You need to be level ${target.level} to pickpocket $owner.")
            return
        }
        val pouch = target.pouch
        if (pouch != null && inv.count(pouch.obj) >= MAX_POUCHES) {
            mes("You need to empty your coin pouches before you can continue pickpocketing.")
            return
        }
        if (inv.isFull() && (pouch == null || inv.count(pouch.obj) == 0)) {
            mes("You don't have enough inventory space.")
            return
        }
        faceEntitySquare(npc)
        mes("You attempt to pick $owner's pocket.", ChatType.Spam)
        delay(1)
        if (!statRandom(THIEVING, target.lowChance, target.highChance, invisibleBoost = 0)) {
            failPickpocket(npc, target, owner)
            return
        }
        mes("You pick $owner's pocket.", ChatType.Spam)
        anim(PICKPOCKET_SEQ)
        soundSynth(PICK_SYNTH)
        if (pouch != null) {
            invAdd(inv, pouch.obj)
        }
        val loot = target.loot?.roll(random)
        if (loot != null) {
            val count = random.of(loot.min, loot.max)
            invAdd(inv, loot.obj, count)
            mes("You steal ${describe(loot.obj, count)}.", ChatType.Spam)
        }
        statAdvance(THIEVING, target.xp)
    }

    private fun ProtectedAccess.openPouches(pouch: CoinPouch, all: Boolean) {
        val count = if (all) inv.count(pouch.obj) else 1
        if (count == 0 || invDel(inv, pouch.obj, count).failure) return
        invAdd(inv, COINS, count * pouch.coins)
        val message = if (count > 1) "You open all of the pouches." else "You open the coin pouch."
        mes(message, ChatType.Spam)
    }

    private suspend fun ProtectedAccess.failPickpocket(npc: Npc, target: Pickpocket, owner: String) {
        mes("You fail to pick $owner's pocket.", ChatType.Spam)
        npc.say(target.caughtShout)
        npc.facePlayer(player)
        stun()
        delay(1)
        spotanim(STUN_SPOTANIM, height = STUN_SPOTANIM_HEIGHT)
        anim(STUN_BLOCK_SEQ)
        soundSynth(STUN_SYNTH)
        queueHit(delay = 1, type = HitType.Typeless, damage = target.stunDamage)
        delay(1)
        mes("You've been stunned!", ChatType.Spam)
    }

    private fun ProtectedAccess.stun() {
        player.frozen = true
        player.routeDestination.clear()
        player.timer(FREEZE_TIMER, STUN_TICKS)
    }

    private fun pocketOwner(npc: Npc, target: Pickpocket): String {
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
        const val CAUGHT_SHOUT = "Hey! Get your hands off there!"
        const val STALL_SEQ = "seq.human_pickuptable"
        const val PICKPOCKET_SEQ = "seq.human_pickpocket"
        const val PICK_SYNTH = "synth.pick"
        const val STUN_SPOTANIM = "spotanim.stunned_thieving"
        const val STUN_SPOTANIM_HEIGHT = 124
        const val STUN_BLOCK_SEQ = "seq.human_unarmedblock"
        const val STUN_SYNTH = "synth.thieving_stunned"
        const val STUN_TICKS = 9
        const val FREEZE_TIMER = "timer.combat_freeze"
        const val MAX_POUCHES = 28
        const val COINS = "obj.coins"
    }
}
