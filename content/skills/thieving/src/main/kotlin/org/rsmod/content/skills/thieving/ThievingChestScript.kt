package org.rsmod.content.skills.thieving

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.table.thieving.ThievingChestConfigRow
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ThievingChestScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val dropRegistry: DropTableRegistry,
    private val invisibleLevels: InvisibleLevels,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (chest in ThievingChestConfigRow.all()) {
            val loc = chest.loc.internalName
            val table =
                dropRegistry.forLoc(loc) ?: error("No chest drop table registered for '$loc'.")
            when (chest.chestMethod) {
                ChestMethod.Trapped -> {
                    onOpLoc1(loc) { triggerTrap(chest) }
                    onOpLoc2(loc) { searchForTraps(it.loc, chest, table) }
                }
                ChestMethod.Locked -> {
                    onOpLoc1(loc) { mes("The chest is locked.") }
                    onOpLoc2(loc) { pickLock(it.loc, chest, table) }
                }
                ChestMethod.Picklock -> onOpLoc1(loc) { picklock(it.loc, chest, table) }
            }
        }
        val emptyLocs =
            ThievingChestConfigRow.all().mapNotNullTo(HashSet()) { it.emptyLoc?.internalName }
        for (loc in emptyLocs) {
            onOpLoc1(loc) { mes(ALREADY_LOOTED) }
            onOpLoc2(loc) { mes(ALREADY_LOOTED) }
        }
    }

    private suspend fun ProtectedAccess.triggerTrap(chest: ThievingChestConfigRow) {
        arriveDelay()
        mes("You have activated a trap on the chest.", ChatType.Spam)
        anim(LOCKED_CHEST_SEQ)
        soundSynth(TRAP_SYNTH)
        val damage = trapDamage(stat(HITPOINTS), chest.trapDamageScale, chest.trapDamageBase)
        queueHit(delay = 1, type = HitType.Typeless, damage = damage)
    }

    private suspend fun ProtectedAccess.searchForTraps(
        loc: BoundLocInfo,
        chest: ThievingChestConfigRow,
        table: ThievingDropTable,
    ) {
        arriveDelay()
        if (!canAttempt(chest)) return
        mes("You search the chest for traps.", ChatType.Spam)
        mes("You find a trap on the chest.", ChatType.Spam)
        anim(LOCKED_CHEST_SEQ)
        delay(2)
        mes("You disable the trap.", ChatType.Spam)
        soundSynth(LOCKED_SYNTH)
        delay(2)
        mes("You open the chest.", ChatType.Spam)
        anim(LOCKED_CHEST_SEQ)
        soundSynth(CHEST_OPEN_SYNTH)
        delay(2)
        val loot = takeLoot(loc, chest, table) ?: return
        anim(OPEN_CHEST_SEQ)
        mes(lootMessage(chest, loot), ChatType.Spam)
        statAdvance(THIEVING, chest.experience)
        alertGuards(chest)
        val teleport = chest.teleport
        if (teleport != null) {
            mes("Suddenly a second magical trap triggers.")
            telejump(teleport, TeleportType.Exempt)
        }
        emptyChest(loc, chest)
    }

    private suspend fun ProtectedAccess.pickLock(
        loc: BoundLocInfo,
        chest: ThievingChestConfigRow,
        table: ThievingDropTable,
    ) {
        arriveDelay()
        if (!canAttempt(chest)) return
        mes("You attempt to pick the lock.", ChatType.Spam)
        mes("You manage to pick the lock.", ChatType.Spam)
        soundSynth(UNLOCK_SYNTH)
        delay(2)
        mes("You open the chest.", ChatType.Spam)
        soundSynth(CHEST_OPEN_SYNTH)
        delay(2)
        val loot = takeLoot(loc, chest, table) ?: return
        anim(LOCKED_CHEST_SEQ)
        mes(lootMessage(chest, loot), ChatType.Spam)
        statAdvance(THIEVING, chest.experience)
        emptyChest(loc, chest)
    }

    private suspend fun ProtectedAccess.picklock(
        loc: BoundLocInfo,
        chest: ThievingChestConfigRow,
        table: ThievingDropTable,
    ) {
        arriveDelay()
        if (!canAttempt(chest)) return
        mes("You attempt to picklock the chest.", ChatType.Spam)
        anim(LOCKED_CHEST_SEQ)
        if (chest.attemptSound) {
            soundSynth(ATTEMPT_SYNTH)
        }
        delay(2)
        val bonus = if (hasLockpick()) chest.lockpickBonus else 0
        val low = chest.successLow + bonus
        val high = chest.successHigh + bonus
        if (!statRandom(THIEVING, low, high, invisibleLevels)) {
            failPicklock(chest)
            return
        }
        mes("You manage to unlock the chest.", ChatType.Spam)
        soundSynth(CHEST_OPEN_SYNTH)
        delay(chest.lootDelay)
        takeLoot(loc, chest, table) ?: return
        anim(OPEN_CHEST_SEQ)
        mes("You steal some loot from the chest.", ChatType.Spam)
        statAdvance(THIEVING, chest.experience)
        soundSynth(CHEST_CLOSE_SYNTH, delay = CHEST_CLOSE_SYNTH_DELAY)
        locRepo.change(loc, chest.openLoc.internalName, chest.respawnTicks)
    }

    private suspend fun ProtectedAccess.failPicklock(chest: ThievingChestConfigRow) {
        mes("You fail to picklock the chest.", ChatType.Spam)
        soundSynth(LOCKED_SYNTH)
        val teleport = chest.teleport ?: return
        if (!statRandom(THIEVING, chest.teleportLow, chest.teleportHigh, invisibleBoost = 0)) return
        if (chest.xericTeleport) {
            anim(XERIC_TELEPORT_SEQ)
            spotanim(XERIC_TELEPORT_SPOTANIM)
        } else {
            anim(TELEPORT_SEQ)
            spotanim(TELEPORT_SPOTANIM, height = TELEPORT_SPOTANIM_HEIGHT)
        }
        delay(2)
        mes("You have activated a trap on the chest.", ChatType.Spam)
        delay(1)
        val dest = mapFindSquareNone(teleport, minRadius = 0, maxRadius = chest.teleportRadius)
        telejump(dest ?: teleport, TeleportType.Exempt)
        resetAnim()
    }

    private fun ProtectedAccess.canAttempt(chest: ThievingChestConfigRow): Boolean {
        if (stat(THIEVING) < chest.level) {
            mes("You need to be level ${chest.level} to steal from this chest.")
            return false
        }
        if (chest.needsLockpick && !hasLockpick()) {
            mes("You need a lockpick for this lock.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.hasLockpick(): Boolean = LOCKPICKS.any { it in inv }

    /**
     * The chest is shared with everyone nearby, so whoever gets here second finds it already
     * emptied and leaves with nothing.
     */
    private fun ProtectedAccess.takeLoot(
        loc: BoundLocInfo,
        chest: ThievingChestConfigRow,
        table: ThievingDropTable,
    ): List<LootDrop>? {
        if (!locRepo.findLoc(loc.coords, chest.loc.internalName)) {
            mes(ALREADY_LOOTED)
            return null
        }
        val loot =
            player.rollDrops(table).map { (drop, obj) -> LootDrop(obj, drop.rollCount(random)) }
        if (!player.addLoot(inv, loot)) {
            mes("You don't have enough inventory space to take anything from the chest.")
            return null
        }
        loot.forEach { CollectionLog.grant(player, it.obj, it.count) }
        return loot
    }

    private fun lootMessage(chest: ThievingChestConfigRow, loot: List<LootDrop>): String {
        val first = loot.firstOrNull()
        if (!chest.describeLoot || first == null) return "You find treasure inside!"
        val name = ServerCacheManager.getItem(first.obj.asRSCM(RSCMType.OBJ))?.name ?: first.obj
        return "You find some ${lootNoun(name, first.count)} inside."
    }

    private suspend fun ProtectedAccess.emptyChest(
        loc: BoundLocInfo,
        chest: ThievingChestConfigRow,
    ) {
        locRepo.change(loc, chest.openLoc.internalName, chest.respawnTicks)
        val emptyLoc = chest.emptyLoc ?: return
        delay(OPEN_TICKS)
        locRepo.change(loc, emptyLoc.internalName, chest.respawnTicks - OPEN_TICKS)
    }

    private fun ProtectedAccess.alertGuards(chest: ThievingChestConfigRow) {
        val guard = chest.guard ?: return
        npcRepo
            .findAll(ZoneKey.from(player.coords), zoneRadius = 1)
            .filter { it.type.id == guard.id && it.coords.level == player.coords.level }
            .filter { it.coords.chebyshevDistance(player.coords) <= GUARD_RANGE }
            .forEach { it.opPlayer2(player, aiPlayerInteractions) }
    }

    private companion object {
        const val THIEVING = "stat.thieving"
        const val HITPOINTS = "stat.hitpoints"
        const val OPEN_TICKS = 3
        const val GUARD_RANGE = 8
        const val ALREADY_LOOTED = "It looks like this chest has already been looted."
        const val LOCKED_CHEST_SEQ = "seq.human_lockedchest"
        const val OPEN_CHEST_SEQ = "seq.human_openchest"
        const val TELEPORT_SEQ = "seq.human_castteleport"
        const val TELEPORT_SPOTANIM = "spotanim.teleport_casting"
        const val TELEPORT_SPOTANIM_HEIGHT = 92
        const val XERIC_TELEPORT_SEQ = "seq.arceuus_necromancy_anim"
        const val XERIC_TELEPORT_SPOTANIM = "spotanim.xeric_teleport_spotanim"
        const val TRAP_SYNTH = "synth.lever"
        const val LOCKED_SYNTH = "synth.locked"
        const val UNLOCK_SYNTH = "synth.unlock"
        const val ATTEMPT_SYNTH = "synth.safe_crack"
        const val CHEST_OPEN_SYNTH = "synth.chest_open"
        const val CHEST_CLOSE_SYNTH = "synth.chest_close"
        const val CHEST_CLOSE_SYNTH_DELAY = 45
        val LOCKPICKS = setOf("obj.lockpick", "obj.kr_hairclip")
    }
}
