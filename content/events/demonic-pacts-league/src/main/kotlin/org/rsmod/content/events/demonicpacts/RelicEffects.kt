package org.rsmod.content.events.demonicpacts

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdd
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.interfaces.bank.bankAppend
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

object RelicNames {
    const val ENDLESS_HARVEST = "Endless Harvest"
    const val BARBARIAN_GATHERING = "Barbarian Gathering"
    const val ABUNDANCE = "Abundance"
    const val HOTFOOT = "Hotfoot"
    const val FRIENDLY_FORAGER = "Friendly Forager"
    const val WOODSMAN = "Woodsman"
    const val BANK_HEIST = "Bank Heist"
    const val EVIL_EYE = "Evil Eye"
    const val MAP_OF_ALACRITY = "Map of Alacrity"
    const val CONNIVING_CLUES = "Conniving Clues"
    const val BUTLERS_BELL = "Butler's Bell"
    const val NATURES_ACCORD = "Nature's Accord"
    const val SOUL_HARVEST = "Soul Harvest"
    const val CULLING_SPREE = "Culling Spree"
    const val FLOW_STATE = "Flow State"
    const val MINION = "Minion"
    const val TRANSMUTE = "Transmute"
    const val LARCENIST = "Larcenist"
    const val GRIMOIRE = "Grimoire"
    const val ETERNAL_SUSTENANCE = "Eternal Sustenance"
    const val RELOADED = "Reloaded"
    const val EXECUTIONER = "Executioner"
    const val FLASK_OF_FERVOUR = "Flask of Fervour"
}

fun Player.hasRelic(name: String): Boolean = isDemonicPactsLeague && LeagueRelics.owns(this, name)

/** Relic behaviour that the league tick, movement and the relic interface call into. */
object RelicEffects {
    /** What each relic does on this server, shown when it is unlocked and by `::relics`. */
    val SUPPORT: Map<String, String> =
        mapOf(
            RelicNames.ENDLESS_HARVEST to
                "Double logs, fish and ore and their XP; trees and rocks don't deplete; resources " +
                    "go to your bank (::relic bank off to stop).",
            RelicNames.BARBARIAN_GATHERING to
                "Chop and mine bare-handed (as crystal tools), failed attempts succeed half the time, " +
                    "and gathering gives 10% bonus Strength and Agility XP. Fishing still needs tools.",
            RelicNames.ABUNDANCE to
                "Non-combat skills +10, 2 bonus XP per drop in them and 2 coins per XP gained in them.",
            RelicNames.HOTFOOT to
                "Agility and Cooking always succeed. With the searing boots on, fish are cooked and ore " +
                    "smelted as you gather, and running gives Agility XP.",
            RelicNames.FRIENDLY_FORAGER to
                "With the pouch carried, gathering finds grimy herbs (Herblore +25). Potions get 4 doses " +
                    "and secondaries are saved 90% of the time.",
            RelicNames.WOODSMAN to
                "Logs you cut are burned for Firemaking XP (::relic burn off to keep them).",
            RelicNames.BANK_HEIST to "The briefcase teleports to banks, deposit boxes and bank chests in unlocked areas.",
            RelicNames.EVIL_EYE to "The necklace teleports to boss and raid entrances in unlocked areas.",
            RelicNames.MAP_OF_ALACRITY to "The map teleports to agility shortcuts in unlocked areas.",
            RelicNames.TRANSMUTE to "The ledger is an unlimited source of nature runes.",
            RelicNames.BUTLERS_BELL to
                "Ring the bell to give the demon butler a job; it gathers or processes items even while " +
                    "you're logged out. Ring again to collect.",
            RelicNames.NATURES_ACCORD to
                "No Farming level requirements, 10x noted harvests with matching XP, and the fairy " +
                    "mushroom teleports to fairy rings and tool leprechauns.",
            RelicNames.LARCENIST to "Thieving always succeeds and gives 10x loot, noted.",
            RelicNames.SOUL_HARVEST to
                "Bones and ashes that monsters drop become soul shards. Shards work as pure essence, or " +
                    "release them from the shard item for Prayer XP.",
            RelicNames.GRIMOIRE to "The grimoire switches spellbooks.",
            RelicNames.ETERNAL_SUSTENANCE to "Eating food doesn't use it up.",
            RelicNames.CULLING_SPREE to "Slayer masters let you choose from three tasks and pick 5-200 kills.",
            RelicNames.FLOW_STATE to
                "Woodcutting, Mining, Fishing, Cooking, smelting, smithing and potion making repeat every " +
                    "2 ticks.",
            RelicNames.RELOADED to "Open the relics window and pick one more relic from a lower tier.",
            RelicNames.EXECUTIONER to
                "While wielding the Sage's axe, a hit that leaves a monster under 20% hitpoints finishes " +
                    "it, except Yama.",
            RelicNames.MINION to
                "Use the whistle to summon a minion for 30 minutes: it hits your target for 3-10 every " +
                    "3 ticks and loots your kills into your inventory.",
            RelicNames.FLASK_OF_FERVOUR to
                "The flask restores Hitpoints, Prayer and special attack, explodes three times around " +
                    "you and blocks all damage for 2.4 seconds. 3-minute recharge, shortened by big hits.",
        )

