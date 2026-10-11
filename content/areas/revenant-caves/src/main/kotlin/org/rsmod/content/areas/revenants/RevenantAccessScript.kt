package org.rsmod.content.areas.revenants

import dev.openrune.rscm.RSCM.asRSCM
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.*
import org.rsmod.game.entity.Player
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal var Player.revenantAdmission by intVarp("varp.revenant_caves_admission")

internal class RevenantAccessScript @Inject constructor(private val access: ProtectedAccessLauncher, private val collision: CollisionFlagMap) : PluginScript() {
    override fun ScriptContext.startup() {
        for (route in Entrance.entries) onOpLoc1(route.symbol) { enter(route.destination) }
        onOpLoc1("loc.wild_cave_exit_low") { exit(SOUTH_EXIT) }
        onOpLoc1("loc.wild_cave_exit_high") { exit(NORTH_EXIT) }
        onCommand("testrevenants") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Revenant Caves surface entrance: ::testrevenants [1-3]"
            cheat {
                val route = when (args.firstOrNull()?.toIntOrNull() ?: 1) { 1 -> SURFACE_SOUTH; 2 -> SURFACE_MIDDLE; 3 -> SURFACE_NORTH; else -> null }
                if (route != null) access.launch(player) { telejump(route, TeleportType.Exempt) }
            }
        }
        onCommand("revsitems") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Give revenant ether, a bracelet, weapons, scrolls, chisel and all ancient artefacts"
            cheat { access.launch(player) {
                val result = player.invTransaction(inv) {
                    val bag = select(inv)
                    add(bag, RevenantCharges.ETHER.asRSCM(), 25_000)
                    add(bag, RevenantCharges.EMPTY_BRACELET.asRSCM(), 1)
                    for (kind in RevenantCharges.weapons.take(3)) add(bag, kind.empty.asRSCM(), 1)
                    add(bag, "obj.wild_cave_amulet".asRSCM(), 1)
                    add(bag, "obj.teleportscroll_revenants".asRSCM(), 10)
                    add(bag, "obj.chisel".asRSCM(), 1)
                    for (artefact in AncientArtefact.entries) add(bag, artefact.item.asRSCM(), 1)
                    add(bag, "obj.coins".asRSCM(), 100_000)
                }
                mes(if (result.success) "Revenant test supplies added." else "Make room for the test supplies. No items were changed.")
            } }
        }
    }
    private suspend fun ProtectedAccess.enter(destination: CoordGrid) {
        if (actionDelay > mapClock || player.pendingLogout || player.hitpoints == 0) return
        if (!collision.isZoneValid(destination)) { mes("This entrance is currently unavailable."); return }
        val origin = coords
        if (player.revenantAdmission and 1 == 0) {
            if (invTotal(inv, "obj.coins").toLong() + invTotal(inv("inv.bank"), "obj.coins") < FEE) {
                mes("You need 100,000 coins in your inventory or bank to enter."); return
            }
            val choice = if (player.revenantAdmission and 2 != 0) 1 else choice3("Pay 100,000 coins", 1, "Pay and don't warn again", 2, "Cancel", 0,
                title = "Wilderness! You lose the fee on cave death or Wilderness PvP death.")
            ifCloseChat(); if (choice == 0) return
            if (choice == 2) player.revenantAdmission = player.revenantAdmission or 2
        }
        if (coords != origin || player.pendingLogout || player.hitpoints == 0) return
        actionDelay = mapClock + 3; arriveDelay(); anim("seq.human_reachforladder"); delay(1)
        if (coords != origin || player.pendingLogout || player.hitpoints == 0) { resetAnim(); return }
        if (player.revenantAdmission and 1 == 0 && !pay(player, inv("inv.bank"))) { resetAnim(); return }
        telejump(destination, TeleportType.Exempt); resetAnim(); resetSpotanim()
    }
    private suspend fun ProtectedAccess.exit(destination: CoordGrid) {
        if (actionDelay > mapClock || player.pendingLogout || player.hitpoints == 0) return
        val origin = coords
        actionDelay = mapClock + 3; arriveDelay(); anim("seq.human_reachforladder"); delay(1)
        if (coords == origin && !player.pendingLogout && player.hitpoints > 0) telejump(destination, TeleportType.Exempt)
        if (player.hitpoints > 0) resetAnim()
    }
    internal companion object {
        const val FEE = 100_000
        val SURFACE_SOUTH = CoordGrid(3075, 3653)
        val SURFACE_MIDDLE = CoordGrid(3069, 3740)
        val SURFACE_NORTH = CoordGrid(3127, 3833)
        val SOUTH_EXIT = CoordGrid(3102, 3655)
        val NORTH_EXIT = CoordGrid(3126, 3806)
        fun pay(player: Player, bank: org.rsmod.game.inv.Inventory): Boolean {
            if (player.revenantAdmission and 1 != 0) return true
            val fromInv = minOf(player.inv.physicalCount("obj.coins"), FEE)
            val fromBank = FEE - fromInv
            val result = player.invTransaction(player.inv, bank) {
                if (fromInv > 0) delete(select(player.inv), "obj.coins".asRSCM(), fromInv)
                if (fromBank > 0) delete(select(bank), "obj.coins".asRSCM(), fromBank)
            }
            if (result.success) player.revenantAdmission = player.revenantAdmission or 1
            return result.success
        }
    }
}

internal enum class Entrance(val symbol: String, val destination: CoordGrid) {
    South("loc.wild_cave_entrance_low", CoordGrid(3197, 10056)),
    Middle("loc.wild_cave_entrance_mid", CoordGrid(3193, 10140)),
    North("loc.wild_cave_entrance_high", CoordGrid(3241, 10233)),
}
