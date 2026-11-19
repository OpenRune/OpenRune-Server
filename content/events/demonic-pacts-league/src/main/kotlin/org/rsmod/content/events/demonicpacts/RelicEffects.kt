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
                "Woodcutting, Fishing and Mining give double resources and XP, trees and rocks " +
                    "don't deplete, and resources go to your bank (::relic bank off to stop).",
            RelicNames.BARBARIAN_GATHERING to
                "Failed Woodcutting, Fishing and Mining attempts succeed half the time, and " +
                    "gathering gives bonus Strength and Agility XP. You still need tools.",
            RelicNames.ABUNDANCE to
                "Non-combat skills are boosted by 10, each XP drop in them gives 2 bonus XP, " +
                    "and you get 2 coins per XP gained in them.",
            RelicNames.HOTFOOT to
                "Fish you catch are cooked and ore you mine is smelted when you have the levels, " +
                    "Cooking never burns, Agility always succeeds and running gives Agility XP.",
            RelicNames.FRIENDLY_FORAGER to
                "Gathering sometimes finds a grimy herb you can clean.",
            RelicNames.WOODSMAN to
                "Logs you cut are burned for Firemaking XP (::relic burn off to keep them), " +
                    "and Hunter always succeeds.",
            RelicNames.BANK_HEIST to
                "Your banker's briefcase teleports you to banks in areas you have unlocked.",
            RelicNames.TRANSMUTE to "Carrying the ledger gives you unlimited nature runes.",
            RelicNames.LARCENIST to
                "Thieving always succeeds and gives 10x loot, noted where possible.",
            RelicNames.GRIMOIRE to "Your grimoire switches spellbooks.",
            RelicNames.ETERNAL_SUSTENANCE to "Eating food doesn't use it up.",
            RelicNames.RELOADED to "Open the relics window and pick one more relic from a lower tier.",
            RelicNames.EXECUTIONER to
                "While wielding the Sage's axe, a hit that leaves a monster under 20% hitpoints " +
                    "finishes it, except Yama.",
            RelicNames.FLASK_OF_FERVOUR to
                "Drinking the flask restores your Hitpoints, Prayer and special attack. It " +
                    "recharges in 3 minutes, faster as you deal damage.",
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
        if (!player.hasRelic(RelicNames.HOTFOOT)) {
            return
        }
        val tiles = player.coords.chebyshevDistance(from)
        if (tiles == 2 && player.coords.level == from.level) {
            player.statAdvance("stat.agility", HOTFOOT_RUN_XP * tiles)
        }
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