    private val NON_COMBAT_STATS =
        listOf(
            "stat.cooking",
            "stat.woodcutting",
            "stat.fletching",
            "stat.fishing",
            "stat.firemaking",
            "stat.crafting",
            "stat.smithing",
            "stat.mining",
            "stat.herblore",
            "stat.agility",
            "stat.thieving",
            "stat.slayer",
            "stat.farming",
            "stat.runecrafting",
            "stat.hunter",
            "stat.construction",
        )

    val nonCombatStats: Set<String> = NON_COMBAT_STATS.toSet()

    private const val ABUNDANCE_BOOST = 10
    private const val HOTFOOT_RUN_XP = 0.5
    private const val FLAGS_VARP = "varp.dp_relic_flags"
    private const val FLAG_NO_BANKING = 1
    private const val FLAG_NO_BURNING = 2

    fun onUnlocked(player: Player, relic: Relic) {
        val support = SUPPORT[relic.name]
        if (support != null) {
            player.mes(support)
        } else {
            player.mes("This relic's effect isn't active on this server yet.")
        }
        giveItem(player, relic)
    }

    fun giveItem(player: Player, relic: Relic): Boolean {
        val item = relic.item ?: return false
        if (player.invAdd(player.inv, item, 1).success) {
            return true
        }
        player.mes("You have no room for the ${relic.name} item. Use ::relicitems when you do.")
        return false
    }

    fun onLeagueTick(player: Player) {
        if (player.hasRelic(RelicNames.ABUNDANCE)) {
            for (stat in NON_COMBAT_STATS) {
                val target = player.statBase(stat) + ABUNDANCE_BOOST
                val current = player.stat(stat)
                if (current < target) {
                    player.statAdd(stat, target - current, 0)
                }
            }
        }
    }

    fun onMoved(player: Player, from: CoordGrid) {
        if (!player.hasRelic(RelicNames.HOTFOOT) || !wearsRelicItem(player, RelicNames.HOTFOOT)) {
            return
        }
        val tiles = player.coords.chebyshevDistance(from)
        if (tiles == 2 && player.coords.level == from.level) {
            player.statAdvance("stat.agility", HOTFOOT_RUN_XP * tiles)
        }
    }

    /** Whether the player is wearing the item [relic] gave them. */
    fun wearsRelicItem(player: Player, relic: String): Boolean {
        val item = LeagueRelics.byName[relic.lowercase()]?.item ?: return true
        return player.worn.any { it?.id == item }
    }

    /** Whether the player is wearing or carrying the item [relic] gave them. */
    fun carriesRelicItem(player: Player, relic: String): Boolean {
        val item = LeagueRelics.byName[relic.lowercase()]?.item ?: return true
        return player.worn.any { it?.id == item } || player.inv.any { it?.id == item }
    }

    fun banksResources(player: Player): Boolean =
        player.hasRelic(RelicNames.ENDLESS_HARVEST) &&
            (player.vars[FLAGS_VARP] and FLAG_NO_BANKING) == 0

    fun burnsLogs(player: Player): Boolean =
        player.hasRelic(RelicNames.WOODSMAN) && (player.vars[FLAGS_VARP] and FLAG_NO_BURNING) == 0

    fun setBanking(player: Player, enabled: Boolean) = setFlag(player, FLAG_NO_BANKING, !enabled)

    fun setBurning(player: Player, enabled: Boolean) = setFlag(player, FLAG_NO_BURNING, !enabled)

    private fun setFlag(player: Player, flag: Int, set: Boolean) {
        val current = player.vars[FLAGS_VARP]
        val updated = if (set) current or flag else current and flag.inv()
        VarPlayerIntMapSetter.set(player, FLAGS_VARP, updated)
    }

    /** Moves the stack of [item] that was just added to the inventory into the bank. */
    fun bankFromInventory(player: Player, item: String): Boolean {
        val id = item.asRSCM(RSCMType.OBJ)
        val slot = (player.inv.size - 1 downTo 0).firstOrNull { player.inv[it]?.id == id }
        return slot != null && player.bankAppend(player.inv, slot)
    }
}
