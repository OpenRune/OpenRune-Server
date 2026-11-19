package org.rsmod.content.events.demonicpacts

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.constants
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductPrepareEvent
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.hook.ItemTeleportCaster
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.SkillModifiers
import org.rsmod.api.player.stat.StatXpModifiers
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.spells.autocast.MagicSpellbookManager
import org.rsmod.api.spells.runes.unlimited.InventoryRuneSources
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.api.table.FiremakingLogsRow
import org.rsmod.api.table.cooking.CookingFoodsRow
import org.rsmod.api.table.herblore.HerbloreCleaningRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.world.WorldType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class RelicEffectsScript
@Inject
constructor(
    private val random: GameRandom,
    private val playerList: PlayerList,
    private val npcList: NpcList,
    private val collision: CollisionFlagMap,
    private val teleports: ItemTeleportCaster,
    private val spellbooks: MagicSpellbookManager,
) : PluginScript() {
    private val logger = InlineLogger()

    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    private val cookedFish by lazy {
        CookingFoodsRow.all().associateBy { it.raw.internalName }
    }

    private val burnableLogs by lazy {
        FiremakingLogsRow.all().associateBy { it.input.internalName }
    }

    private val grimyHerbs by lazy { HerbloreCleaningRow.all() }

    override fun ScriptContext.startup() {
        installHooks()
        onEvent<SkillingProductPrepareEvent> { onPrepare(product) }
        onEvent<SkillingActionCompleteEvent> { onComplete(player, context) }
        onEvent<NpcHitEvents.ModifyAny> { onPlayerHitsNpc(npc, hit) }

        onRelicItemOp(BANK_HEIST_ITEM, TELEPORT_OPS) { bankHeist() }
        onRelicItemOp(GRIMOIRE_ITEM, GRIMOIRE_OPS) { switchSpellbook() }
        onRelicItemOp(FLASK_ITEM, FLASK_OPS) { drinkFlask() }

        onCommand("relics") {
            desc = "List your relics and what they do here"
            cheat { player.listRelics() }
        }
        onCommand("relic") {
            desc = "Toggle relic options: ::relic bank on|off, ::relic burn on|off"
            cheat { player.toggleRelicOption(args) }
        }
        onCommand("relicitems") {
            desc = "Get back the items your relics give"
            cheat { player.reclaimRelicItems() }
        }
    }

    private fun installHooks() {
        val previousSuccess = SkillModifiers.successRate
        SkillModifiers.successRate = { player, stat, rate ->
            relicSuccessRate(player, stat, previousSuccess(player, stat, rate))
        }
        val previousLoot = SkillModifiers.lootMultiplier
        SkillModifiers.lootMultiplier = { player, stat ->
            val base = previousLoot(player, stat)
            if (stat == THIEVING && player.hasRelic(RelicNames.LARCENIST)) base * LARCENIST_LOOT else base
        }
        val previousFood = SkillModifiers.keepsFood
        SkillModifiers.keepsFood = { player ->
            previousFood(player) || player.hasRelic(RelicNames.ETERNAL_SUSTENANCE)
        }
        val previousBonus = StatXpModifiers.bonus
        StatXpModifiers.bonus = { player, stat ->
            val abundance =
                if (stat in RelicEffects.nonCombatStats && player.hasRelic(RelicNames.ABUNDANCE)) {
                    ABUNDANCE_BONUS_XP
                } else {
                    0.0
                }
            previousBonus(player, stat) + abundance
        }
        val previousGained = StatXpModifiers.onGained
        StatXpModifiers.onGained = { player, stat, xp ->
            previousGained(player, stat, xp)
            if (xp > 0 && stat in RelicEffects.nonCombatStats && player.hasRelic(RelicNames.ABUNDANCE)) {
                player.invAdd(player.inv, COINS.asRSCM(RSCMType.OBJ), xp * ABUNDANCE_COINS_PER_XP)
            }
        }
        val previousRunes = InventoryRuneSources.provides
        val natureRune = NATURE_RUNE.asRSCM(RSCMType.OBJ)
        val ledger = TRANSMUTE_ITEM.asRSCM(RSCMType.OBJ)
        InventoryRuneSources.provides = { inv, rune ->
            previousRunes(inv, rune) || (rune == natureRune && inv.any { it?.id == ledger })
        }
    }

    private fun relicSuccessRate(player: Player, stat: String, rate: Double): Double {
        if (!player.isDemonicPactsLeague) {
            return rate
        }
        val guaranteed =
            (stat == THIEVING && player.hasRelic(RelicNames.LARCENIST)) ||
                (stat in HOTFOOT_STATS && player.hasRelic(RelicNames.HOTFOOT)) ||
                (stat == HUNTER && player.hasRelic(RelicNames.WOODSMAN))
        if (guaranteed) {
            return 1.0
        }
        if (stat in GATHERING_STATS && player.hasRelic(RelicNames.BARBARIAN_GATHERING)) {
            return rate + (1.0 - rate) * BARBARIAN_RESCUE_CHANCE
        }
        return rate
    }

    private fun onPrepare(product: SkillingProduct) {
        val player = product.player
        if (!player.isDemonicPactsLeague || product.isBonus || product.skill !in GATHERING_STATS) {
            return
        }
        if (player.hasRelic(RelicNames.ENDLESS_HARVEST)) {
            product.count *= 2
            product.experience *= 2
            product.depletes = false
        }
        if (player.hasRelic(RelicNames.BARBARIAN_GATHERING)) {
            val bonus = product.experience * BARBARIAN_BONUS_XP
            player.statAdvance("stat.strength", bonus)
            player.statAdvance("stat.agility", bonus)
        }
        if (player.hasRelic(RelicNames.HOTFOOT)) {
            hotfoot(player, product)
        }
        if (player.hasRelic(RelicNames.FRIENDLY_FORAGER) && random.of(FORAGER_ODDS) == 0) {
            forageHerb(player)
        }
    }

    private fun hotfoot(player: Player, product: SkillingProduct) {
        when (product.source) {
            is SkillingProductSource.Fishing -> {
                val food = cookedFish[product.item] ?: return
                if (player.statBase(COOKING) < food.level) {
                    return
                }
                product.item = food.cooked.internalName
                player.statAdvance(COOKING, food.xp.toDouble() * product.count)
            }
            is SkillingProductSource.Mining -> {
                val bar = ORE_TO_BAR[product.item] ?: return
                val smithXp = BAR_SMITH_XP[bar] ?: return
                product.item = bar
                player.statAdvance(SMITHING, smithXp * product.count)
            }
            else -> Unit
        }
    }

    private fun forageHerb(player: Player) {
        val herblore = player.statBase("stat.herblore")
        val herbs = grimyHerbs.filter { row -> row.statReq.all { herblore >= it.t1 } }
        if (herbs.isEmpty()) {
            return
        }
        val herb = herbs[random.of(herbs.size)].input.internalName
        if (player.invAdd(player.inv, herb.asRSCM(RSCMType.OBJ), 1).success) {
            player.mes("Your forager's pouch finds a grimy herb.")
            if (RelicEffects.banksResources(player)) {
                RelicEffects.bankFromInventory(player, herb)
            }
        }
    }

    private fun onComplete(player: Player, context: SkillingActionContext) {
        val product = context as? SkillingActionContext.Product ?: return
        if (!player.isDemonicPactsLeague || product.skill !in GATHERING_STATS) {
            return
        }
        if (product.skill == WOODCUTTING && RelicEffects.burnsLogs(player)) {
            val log = burnableLogs[product.item]
            val level = log?.statReq?.firstOrNull()?.t1 ?: Int.MAX_VALUE
            if (log != null && player.statBase(FIREMAKING) >= level) {
                val id = product.item.asRSCM(RSCMType.OBJ)
                if (player.invDel(player.inv, id, product.count).success) {
                    player.statAdvance(FIREMAKING, log.xp.toDouble() * product.count)
                    return
                }
            }
        }
        if (RelicEffects.banksResources(player)) {
            RelicEffects.bankFromInventory(player, product.item)
        }
    }

    private fun onPlayerHitsNpc(npc: Npc, hit: HitBuilder) {
        if (!hit.isFromPlayer || hit.type == HitType.Typeless || hit.damage <= 0) {
            return
        }
        val attacker = hit.sourceUid?.let { PlayerUid(it).resolve(playerList) } ?: return
        if (!attacker.isDemonicPactsLeague) {
            return
        }
        if (attacker.hasRelic(RelicNames.FLASK_OF_FERVOUR)) {
            val ready = attacker.vars[FLASK_READY_VARP]
            if (ready > 0) {
                val reduced = max(0, ready - hit.damage / FLASK_DAMAGE_PER_TICK)
                VarPlayerIntMapSetter.set(attacker, FLASK_READY_VARP, reduced)
            }
        }
        val weapon = attacker.righthand ?: return
        if (weapon.id != EXECUTIONER_ITEM.asRSCM(RSCMType.OBJ)) {
            return
        }
        if (!attacker.hasRelic(RelicNames.EXECUTIONER) || npc.name.equals(YAMA, ignoreCase = true)) {
            return
        }
        val remaining = npc.hitpoints - hit.damage
        if (remaining > 0 && remaining * EXECUTE_DIVISOR <= npc.baseHitpointsLvl) {
            hit.damage = npc.hitpoints
        }
    }

    private suspend fun ProtectedAccess.bankHeist() {
        if (!player.hasRelic(RelicNames.BANK_HEIST)) {
            mes("You need the Bank Heist relic to use this.")
            return
        }
        val banks = bankDestinations(player)
        if (banks.isEmpty()) {
            mes("There are no banks in the areas you have unlocked.")
            return
        }
        val shown = banks.take(MAX_MENU_CHOICES)
        val choice = menu("Bank Heist", hotkeys = false, choices = shown.map { it.first })
        val dest = shown.getOrNull(choice)?.second ?: return
        with(teleports) { cast(TeleportType.Exempt, dest) }
    }

    /** Every banker in an unlocked area, landing on the free tile in front of their booth. */
    private fun bankDestinations(player: Player): List<Pair<String, CoordGrid>> {
        val found = mutableListOf<CoordGrid>()
        for (npc in npcList) {
            if (!npc.isContentType(BANKER_CONTENT)) {
                continue
            }
            val front = frontOfBooth(npc.coords) ?: continue
            if (!LeagueRegions.isAllowed(player, front)) {
                continue
            }
            if (found.none { it.level == front.level && it.chebyshevDistance(front) <= BANK_CLUSTER }) {
                found += front
            }
        }
        val named = found.map { bankName(it) to it }
        return named.sortedBy { it.first }
    }

    private fun frontOfBooth(banker: CoordGrid): CoordGrid? {
        for ((dx, dz) in DIRECTIONS) {
            val booth = banker.translate(dx, dz, 0)
            val front = banker.translate(dx * 2, dz * 2, 0)
            if (!isWalkable(booth) && isWalkable(front)) {
                return front
            }
        }
        return null
    }

    private fun isWalkable(coords: CoordGrid): Boolean =
        collision.isZoneValid(coords) &&
            (collision[coords.x, coords.z, coords.level] and CollisionFlag.BLOCK_WALK) == 0

    private fun bankName(coords: CoordGrid): String {
        val town = TOWNS.minByOrNull { (_, centre) -> centre.chebyshevDistance(coords) }
        if (town != null && town.second.chebyshevDistance(coords) <= TOWN_RADIUS) {
            return town.first
        }
        val region = LeagueRegions.regionAt(coords)?.name ?: "Bank"
        return "$region (${coords.x}, ${coords.z})"
    }

    private suspend fun ProtectedAccess.switchSpellbook() {
        if (!player.hasRelic(RelicNames.GRIMOIRE)) {
            mes("You need the Grimoire relic to use this.")
            return
        }
        val books = Spellbook.entries
        val choice = menu("Choose a spellbook", hotkeys = true, choices = SPELLBOOK_NAMES)
        val book = books.getOrNull(choice) ?: return
        spellbooks.setSpellbook(player, book)
        mes("Your spellbook is now the ${SPELLBOOK_NAMES[choice]}.")
    }

    private fun ProtectedAccess.drinkFlask() {
        if (!player.hasRelic(RelicNames.FLASK_OF_FERVOUR)) {
            mes("You need the Flask of Fervour relic to use this.")
            return
        }
        val ready = player.vars[FLASK_READY_VARP]
        if (ready > mapClock) {
            val seconds = (ready - mapClock) * 6 / 10
            mes("The flask is still recharging. It will be ready in about $seconds seconds.")
            return
        }
        player.statHeal("stat.hitpoints", 0, 100)
        player.statHeal("stat.prayer", 0, 100)
        VarPlayerIntMapSetter.set(player, SPEC_ENERGY_VARP, constants.sa_max_energy)
        VarPlayerIntMapSetter.set(player, FLASK_READY_VARP, mapClock + FLASK_COOLDOWN_TICKS)
        anim(DRINK_ANIM)
        mes("You drink from the Flask of Fervour and feel restored.")
    }

    private fun Player.listRelics() {
        val owned = LeagueRelics.selected(this)
        if (owned.isEmpty()) {
            mes("You have no relics yet. Open Journal > Leagues > Relics to choose one.")
            return
        }
        for (relic in owned) {
            val support = RelicEffects.SUPPORT[relic.name] ?: "Not active on this server yet."
            mes("<col=ef1020>${relic.name}</col>: $support")
        }
    }

    private fun Player.toggleRelicOption(args: List<String>) {
        val option = args.getOrNull(0)?.lowercase()
        val enabled =
            when (args.getOrNull(1)?.lowercase()) {
                "on" -> true
                "off" -> false
                else -> null
            }
        if (enabled == null) {
            mes("Use ::relic bank on|off or ::relic burn on|off.")
            return
        }
        when (option) {
            "bank" -> {
                RelicEffects.setBanking(this, enabled)
                mes("Endless Harvest banking is now ${if (enabled) "on" else "off"}.")
            }
            "burn" -> {
                RelicEffects.setBurning(this, enabled)
                mes("Woodsman log burning is now ${if (enabled) "on" else "off"}.")
            }
            else -> mes("Use ::relic bank on|off or ::relic burn on|off.")
        }
    }

    private fun Player.reclaimRelicItems() {
        var given = 0
        for (relic in LeagueRelics.selected(this)) {
            val item = relic.item ?: continue
            val bank = invMap.getOrPut("inv.bank")
            val held = inv.any { it?.id == item } || worn.any { it?.id == item } || bank.any { it?.id == item }
            if (!held && RelicEffects.giveItem(this, relic)) {
                given++
            }
        }
        mes(if (given > 0) "Your relic items have been returned." else "You already have your relic items.")
    }

    private fun ScriptContext.onRelicItemOp(
        item: String,
        names: Set<String>,
        action: suspend ProtectedAccess.() -> Unit,
    ) {
        val id = runCatching { item.asRSCM(RSCMType.OBJ) }.getOrNull()
        val type = id?.let { ServerCacheManager.getItem(it) }
        if (type == null) {
            logger.warn { "Demonic Pacts League: relic item $item is not in the cache." }
            return
        }
        val ops = type.interfaceOptions
        val index = ops.indexOfFirst { op -> op != null && names.any { it.equals(op, ignoreCase = true) } }
        when (index) {
            0 -> onOpHeld1(item) { action() }
            1 -> onOpHeld2(item) { action() }
            2 -> onOpHeld3(item) { action() }
            3 -> onOpHeld4(item) { action() }
            else -> logger.warn { "Demonic Pacts League: no usable op on $item (ops=$ops)." }
        }
    }

    private companion object {
        const val THIEVING = "stat.thieving"
        const val HUNTER = "stat.hunter"
        const val COOKING = "stat.cooking"
        const val SMITHING = "stat.smithing"
        const val FIREMAKING = "stat.firemaking"
        const val WOODCUTTING = "stat.woodcutting"
        const val COINS = "obj.coins"
        const val NATURE_RUNE = "obj.naturerune"

        const val BANK_HEIST_ITEM = "obj.league_bank_heist_teleport"
        const val GRIMOIRE_ITEM = "obj.league_3_magic_book"
        const val FLASK_ITEM = "obj.league_flask_of_fervour"
        const val TRANSMUTE_ITEM = "obj.league_transmute_book"
        const val EXECUTIONER_ITEM = "obj.league_executioner_weapon"

        const val BANKER_CONTENT = "content.banker"
        const val YAMA = "Yama"
        const val SPEC_ENERGY_VARP = "varp.sa_energy"
        const val FLASK_READY_VARP = "varp.dp_flask_ready"
        const val DRINK_ANIM = "seq.human_eat"

        const val LARCENIST_LOOT = 10
        const val ABUNDANCE_BONUS_XP = 2.0
        const val ABUNDANCE_COINS_PER_XP = 2
        const val BARBARIAN_RESCUE_CHANCE = 0.5
        const val BARBARIAN_BONUS_XP = 1.0 / 9.0
        const val FORAGER_ODDS = 6
        const val EXECUTE_DIVISOR = 5
        const val FLASK_COOLDOWN_TICKS = 300
        const val FLASK_DAMAGE_PER_TICK = 2
        const val MAX_MENU_CHOICES = 127
        const val BANK_CLUSTER = 12
        const val TOWN_RADIUS = 60

        val GATHERING_STATS = setOf("stat.woodcutting", "stat.fishing", "stat.mining")
        val HOTFOOT_STATS = setOf("stat.cooking", "stat.agility")
        val TELEPORT_OPS = setOf("Teleport", "Rub", "Open", "Use", "Activate", "Heist")
        val GRIMOIRE_OPS = setOf("Read", "Open", "Switch", "Use", "Change", "Spellbook")
        val FLASK_OPS = setOf("Drink", "Sip", "Quaff", "Consume", "Use")
        val SPELLBOOK_NAMES =
            listOf("Standard spellbook", "Ancient Magicks", "Lunar spellbook", "Arceuus spellbook")
        val DIRECTIONS = listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)

        val ORE_TO_BAR =
            mapOf(
                "obj.copper_ore" to "obj.bronze_bar",
                "obj.tin_ore" to "obj.bronze_bar",
                "obj.iron_ore" to "obj.iron_bar",
                "obj.silver_ore" to "obj.silver_bar",
                "obj.gold_ore" to "obj.gold_bar",
                "obj.mithril_ore" to "obj.mithril_bar",
                "obj.adamantite_ore" to "obj.adamantite_bar",
                "obj.runite_ore" to "obj.runite_bar",
            )

        val BAR_SMITH_XP =
            mapOf(
                "obj.bronze_bar" to 6.25,
                "obj.iron_bar" to 12.5,
                "obj.silver_bar" to 13.67,
                "obj.gold_bar" to 22.5,
                "obj.mithril_bar" to 30.0,
                "obj.adamantite_bar" to 37.5,
                "obj.runite_bar" to 50.0,
            )

        /** Rough town centres, only used to name bank teleports. */
        val TOWNS =
            listOf(
                "Lumbridge" to CoordGrid(3222, 3218, 0),
                "Varrock" to CoordGrid(3213, 3424, 0),
                "Grand Exchange" to CoordGrid(3164, 3486, 0),
                "Edgeville" to CoordGrid(3093, 3493, 0),
                "Draynor Village" to CoordGrid(3093, 3244, 0),
                "Falador" to CoordGrid(2964, 3378, 0),
                "Al Kharid" to CoordGrid(3293, 3174, 0),
                "Catherby" to CoordGrid(2808, 3434, 0),
                "Seers' Village" to CoordGrid(2725, 3485, 0),
                "East Ardougne" to CoordGrid(2662, 3305, 0),
                "Yanille" to CoordGrid(2606, 3093, 0),
                "Burthorpe" to CoordGrid(2898, 3544, 0),
                "Rellekka" to CoordGrid(2660, 3660, 0),
                "Canifis" to CoordGrid(3506, 3496, 0),
                "Port Phasmatys" to CoordGrid(3687, 3467, 0),
                "Shilo Village" to CoordGrid(2852, 2955, 0),
                "Nardah" to CoordGrid(3428, 2892, 0),
                "Civitas illa Fortis" to CoordGrid(1680, 3120, 0),
                "Hosidius" to CoordGrid(1740, 3600, 0),
                "Port Piscarilius" to CoordGrid(1800, 3780, 0),
                "Arceuus" to CoordGrid(1630, 3750, 0),
                "Lletya" to CoordGrid(2350, 3165, 0),
            )
    }
}
