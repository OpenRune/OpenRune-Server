package org.rsmod.content.minigames.wintertodt

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MoveRestrict
import dev.openrune.types.NpcMode
import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.queueDeath
import org.rsmod.api.player.stat.baseConstructionLvl
import org.rsmod.api.player.stat.baseFiremakingLvl
import org.rsmod.api.player.stat.baseFletchingLvl
import org.rsmod.api.player.stat.baseHerbloreLvl
import org.rsmod.api.player.stat.baseWoodcuttingLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import kotlin.math.min

@Singleton
class WintertodtGame
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val playerList: PlayerList,
    private val mapClock: MapClock,
    private val random: GameRandom,
    private val worldRepo: WorldRepository,
    private val eventBus: EventBus,
) {
    private enum class Phase {
        WAITING,
        ACTIVE,
        SUBDUED,
    }

    private data class Pending(val at: Int, val action: () -> Unit)

    private val sessions = linkedMapOf<Player, WintertodtSession>()
    private val braziers = Wintertodt.BRAZIER_COORDS.mapIndexed { i, coords -> Brazier(i, coords) }
    private val pyromancers =
        Wintertodt.PYROMANCER_COORDS.mapIndexed { i, coords -> Pyromancer(i, coords) }
    private val pending = mutableListOf<Pending>()

    private var phase = Phase.WAITING
    private var energy = Wintertodt.ENERGY_MAX
    private var respawnTimer = 0
    private var nextAttack = 0
    private var nextHud = 0
    private var worldReady = false

    private var storm: LocInfo? = null

    private val brazierIds: Set<Int> by lazy {
        listOf(
            Wintertodt.BRAZIER_UNLIT,
            Wintertodt.BRAZIER_BROKEN,
            Wintertodt.BRAZIER_LIT,
        )
            .mapNotNull { ServerCacheManager.getObject(it.asRSCM(RSCMType.LOC))?.id }
            .toSet()
    }

    private val pyromancerType: NpcServerType? by lazy {
        ServerCacheManager.getNpc(Wintertodt.NPC_PYROMANCER.asRSCM(RSCMType.NPC))
    }

    private val pyromancerDownType: NpcServerType? by lazy {
        ServerCacheManager.getNpc(Wintertodt.NPC_PYROMANCER_DOWN.asRSCM(RSCMType.NPC))
    }

    private val warmItemIds: Set<Int> by lazy {
        listOf(
            "obj.pyromancer_hood",
            "obj.pyromancer_top",
            "obj.pyromancer_bottom",
            "obj.pyromancer_gloves",
            "obj.pyromancer_boots",
            "obj.wint_torch",
            "obj.wint_torch_offhand",
            "obj.firemaking_cape",
            "obj.firemaking_cape_t",
            "obj.firemaking_hood",
            "obj.max_cape",
            "obj.fire_cape",
            "obj.infernal_cape",
            "obj.obsidian_cape",
            "obj.hitpoints_cape",
            "obj.rainbow_jumper",
            "obj.rainbow_scarf",
            "obj.clue_hunter_boots",
            "obj.clue_hunter_gloves",
            "obj.wolf_mask",
            "obj.earmuffs",
            "obj.cap_and_goggles",
            "obj.woolly_hat",
            "obj.bobble_hat",
            "obj.santa_hat",
            "obj.santa_jacket",
            "obj.santa_pantaloons",
            "obj.santa_boots",
        )
            .mapNotNull { ServerCacheManager.getItem(it.asRSCM(RSCMType.OBJ))?.id }
            .toSet()
    }

    private val updateScript: Int by lazy {
        Wintertodt.SCRIPT_UPDATE.asRSCM(RSCMType.CLIENTSCRIPT)
    }

    private val insideScript: Int by lazy {
        Wintertodt.SCRIPT_INSIDE.asRSCM(RSCMType.CLIENTSCRIPT)
    }

    private val outsideScript: Int by lazy {
        Wintertodt.SCRIPT_OUTSIDE.asRSCM(RSCMType.CLIENTSCRIPT)
    }

    fun tick() {
        if (!worldReady && sessions.isEmpty()) return
        ensureWorld()

        processPending()

        if (phase == Phase.ACTIVE) {
            drainEnergy()
            regenerateEnergy()
            runWarmth()
            runAttacks()
            if (energy <= 0) endRound()
        }

        if (phase == Phase.SUBDUED) {
            tickRespawn()
        }

        if (mapClock.cycle >= nextHud) {
            for (session in sessions.values.toList()) {
                if (session.player.canProcess) updateHud(session)
            }
            nextHud = mapClock.cycle + 2
        }
    }

    internal fun session(player: Player): WintertodtSession? = sessions[player]

    fun enter(player: Player) {
        ensureWorld()
        if (sessions.containsKey(player)) return

        val session = WintertodtSession(player)
        sessions[player] = session
        player.wintWarmth = Wintertodt.WARMTH_MAX
        session.nextHud = mapClock.cycle
        openHud(player, session)

        if (warmItems(player) < Wintertodt.WARM_ITEMS_MAX) {
            player.mes(
                "You feel the cold wind strike you as you enter; " +
                    "perhaps you should find some warmer clothes.",
            )
        }

        if (phase == Phase.WAITING) startRound()
        updateHud(session)
    }

    fun leave(player: Player) {
        val session = sessions.remove(player) ?: return
        session.points = 0
        player.wintWarmth = Wintertodt.WARMTH_MAX
        closeHud(player)
        player.wintAreaAttack = 0
    }

    fun onLogin(player: Player) {
        player.wintWarmth = Wintertodt.WARMTH_MAX
        if (Wintertodt.isInPrison(player.coords)) enter(player)
    }

    fun onLogout(player: Player) {
        sessions.remove(player)
    }

    fun onCoordsChanged(player: Player) {
        val inside = Wintertodt.isInPrison(player.coords)
        if (inside && session(player) == null) {
            enter(player)
        } else if (!inside && session(player) != null) {
            leave(player)
        }
    }

    fun peek(player: Player) {
        val percent = (energy * 100.0 / Wintertodt.ENERGY_MAX).toInt()
        val count = sessions.keys.count { Wintertodt.isInPrison(it.coords) }
        val players =
            when (count) {
                0 -> "There are no players within the prison."
                1 -> "There is 1 player within the prison."
                else -> "There are $count players within the prison."
            }
        player.mes("The Wintertodt has $percent% energy left. $players")
    }

    fun points(player: Player): Int = session(player)?.points ?: 0

    fun addPoints(player: Player, amount: Int) {
        val session = session(player) ?: return
        session.points += amount
        updateHud(session)
    }

    fun lightBrazier(player: Player, index: Int) {
        val brazier = braziers.getOrNull(index) ?: return
        val pyro = pyromancers.getOrNull(index) ?: return
        if (pyro.down) {
            player.mes("Heal the Pyromancer before lighting the brazier.")
            return
        }
        if (!hasTinderbox(player)) {
            player.mes("You need a tinderbox to light the brazier.")
            return
        }
        if (brazier.state == BrazierState.LIT) {
            player.mes("The brazier is already lit.")
            return
        }
        setBrazierState(brazier, BrazierState.LIT)
        player.anim(Wintertodt.ANIM_LIGHT)
        player.soundSynth(2596)
        player.mes("You light the brazier.")
        addPoints(player, 25)
        player.statAdvance("stat.firemaking", 6.0 * player.baseFiremakingLvl)
        updateHud()
    }

    fun fixBrazier(player: Player, index: Int) {
        val brazier = braziers.getOrNull(index) ?: return
        if (brazier.state != BrazierState.BROKEN) return
        if (!hasHammer(player)) {
            player.mes("You need a hammer to fix the brazier.")
            return
        }
        setBrazierState(brazier, BrazierState.UNLIT)
        player.anim(Wintertodt.ANIM_FIX)
        player.soundSynth(2581)
        player.mes("You fix the brazier.")
        addPoints(player, 25)
        if (hasPoh(player)) {
            player.statAdvance("stat.construction", 4.0 * player.baseConstructionLvl)
        }
        updateHud()
    }

    fun feed(player: Player, index: Int) {
        val brazier = braziers.getOrNull(index) ?: return
        if (brazier.state != BrazierState.LIT) return
        val kindling = player.inv.count(Wintertodt.KINDLING) > 0
        val item = if (kindling) Wintertodt.KINDLING else Wintertodt.ROOT
        if (player.inv.count(item) <= 0) {
            player.mes("You have run out of bruma roots.")
            return
        }
        if (player.invDel(player.inv, item, 1).failure) return
        if (kindling) {
            addPoints(player, 25)
            player.statAdvance("stat.firemaking", 3.8 * player.baseFiremakingLvl)
        } else {
            addPoints(player, 10)
            player.statAdvance("stat.firemaking", 3.0 * player.baseFiremakingLvl)
        }
        player.anim(Wintertodt.ANIM_LIGHT)
    }

    fun chopRoot(player: Player) {
        if (player.inv.isFull()) {
            player.mes("Your inventory is too full to hold any more roots.")
            return
        }
        if (player.invAdd(player.inv, Wintertodt.ROOT, 1).failure) return
        player.statAdvance("stat.woodcutting", 0.3 * player.baseWoodcuttingLvl)
        player.mes("You get a bruma root.")
    }

    fun fletch(player: Player) {
        if (player.inv.count(Wintertodt.ROOT) <= 0) return
        if (player.invDel(player.inv, Wintertodt.ROOT, 1).failure) return
        player.invAdd(player.inv, Wintertodt.KINDLING, 1)
        player.statAdvance("stat.fletching", 0.6 * player.baseFletchingLvl)
        player.mes("You carefully fletch the root into a bundle of kindling.")
    }

    fun pickHerb(player: Player) {
        if (player.inv.isFull()) {
            player.mes("The herb shatters as it hits the floor.")
            return
        }
        if (player.invAdd(player.inv, Wintertodt.HERB, 1).failure) return
        player.mes("You pick a bruma herb.")
    }

    fun combinePotion(player: Player) {
        if (!Wintertodt.isInPrison(player.coords)) {
            player.mes("You can only do that within the influence of the Bruma tree.")
            return
        }
        if (
            player.inv.count(Wintertodt.HERB) <= 0 ||
                player.inv.count(Wintertodt.VIAL) <= 0
        ) {
            return
        }
        if (player.invDel(player.inv, Wintertodt.HERB, 1).failure) return
        if (player.invDel(player.inv, Wintertodt.VIAL, 1).failure) return
        player.invAdd(player.inv, Wintertodt.POTION4, 1)
        player.statAdvance("stat.herblore", 0.1 * player.baseHerbloreLvl)
        player.mes("You combine the bruma herb into the unfinished potion.")
    }

    fun drinkPotion(player: Player, slot: Int) {
        val obj = player.inv[slot] ?: return
        val dose =
            when (obj.id) {
                itemId(Wintertodt.POTION4) -> 4
                itemId(Wintertodt.POTION3) -> 3
                itemId(Wintertodt.POTION2) -> 2
                itemId(Wintertodt.POTION1) -> 1
                else -> return
            }
        val next =
            when (dose) {
                4 -> Wintertodt.POTION3
                3 -> Wintertodt.POTION2
                2 -> Wintertodt.POTION1
                else -> Wintertodt.EMPTY_VIAL
            }
        val current =
            when (dose) {
                4 -> Wintertodt.POTION4
                3 -> Wintertodt.POTION3
                2 -> Wintertodt.POTION2
                else -> Wintertodt.POTION1
            }
        if (player.invDel(player.inv, current, 1, slot).failure) return
        player.invAdd(player.inv, next, 1, slot)
        player.wintWarmth = min(Wintertodt.WARMTH_MAX, player.wintWarmth + 300)
        player.anim(Wintertodt.ANIM_DRINK)
        player.mes("You drink some of your potion.")
        if (dose > 1) player.mes("You have ${dose - 1} doses of potion left.")
        updateHud()
    }

    fun healPyromancer(player: Player, npc: Npc) {
        val pyro = pyromancers.firstOrNull { it.npc == npc } ?: return
        if (!pyro.down) {
            player.mes("Pyromancer|No need! I'm fine, thanks.")
            return
        }
        val potion = lowestDosePotion(player)
        if (potion == null) {
            player.mes("You need a rejuvenation potion to heal the Pyromancer.")
            return
        }
        val (slot, dose) = potion
        val next =
            when (dose) {
                4 -> Wintertodt.POTION3
                3 -> Wintertodt.POTION2
                2 -> Wintertodt.POTION1
                else -> Wintertodt.EMPTY_VIAL
            }
        val current =
            when (dose) {
                4 -> Wintertodt.POTION4
                3 -> Wintertodt.POTION3
                2 -> Wintertodt.POTION2
                else -> Wintertodt.POTION1
            }
        if (player.invDel(player.inv, current, 1, slot).failure) return
        player.invAdd(player.inv, next, 1, slot)
        pyro.hp = 14
        pyro.down = false
        pyro.resumeAt = mapClock.cycle + random.of(2, 8)
        pyro.npc?.let { npc2 -> pyromancerType?.let { npc2.transmog(it, Int.MAX_VALUE) } }
        addPoints(player, 75)
        player.anim(Wintertodt.ANIM_DRINK)
        player.mes("You heal the Pyromancer.")
        updateHud()
    }

    fun takeVials(player: Player, count: Int) {
        val space = player.inv.freeSpace()
        if (space <= 0) {
            player.mes("Your inventory is too full to hold any more potions.")
            return
        }
        val amount = min(space, count)
        for (i in 0 until amount) {
            player.invAdd(player.inv, Wintertodt.VIAL, 1)
        }
        player.mes(
            if (amount == 1) "You take an unfinished potion from the crate."
            else "You take the unfinished potions from the crate.",
        )
    }

    fun takeTool(player: Player, tool: String) {
        if (player.inv.count(tool) > 0 || player.worn.count(tool) > 0) {
            player.mes("You already have one.")
            return
        }
        if (player.inv.isFull()) {
            player.mes("Your inventory is too full to hold that.")
            return
        }
        player.invAdd(player.inv, tool, 1)
    }

    fun brazierIndex(coords: CoordGrid): Int =
        braziers.indexOfFirst { it.coords == coords }

    fun brazierIndexByNpc(npc: Npc): Int =
        pyromancers.indexOfFirst { it.npc == npc }

    fun hasAxe(player: Player): Boolean =
        (player.inv.objs.asSequence() + player.worn.objs.asSequence()).any {
            val obj = it ?: return@any false
            val type = ServerCacheManager.getItem(obj.id) ?: return@any false
            type.internalName.endsWith("_axe")
        }

    fun hasTinderbox(player: Player): Boolean =
        player.inv.count("obj.tinderbox") > 0 ||
            player.worn.count("obj.tinderbox") > 0 ||
            player.inv.count("obj.wint_torch") > 0 ||
            player.inv.count("obj.wint_torch_offhand") > 0 ||
            player.worn.count("obj.wint_torch") > 0 ||
            player.worn.count("obj.wint_torch_offhand") > 0

    fun hasHammer(player: Player): Boolean =
        player.inv.count("obj.hammer") > 0 ||
            player.worn.count("obj.hammer") > 0 ||
            player.inv.count("obj.imcando_hammer") > 0 ||
            player.worn.count("obj.imcando_hammer") > 0

    fun warmItems(player: Player): Int =
        player.worn.objs.count { it != null && it.id in warmItemIds }

    fun setEnergy(value: Int) {
        energy = value.coerceIn(0, Wintertodt.ENERGY_MAX)
        updateHud()
    }

    fun setPoints(player: Player, value: Int) {
        val session = session(player) ?: return
        session.points = value.coerceAtLeast(0)
        updateHud(session)
    }

    fun setWarmth(player: Player, value: Int) {
        player.wintWarmth = value.coerceIn(0, Wintertodt.WARMTH_MAX)
    }

    fun forceAttack() {
        if (phase != Phase.ACTIVE) return
        specialAttack()
    }

    fun updateHud() {
        for (session in sessions.values.toList()) updateHud(session)
    }

    internal fun updateHud(session: WintertodtSession) {
        val player = session.player
        val pyro = pyromancers.map { if (it.down) 0 else 1 }
        val brazier = braziers.map { it.state.state }
        player.runClientScript(
            updateScript,
            session.points,
            energy,
            pyro[0],
            pyro[1],
            pyro[2],
            pyro[3],
            brazier[0],
            brazier[1],
            brazier[2],
            brazier[3],
        )
    }

    fun startRound() {
        ensureWorld()
        if (phase == Phase.ACTIVE) return
        phase = Phase.ACTIVE
        energy = Wintertodt.ENERGY_MAX
        respawnTimer = 0
        for (brazier in braziers) {
            setBrazierState(brazier, BrazierState.UNLIT)
            brazier.lastHit = Int.MIN_VALUE / 2
        }
        for (pyro in pyromancers) {
            pyro.hp = 14
            pyro.down = false
            pyro.lastHit = Int.MIN_VALUE / 2
            pyro.resumeAt = mapClock.cycle
            pyro.npc?.let { npc -> pyromancerType?.let { npc.transmog(it, Int.MAX_VALUE) } }
        }
        setStorm(active = true)
        nextAttack = mapClock.cycle + Wintertodt.ATTACK_INTERVAL
        nextHud = mapClock.cycle
        for (session in sessions.values) {
            session.points = 0
            session.lastCold = Int.MIN_VALUE / 2
            session.regenTenths = 0
            session.player.wintRespawnDelay = 0
            updateHud(session)
        }
    }

    private fun endRound() {
        if (phase != Phase.ACTIVE) return
        phase = Phase.SUBDUED
        energy = 0
        respawnTimer = Wintertodt.RESPAWN_TIMER
        pending.clear()
        for (brazier in braziers) setBrazierState(brazier, BrazierState.UNLIT)
        for (pyro in pyromancers) {
            pyro.hp = 14
            pyro.down = false
            pyro.npc?.let { npc -> pyromancerType?.let { npc.transmog(it, Int.MAX_VALUE) } }
        }
        setStorm(active = false)
        for (session in sessions.values) {
            awardRound(session)
            session.player.wintRespawnDelay = Wintertodt.RESPAWN_TIMER
        }
        updateHud()
    }

    private fun awardRound(session: WintertodtSession) {
        val player = session.player
        val points = session.points
        if (points < Wintertodt.POINT_THRESHOLD) {
            player.mes(
                "You did not earn enough points to be worthy of a gift from the citizens of " +
                    "Kourend this time.",
            )
            session.points = 0
            return
        }

        val guaranteed = Wintertodt.pointsToRewards(points)
        val extra = if (random.of(0, Wintertodt.POINT_THRESHOLD) < points % 500) 1 else 0
        val earned = guaranteed + extra
        val pool = min(Wintertodt.REWARD_POOL_MAX, player.wintRewardPool + earned)
        player.wintRewardPool = pool

        val xp = 100 * player.baseFiremakingLvl
        player.statAdvance("stat.firemaking", xp.toDouble())
        player.mes("You have gained $xp Firemaking XP.")
        player.wintertodtKills += 1
        player.mes("Your subdued Wintertodt count is: <col=ff0000>${player.wintertodtKills}</col>.")
        player.mes(
            "You're owed an additional $earned rewards from the reward cart. " +
                "You're now owed $pool rewards.",
        )
        session.points = 0
    }

    private fun tickRespawn() {
        if (mapClock.cycle % 2 != 0 || respawnTimer <= 0) return
        respawnTimer = (respawnTimer - Wintertodt.RESPAWN_STEP).coerceAtLeast(0)
        for (session in sessions.values) {
            session.player.wintRespawnDelay = respawnTimer
        }
        if (respawnTimer == 0) {
            phase = Phase.WAITING
            if (sessions.isNotEmpty()) startRound()
        }
    }

    private fun drainEnergy() {
        for (brazier in braziers) {
            if (brazier.state != BrazierState.LIT) continue
            val pyro = pyromancers[brazier.index]
            if (pyro.down) continue
            if (mapClock.cycle < brazier.nextDrain) continue
            if (mapClock.cycle < pyro.resumeAt) continue
            energy = (energy - Wintertodt.ENERGY_PER_PERCENT).coerceAtLeast(0)
            brazier.nextDrain = mapClock.cycle + Wintertodt.DRAIN_INTERVAL
            if (energy <= 0) return
        }
    }

    private fun regenerateEnergy() {
        if (braziers.any { it.state == BrazierState.LIT }) return
        if (energy >= Wintertodt.ENERGY_MAX) return
        if (mapClock.cycle % 35 != 0) return
        energy = min(Wintertodt.ENERGY_MAX, energy + Wintertodt.ENERGY_PER_PERCENT)
    }

    private fun runWarmth() {
        for (session in sessions.values.toList()) {
            val player = session.player
            if (!player.canProcess) continue
            val warm = warmItems(player)
            session.regenTenths += 8 + warm
            if (session.regenTenths >= 10) {
                val add = session.regenTenths / 10
                session.regenTenths %= 10
                player.wintWarmth =
                    min(Wintertodt.WARMTH_MAX, player.wintWarmth + add)
            }
        }
    }

    private fun runAttacks() {
        if (mapClock.cycle < nextAttack) return
        nextAttack = mapClock.cycle + Wintertodt.ATTACK_INTERVAL
        if (sessions.isEmpty()) return
        if (random.of(0, Wintertodt.ENERGY_MAX) < energy) {
            standardAttack()
        } else {
            specialAttack()
        }
    }

    private fun standardAttack() {
        val candidates =
            sessions.values.filter {
                val player = it.player
                player.canProcess &&
                    !Wintertodt.isSafe(player.coords) &&
                    mapClock.cycle - it.lastCold >= Wintertodt.TARGET_COOLDOWN
            }
        if (candidates.isEmpty()) return
        val session = candidates[random.of(candidates.size)]
        val player = session.player
        val damage =
            Wintertodt.standardDamage(
                player.baseFiremakingLvl,
                warmItems(player),
                braziers.count { it.state == BrazierState.LIT },
            )
        player.mes("The cold of the Wintertodt seeps into your bones.")
        applyWarmth(player, damage)
        session.lastCold = mapClock.cycle
    }

    private fun specialAttack() {
        when (random.of(0, 9)) {
            0 -> areaAttack()
            in 1..4 -> brazierAttack(random.of(0, 4))
            else -> pyromancerAttack(random.of(0, 4))
        }
    }

    private fun brazierAttack(index: Int) {
        val brazier = braziers[index]
        if (brazier.state != BrazierState.LIT) return
        if (mapClock.cycle - brazier.lastHit < Wintertodt.TARGET_COOLDOWN) return
        brazier.lastHit = mapClock.cycle
        warning(brazier.coords)
        val heavy = random.of(0, 3) == 0
        pending +=
            Pending(mapClock.cycle + 3) {
                when {
                    heavy && brazier.state == BrazierState.LIT -> {
                        setBrazierState(brazier, BrazierState.BROKEN)
                        for (session in sessions.values.toList()) {
                            val player = session.player
                            if (near(player.coords, brazier.coords, 1)) {
                                player.mes("The brazier is broken and shrapnel damages you.")
                                applyWarmth(
                                    player,
                                    Wintertodt.brazierDamage(
                                        player.baseFiremakingLvl,
                                        warmItems(player),
                                    ),
                                )
                            }
                        }
                    }
                    !heavy && brazier.state == BrazierState.LIT -> {
                        setBrazierState(brazier, BrazierState.UNLIT)
                        for (session in sessions.values.toList()) {
                            if (near(session.player.coords, brazier.coords, 8)) {
                                session.player.mes("The brazier has gone out.")
                            }
                        }
                    }
                }
                clearWarning()
                updateHud()
            }
    }

    private fun pyromancerAttack(index: Int) {
        val pyro = pyromancers[index]
        if (pyro.down) return
        if (mapClock.cycle - pyro.lastHit < Wintertodt.TARGET_COOLDOWN) return
        pyro.lastHit = mapClock.cycle
        warning(pyro.coords)
        pending +=
            Pending(mapClock.cycle + 3) {
                if (!pyro.down) {
                    pyro.hp -= 7
                    if (pyro.hp <= 0) {
                        pyro.down = true
                        pyro.hp = 0
                        pyro.npc?.say("Arg, it got me!")
                        pyro.npc?.let { npc ->
                            pyromancerDownType?.let { npc.transmog(it, Int.MAX_VALUE) }
                        }
                    }
                }
                clearWarning()
                updateHud()
            }
    }

    private fun areaAttack() {
        val candidates =
            sessions.values.filter {
                it.player.canProcess && !Wintertodt.isSafe(it.player.coords)
            }
        if (candidates.isEmpty()) return
        val center = candidates[random.of(candidates.size)].player.coords
        warning(center)
        pending +=
            Pending(mapClock.cycle + 3) {
                for (session in sessions.values.toList()) {
                    val player = session.player
                    if (near(player.coords, center, 1)) {
                        applyWarmth(
                            player,
                            Wintertodt.areaDamage(
                                player.baseFiremakingLvl,
                                warmItems(player),
                            ),
                        )
                    }
                }
                clearWarning()
            }
    }

    private fun warning(center: CoordGrid) {
        locRepo.add(
            center,
            Wintertodt.WARNING_ICICLE,
            3,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )
        for (dx in listOf(-1, 1)) {
            for (dz in listOf(-1, 1)) {
                locRepo.add(
                    center.translate(dx, dz),
                    Wintertodt.WARNING_SNOW,
                    3,
                    LocAngle.West,
                    LocShape.CentrepieceStraight,
                )
            }
        }
        for (session in sessions.values) session.player.wintAreaAttack = 1
    }

    private fun clearWarning() {
        for (session in sessions.values) session.player.wintAreaAttack = 0
    }

    private fun applyWarmth(player: Player, damage: Int) {
        if (damage <= 0) return
        val next = (player.wintWarmth - damage).coerceAtLeast(0)
        player.wintWarmth = next
        player.showHitmark(
            Hitmark.fromNoSource(
                self = 75,
                source = 75,
                public = 75,
                damage = (damage / 10).coerceAtLeast(0),
                delay = 0,
            ),
        )
        if (next <= 0) player.queueDeath()
    }

    private fun processPending() {
        if (pending.isEmpty()) return
        val due = pending.filter { mapClock.cycle >= it.at }
        if (due.isEmpty()) return
        pending.removeAll(due)
        for (entry in due) entry.action()
    }

    private fun ensureWorld() {
        if (worldReady) return
        val wizardType = pyromancerType ?: return
        val wizardDownType = pyromancerDownType ?: return

        for (brazier in braziers) {
            val loc =
                locRepo.findAll(brazier.coords).firstOrNull { it.id in brazierIds }
                    ?: locRepo.add(
                        brazier.coords,
                        Wintertodt.BRAZIER_UNLIT,
                        Int.MAX_VALUE,
                        LocAngle.West,
                        LocShape.CentrepieceStraight,
                    )
            brazier.loc = loc
            brazier.state =
                when (loc.id) {
                    itemId(Wintertodt.BRAZIER_LIT) -> BrazierState.LIT
                    itemId(Wintertodt.BRAZIER_BROKEN) -> BrazierState.BROKEN
                    else -> BrazierState.UNLIT
                }
        }

        for (pyro in pyromancers) {
            val npc =
                npcRepo
                    .findAll(ZoneKey.from(pyro.coords), 1)
                    .firstOrNull { it.coords == pyro.coords && it.type.id == wizardType.id }
                    ?: Npc(wizardType, pyro.coords).also {
                        it.mode = NpcMode.None
                        it.moveRestrict = MoveRestrict.NoMove
                        npcRepo.add(it, Int.MAX_VALUE)
                    }
            pyro.npc = npc
        }

        val stormLoc =
            locRepo.findAll(Wintertodt.STORM).firstOrNull {
                it.id == itemId(Wintertodt.STORM_ACTIVE) ||
                    it.id == itemId(Wintertodt.STORM_IDLE)
            }
                ?: locRepo.add(
                    Wintertodt.STORM,
                    Wintertodt.STORM_IDLE,
                    Int.MAX_VALUE,
                    LocAngle.West,
                    LocShape.CentrepieceStraight,
                )
        storm = stormLoc

        worldReady = true
    }

    private fun setBrazierState(brazier: Brazier, state: BrazierState) {
        if (brazier.state == state) return
        val loc = brazier.loc ?: locRepo.findAll(brazier.coords).firstOrNull { it.id in brazierIds }
        if (loc != null) {
            locRepo.add(loc.coords, state.loc, Int.MAX_VALUE, loc.angle, loc.shape)
            brazier.loc = locRepo.findAll(brazier.coords).firstOrNull { it.id in brazierIds } ?: loc
        }
        brazier.state = state
        brazier.nextDrain =
            if (state == BrazierState.LIT) {
                mapClock.cycle + Wintertodt.DRAIN_INTERVAL + random.of(2, 8)
            } else {
                0
            }
    }

    private fun setStorm(active: Boolean) {
        val loc = storm ?: return
        val target = if (active) Wintertodt.STORM_ACTIVE else Wintertodt.STORM_IDLE
        if (loc.id == itemId(target)) return
        locRepo.add(loc.coords, target, Int.MAX_VALUE, loc.angle, loc.shape)
        storm = locRepo.findAll(Wintertodt.STORM).firstOrNull {
            it.id == itemId(Wintertodt.STORM_ACTIVE) || it.id == itemId(Wintertodt.STORM_IDLE)
        } ?: loc
    }

    private fun openHud(player: Player, session: WintertodtSession) {
        player.ifOpenOverlay(Wintertodt.HUD, Wintertodt.HUD_TARGET, eventBus)
        player.runClientScript(insideScript)
        updateHud(session)
    }

    private fun closeHud(player: Player) {
        player.runClientScript(outsideScript)
        player.ifCloseOverlay(Wintertodt.HUD, eventBus)
    }

    private fun lowestDosePotion(player: Player): Pair<Int, Int>? {
        for ((name, dose) in
            listOf(
                Wintertodt.POTION1 to 1,
                Wintertodt.POTION2 to 2,
                Wintertodt.POTION3 to 3,
                Wintertodt.POTION4 to 4,
            )) {
            val slot = player.inv.objs.indexOfFirst { it != null && it.id == itemId(name) }
            if (slot != -1) return slot to dose
        }
        return null
    }

    private fun hasPoh(player: Player): Boolean = player.attr[POH_ATTR] ?: true

    private fun near(a: CoordGrid, b: CoordGrid, radius: Int): Boolean =
        a.level == b.level &&
            kotlin.math.abs(a.x - b.x) <= radius &&
            kotlin.math.abs(a.z - b.z) <= radius

    private fun itemId(name: String): Int =
        ServerCacheManager.getItem(name.asRSCM(RSCMType.OBJ))?.id ?: -1

    private companion object {
        val POH_ATTR = org.rsmod.api.attr.AttributeKey<Boolean>()
    }
}
