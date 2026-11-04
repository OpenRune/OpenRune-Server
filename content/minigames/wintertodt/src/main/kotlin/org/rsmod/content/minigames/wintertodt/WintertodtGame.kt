package org.rsmod.content.minigames.wintertodt

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.MoveRestrict
import dev.openrune.types.NpcMode
import dev.openrune.types.NpcServerType
import dev.openrune.types.ObjectServerType
import dev.openrune.types.SequenceServerType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.types.enums.EnumTypeMap
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Locale
import kotlin.math.min
import org.rsmod.api.config.objParam
import org.rsmod.api.config.refs.params
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.output.spam
import org.rsmod.api.player.queueDeath
import org.rsmod.api.player.stat.baseConstructionLvl
import org.rsmod.api.player.stat.baseFiremakingLvl
import org.rsmod.api.player.stat.baseFletchingLvl
import org.rsmod.api.player.stat.baseHerbloreLvl
import org.rsmod.api.player.stat.baseWoodcuttingLvl
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statRandom
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.BoundValidator
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap

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
    private val boundValidator: BoundValidator,
    private val flags: CollisionFlagMap,
    private val invisibleLevels: InvisibleLevels,
) {
    private enum class Phase {
        WAITING,
        ACTIVE,
        SUBDUED,
    }

    private data class Pending(val at: Int, val action: () -> Unit)

    private val sessions = linkedMapOf<Player, WintertodtSession>()
    private val braziers = Wintertodt.BRAZIER_COORDS.mapIndexed { i, coords -> Brazier(i, coords) }
    private val areaOffsets = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
    private val brazierWarningOffsets = listOf(0 to 0, -1 to 0, 1 to 0, 0 to -1, 0 to 1)
    private val pyromancers =
        Wintertodt.PYROMANCER_COORDS.mapIndexed { i, coords -> Pyromancer(i, coords) }
    private val pending = mutableListOf<Pending>()

    private var phase = Phase.WAITING
    private var energy = Wintertodt.ENERGY_MAX
    private var respawnTimer = 0
    private var nextAttack = 0
    private var nextHud = 0
    private var worldReady = false
    private var warmthLock = false
    private var pyroDownThisRound = false
    private var allBraziersBrokenThisRound = false

    private enum class ActionType {
        CHOP,
        FLETCH,
        FEED,
        PICK,
    }

    enum class CartResult {
        CONTINUE,
        FINISHED,
        FULL,
        EMPTY,
    }

    private class ActionState(
        val type: ActionType,
        val anchor: CoordGrid,
        val target: CoordGrid?,
        val brazierIndex: Int,
        val anim: String?,
        var nextCycle: Int,
        var nextAnimCycle: Int,
    )

    private val actions = mutableMapOf<Player, ActionState>()

    private var cart: LocInfo? = null

    private val cartId: Int by lazy {
        ServerCacheManager.getObject("loc.wint_reward_pile".asRSCM(RSCMType.LOC))?.id ?: -1
    }

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

    private val fireType: NpcServerType? by lazy {
        ServerCacheManager.getNpc(Wintertodt.NPC_FIRE.asRSCM(RSCMType.NPC))
    }

    private val brewmaType: NpcServerType? by lazy {
        ServerCacheManager.getNpc(Wintertodt.NPC_BREWMA.asRSCM(RSCMType.NPC))
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
            "obj.skillcape_firemaking",
            "obj.skillcape_firemaking_trimmed",
            "obj.skillcape_firemaking_hood",
            "obj.skillcape_max",
            "obj.tzhaar_cape_fire",
            "obj.infernal_cape",
            "obj.tzhaar_cape_obsidian",
            "obj.skillcape_hitpoints",
            "obj.pride23_jumper",
            "obj.pride17_scarf",
            "obj.clue_hunter_boots",
            "obj.clue_hunter_gloves",
            "obj.wolf_mask",
            "obj.earmuffs",
            "obj.zep_bomber_cap_goggles",
            "obj.win05_hat4",
            "obj.win05_hat1",
            "obj.santa_hat",
            "obj.santa_jacket",
            "obj.santa_pants",
            "obj.santa_boots",
        )
            .mapNotNull {
                runCatching { ServerCacheManager.getItem(it.asRSCM(RSCMType.OBJ)) }.getOrNull()?.id
            }
            .toSet()
    }

    private val updateScript: Int = Wintertodt.SCRIPT_UPDATE

    private val ItemServerType.axeWoodcuttingAnim: SequenceServerType by objParam(
        params.skill_anim
    )

    private val ItemServerType.axeLevelReq: Int by objParam(params.levelrequire)

    private val treeType: ObjectServerType? by lazy {
        ServerCacheManager.getObject("loc.tree".asRSCM(RSCMType.LOC))
    }

    private val treeRates: EnumTypeMap<ItemServerType, Int>? by lazy {
        treeType?.let { runCatching { it.param(WintertodtParams.successRates) }.getOrNull() }
    }

    private val insideScript: Int = Wintertodt.SCRIPT_INSIDE

    private val outsideScript: Int = Wintertodt.SCRIPT_OUTSIDE

    fun tick() {
        if (!worldReady && sessions.isEmpty()) return
        ensureWorld()

        tickActions()
        processPending()
        if (phase == Phase.ACTIVE && braziers.all { it.state == BrazierState.BROKEN }) {
            allBraziersBrokenThisRound = true
        }

        if (phase == Phase.ACTIVE) {
            drainEnergy()
            regenerateEnergy()
            runWarmth()
            runAttacks()
            runPyromancerSpeech()
            tickFireStreams()
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
        stopAction(player)
        pillarLocks.keys.removeIf { it.first == player }
        val session = sessions.remove(player) ?: return
        session.points = 0
        player.wintWarmth = Wintertodt.WARMTH_MAX
        closeHud(player)
        player.wintAreaAttack = 0
        clearEncounterItems(player)
    }

    private fun clearEncounterItems(player: Player) {
        for (item in ENCOUNTER_ITEMS) {
            while (player.inv.count(item) > 0) {
                if (player.invDel(player.inv, item, 1).failure) break
            }
        }
    }

    fun onLogin(player: Player) {
        ensureCart()
        player.wintWarmth = Wintertodt.WARMTH_MAX
        if (Wintertodt.isInPrison(player.coords)) {
            PathingEntityCommon.teleport(player, flags, Wintertodt.EXIT)
        }
        clearEncounterItems(player)
    }

    fun onLogout(player: Player) {
        stopAction(player)
        sessions.remove(player)
        if (Wintertodt.isInPrison(player.coords)) {
            PathingEntityCommon.teleport(player, flags, Wintertodt.EXIT)
        }
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

    private val pillarLocks = mutableMapOf<Pair<Player, CoordGrid>, Int>()

    fun pillarJumpLocked(player: Player, coords: CoordGrid): Boolean {
        val until = pillarLocks[player to coords] ?: return false
        return mapClock.cycle < until
    }

    fun lockPillarJump(player: Player, coords: CoordGrid) {
        pillarLocks[player to coords] = mapClock.cycle + Wintertodt.PILLAR_JUMP_LOCK
    }

    fun checkCart(player: Player) {
        ensureCart()
        val pool = player.wintRewardPool
        when {
            pool <= 0 -> player.mes("You aren't owed any rewards from the cart.")
            pool == 1 -> player.mes("You are owed one more reward from the cart.")
            else -> player.mes("You are owed $pool more rewards from the cart.")
        }
    }

    fun cartPool(player: Player): Int {
        ensureCart()
        return player.wintRewardPool
    }

    fun searchCart(player: Player, batch: Int): CartResult {
        ensureCart()
        if (player.wintRewardPool <= 0) return CartResult.EMPTY
        repeat(batch) {
            if (player.wintRewardPool <= 0) return CartResult.FINISHED
            val reward = WintertodtRewards.roll(player, random)
            if (!WintertodtRewards.add(player, reward)) return CartResult.FULL
            player.wintRewardPool = player.wintRewardPool - 1
            player.spam("You found some loot: ${lootCount(reward.count)} x ${lootName(reward.item)}")
            player.anim(Wintertodt.CART_PICK_ANIM)
            player.soundSynth(Wintertodt.CART_PICK_SYNTH)
            if (reward.item in WintertodtRewards.UNIQUES) {
                CollectionLog.grant(player, reward.item, 1)
            }
            if (reward.item == "obj.phoenixpet") {
                player.mes("You have a funny feeling like you're being followed.")
            }
            if (player.wintRewardPool <= 0) return CartResult.FINISHED
        }
        return CartResult.CONTINUE
    }

    private fun lootCount(count: Int): String = String.format(Locale.US, "%,d", count)

    private fun lootName(item: String): String =
        ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ))?.name ?: item.removePrefix("obj.")

    fun openExtraCrate(player: Player, slot: Int) {
        val inv = player.inv
        val crate = inv[slot] ?: return
        if (crate.id != EXTRA_CRATE.asRSCM(RSCMType.OBJ)) return
        val rolls = crate.vars.coerceIn(1, 5)
        val rewards = List(rolls) { WintertodtRewards.rollExtra(player, random) }
        val opened =
            player.invTransaction(inv) {
                val from = select(inv)
                delete {
                    this.from = from
                    this.obj = crate.id
                    this.strictSlot = slot
                }
                for (reward in rewards) {
                    insert {
                        this.into = from
                        this.obj = reward.item.asRSCM(RSCMType.OBJ)
                        this.count = reward.count
                        this.cert = reward.noted
                    }
                }
            }
        if (opened.failure) {
            player.mes("You don't have enough inventory space to open that.")
            return
        }
        player.mes("You open the extra supply crate.")
    }

    fun addPoints(player: Player, amount: Int) {
        val session = session(player) ?: return
        session.points += amount
        updateHud(session)
    }

    fun canLight(player: Player, index: Int): Boolean {
        val brazier = braziers.getOrNull(index) ?: return false
        val pyro = pyromancers.getOrNull(index) ?: return false
        if (phase != Phase.ACTIVE) {
            player.mes("There's no need to do that at this time.")
            return false
        }
        if (brazier.state == BrazierState.LIT) return false
        if (pyro.down) {
            player.mes("Heal the Pyromancer before lighting the brazier.")
            return false
        }
        if (!hasTinderbox(player)) {
            player.mes("You need a tinderbox to light the brazier.")
            return false
        }
        return true
    }

    fun canFix(player: Player, index: Int): Boolean {
        val brazier = braziers.getOrNull(index) ?: return false
        if (phase != Phase.ACTIVE) {
            player.mes("There's no need to do that at this time.")
            return false
        }
        if (brazier.state != BrazierState.BROKEN) return false
        if (!hasHammer(player)) {
            player.mes("You need a hammer to fix the brazier.")
            return false
        }
        return true
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
        player.soundSynth(2581)
        player.mes("You fix the brazier.")
        addPoints(player, 25)
        if (hasPoh(player)) {
            player.statAdvance("stat.construction", 4.0 * player.baseConstructionLvl)
        }
        completeTask(player, { caWintertodtHandyman }, { caWintertodtHandyman = it })
        updateHud()
    }

    fun isAtBrazier(player: Player, index: Int): Boolean {
        val brazier = braziers.getOrNull(index) ?: return false
        return nearBrazier(player.coords, brazier.coords)
    }

    fun hasFeed(player: Player): Boolean =
        player.inv.count(Wintertodt.KINDLING) > 0 || player.inv.count(Wintertodt.ROOT) > 0

    fun feed(player: Player, index: Int): Boolean {
        if (!isAtBrazier(player, index)) return false
        val brazier = braziers.getOrNull(index) ?: return false
        if (brazier.state != BrazierState.LIT) return false
        val kindling = player.inv.count(Wintertodt.KINDLING) > 0
        val item = if (kindling) Wintertodt.KINDLING else Wintertodt.ROOT
        if (player.inv.count(item) <= 0) {
            player.mes("You have run out of bruma roots.")
            return false
        }
        if (player.invDel(player.inv, item, 1).failure) return false
        if (kindling) {
            addPoints(player, 25)
            player.statAdvance("stat.firemaking", 3.8 * player.baseFiremakingLvl)
        } else {
            addPoints(player, 10)
            player.statAdvance("stat.firemaking", 3.0 * player.baseFiremakingLvl)
        }
        return true
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
        player.soundSynth(2581)
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

    fun onFoodConsumed(player: Player, item: String, healAmount: Int) {
        if (!Wintertodt.isInPrison(player.coords)) return
        if (healAmount < 4) return
        if (item in Wintertodt.WARMTH_EXCLUDED_FOOD) return
        player.wintWarmth = min(Wintertodt.WARMTH_MAX, player.wintWarmth + 350)
        updateHud()
    }

    fun healPyromancer(player: Player, npc: Npc, useSlot: Int? = null) {
        val pyro = pyromancers.firstOrNull { it.npc == npc } ?: return
        if (!pyro.down) {
            npc.say("No need! I'm fine thanks.")
            pyro.nextSpeak = mapClock.cycle + 10
            return
        }
        val potion = if (useSlot != null) doseAt(player, useSlot) else lowestDosePotion(player)
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
        showPyromancerHeal(pyro)
        addPoints(player, 75)
        completeTask(player, { caWintertodtMummy }, { caWintertodtMummy = it })
        updateHud()
    }

    private fun showPyromancerDamage(pyro: Pyromancer, damage: Int) {
        val npc = pyro.npc ?: return
        npc.showHitmark(
            Hitmark.fromNoSource(
                self = Wintertodt.HITMARK_NPC_DAMAGE,
                source = Wintertodt.HITMARK_NPC_DAMAGE,
                public = Wintertodt.HITMARK_NPC_DAMAGE,
                damage = damage,
                delay = 0,
            ),
        )
        showPyromancerHealth(pyro)
    }

    private fun showPyromancerHeal(pyro: Pyromancer) {
        val npc = pyro.npc ?: return
        npc.showHitmark(
            Hitmark.fromNoSource(
                self = Wintertodt.HITMARK_NPC_HEAL,
                source = Wintertodt.HITMARK_NPC_HEAL,
                public = Wintertodt.HITMARK_NPC_HEAL,
                damage = pyro.hp,
                delay = 0,
            ),
        )
        showPyromancerHealth(pyro)
    }

    private fun showPyromancerHealth(pyro: Pyromancer) {
        val npc = pyro.npc ?: return
        npc.hitpoints = pyro.hp.coerceAtLeast(1)
        npc.baseHitpointsLvl = 14
        val fill = (pyro.hp.coerceIn(0, 14) * Wintertodt.HEADBAR_SEGMENTS) / 14
        npc.showHeadbar(
            Headbar.fromNoSource(
                self = Wintertodt.HEADBAR_HEALTH,
                public = Wintertodt.HEADBAR_HEALTH,
                startFill = fill,
                endFill = fill,
                startTime = 0,
                endTime = 0,
            ),
        )
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

    fun hasAxe(player: Player): Boolean = bestAxe(player) != null

    fun axeAnim(player: Player): String? {
        val axe = bestAxe(player) ?: return null
        return runCatching {
                RSCM.getReverseMapping(RSCMType.SEQ, axe.axeWoodcuttingAnim.id)
            }
            .getOrNull()
    }

    fun chopSuccess(player: Player): Boolean {
        val axe = bestAxe(player) ?: return false
        val (low, high) = axeRates(axe) ?: return false
        return player.statRandom(random, "stat.woodcutting", low, high, invisibleLevels)
    }

    fun beginChop(player: Player, loc: BoundLocInfo) {
        if (phase != Phase.ACTIVE) {
            player.mes("There's no use for bruma roots at this time.")
            return
        }
        if (!hasAxe(player)) {
            player.mes("You need an axe to chop down this tree.")
            return
        }
        if (player.inv.isFull()) {
            player.mes("Your inventory is too full to hold any more roots.")
            return
        }
        val anim = axeAnim(player) ?: Wintertodt.ANIM_CHOP
        actions[player] =
            ActionState(
                type = ActionType.CHOP,
                anchor = player.coords,
                target = loc.coords,
                brazierIndex = -1,
                anim = anim,
                nextCycle = mapClock.cycle + 1,
                nextAnimCycle = mapClock.cycle + 6,
            )
        player.anim(anim)
        player.mes("You swing your axe at the roots.")
    }

    fun beginPick(player: Player, loc: BoundLocInfo) {
        if (phase != Phase.ACTIVE) {
            player.mes("There's no need to do that at this time.")
            return
        }
        if (player.inv.isFull()) {
            player.mes("The herb shatters as it hits the floor.")
            return
        }
        actions[player] =
            ActionState(
                type = ActionType.PICK,
                anchor = player.coords,
                target = loc.coords,
                brazierIndex = -1,
                anim = Wintertodt.ANIM_HERB,
                nextCycle = mapClock.cycle + 3,
                nextAnimCycle = 0,
            )
        player.anim(Wintertodt.ANIM_HERB)
    }

    fun beginFletch(player: Player) {
        if (player.inv.count(Wintertodt.ROOT) <= 0) return
        actions[player] =
            ActionState(
                type = ActionType.FLETCH,
                anchor = player.coords,
                target = null,
                brazierIndex = -1,
                anim = Wintertodt.ANIM_FLETCH,
                nextCycle = mapClock.cycle + 4,
                nextAnimCycle = 0,
            )
        player.anim(Wintertodt.ANIM_FLETCH)
    }

    fun beginFeed(player: Player, index: Int) {
        if (phase != Phase.ACTIVE) {
            player.mes("There's no need to do that at this time.")
            return
        }
        if (!isAtBrazier(player, index)) return
        if (!hasFeed(player)) {
            player.mes("You have run out of bruma roots.")
            return
        }
        actions[player] =
            ActionState(
                type = ActionType.FEED,
                anchor = player.coords,
                target = braziers.getOrNull(index)?.coords,
                brazierIndex = index,
                anim = Wintertodt.ANIM_FEED,
                nextCycle = mapClock.cycle + 3,
                nextAnimCycle = 0,
            )
        player.anim(Wintertodt.ANIM_FEED)
        player.soundSynth(2596)
        player.soundSynth(2596, delay = 15)
    }

    fun stopAction(player: Player) {
        actions.remove(player)
    }

    private fun tickActions() {
        if (actions.isEmpty()) return
        val cycle = mapClock.cycle
        for ((player, action) in actions.entries.toList()) {
            if (!player.canProcess) {
                actions.remove(player)
                continue
            }
            if (action.type != ActionType.FLETCH && player.coords != action.anchor) {
                actions.remove(player)
                continue
            }
            if (action.type == ActionType.FEED && !isAtBrazier(player, action.brazierIndex)) {
                actions.remove(player)
                continue
            }
            if (action.nextAnimCycle > 0 && cycle >= action.nextAnimCycle) {
                action.anim?.let { player.anim(it) }
                action.nextAnimCycle = cycle + 6
            }
            if (cycle < action.nextCycle) continue
            when (action.type) {
                ActionType.CHOP -> {
                    if (player.inv.isFull()) {
                        player.mes("Your inventory is too full to hold any more roots.")
                        actions.remove(player)
                        continue
                    }
                    chopSound(player)
                    if (chopSuccess(player)) chopRoot(player)
                    action.nextCycle = cycle + 3
                }
                ActionType.PICK -> {
                    if (player.inv.isFull()) {
                        player.mes("The herb shatters as it hits the floor.")
                        actions.remove(player)
                        continue
                    }
                    pickHerb(player)
                    player.anim(Wintertodt.ANIM_HERB)
                    action.nextCycle = cycle + 3
                }
                ActionType.FLETCH -> {
                    if (player.inv.count(Wintertodt.ROOT) <= 0) {
                        actions.remove(player)
                        continue
                    }
                    fletch(player)
                    player.anim(Wintertodt.ANIM_FLETCH)
                    action.nextCycle = cycle + 4
                }
                ActionType.FEED -> {
                    if (!feed(player, action.brazierIndex)) {
                        actions.remove(player)
                        continue
                    }
                    player.anim(Wintertodt.ANIM_FEED)
                    player.soundSynth(2596)
                    player.soundSynth(2596, delay = 15)
                    action.nextCycle = cycle + 3
                }
            }
        }
    }

    fun chopSound(player: Player) {
        runCatching {
                val name = RSCM.getReverseMapping(RSCMType.SYNTH, 2735)
                worldRepo.soundArea(player.coords, name, radius = 1)
            }
            .onFailure { player.soundSynth(2735) }
    }

    fun hasBrumaTorch(player: Player): Boolean =
        player.inv.count("obj.wint_torch") > 0 ||
            player.inv.count("obj.wint_torch_offhand") > 0 ||
            player.worn.count("obj.wint_torch") > 0 ||
            player.worn.count("obj.wint_torch_offhand") > 0

    private fun bestAxe(player: Player): ItemServerType? {
        val candidates =
            (player.worn.objs.asSequence() + player.inv.objs.asSequence())
                .filterNotNull()
                .mapNotNull { ServerCacheManager.getItem(it.id) }
                .filter { isUsableAxe(player, it) }
        return candidates.maxWithOrNull(
            compareBy<ItemServerType> { chopRate(player, it) ?: -1.0 }
                .thenBy { it.axeLevelReq }
                .thenBy { it.id }
        )
    }

    private fun isUsableAxe(player: Player, type: ItemServerType): Boolean {
        if (!type.isContentType("content.woodcutting_axe")) return false
        if (player.baseWoodcuttingLvl < type.axeLevelReq) return false
        if (type.internalName == "obj.infernal_axe" && player.baseFiremakingLvl < 85) {
            return false
        }
        return axeRates(type) != null
    }

    private fun chopRate(player: Player, it: ItemServerType): Double? {
        val (low, high) = axeRates(it) ?: return null
        val level =
            (player.stat("stat.woodcutting") + invisibleLevels.get(player, "stat.woodcutting"))
                .coerceIn(1, 99)
        return SkillingSuccessRate.successRate(low, high, level, 99)
    }

    private fun axeRates(type: ItemServerType): Pair<Int, Int>? {
        val packed = treeRates?.firstOrNull { it.key.id == type.id }?.value ?: return null
        return (packed shr 16) to (packed and 0xFFFF)
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

    fun setRewardPool(player: Player, value: Int) {
        player.wintRewardPool = value.coerceIn(0, Wintertodt.REWARD_POOL_MAX)
    }

    fun debugStatus(player: Player) {
        val session = session(player)
        player.mes(
            "phase=$phase energy=$energy points=${session?.points ?: -1} " +
                "warmth=${player.wintWarmth} cart=${cart?.coords}",
        )
        for (brazier in braziers) {
            player.mes(
                "brazier ${brazier.index}: state=${brazier.state} loc=${brazier.loc?.id} " +
                    "coords=${brazier.loc?.coords}",
            )
        }
        for (pyro in pyromancers) {
            player.mes(
                "pyro ${pyro.index}: down=${pyro.down} hp=${pyro.hp} npc=${pyro.npc?.coords} " +
                    "nextSpeak=${pyro.nextSpeak}",
            )
        }
    }

    fun debugPyroSay() {
        for (pyro in pyromancers) {
            pyro.npc?.say("Yemalo shi cardito!")
        }
    }

    fun debugLocations(player: Player) {
        val ids =
            brazierIds +
                setOf(
                    locId(Wintertodt.STORM_ACTIVE),
                    locId(Wintertodt.STORM_IDLE),
                )
        val origin = ZoneKey.from(Wintertodt.STORM)
        for (dx in -3..3) {
            for (dz in -3..3) {
                for (loc in locRepo.findAll(origin.translate(dx, dz))) {
                    if (loc.id in ids) {
                        val type = ServerCacheManager.getObject(loc.id)
                        val bound = type?.let { BoundLocInfo(loc, it) }
                        val touches =
                            bound != null && boundValidator.touches(player.avatar, bound)
                        player.mes(
                            "loc id=${loc.id} coords=${loc.coords} shape=${loc.shape} " +
                                "angle=${loc.angle} w=${type?.width} l=${type?.length} " +
                                "faf=${type?.forceApproachFlags} touch=$touches",
                        )
                    }
                }
            }
        }
        player.mes("player coords=${player.coords}")
        for (dx in -1..1) {
            for (dz in -1..1) {
                val coords = player.coords.translate(dx, dz)
                for (loc in locRepo.findAll(coords)) {
                    val type = ServerCacheManager.getObject(loc.id)
                    player.mes(
                        "tile ${coords.x},${coords.z} loc=${type?.internalName} " +
                            "layer=${loc.layer} block=${type?.blockWalk}",
                    )
                }
            }
        }
        val originLoc = braziers.firstOrNull()?.loc ?: return
        for (dx in -1..3) {
            for (dz in -1..3) {
                val x = originLoc.coords.x + dx
                val z = originLoc.coords.z + dz
                player.mes("flag $x,$z=${flags[x, z, originLoc.coords.level]}")
            }
        }
    }

    fun toggleWarmthLock(player: Player) {
        warmthLock = !warmthLock
        player.mes("warmthLock=$warmthLock")
        if (warmthLock) player.wintWarmth = Wintertodt.WARMTH_MAX
    }

    fun forceAttack() {
        if (phase != Phase.ACTIVE) return
        specialAttack()
    }

    fun forceAreaAttack() {
        if (phase != Phase.ACTIVE) return
        areaAttack()
    }

    fun forceBrazierAttack(index: Int = 0) {
        if (phase != Phase.ACTIVE) return
        brazierAttack(index.coerceIn(0, 3))
    }

    fun forcePyromancerAttack(index: Int = 0) {
        if (phase != Phase.ACTIVE) return
        pyromancerAttack(index.coerceIn(0, 3))
    }

    fun forceBrazierBreak(index: Int = 0) {
        if (phase != Phase.ACTIVE) return
        val brazier = braziers[index.coerceIn(0, 3)]
        if (brazier.state == BrazierState.LIT) damageBrazier(brazier)
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
        pyroDownThisRound = false
        allBraziersBrokenThisRound = false
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
            pyro.npc?.say("We can rest for a time.")
        }
        setStorm(active = false)
        for (session in sessions.values) {
            stopAction(session.player)
            awardRound(session)
            clearEncounterItems(session.player)
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
        val extra =
            if (random.of(Wintertodt.POINT_THRESHOLD) < points % 500) 1 else 0
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
        if (player.wintertodtKills >= 5) {
            completeTask(player, { caWintertodtKill5 }, { caWintertodtKill5 = it })
        }
        if (player.wintertodtKills >= 10) {
            completeTask(player, { caWintertodtKill10 }, { caWintertodtKill10 = it })
        }
        if (!pyroDownThisRound) {
            completeTask(player, { caWintertodtNoOneBehind }, { caWintertodtNoOneBehind = it })
        }
        if (!allBraziersBrokenThisRound) {
            completeTask(player, { caWintertodtCanWeFixIt }, { caWintertodtCanWeFixIt = it })
        }
        if (warmItems(player) >= Wintertodt.WARM_ITEMS_MAX) {
            completeTask(player, { caWintertodtCosy }, { caWintertodtCosy = it })
        }
        if (points >= 3000) {
            completeTask(player, { caWintertodtWhyFletch }, { caWintertodtWhyFletch = it })
        }
        session.points = 0
    }

    private fun completeTask(player: Player, get: Player.() -> Int, set: Player.(Int) -> Unit) {
        if (player.get() != 0) return
        player.set(1)
        player.caWintertodtTotal = player.caWintertodtTotal + 1
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
            if (player.wintWarmth <= 0) continue
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
        if (random.of(Wintertodt.ENERGY_MAX) < energy) {
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
                    player.wintWarmth > 0 &&
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
            in 1..4 -> brazierAttack(random.of(0, 3))
            else -> pyromancerAttack(random.of(0, 3))
        }
    }

    private fun brazierAttack(index: Int) {
        val brazier = braziers[index]
        if (brazier.state != BrazierState.LIT) return
        if (mapClock.cycle - brazier.lastHit < Wintertodt.TARGET_COOLDOWN) return
        brazier.lastHit = mapClock.cycle
        val heavy = random.of(0, 2) == 0
        addSnowfall(brazier.coords, brazierWarningOffsets, duration = 3)
        worldRepo.projAnim(travelBreath(brazier.coords, startHeight = 150, endTime = 120))
        worldRepo.spotanimMap(
            SpotanimType(Wintertodt.SPOTANIM_AREA_BREATH.asRSCM(RSCMType.SPOTANIM)),
            brazier.coords,
            height = 70,
            delay = 4,
        )
        pending +=
            Pending(mapClock.cycle + 4) {
                if (brazier.state == BrazierState.LIT) {
                    if (heavy) {
                        damageBrazier(brazier)
                        worldRepo.soundArea(brazier.coords, Wintertodt.SOUND_EXPLOSION, radius = 2)
                    } else {
                        setBrazierState(brazier, BrazierState.UNLIT)
                        worldRepo.soundArea(brazier.coords, Wintertodt.SOUND_HIT, radius = 10)
                    }
                }
                updateHud()
            }
    }

    private fun addSnowfall(center: CoordGrid, offsets: List<Pair<Int, Int>>, duration: Int) {
        for ((dx, dz) in offsets) {
            locRepo.add(
                center.translate(dx, dz),
                Wintertodt.SNOWFALL_WARNING,
                duration,
                LocAngle.entries[random.of(0, LocAngle.entries.lastIndex)],
                LocShape.CentrepieceStraight,
            )
        }
    }

    private fun travelBreath(target: CoordGrid, startHeight: Int, endTime: Int): ProjAnim =
        ProjAnim(
            spotanim = Wintertodt.SPOTANIM_TRAVEL_BREATH.asRSCM(RSCMType.SPOTANIM),
            startHeight = startHeight,
            endHeight = 50,
            startTime = 0,
            endTime = endTime,
            angle = 64,
            progress = 0,
            sourceIndex = 0,
            targetIndex = 0,
            startCoord = Wintertodt.STORM.translate(3, 3),
            endCoord = target,
        )

    private fun damageBrazier(brazier: Brazier) {
        setBrazierState(brazier, BrazierState.BROKEN)
        for (session in sessions.values.toList()) {
            val player = session.player
            if (player.wintWarmth <= 0) continue
            if (nearBrazier(player.coords, brazier.coords)) {
                applyWarmth(
                    player,
                    Wintertodt.brazierDamage(player.baseFiremakingLvl, warmItems(player)),
                )
            }
        }
    }

    private fun tickFireStreams() {
        if (mapClock.cycle % 2 != 0) return
        val type = fireType ?: return
        for (brazier in braziers) {
            if (brazier.state != BrazierState.LIT) continue
            if (pyromancers[brazier.index].down) continue
            val spawn = brazier.coords.translate(1, 1)
            val npc = Npc(type, spawn)
            npc.mode = NpcMode.None
            npcRepo.add(npc, 16)
            npc.walk(firePath(spawn)) { npcRepo.del(npc, Int.MAX_VALUE) }
        }
    }

    private fun firePath(start: CoordGrid): List<CoordGrid> {
        val storm = Wintertodt.STORM
        val center = storm.translate(3, 3)
        val minX = storm.x
        val maxX = storm.x + 6
        val minZ = storm.z
        val maxZ = storm.z + 6
        val path = mutableListOf<CoordGrid>()
        var x = start.x
        var z = start.z
        while (true) {
            val dx = (center.x - x).coerceIn(-1, 1)
            val dz = (center.z - z).coerceIn(-1, 1)
            if (dx == 0 && dz == 0) break
            x += dx
            z += dz
            path += CoordGrid(x, z, start.level)
            if (x in minX..maxX && z in minZ..maxZ) break
        }
        return path
    }

    private fun runPyromancerSpeech() { for (pyro in pyromancers) {
            val npc = pyro.npc ?: continue
            if (mapClock.cycle < pyro.nextSpeak) continue
            if (pyro.down) {
                npc.say(Wintertodt.PYROMANCER_DOWN_LINES[random.of(0, Wintertodt.PYROMANCER_DOWN_LINES.lastIndex)])
                pyro.nextSpeak = mapClock.cycle + random.of(10, 14)
                continue
            }
            when (braziers[pyro.index].state) {
                BrazierState.UNLIT -> {
                    npc.say("Light this brazier!")
                    pyro.nextSpeak = mapClock.cycle + random.of(10, 14)
                }
                BrazierState.BROKEN -> {
                    npc.say("Fix this brazier!")
                    pyro.nextSpeak = mapClock.cycle + random.of(10, 14)
                }
                BrazierState.LIT -> {
                    npc.say("Yemalo shi cardito!")
                    pyro.nextSpeak = mapClock.cycle + random.of(1, 30)
                }
            }
        }
    }

    private fun pyromancerAttack(index: Int) {
        val pyro = pyromancers[index]
        if (pyro.down) return
        if (mapClock.cycle - pyro.lastHit < Wintertodt.TARGET_COOLDOWN) return
        pyro.lastHit = mapClock.cycle
        addSnowfall(pyro.coords, listOf(0 to 0), duration = 3)
        worldRepo.projAnim(travelBreath(pyro.coords, startHeight = 0, endTime = 120))
        worldRepo.spotanimMap(
            SpotanimType(Wintertodt.SPOTANIM_AREA_BREATH.asRSCM(RSCMType.SPOTANIM)),
            pyro.coords,
            delay = 4,
        )
        pending +=
            Pending(mapClock.cycle + 6) {
                if (!pyro.down) {
                    val hpBefore = pyro.hp
                    val damage = hpBefore.coerceAtMost(7)
                    pyro.hp = hpBefore - damage
                    showPyromancerDamage(pyro, damage)
                    worldRepo.soundArea(pyro.coords, Wintertodt.SOUND_HIT, radius = 10)
                    if (pyro.hp <= 0) {
                        pyro.down = true
                        pyroDownThisRound = true
                        pyro.hp = 0
                        showPyromancerHealth(pyro)
                        pyro.npc?.say("Arg, it got me!")
                        pyro.npc?.anim("seq.dream_spell_start")
                        pyro.npc?.let { npc ->
                            pyromancerDownType?.let { npc.transmog(it, Int.MAX_VALUE) }
                        }
                        worldRepo.soundArea(pyro.coords, Wintertodt.SOUND_DEATH, radius = 3)
                        if (braziers[index].state == BrazierState.LIT) {
                            setBrazierState(braziers[index], BrazierState.UNLIT)
                        }
                    }
                }
                updateHud()
            }
    }

    private fun areaAttack() {
        val candidates =
            sessions.values.filter {
                val player = it.player
                player.canProcess &&
                    player.wintWarmth > 0 &&
                    !Wintertodt.isSafe(player.coords) &&
                    canAreaTarget(player.coords)
            }
        if (candidates.isEmpty()) return
        val session = candidates[random.of(candidates.size)]
        val center = session.player.coords
        val spot = Wintertodt.SPOTANIM_AREA_SPOT.asRSCM(RSCMType.SPOTANIM)
        val impact = SpotanimType(Wintertodt.SPOTANIM_AREA_IMPACT.asRSCM(RSCMType.SPOTANIM))
        val breath = SpotanimType(Wintertodt.SPOTANIM_AREA_BREATH.asRSCM(RSCMType.SPOTANIM))
        addSnowfall(center, areaOffsets + (0 to 0), duration = 13)
        worldRepo.projAnim(travelBreath(center, startHeight = 0, endTime = 200))
        pending +=
            Pending(mapClock.cycle + 3) {
                worldRepo.projAnim(travelBreath(center, startHeight = 0, endTime = 150))
                for ((dx, dz) in areaOffsets) {
                    val corner = center.translate(dx, dz)
                    val source = corner.translate(random.of(-1, 1), 3)
                    worldRepo.projAnim(
                        ProjAnim(
                            spotanim = spot,
                            startHeight = 400,
                            endHeight = 0,
                            startTime = 50,
                            endTime = 90,
                            angle = 0,
                            progress = 0,
                            sourceIndex = 0,
                            targetIndex = 0,
                            startCoord = source,
                            endCoord = corner,
                        )
                    )
                    worldRepo.spotanimMap(impact, corner, delay = 3)
                }
                worldRepo.spotanimMap(breath, center, delay = 3)
            }
        pending +=
            Pending(mapClock.cycle + 5) {
                for (session in sessions.values) session.player.wintAreaAttack = 1
            }
        pending +=
            Pending(mapClock.cycle + 6) {
                showAreaImpact(center)
                worldRepo.soundArea(center, Wintertodt.SOUND_EXPLOSION, radius = 2)
                for (session in sessions.values.toList()) {
                    val player = session.player
                    if (!player.canProcess) continue
                    if (player.wintWarmth <= 0) continue
                    if (Wintertodt.isSafe(player.coords)) continue
                    if (near(player.coords, center, 1)) {
                        player.mes(
                            "The freezing cold attack of the Wintertodt's magic hits you.",
                        )
                        applyWarmth(
                            player,
                            Wintertodt.areaDamage(
                                player.baseFiremakingLvl,
                                warmItems(player),
                            ),
                        )
                        player.soundSynth(
                            if (random.of(0, 1) == 0) Wintertodt.SOUND_HIT_2
                            else Wintertodt.SOUND_HIT_4,
                            delay = 20,
                        )
                    }
                }
                clearWarning()
            }
    }

    private fun canAreaTarget(center: CoordGrid): Boolean {
        for (dx in -1..1) {
            for (dz in -1..1) {
                val tile = center.translate(dx, dz)
                if (flags.isWalkBlocked(tile)) return false
                if (hasObstacleLoc(tile)) return false
            }
        }
        return true
    }

    private fun hasObstacleLoc(coords: CoordGrid): Boolean =
        locRepo.findAll(coords).any { loc ->
            val type = ServerCacheManager.getObject(loc.id) ?: return@any false
            type.blockWalk != 0 || (loc.id in brazierIds && (type.width > 1 || type.length > 1))
        }

    private fun showAreaImpact(center: CoordGrid) {
        locRepo.add(
            center,
            Wintertodt.WARNING_ICICLE,
            2,
            LocAngle.entries[random.of(0, LocAngle.entries.lastIndex)],
            LocShape.CentrepieceStraight,
        )
        for ((dx, dz) in areaOffsets) {
            locRepo.add(
                center.translate(dx, dz),
                Wintertodt.WARNING_SNOW,
                2,
                LocAngle.entries[random.of(0, LocAngle.entries.lastIndex)],
                LocShape.CentrepieceStraight,
            )
        }
    }

    private fun clearWarning() {
        for (session in sessions.values) session.player.wintAreaAttack = 0
    }

    private fun applyWarmth(player: Player, damage: Int) {
        if (damage <= 0) return
        if (player.wintWarmth <= 0) return
        if (warmthLock) {
            player.wintWarmth = Wintertodt.WARMTH_MAX
            return
        }
        val before = player.wintWarmth
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
        val fill = (next * Wintertodt.HEADBAR_SEGMENTS) / Wintertodt.WARMTH_MAX
        player.showHeadbar(
            Headbar.fromNoSource(
                self = Wintertodt.HEADBAR_WARMTH,
                public = Wintertodt.HEADBAR_WARMTH,
                startFill = fill,
                endFill = fill,
                startTime = 0,
                endTime = 0,
            ),
        )
        if (next <= 0 && before > 0) {
            player.mes("<col=ef1020>The cold of the Wintertodt has overcome you!")
            player.queueDeath()
        }
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

        for (id in brazierIds) {
            ServerCacheManager.getObject(id)?.forceApproachFlags = 0
        }

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
                    locId(Wintertodt.BRAZIER_LIT) -> BrazierState.LIT
                    locId(Wintertodt.BRAZIER_BROKEN) -> BrazierState.BROKEN
                    else -> BrazierState.UNLIT
                }
        }

        for (pyro in pyromancers) {
            val existing =
                npcRepo
                    .findAll(ZoneKey.from(Wintertodt.STORM), 2)
                    .filter { it.type.id == wizardType.id }
                    .toList()
            val npc =
                existing
                    .filter { candidate -> pyromancers.none { it.npc === candidate } }
                    .minByOrNull { it.coords.chebyshevDistance(pyro.coords) }
                    ?: Npc(wizardType, pyro.coords).also {
                        it.mode = NpcMode.None
                        it.moveRestrict = MoveRestrict.NoMove
                        npcRepo.add(it, Int.MAX_VALUE)
                    }
            if (npc.coords != pyro.coords) {
                npc.teleport(flags, pyro.coords)
            }
            npc.mode = NpcMode.None
            npc.moveRestrict = MoveRestrict.NoMove
            npc.hitpoints = 14
            npc.baseHitpointsLvl = 14
            pyro.npc = npc
        }
        val assigned = pyromancers.mapNotNull { it.npc }.toSet()
        for (extra in npcRepo.findAll(ZoneKey.from(Wintertodt.STORM), 2).toList()) {
            if (extra.type.id == wizardType.id && extra !in assigned) {
                npcRepo.del(extra, Int.MAX_VALUE)
            }
        }

        brewmaType?.let { type ->
            for (npc in npcRepo.findAll(ZoneKey.from(BREWMA_COORDS), 2)) {
                if (npc.type.id != type.id) continue
                npc.mode = NpcMode.None
                npc.moveRestrict = MoveRestrict.NoMove
                if (npc.coords != BREWMA_COORDS) npc.teleport(flags, BREWMA_COORDS)
            }
        }

        val stormLoc =
            locRepo.findAll(Wintertodt.STORM).firstOrNull {
                it.id == locId(Wintertodt.STORM_ACTIVE) ||
                    it.id == locId(Wintertodt.STORM_IDLE)
            }
                ?: locRepo.add(
                    Wintertodt.STORM,
                    Wintertodt.STORM_IDLE,
                    Int.MAX_VALUE,
                    LocAngle.West,
                    LocShape.CentrepieceStraight,
                )
        storm = stormLoc

        findCart()
        worldReady = true
    }

    private fun findCart() {
        val origin = ZoneKey.from(CoordGrid(1636, 3942, 0))
        for (dx in -1..1) {
            for (dz in -1..1) {
                val loc =
                    locRepo.findAll(origin.translate(dx, dz)).firstOrNull { it.id == cartId }
                        ?: continue
                cart = loc
                return
            }
        }
    }

    private fun ensureCart() {
        if (cart != null) return
        findCart()
        if (cart != null) return
        cart =
            locRepo.add(
                CoordGrid(1635, 3941, 0),
                "loc.wint_reward_pile",
                Int.MAX_VALUE,
                LocAngle.South,
                LocShape.CentrepieceStraight,
            )
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
        if (loc.id == locId(target)) return
        locRepo.add(loc.coords, target, Int.MAX_VALUE, loc.angle, loc.shape)
        storm = locRepo.findAll(Wintertodt.STORM).firstOrNull {
            it.id == locId(Wintertodt.STORM_ACTIVE) || it.id == locId(Wintertodt.STORM_IDLE)
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

    private fun doseAt(player: Player, slot: Int): Pair<Int, Int>? {
        val item = player.inv[slot] ?: return null
        val dose =
            when (item.id) {
                itemId(Wintertodt.POTION4) -> 4
                itemId(Wintertodt.POTION3) -> 3
                itemId(Wintertodt.POTION2) -> 2
                itemId(Wintertodt.POTION1) -> 1
                else -> return null
            }
        return slot to dose
    }

    private fun hasPoh(player: Player): Boolean = player.attr[POH_ATTR] ?: true

    private fun near(a: CoordGrid, b: CoordGrid, radius: Int): Boolean =
        a.level == b.level &&
            kotlin.math.abs(a.x - b.x) <= radius &&
            kotlin.math.abs(a.z - b.z) <= radius

    private fun nearBrazier(coords: CoordGrid, base: CoordGrid): Boolean =
        coords.level == base.level &&
            coords.x in (base.x - 1)..(base.x + 3) &&
            coords.z in (base.z - 1)..(base.z + 3)

    private fun itemId(name: String): Int =
        ServerCacheManager.getItem(name.asRSCM(RSCMType.OBJ))?.id ?: -1

    private fun locId(name: String): Int =
        ServerCacheManager.getObject(name.asRSCM(RSCMType.LOC))?.id ?: -1

    private companion object {
        val POH_ATTR = org.rsmod.api.attr.AttributeKey<Boolean>()
        val BREWMA_COORDS = CoordGrid(1635, 3986, 0)
        val ENCOUNTER_ITEMS =
            listOf(
                Wintertodt.ROOT,
                Wintertodt.KINDLING,
                Wintertodt.HERB,
                Wintertodt.VIAL,
                Wintertodt.POTION4,
                Wintertodt.POTION3,
                Wintertodt.POTION2,
                Wintertodt.POTION1,
            )
    }
}
