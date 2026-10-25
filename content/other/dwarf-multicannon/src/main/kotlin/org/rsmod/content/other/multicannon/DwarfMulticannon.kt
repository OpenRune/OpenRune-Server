package org.rsmod.content.other.multicannon

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.EnumSet
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.styles.RangedAttackStyle
import org.rsmod.api.combat.commons.types.AttackType
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.combat.formulas.accuracy.AccuracyOperations
import org.rsmod.api.combat.formulas.accuracy.melee.PvNMeleeAccuracy
import org.rsmod.api.combat.formulas.accuracy.ranged.PvNRangedAccuracy
import org.rsmod.api.combat.formulas.attributes.CombatNpcAttributes
import org.rsmod.api.combat.formulas.attributes.collector.CombatMeleeAttributeCollector
import org.rsmod.api.combat.formulas.attributes.collector.CombatRangedAttributeCollector
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.config.constants
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.npc.vars.intVarn
import org.rsmod.api.npc.vars.typePlayerUidVarn
import org.rsmod.api.player.isInPvnCombat
import org.rsmod.api.player.isInPvpCombat
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.player.vars.typeNpcUidVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.MapClock
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
    private val gameClock: MapClock,
    private val areas: AreaChecker,
    private val serverConfig: ServerConfig,
    private val attackTypes: AttackTypes,
    private val attackStyles: AttackStyles,
    private val rangedAccuracy: PvNRangedAccuracy,
    private val meleeAccuracy: PvNMeleeAccuracy,
    private val rangedAttributes: CombatRangedAttributeCollector,
    private val meleeAttributes: CombatMeleeAttributeCollector,
) : PluginScript() {
    private val cannons = HashMap<Long, Cannon>()
    private val rayCast by lazy { RayCastValidator(collision) }

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
        onOpLoc4(CANNON_LOC) { loadX(it.loc.coords) }
        onOpLocU(CANNON_LOC, CANNONBALL) { load(it.loc.coords, startFiring = false) }
        onOpLocU(CANNON_LOC, GRANITE_CANNONBALL) { load(it.loc.coords, startFiring = false) }
        onOpLoc1(BROKEN_LOC) { repair(it.loc.coords) }
        onOpLoc2(BROKEN_LOC) { pickUp(it.loc.coords) }
        onPlayerSoftTimer(FIRE_TIMER) { player.fireTick() }
        onPlayerLogout { player.logoutCannon() }
        onPlayerLogin { player.loginCannon() }
    }

    fun cannonOf(player: Player): Cannon? = cannons[player.key]

    suspend fun ProtectedAccess.setUp() {
        if (cannons.containsKey(player.key) || player.cannonStage != 0) {
            mes("You cannot construct more than one cannon at a time.")
            return
        }
        if (!QuestRequirements.hasCompleted(player, DWARF_CANNON_QUEST)) {
            mes(QUEST_REQUIRED_MESSAGE)
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
        val now = gameClock.cycle
        val cannon = Cannon(player.key, origin, breakCycle = now + BREAK_CYCLES)
        cannon.owner = player
        cannon.ammo = player.cannonAmmo.coerceIn(0, MAX_AMMO)
        cannon.ball = BALLS[player.cannonBallType.coerceIn(0, BALLS.size - 1)]
        cannons[player.key] = cannon
        buildStages(cannon)
    }

    private suspend fun ProtectedAccess.continueSetUp(coords: CoordGrid) {
        val cannon = ownCannon(coords) ?: return
        buildStages(cannon)
    }

    private suspend fun ProtectedAccess.buildStages(cannon: Cannon) {
        repeat(PARTS.size - cannon.stage) {
            val part = PARTS[cannon.stage]
            if (inv.count(part) == 0) {
                return
            }
            delay(SETUP_DELAYS[cannon.stage])
            if (cannons[player.key] !== cannon || inv.count(part) == 0) {
                return
            }
            anim(SETUP_ANIM)
            if (!invDel(inv, part, 1).success) {
                return
            }
            cannon.stage++
            if (cannon.stage == 1) {
                player.cannonDecayed = false
                player.cannonCoord = packCoord(cannon.origin)
                player.cannonCoordTemp = packCoord(cannon.origin)
                player.cannonWorld = worldVarbit(serverConfig.world)
            }
            player.cannonStage = cannon.stage
            player.cannonSetupTime = gameClock.cycle
            placeLoc(cannon)
            spam(STAGE_MESSAGES[cannon.stage - 1])
            soundArea(worldRepo, player.coords, SETUP_SYNTH, radius = SOUND_RADIUS)
        }
        delay(1)
        if (cannons[player.key] === cannon && BALLS.any { inv.count(it) > 0 }) {
            loadBalls(cannon, MAX_AMMO)
        }
    }

    fun ProtectedAccess.fire(coords: CoordGrid) {
        val cannon = ownCannon(coords) ?: return
        if (cannon.firing) {
            load(coords, startFiring = false)
            return
        }
        load(coords, startFiring = true)
    }

    fun ProtectedAccess.load(coords: CoordGrid, startFiring: Boolean) {
        val cannon = ownCannon(coords) ?: return
        if (cannon.stage < 4 || cannon.broken) {
            return
        }
        val loaded = loadBalls(cannon, MAX_AMMO)
        if (loaded == 0) {
            if (cannon.ammo == 0) {
                mes(if (startFiring) OUT_OF_AMMO else NO_AMMO)
                return
            }
            if (!startFiring) {
                mes(if (cannon.ammo >= MAX_AMMO) ALREADY_FULL else NO_AMMO)
                return
            }
        }
        if (startFiring && !cannon.firing) {
            cannon.firing = true
            player.cannonFiring = FIRING_FLAG
            player.softTimer(FIRE_TIMER, 1)
        }
    }

    suspend fun ProtectedAccess.loadX(coords: CoordGrid) {
        val cannon = ownCannon(coords) ?: return
        if (cannon.stage < 4 || cannon.broken) {
            return
        }
        if (player.cannonLoadX == 0) {
            val amount = countDialog("Set load-x cannonballs (1 - $MAX_AMMO):").coerceIn(0, MAX_AMMO)
            if (amount == 0) {
                return
            }
            player.cannonLoadX = amount
            mes("You will now load $amount cannonballs when using Load-x.")
            return
        }
        if (loadBalls(cannon, player.cannonLoadX) == 0) {
            mes(if (cannon.ammo >= MAX_AMMO) ALREADY_FULL else NO_AMMO)
        }
    }

    private fun ProtectedAccess.loadBalls(cannon: Cannon, upTo: Int): Int {
        val ball = if (cannon.ammo > 0) cannon.ball else BALLS.lastOrNull { inv.count(it) > 0 } ?: return 0
        val room = MAX_AMMO - cannon.ammo
        val loaded = minOf(room, upTo, inv.count(ball))
        if (loaded <= 0 || !invDel(inv, ball, loaded).success) {
            return 0
        }
        cannon.ball = ball
        player.cannonBallType = BALLS.indexOf(ball)
        cannon.ammo += loaded
        player.cannonAmmo = cannon.ammo
        mes("You load the cannon with $loaded cannonball${if (loaded == 1) "" else "s"}.")
        return loaded
    }

    fun ProtectedAccess.empty(coords: CoordGrid) {
        val cannon = ownCannon(coords) ?: return
        if (cannon.ammo == 0) {
            mes("Your cannon is empty.")
            return
        }
        if (!invAdd(inv, cannon.ball, cannon.ammo).success) {
            mes("You don't have enough inventory space to unload the cannon.")
            return
        }
        mes("You unload your cannon and receive ${BALL_NAMES[BALLS.indexOf(cannon.ball)]} x ${cannon.ammo}.")
        cannon.ammo = 0
        player.cannonAmmo = 0
        stopFiring(player, cannon)
    }

    fun ProtectedAccess.repair(coords: CoordGrid) {
        val cannon = ownCannon(coords) ?: return
        if (!cannon.broken) {
            return
        }
        cannon.broken = false
        cannon.breakCycle = gameClock.cycle + BREAK_CYCLES
        placeLoc(cannon)
        mes(REPAIR_MESSAGE)
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
                    add(target, cannon.ball.asRSCM(RSCMType.OBJ), count = cannon.ammo)
                }
            }
        if (!returned.success) {
            mes("You don't have enough inventory space to pick up the cannon.")
            return
        }
        removeLoc(cannon)
        clearCannon(player)
        player.cannonCoord = packCoord(player.coords)
        player.cannonCoordTemp = -1
        player.cannonSetupTime = 0
        spam("You pick up the cannon. It's really heavy.")
    }

    private fun Player.fireTick() {
        val cannon = cannons[key]
        if (cannon == null || !cannon.firing || cannon.broken || cannon.owner !== this) {
            clearSoftTimer(FIRE_TIMER)
            return
        }
        if (cannon.ammo <= 0) {
            mes(OUT_OF_AMMO)
            stopFiring(this, cannon)
            return
        }
        val loc = cannon.loc ?: return
        findTarget(this, cannon)?.let { shoot(cannon, loc, it) }
        cannon.direction = (cannon.direction + 1) % DIRECTIONS.size
        worldRepo.soundArea(cannon.origin, TURN_SYNTH, radius = SOUND_RADIUS)
        worldRepo.locAnim(loc, "seq.mcannon_${DIRECTIONS[cannon.direction]}_turn")
    }

    private fun Player.shoot(cannon: Cannon, loc: LocInfo, target: Npc) {
        worldRepo.locAnim(loc, "seq.mcannon_${DIRECTIONS[cannon.direction]}_fire")
        worldRepo.soundArea(cannon.origin, FIRE_SYNTH, radius = SOUND_RADIUS)
        val center = cannon.center
        val proj = ProjAnim.fromCoordToNpc(center, target, PROJECTILE.asRSCM(RSCMType.SPOTANIM), PROJ_TYPE)
        worldRepo.projAnim(proj)
        cannon.ammo--
        cannonAmmo = cannon.ammo
        val maxHit = if (cannon.ball == GRANITE_CANNONBALL) GRANITE_MAX_HIT else MAX_HIT
        val immune = DAMAGE_IMMUNE.any { target.isType(it) }
        val damage = if (!immune && rollHit(this, target)) random.of(0, maxHit) else 0
        target.queueHit(this, 1, HitType.Ranged, damage, npcHitModifier)
        val xp = rangedXp(damage, target.hitpoints)
        if (xp > 0) {
            statAdvance("stat.ranged", xp)
        }
    }

    fun findTarget(owner: Player, cannon: Cannon): Npc? {
        val center = cannon.center
        val candidates =
            npcRepo
                .findAll(ZoneKey.from(center), zoneRadius = 3)
                .filter { it.coords.level == center.level && it.isValidTarget() && it.isAttackable() }
                .filter { npc -> UNTARGETED.none { npc.isType(it) } && owner.mayShoot(npc) }
                .toList()
        return RING_RADII.indices.firstNotNullOfOrNull { ring ->
            candidates
                .filter { inTargetRing(cannon.direction, ring, it.coords.x - center.x, it.coords.z - center.z) }
                .filter { rayCast.hasLineOfSight(center, it.coords) }
                .minByOrNull { max(abs(it.coords.x - center.x), abs(it.coords.z - center.z)) }
        }
    }

    private fun Player.mayShoot(npc: Npc): Boolean {
        if (areas.inArea("area.multiway", npc.coords)) {
            return true
        }
        if (isInPvpCombat()) {
            return false
        }
        if (isInPvnCombat() && aggressiveNpc != null && aggressiveNpc != npc.uid) {
            return false
        }
        val npcTarget = npc.aggressivePlayer
        return npc.lastCombat + constants.combat_activecombat_delay <= gameClock.cycle ||
            npcTarget == null ||
            npcTarget == uid
    }

    private fun Npc.isAttackable(): Boolean =
        (0 until 5).any { visType.actions.getOpOrNull(it).equals("Attack", ignoreCase = true) }

    /**
     * The owner's accuracy roll for the current weapon and style (powered staves: the better of
     * ranged and crush) against the target's heavy ranged defence, without target-dependent
     * effects such as a slayer helmet on task.
     */
    private fun rollHit(player: Player, target: Npc): Boolean {
        val noTargetEffects = EnumSet.noneOf(CombatNpcAttributes::class.java)
        val attackType = attackTypes.get(player)
        val attackStyle = attackStyles.get(player)
        val attackRoll =
            when {
                attackType?.isRanged == true -> rangedAttackRoll(player, target, attackType, attackStyle)
                attackType?.isMagic == true ->
                    max(
                        rangedAttackRoll(player, target, null, null),
                        meleeAttackRoll(player, MeleeAttackType.Crush, null),
                    )
                else -> meleeAttackRoll(player, MeleeAttackType.from(attackType) ?: MeleeAttackType.Crush, attackStyle)
            }
        val defenceRoll =
            rangedAccuracy.computeDefenceRoll(
                target = target.visType,
                targetDefence = target.defenceLvl,
                amascutInvocationLvl = 0,
                blockType = RangedAttackType.Heavy,
                npcAttributes = noTargetEffects,
            )
        val hitChance = AccuracyOperations.calculateHitChance(attackRoll, defenceRoll, noTargetEffects)
        return hitChance > random.of(maxExclusive = HIT_CHANCE_SCALE)
    }

    private fun rangedAttackRoll(
        player: Player,
        target: Npc,
        attackType: AttackType?,
        attackStyle: org.rsmod.api.combat.commons.styles.AttackStyle?,
    ): Int {
        val type = RangedAttackType.from(attackType)
        val style = RangedAttackStyle.from(attackStyle)
        return rangedAccuracy.computeAttackRoll(
            source = player,
            targetMagic = target.magicLvl,
            targetDistance = player.coords.chebyshevDistance(target.coords),
            attackStyle = style,
            rangeAttributes = rangedAttributes.collect(player, type, style),
            npcAttributes = EnumSet.noneOf(CombatNpcAttributes::class.java),
        )
    }

    private fun meleeAttackRoll(
        player: Player,
        attackType: MeleeAttackType,
        attackStyle: org.rsmod.api.combat.commons.styles.AttackStyle?,
    ): Int =
        meleeAccuracy.computeAttackRoll(
            source = player,
            attackType = attackType,
            attackStyle = MeleeAttackStyle.from(attackStyle),
            meleeAttributes = meleeAttributes.collect(player, attackType),
            npcAttributes = EnumSet.noneOf(CombatNpcAttributes::class.java),
        )

    private fun onLocExpired(cannon: Cannon, token: Int) {
        if (cannons[cannon.ownerKey] !== cannon || cannon.token != token) {
            return
        }
        cannon.loc = null
        if (cannon.stage == 4 && !cannon.broken) {
            breakCannon(cannon)
        } else {
            decay(cannon)
        }
    }

    private fun breakCannon(cannon: Cannon) {
        cannon.broken = true
        cannon.owner?.let { stopFiring(it, cannon) }
        cannon.firing = false
        placeLoc(cannon)
        cannon.owner?.mes(BROKEN_MESSAGE)
    }

    private fun decay(cannon: Cannon) {
        cannons.remove(cannon.ownerKey)
        val owner = cannon.owner ?: return
        owner.clearSoftTimer(FIRE_TIMER)
        owner.cannonFiring = 0
        owner.cannonDecayed = true
        owner.mes(DECAYED_MESSAGE)
    }

    fun Player.logoutCannon() {
        val cannon = cannons[key] ?: return
        stopFiring(this, cannon)
        cannon.owner = null
    }

    fun Player.loginCannon() {
        val cannon = cannons[key]
        if (cannon != null) {
            cannon.owner = this
            cannonStage = cannon.stage
            cannonAmmo = cannon.ammo
            cannonCoord = packCoord(cannon.origin)
            cannonCoordTemp = packCoord(cannon.origin)
            cannonWorld = worldVarbit(serverConfig.world)
            return
        }
        cannonCoordTemp = -1
        if (cannonStage != 0 && !cannonDecayed) {
            cannonDecayed = true
        }
    }

    private fun ProtectedAccess.ownCannon(coords: CoordGrid): Cannon? {
        val cannon = cannons.values.firstOrNull { it.origin == coords } ?: return null
        if (cannon.ownerKey != player.key) {
            mes("This isn't your cannon!")
            return null
        }
        cannon.owner = player
        return cannon
    }

    private fun stopFiring(player: Player, cannon: Cannon) {
        cannon.firing = false
        player.cannonFiring = 0
        player.clearSoftTimer(FIRE_TIMER)
    }

    private fun clearCannon(player: Player) {
        cannons.remove(player.key)
        player.clearSoftTimer(FIRE_TIMER)
        player.cannonFiring = 0
        player.cannonStage = 0
        player.cannonAmmo = 0
        player.cannonWorld = 0
    }

    private fun removeLoc(cannon: Cannon) {
        cannon.token++
        cannon.loc?.let { locRepo.del(it, Int.MAX_VALUE) }
        cannon.loc = null
    }

    private fun placeLoc(cannon: Cannon) {
        val internal =
            when {
                cannon.broken -> BROKEN_LOC
                else -> STAGE_LOCS[cannon.stage - 1]
            }
        val expiresAt =
            if (cannon.stage == 4 && !cannon.broken) cannon.breakCycle else cannon.decayCycle
        val duration = max(1, expiresAt - gameClock.cycle)
        val token = ++cannon.token
        cannon.loc =
            locRepo.add(cannon.origin, internal, duration, LocAngle.West, LocShape.CentrepieceStraight) {
                onLocExpired(cannon, token)
            }
    }

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

    class Cannon(val ownerKey: Long, val origin: CoordGrid, var breakCycle: Int) {
        var owner: Player? = null
        var stage: Int = 0
        var ammo: Int = 0
        var direction: Int = 0
        var ball: String = CANNONBALL
        var firing: Boolean = false
        var broken: Boolean = false
        var loc: LocInfo? = null
        var token: Int = 0

        val decayCycle: Int
            get() = breakCycle + DECAY_AFTER_BREAK_CYCLES

        val center: CoordGrid
            get() = origin.translate(1, 1)
    }

    companion object {
        const val CANNONBALL = "obj.mcannonball"
        const val GRANITE_CANNONBALL = "obj.granite_cannonball"
        const val CANNON_LOC = "loc.dwarf_multicannon1"
        const val BROKEN_LOC = "loc.dwarf_multicannon1_broken"
        const val FIRE_TIMER = "timer.mcannon_fire"
        const val MAX_AMMO = 30
        const val MAX_HIT = 30
        const val GRANITE_MAX_HIT = 35
        const val SIZE = 3
        const val XP_PER_DAMAGE = 2.0

        const val BREAK_CYCLES = 2500
        const val DECAY_AFTER_BREAK_CYCLES = 1000

        val SETUP_DELAYS = listOf(2, 2, 3, 3)

        private const val HIT_CHANCE_SCALE = 10_000
        private const val FIRING_FLAG = 1 shl 20
        private const val DWARF_CANNON_QUEST = "quest_dwarfcannon"

        const val BROKEN_MESSAGE = "<col=ef1020>Your cannon has broken!"
        const val NO_AMMO = "You don't have any ammo to load into the cannon."
        const val ALREADY_FULL = "Your cannon is already full."
        const val REPAIR_MESSAGE = "You repair your cannon, restoring it to working order."
        const val DECAYED_MESSAGE =
            "<col=ef1020>Your cannon has decayed. Speak to Nulodion to get a new one!"
        const val OUT_OF_AMMO = "<col=ef1020>Your cannon is out of ammo.</col>"
        const val QUEST_REQUIRED_MESSAGE =
            "You need to complete the Dwarf Cannon quest before you can set up a cannon."

        private const val SETUP_ANIM = "seq.human_pickupfloor"
        private const val PROJECTILE = "spotanim.cannonball_travel"
        private const val PROJ_TYPE = "projanim.mcannon"
        private const val SETUP_SYNTH = "synth.mcannon_setup"
        private const val TURN_SYNTH = "synth.mcannon_turn"
        private const val FIRE_SYNTH = "synth.mcannon_fire"
        private const val SOUND_RADIUS = 10

        val PARTS = listOf("obj.twpart1", "obj.twpart2", "obj.twpart3", "obj.twpart4")
        val STAGE_LOCS =
            listOf(
                "loc.multicannon_base",
                "loc.multicannon_stand",
                "loc.multicannon_barrels",
                CANNON_LOC,
            )
        val BALLS = listOf(CANNONBALL, GRANITE_CANNONBALL)
        val BALL_NAMES = listOf("Steel cannonball", "Granite cannonball")

        val STAGE_MESSAGES =
            listOf(
                "You place the cannon base on the ground.",
                "You add the stand.",
                "You add the barrels.",
                "You add the furnace.",
            )

        val DIRECTIONS = listOf("n", "ne", "e", "se", "s", "sw", "w", "nw")
        private val DIRECTION_STEPS =
            listOf(0 to 1, 1 to 1, 1 to 0, 1 to -1, 0 to -1, -1 to -1, -1 to 0, -1 to 1)
        private val STRAIGHT_REACH = listOf(3, 7, 14)
        private val DIAGONAL_REACH = listOf(2, 5, 12)
        private val RING_RADII = listOf(1, 2, 5)
        private val UNTARGETED =
            setOf(
                "npc.zeah_sandcrab",
                "npc.zeah_sandcrab_small",
                "npc.fossil_ammonitecrab",
                "npc.kolhuman",
                "npc.kologre",
                "npc.kolspider",
                "npc.kolethereal",
                "npc.koldemon",
                "npc.zamorak_mage",
                "npc.saradomin_mage",
                "npc.guthix_mage",
            )
        private val DAMAGE_IMMUNE =
            setOf(
                "npc.mm2_demon_gorilla_1_melee",
                "npc.mm2_demon_gorilla_1_ranged",
                "npc.mm2_demon_gorilla_1_magic",
                "npc.mm2_demon_gorilla_2_melee",
                "npc.mm2_demon_gorilla_2_ranged",
                "npc.mm2_demon_gorilla_2_magic",
                "npc.slayer_kursk_1",
                "npc.slayer_kursk_2",
                "npc.superior_kurask",
            )

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

        fun inTargetRing(direction: Int, ring: Int, dx: Int, dz: Int): Boolean {
            val (stepX, stepZ) = DIRECTION_STEPS[direction]
            val reach = (if (stepX != 0 && stepZ != 0) DIAGONAL_REACH else STRAIGHT_REACH)[ring]
            val radius = RING_RADII[ring]
            return abs(dx - stepX * reach) <= radius && abs(dz - stepZ * reach) <= radius
        }

        fun rangedXp(damage: Int, targetHitpoints: Int): Double =
            min(damage, targetHitpoints).coerceAtLeast(0) * XP_PER_DAMAGE

        fun packCoord(coords: CoordGrid): Int = (coords.level shl 28) or (coords.x shl 14) or coords.z

        // 7 bits: the client compares it with map_world % 128.
        fun worldVarbit(world: Int): Int = world % 128
    }
}

private val Player.key: Long
    get() = uuid ?: slotId.toLong()

var Player.cannonStage: Int by intVarp("varp.dropcannon")
var Player.cannonAmmo: Int by intVarp("varp.rockthrower")
var Player.cannonCoord: Int by intVarp("varp.ownedmcannon")
var Player.cannonCoordTemp: Int by intVarp("varp.ownedmcannon_temp")
var Player.cannonSetupTime: Int by intVarBit("varbit.mcannon_setuptime")
var Player.cannonBallType: Int by intVarBit("varbit.mcannon_last_balltype")
var Player.cannonFiring: Int by intVarp("varp.mcannonmulti")
var Player.cannonDecayed: Boolean by boolVarBit("varbit.mcannon_decayed")
var Player.cannonWorld: Int by intVarBit("varbit.mcannon_world")
var Player.cannonLoadX: Int by intVarBit("varbit.option_cannonballs_load_x")
private val Player.aggressiveNpc by typeNpcUidVarp("varp.aggressive_npc")
private val Npc.lastCombat: Int by intVarn("varn.lastcombat")
private val Npc.aggressivePlayer by typePlayerUidVarn("varn.aggressive_player")
