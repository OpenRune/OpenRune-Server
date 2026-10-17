package org.rsmod.content.other.multicannon

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.rangedLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * Dwarf multicannon: set-up, loading, firing, pick-up and decay, with the varps RuneLite's Cannon
 * plugin reads (`dropcannon` stage, `rockthrower` ammo, `ownedmcannon` coord, `mcannon_decayed`).
 * A cannon left behind by a logout, or one that decays, goes back to the owner's bank.
 */
@Singleton
class DwarfMulticannon
@Inject
constructor(
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val npcRepo: NpcRepository,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
    private val npcHitModifier: NpcHitModifier,
) : PluginScript() {
    private val cannons = HashMap<Player, Cannon>()

    override fun ScriptContext.startup() {
        onOpHeld1(PARTS[0]) { setUp() }
        for (stage in 1..3) {
            onOpLoc1(STAGE_LOCS[stage - 1]) { pickUp(it.loc.coords) }
            for (part in PARTS) {
                onOpLocU(STAGE_LOCS[stage - 1], part) { continueSetUp(it.loc.coords) }
            }
        }
        onOpLoc1(CANNON_LOC) { fire(it.loc.coords) }
        onOpLoc2(CANNON_LOC) { pickUp(it.loc.coords) }
        onOpLoc3(CANNON_LOC) { empty(it.loc.coords) }
        onOpLoc4(CANNON_LOC) { load(it.loc.coords, startFiring = false) }
        onOpLocU(CANNON_LOC, CANNONBALL) { load(it.loc.coords, startFiring = false) }
        onPlayerSoftTimer(FIRE_TIMER) { player.fireTick() }
        onPlayerSoftTimer(DECAY_TIMER) { player.decay() }
        onPlayerLogout { player.removeCannon() }
        onPlayerLogin { player.reclaimToBank() }
    }

    fun cannonOf(player: Player): Cannon? = cannons[player]

    suspend fun ProtectedAccess.setUp() {
        if (cannons.containsKey(player) || player.cannonStage != 0) {
            mes("You cannot construct more than one cannon at a time.")
            return
        }
        val origin = player.coords.translate(-1, -1)
        noCannonMessage(origin)?.let {
            mes(it)
            return
        }
        if (!hasSpace(origin)) {
            mes("There isn't enough space to set up here.")
            return
        }
        val cannon = Cannon(player, origin)
        cannons[player] = cannon
        player.cannonDecayed = false
        player.cannonCoord = packCoord(origin)
        player.softTimer(DECAY_TIMER, DECAY_CYCLES)
        buildStages(cannon)
    }

    private suspend fun ProtectedAccess.continueSetUp(coords: CoordGrid) {
        val cannon = ownCannon(coords) ?: return
        buildStages(cannon)
    }

    private suspend fun ProtectedAccess.buildStages(cannon: Cannon) {
        while (cannon.stage < 4) {
            val part = PARTS[cannon.stage]
            if (inv.count(part) == 0) {
                return
            }
            anim(SETUP_ANIM)
            if (!invDel(inv, part, 1).success) {
                return
            }
            cannon.stage++
            player.cannonStage = cannon.stage
            cannon.loc = placeLoc(cannon.origin, STAGE_LOCS[cannon.stage - 1])
            mes(STAGE_MESSAGES[cannon.stage - 1])
            delay(SETUP_DELAY)
            if (cannons[player] !== cannon) {
                return
            }
        }
    }

    fun ProtectedAccess.fire(coords: CoordGrid) {
        val cannon = ownCannon(coords) ?: return
        if (cannon.firing) {
            mes("Your cannon is already firing.")
            return
        }
        load(coords, startFiring = true)
    }

    fun ProtectedAccess.load(coords: CoordGrid, startFiring: Boolean) {
        val cannon = ownCannon(coords) ?: return
        if (cannon.stage < 4) {
            return
        }
        val room = MAX_AMMO - cannon.ammo
        val loaded = minOf(room, inv.count(CANNONBALL))
        if (loaded > 0 && invDel(inv, CANNONBALL, loaded).success) {
            cannon.ammo += loaded
            player.cannonAmmo = cannon.ammo
            mes("You load the cannon with $loaded cannonball${if (loaded == 1) "" else "s"}.")
        } else if (cannon.ammo == 0) {
            mes("You don't have any ammo to load into the cannon.")
            return
        } else if (!startFiring) {
            mes("Your cannon is already full.")
            return
        }
        if (startFiring && !cannon.firing) {
            cannon.firing = true
            player.softTimer(FIRE_TIMER, 1)
        }
    }

    private fun ProtectedAccess.empty(coords: CoordGrid) {
        val cannon = ownCannon(coords) ?: return
        if (cannon.ammo == 0) {
            mes("Your cannon is empty.")
            return
        }
        if (!invAdd(inv, CANNONBALL, cannon.ammo).success) {
            mes("You don't have enough inventory space to unload the cannon.")
            return
        }
        mes("You unload ${cannon.ammo} cannonball${if (cannon.ammo == 1) "" else "s"} from your cannon.")
        cannon.ammo = 0
        player.cannonAmmo = 0
        stopFiring(player, cannon)
    }

    fun ProtectedAccess.pickUp(coords: CoordGrid) {
        val cannon = ownCannon(coords) ?: return
        val returned =
            player.invTransaction(inv) {
                val target = select(inv)
                for (part in PARTS.take(cannon.stage)) {
                    add(target, part.asRSCM(RSCMType.OBJ), count = 1)
                }
                if (cannon.ammo > 0) {
                    add(target, CANNONBALL.asRSCM(RSCMType.OBJ), count = cannon.ammo)
                }
            }
        if (!returned.success) {
            mes("You don't have enough inventory space to pick up the cannon.")
            return
        }
        anim(SETUP_ANIM)
        removeLoc(cannon)
        clearCannon(player)
        mes("You pick up the cannon. It's really heavy.")
    }

    private fun Player.fireTick() {
        val cannon = cannons[this]
        if (cannon == null || !cannon.firing) {
            clearSoftTimer(FIRE_TIMER)
            return
        }
        if (cannon.ammo <= 0) {
            mes("Your cannon is out of ammo!")
            stopFiring(this, cannon)
            return
        }
        cannon.direction = (cannon.direction + 1) % DIRECTIONS.size
        val loc = cannon.loc ?: return
        val target = findTarget(cannon)
        if (target == null) {
            worldRepo.locAnim(loc, "seq.mcannon_${DIRECTIONS[cannon.direction]}_turn")
            return
        }
        worldRepo.locAnim(loc, "seq.mcannon_${DIRECTIONS[cannon.direction]}_fire")
        val center = cannon.center
        val proj = ProjAnim.fromCoordToNpc(center, target, PROJECTILE.asRSCM(RSCMType.SPOTANIM), PROJ_TYPE)
        worldRepo.projAnim(proj)
        cannon.ammo--
        cannonAmmo = cannon.ammo
        val damage = if (rollHit(this, target)) random.of(0, MAX_HIT) else 0
        target.queueHit(this, max(1, proj.serverCycles), HitType.Ranged, damage, npcHitModifier)
        if (damage > 0) {
            statAdvance("stat.ranged", damage * XP_PER_DAMAGE)
        }
    }

    private fun findTarget(cannon: Cannon): Npc? {
        val center = cannon.center
        return npcRepo
            .findAll(ZoneKey.from(center), zoneRadius = 2)
            .filter { it.coords.level == center.level && it.isValidTarget() && it.isAttackable() }
            .filter { npc ->
                val dx = npc.coords.x - center.x
                val dz = npc.coords.z - center.z
                max(abs(dx), abs(dz)) in 1..RANGE && directionOf(dx, dz) == cannon.direction
            }
            .minByOrNull { max(abs(it.coords.x - center.x), abs(it.coords.z - center.z)) }
    }

    private fun Npc.isAttackable(): Boolean =
        (0 until 5).any { visType.actions.getOpOrNull(it).equals("Attack", ignoreCase = true) }

    private fun rollHit(player: Player, target: Npc): Boolean {
        val attackRoll = (player.rangedLvl + 8) * 64
        val defenceRoll = (target.visType.defence + 9) * 64
        val chance =
            if (attackRoll > defenceRoll) {
                1.0 - (defenceRoll + 2.0) / (2.0 * (attackRoll + 1))
            } else {
                attackRoll / (2.0 * (defenceRoll + 1))
            }
        return random.randomDouble() < chance
    }

    private fun Player.decay() {
        clearSoftTimer(DECAY_TIMER)
        val cannon = cannons[this] ?: return
        removeLoc(cannon)
        cannons.remove(this)
        clearSoftTimer(FIRE_TIMER)
        cannonDecayed = true
        reclaimToBank()
        mes("Your cannon has decayed. Its parts have been returned to your bank.")
    }

    private fun Player.removeCannon() {
        val cannon = cannons.remove(this) ?: return
        removeLoc(cannon)
        clearSoftTimer(FIRE_TIMER)
        clearSoftTimer(DECAY_TIMER)
    }

    /** Returns the parts and cannonballs recorded in the varps to the bank, then clears them. */
    fun Player.reclaimToBank() {
        val stage = cannonStage.coerceIn(0, 4)
        val ammo = cannonAmmo.coerceAtLeast(0)
        if (stage == 0) {
            return
        }
        val bank = invMap.getOrPut("inv.bank")
        val returned =
            invTransaction(bank) {
                val target = select(bank)
                for (part in PARTS.take(stage)) {
                    add(target, part.asRSCM(RSCMType.OBJ), count = 1)
                }
                if (ammo > 0) {
                    add(target, CANNONBALL.asRSCM(RSCMType.OBJ), count = ammo)
                }
            }
        if (!returned.success) {
            return
        }
        cannonStage = 0
        cannonAmmo = 0
        cannonCoord = 0
    }

    private fun ProtectedAccess.ownCannon(coords: CoordGrid): Cannon? {
        val cannon = cannons.values.firstOrNull { it.origin == coords } ?: return null
        if (cannon.owner !== player) {
            mes("This isn't your cannon!")
            return null
        }
        return cannon
    }

    private fun stopFiring(player: Player, cannon: Cannon) {
        cannon.firing = false
        player.clearSoftTimer(FIRE_TIMER)
    }

    private fun clearCannon(player: Player) {
        cannons.remove(player)
        player.clearSoftTimer(FIRE_TIMER)
        player.clearSoftTimer(DECAY_TIMER)
        player.cannonStage = 0
        player.cannonAmmo = 0
        player.cannonCoord = 0
    }

    private fun removeLoc(cannon: Cannon) {
        cannon.loc?.let { locRepo.del(it, Int.MAX_VALUE) }
        cannon.loc = null
    }

    private fun placeLoc(origin: CoordGrid, internal: String): LocInfo =
        locRepo.add(origin, internal, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)

    private fun hasSpace(origin: CoordGrid): Boolean {
        for (dx in 0 until SIZE) {
            for (dz in 0 until SIZE) {
                val tile = origin.translate(dx, dz)
                if (collision.isWalkBlocked(tile)) {
                    return false
                }
            }
        }
        return cannons.values.none { other ->
            abs(other.origin.x - origin.x) < SIZE + 2 && abs(other.origin.z - origin.z) < SIZE + 2 &&
                other.origin.level == origin.level
        }
    }

    class Cannon(val owner: Player, val origin: CoordGrid) {
        var stage: Int = 0
        var ammo: Int = 0
        var direction: Int = DIRECTIONS.size - 1
        var firing: Boolean = false
        var loc: LocInfo? = null

        val center: CoordGrid
            get() = origin.translate(1, 1)
    }

    companion object {
        const val CANNONBALL = "obj.mcannonball"
        const val CANNON_LOC = "loc.dwarf_multicannon1"
        const val FIRE_TIMER = "timer.mcannon_fire"
        const val DECAY_TIMER = "timer.mcannon_decay"
        const val MAX_AMMO = 30
        const val MAX_HIT = 30
        const val RANGE = 8
        const val SIZE = 3
        const val SETUP_DELAY = 2
        const val DECAY_CYCLES = 2500
        const val XP_PER_DAMAGE = 2.0

        private const val SETUP_ANIM = "seq.human_pickupfloor"
        private const val PROJECTILE = "spotanim.cannonball_travel"
        private const val PROJ_TYPE = "projanim.arrow"

        val PARTS = listOf("obj.twpart1", "obj.twpart2", "obj.twpart3", "obj.twpart4")
        val STAGE_LOCS =
            listOf(
                "loc.multicannon_base",
                "loc.multicannon_stand",
                "loc.multicannon_barrels",
                CANNON_LOC,
            )
        val STAGE_MESSAGES =
            listOf(
                "You place the cannon base on the ground.",
                "You add the stand.",
                "You add the barrels.",
                "You add the furnace.",
            )

        /** Turn/fire seq order: clockwise from north, matching [directionOf]. */
        val DIRECTIONS = listOf("n", "ne", "e", "se", "s", "sw", "w", "nw")

        private class Area(val minX: Int, val minZ: Int, val maxX: Int, val maxZ: Int)

        private val GRAND_EXCHANGE = Area(3139, 3463, 3190, 3515)

        fun noCannonMessage(origin: CoordGrid): String? {
            val inGe =
                origin.level == 0 &&
                    origin.x + SIZE - 1 >= GRAND_EXCHANGE.minX &&
                    origin.x <= GRAND_EXCHANGE.maxX &&
                    origin.z + SIZE - 1 >= GRAND_EXCHANGE.minZ &&
                    origin.z <= GRAND_EXCHANGE.maxZ
            return if (inGe) {
                "The Grand Exchange staff prefer not to have heavy artillery operated around their premises."
            } else {
                null
            }
        }

        fun directionOf(dx: Int, dz: Int): Int {
            val octant = (atan2(dx.toDouble(), dz.toDouble()) / (PI / 4)).roundToInt()
            return (octant + DIRECTIONS.size) % DIRECTIONS.size
        }

        fun packCoord(coords: CoordGrid): Int = (coords.level shl 28) or (coords.x shl 14) or coords.z
    }
}

var Player.cannonStage: Int by intVarp("varp.dropcannon")
var Player.cannonAmmo: Int by intVarp("varp.rockthrower")
var Player.cannonCoord: Int by intVarp("varp.ownedmcannon")
var Player.cannonDecayed: Boolean by boolVarBit("varbit.mcannon_decayed")
