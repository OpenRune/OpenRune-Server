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
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductPrepareEvent
import org.rsmod.api.player.events.skilling.SkillingProductSource
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
import org.rsmod.api.table.FiremakingLogsRow
import org.rsmod.api.table.cooking.CookingFoodsRow
import org.rsmod.api.table.herblore.HerbloreCleaningRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RelicEffectsScript
@Inject
constructor(
    private val random: GameRandom,
    private val playerList: PlayerList,
    private val npcList: NpcList,
    private val npcHitModifier: NpcHitModifier,
    private val spellbooks: MagicSpellbookManager,
) : PluginScript() {
    private val logger = InlineLogger()

    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    private val cookedFish: Map<String, CookingFoodsRow> by lazy {
        CookingFoodsRow.all().associateBy { it.input.internalName }
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
        onEvent<PlayerHitEvents.Modify> {
            if (player.vars[FLASK_SHIELD_VARP] > player.currentMapClock && player.hasRelic(RelicNames.FLASK_OF_FERVOUR)) {
                hit.damage = 0
            }
        }

        onRelicItemOp(GRIMOIRE_ITEM, GRIMOIRE_OPS) { switchSpellbook() }
        onRelicItemOp(FLASK_ITEM, FLASK_OPS) { drinkFlask() }
        onRelicItemOp(SOUL_SHARD_ITEM, SOUL_SHARD_OPS) { sacrificeShards() }

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
        val previousLevels = SkillModifiers.ignoresLevels
        SkillModifiers.ignoresLevels = { player, stat ->
            previousLevels(player, stat) || (stat == FARMING && player.hasRelic(RelicNames.NATURES_ACCORD))
        }
        val previousYield = SkillModifiers.yieldMultiplier
        SkillModifiers.yieldMultiplier = { player, stat ->
            val base = previousYield(player, stat)
            if (stat == FARMING && player.hasRelic(RelicNames.NATURES_ACCORD)) base * NATURES_ACCORD_YIELD else base
        }
        val previousSlayer = SkillModifiers.choosesSlayerTasks
        SkillModifiers.choosesSlayerTasks = { player ->
            previousSlayer(player) || player.hasRelic(RelicNames.CULLING_SPREE)
        }
        val previousTicks = SkillModifiers.actionTicks
        SkillModifiers.actionTicks = { player, stat, ticks ->
            val base = previousTicks(player, stat, ticks)
            if (player.hasRelic(RelicNames.FLOW_STATE)) minOf(base, FLOW_STATE_TICKS) else base
        }
        val previousHands = SkillModifiers.bareHanded
        SkillModifiers.bareHanded = { player, stat ->
            previousHands(player, stat) ||
                (stat in BARE_HAND_STATS && player.hasRelic(RelicNames.BARBARIAN_GATHERING))
        }
        val previousSecondaries = SkillModifiers.secondarySaveChance
        SkillModifiers.secondarySaveChance = { player ->
            val forager = if (player.hasRelic(RelicNames.FRIENDLY_FORAGER)) FORAGER_SECONDARY_SAVE else 0
            previousSecondaries(player) + forager
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
        if (player.hasRelic(RelicNames.HOTFOOT) && RelicEffects.wearsRelicItem(player, RelicNames.HOTFOOT)) {
            hotfoot(player, product)
        }
        if (
            player.hasRelic(RelicNames.FRIENDLY_FORAGER) &&
                RelicEffects.carriesRelicItem(player, RelicNames.FRIENDLY_FORAGER) &&
                random.of(FORAGER_ODDS) == 0
        ) {
            forageHerb(player)
        }
    }

    private fun hotfoot(player: Player, product: SkillingProduct) {
        when (product.source) {
            is SkillingProductSource.Fishing -> {
                val food = cookedFish[product.item] ?: return
                val level = food.statReq.firstOrNull()?.t1 ?: return
                if (player.statBase(COOKING) < level) {
                    return
                }
                product.item = food.output.internalName
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
        val herblore = player.statBase("stat.herblore") + FORAGER_LEVEL_BONUS
        val herbs = grimyHerbs.filter { row -> row.statReq.all { herblore >= it.t1 } }
        if (herbs.isEmpty()) {
            return
        }
        val row = herbs[random.of(herbs.size)]
        val herb = row.input.internalName
        if (player.invAdd(player.inv, herb.asRSCM(RSCMType.OBJ), 1).success) {
            player.statAdvance("stat.herblore", row.xp / 10.0)
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
        VarPlayerIntMapSetter.set(player, FLASK_SHIELD_VARP, mapClock + FLASK_SHIELD_TICKS)
        flaskExplosions(player)
        anim(DRINK_ANIM)
        mes("You drink from the Flask of Fervour and feel restored.")
    }

    private fun ProtectedAccess.sacrificeShards() {
        val id = SOUL_SHARD_ITEM.asRSCM(RSCMType.OBJ)
        val count = inv.count(SOUL_SHARD_ITEM)
        if (count <= 0 || player.invDel(player.inv, id, count).failure) {
            return
        }
        anim(BURY_ANIM)
        player.statAdvance("stat.prayer", SOUL_SHARD_XP * count)
        mes("You release the souls of $count shards.")
    }

    private fun flaskExplosions(player: Player) {
        val damage = player.statBase("stat.prayer") * FLASK_PRAYER_PERCENT / 100
        if (damage <= 0) {
            return
        }
        val targets =
            npcList.filter { npc ->
                npc.hitpoints > 0 &&
                    npc.coords.level == player.coords.level &&
                    npc.coords.chebyshevDistance(player.coords) <= FLASK_RADIUS &&
                    !npc.name.equals(YAMA, ignoreCase = true)
            }
        for (npc in targets) {
            for (explosion in 1..FLASK_EXPLOSIONS) {
                npc.queueHit(player, explosion, HitType.Typeless, damage, npcHitModifier)
            }
        }
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
        const val FARMING = "stat.farming"
        const val WOODCUTTING = "stat.woodcutting"
        const val COINS = "obj.coins"
        const val NATURE_RUNE = "obj.naturerune"

        const val GRIMOIRE_ITEM = "obj.league_3_magic_book"
        const val FLASK_ITEM = "obj.league_flask_of_fervour"
        const val TRANSMUTE_ITEM = "obj.league_transmute_book"
        const val EXECUTIONER_ITEM = "obj.league_executioner_weapon"
        const val SOUL_SHARD_ITEM = "obj.league_soul_shard"
        const val BURY_ANIM = "seq.human_pickupfloor"

        const val YAMA = "Yama"
        const val SPEC_ENERGY_VARP = "varp.sa_energy"
        const val FLASK_READY_VARP = "varp.dp_flask_ready"
        const val FLASK_SHIELD_VARP = "varp.dp_flask_shield"
        const val DRINK_ANIM = "seq.human_eat"

        const val LARCENIST_LOOT = 10
        const val ABUNDANCE_BONUS_XP = 2.0
        const val ABUNDANCE_COINS_PER_XP = 2
        const val BARBARIAN_RESCUE_CHANCE = 0.5
        const val BARBARIAN_BONUS_XP = 0.1
        const val FORAGER_ODDS = 6
        const val EXECUTE_DIVISOR = 5
        const val FLASK_COOLDOWN_TICKS = 300
        const val FLASK_DAMAGE_PER_TICK = 10
        const val FLASK_SHIELD_TICKS = 4
        const val FLASK_EXPLOSIONS = 3
        const val FLASK_RADIUS = 3
        const val FLASK_PRAYER_PERCENT = 60
        const val FORAGER_LEVEL_BONUS = 25
        const val FORAGER_SECONDARY_SAVE = 90
        const val NATURES_ACCORD_YIELD = 10
        const val FLOW_STATE_TICKS = 2

        /** 100 shards give 550 Prayer xp at the Libation bowl. */
        const val SOUL_SHARD_XP = 5.5

        val GATHERING_STATS = setOf("stat.woodcutting", "stat.fishing", "stat.mining")
        val HOTFOOT_STATS = setOf("stat.cooking", "stat.agility")
        val BARE_HAND_STATS = setOf("stat.woodcutting", "stat.mining")
        val GRIMOIRE_OPS = setOf("Change Spellbook", "Read", "Open", "Switch", "Use", "Change", "Spellbook")
        val SOUL_SHARD_OPS =
            setOf("Sacrifice", "Offer", "Release", "Absorb", "Consume", "Crush", "Break", "Bury", "Scatter")
        val FLASK_OPS = setOf("Activate", "Drink", "Sip", "Quaff", "Consume", "Use")
        val SPELLBOOK_NAMES =
            listOf("Standard spellbook", "Ancient Magicks", "Lunar spellbook", "Arceuus spellbook")

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
    }
}
