package org.rsmod.content.skills.magic.arceuus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.npc.owner.assignSpawnOwner
import org.rsmod.api.npc.owner.ownedBy
import org.rsmod.api.player.output.UpdateInventory.resendSlot
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.ui.IfOverlayButtonT
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onApObjT
import org.rsmod.api.script.onEvent
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.LineValidator
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class ReanimationScript
@Inject
constructor(
    private val eventBus: EventBus,
    private val protectedAccess: ProtectedAccessLauncher,
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val objRepo: ObjRepository,
    private val npcRepo: NpcRepository,
    private val npcList: NpcList,
    private val collision: CollisionFlagMap,
    private val worldRepo: WorldRepository,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : PluginScript() {
    private val lineValidator = LineValidator(collision)

    // TODO: Proper `_initial` head handling. Drop tables currently spawn plain heads, so nothing
    //  produces `_initial` variants. They need a drop handler that spawns them with the real
    //  params (ownership=self, despawn=200, neverturnpublic), plus a per-head drop location for
    //  the 31x31 "where it died" rule instead of allowing initial heads anywhere.
    private val corpseIds: Map<Int, Pair<ReanimatedHead, Boolean>> by lazy {
        buildMap {
            for (head in ReanimatedHead.entries) {
                put(head.corpseObj.asRSCM(RSCMType.OBJ), head to false)
                put(head.initialCorpseObj.asRSCM(RSCMType.OBJ), head to true)
            }
        }
    }

    override fun ScriptContext.startup() {
        for (spell in ReanimationSpell.entries) {
            onApObjT(spell.component) { reanimate(spell, it.obj.type, Origin.Ground(it.obj)) }

            val key =
                EventBus.composeLongKey(
                    spell.component.asRSCM(RSCMType.COMPONENT),
                    INVENTORY_ITEMS.asRSCM(RSCMType.COMPONENT),
                )
            onEvent<IfOverlayButtonT>(key) { player.castOnInventory(spell, targetSlot, targetObj?.id) }
        }
    }

    private fun Player.castOnInventory(spell: ReanimationSpell, slot: Int, headId: Int?) {
        if (headId == null || isDelayed || isAccessProtected) {
            resendSlot(inv, 0)
            return
        }
        clearPendingAction(eventBus)
        resetFaceEntity()
        protectedAccess.launch(this) { reanimate(spell, headId, Origin.Inventory(slot)) }
    }

    private suspend fun ProtectedAccess.reanimate(
        spell: ReanimationSpell,
        headId: Int,
        origin: Origin,
    ) {
        val (head, initial) = corpseIds[headId] ?: return mes(CANNOT_REANIMATE_MESSAGE)
        if (head.spell != spell) {
            mes(CANNOT_REANIMATE_MESSAGE)
            return
        }
        if (!initial && !inArea(DARK_ALTAR_AREA, player.coords)) {
            mes(TOO_FAR_MESSAGE)
            return
        }
        if (hasLivingReanimation()) {
            mes("You should finish off your last one first.")
            return
        }
        val tile = spawnTile(origin.spawnNear(player))
        val stepAway = if (origin is Origin.Inventory) freeAdjacentTile(player.coords) else null
        if (tile == null || (origin is Origin.Inventory && stepAway == null)) {
            mes("The creature wouldn't have room to re-animate there.")
            return
        }

        val spellObj = ServerCacheManager.getItem(spell.obj.asRSCM(RSCMType.OBJ)) ?: return
        val magicSpell = spells.getObjSpell(spellObj) ?: return
        if (runes.attemptCast(player, magicSpell).isFailure()) {
            return
        }
        if (origin is Origin.Inventory && invDel(inv, head.corpseObjFor(initial), 1, origin.slot).failure) {
            return
        }
        val dropped =
            if (origin is Origin.Inventory) {
                objRepo.add(head.corpseObjFor(initial), tile, DROP_DURATION_TICKS, player)
            } else {
                null
            }

        anim(CAST_ANIM)
        spotanim(CAST_SPOTANIM)
        statAdvance("stat.magic", magicSpell.castXp)
        player.faceSquare(tile)

        val projectile = projectileTo(tile)
        worldRepo.projAnim(projectile)
        var travelTicks = maxOf(1, projectile.serverCycles)
        if (stepAway != null) {
            delay(1)
            walk(stepAway)
            travelTicks--
        }
        if (travelTicks > 0) {
            delay(travelTicks)
        }
        when (origin) {
            is Origin.Ground -> if (!objRepo.del(origin.obj)) return
            is Origin.Inventory -> dropped?.let { objRepo.del(it) }
        }
        spotanimMap(worldRepo, IMPACT_SPOTANIM, tile)
        delay(SPAWN_DELAY_TICKS)

        if (!isWalkable(tile) || hasLivingReanimation()) {
            return
        }
        val npc = Npc(head.npc, tile)
        npc.respawns = false
        npcRepo.add(npc, LIFETIME_TICKS)
        npc.assignSpawnOwner(player, mapClock)
        npc.opPlayer2(player, aiPlayerInteractions)
    }

    private fun ReanimatedHead.corpseObjFor(initial: Boolean): String =
        if (initial) initialCorpseObj else corpseObj

    private fun ProtectedAccess.projectileTo(tile: CoordGrid): ProjAnim {
        val base =
            ProjAnim.fromPlayerToCoord(
                player,
                tile,
                PROJECTILE_SPOTANIM.asRSCM(RSCMType.SPOTANIM),
                PROJECTILE_TYPE,
            )
        val distance = player.bounds().distanceTo(Bounds(tile))
        return base.copy(
            startHeight = PROJECTILE_START_HEIGHT,
            endHeight = PROJECTILE_END_HEIGHT,
            startTime = PROJECTILE_DELAY,
            endTime = PROJECTILE_DELAY + PROJECTILE_DURATION + PROJECTILE_STEP * distance,
            angle = PROJECTILE_ANGLE,
            progress = 0,
        )
    }

    private fun ProtectedAccess.hasLivingReanimation(): Boolean =
        npcList.ownedBy(player).any { it.id in ReanimatedHead.byNpcId && it.hitpoints > 0 }

    private fun spawnTile(near: CoordGrid): CoordGrid? =
        if (isWalkable(near)) near else freeAdjacentTile(near)

    private fun freeAdjacentTile(near: CoordGrid): CoordGrid? {
        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx == 0 && dz == 0) continue
                val candidate = near.translate(dx, dz)
                if (isWalkable(candidate) && canWalk(near, candidate)) return candidate
            }
        }
        return null
    }

    private fun isWalkable(coords: CoordGrid): Boolean =
        collision[coords.x, coords.z, coords.level] and BLOCKED_FLAGS == 0

    private fun canWalk(from: CoordGrid, to: CoordGrid): Boolean =
        lineValidator.hasLineOfWalk(from.level, from.x, from.z, to.x, to.z)

    private sealed class Origin {
        abstract fun spawnNear(player: Player): CoordGrid

        class Ground(val obj: Obj) : Origin() {
            override fun spawnNear(player: Player): CoordGrid = obj.coords
        }

        class Inventory(val slot: Int) : Origin() {
            override fun spawnNear(player: Player): CoordGrid = player.coords
        }
    }

    internal companion object {
        const val INVENTORY_ITEMS = "component.inventory:items"
        const val DARK_ALTAR_AREA = "area.dark_altar"
        const val CAST_ANIM = "seq.arceuus_necromancy_playeranim"
        const val CAST_SPOTANIM = "spotanim.arceuus_necromancy_playerspot"
        const val PROJECTILE_SPOTANIM = "spotanim.arceuus_necromancy_projanim"
        const val IMPACT_SPOTANIM = "spotanim.arceuus_necromancy_spawning"
        const val PROJECTILE_TYPE = "projanim.magic_spell"
        const val PROJECTILE_START_HEIGHT = 120
        const val PROJECTILE_END_HEIGHT = 0
        const val PROJECTILE_DELAY = 50
        const val PROJECTILE_DURATION = 30
        const val PROJECTILE_STEP = 5
        const val PROJECTILE_ANGLE = 15
        const val SPAWN_DELAY_TICKS = 5
        const val LIFETIME_TICKS = 300
        const val DROP_DURATION_TICKS = 100
        const val CANNOT_REANIMATE_MESSAGE = "This spell cannot reanimate that item."
        const val TOO_FAR_MESSAGE =
            "That creature cannot be reanimated here. The power of the crystals by the Dark " +
                "Altar will increase the potency of the spell."
        const val BLOCKED_FLAGS =
            CollisionFlag.LOC or
                CollisionFlag.BLOCK_WALK or
                CollisionFlag.GROUND_DECOR or
                CollisionFlag.BLOCK_PLAYERS
    }
}
