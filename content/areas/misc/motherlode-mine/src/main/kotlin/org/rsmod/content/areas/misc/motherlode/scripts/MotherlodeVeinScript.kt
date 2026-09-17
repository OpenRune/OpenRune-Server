package org.rsmod.content.areas.misc.motherlode.scripts

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.areas.misc.motherlode.MotherlodeMine
import org.rsmod.content.areas.misc.motherlode.PayDirtOre
import org.rsmod.content.areas.misc.motherlode.addHeldPayDirt
import org.rsmod.content.areas.misc.motherlode.cleaningPayDirtTotal
import org.rsmod.content.areas.misc.motherlode.sackCapacity
import org.rsmod.content.areas.misc.motherlode.sackTotal
import org.rsmod.content.areas.misc.motherlode.veins.MotherlodeVeins
import org.rsmod.content.skills.mining.configs.MiningParams
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeAnim
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeWallAnim
import org.rsmod.game.MapClock
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MotherlodeVeinScript
@Inject
constructor(
    private val veins: MotherlodeVeins,
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (vein in VEIN_LOCS) {
            onOpLoc1(vein) { attempt(it.loc) }
            onOpLoc3(vein) { mine(it.loc) }
        }
    }

    private fun ProtectedAccess.attempt(vein: BoundLocInfo) {
        if (!canMine(vein)) {
            return
        }
        if (actionDelay < mapClock) {
            if (hasEnoughPayDirtForSack()) {
                mes("You already have enough pay-dirt to fill the sack.")
            }
            actionDelay = mapClock + 3
            skillAnimDelay = mapClock + 3
            opLoc1(vein)
            return
        }
        val pickaxe = Mining.findPickaxe(player) ?: return
        anim(wallAnim(pickaxe))
        spam("You swing your pick at the rock.")
        mine(vein)
    }

    private fun ProtectedAccess.mine(vein: BoundLocInfo) {
        if (!canMine(vein)) {
            resetAnim()
            return
        }
        val pickaxe = Mining.findPickaxe(player) ?: return

        if (skillAnimDelay <= mapClock) {
            skillAnimDelay = mapClock + 4
            anim(wallAnim(pickaxe))
        }

        val delay = Mining.pickaxeActionDelay(pickaxe, random)
        if (actionDelay < mapClock) {
            actionDelay = mapClock + delay
        } else if (actionDelay == mapClock) {
            actionDelay = mapClock + delay
            if (statRandom("stat.mining", SUCCESS_LOW, SUCCESS_HIGH, invisibleLvls)) {
                awardPayDirt(vein)
            }
        }
        opLoc3(vein)
    }

    private fun ProtectedAccess.canMine(vein: BoundLocInfo): Boolean {
        if (!veins.isActive(vein.coords)) {
            return false
        }
        if (player.miningLvl < MotherlodeMine.PAYDIRT_MINING_LEVEL) {
            mes("You need a Mining level of ${MotherlodeMine.PAYDIRT_MINING_LEVEL} to mine this vein.")
            return false
        }
        if (Mining.findPickaxe(player) == null) {
            mes("You need a pickaxe to mine this rock.")
            return false
        }
        if (player.sackTotal >= player.sackCapacity) {
            mes("The sack is full. You should collect your ore before mining any more pay-dirt.")
            return false
        }
        if (inv.isFull()) {
            mes("Your inventory is too full to hold any more pay-dirt.")
            soundSynth("synth.pillory_wrong")
            return false
        }
        return true
    }

    private fun ProtectedAccess.hasEnoughPayDirtForSack(): Boolean {
        val total = player.sackTotal + player.cleaningPayDirtTotal + inv.count(MotherlodeMine.PAYDIRT)
        return total >= player.sackCapacity
    }

    private fun ProtectedAccess.awardPayDirt(vein: BoundLocInfo) {
        if (invAdd(inv, MotherlodeMine.PAYDIRT).failure) {
            return
        }
        player.addHeldPayDirt(PayDirtOre.roll(player.miningLvl, random))

        statAdvance("stat.mining", PAYDIRT_XP * xpMods.get(player, "stat.mining"))
        spam("You manage to mine some pay-dirt.")
        soundSynth(PAYDIRT_SOUND)
        veins.onPayDirtMined(vein.coords)
    }

    private fun wallAnim(pickaxe: InvObj): String {
        val type = getInvObj(pickaxe)
        val seq =
            if (type.hasParam(MiningParams.skill_wall_anim)) {
                type.pickaxeWallAnim
            } else {
                type.pickaxeAnim
            }
        return RSCM.getReverseMapping(RSCMType.SEQ, seq.id)
    }

    private companion object {
        const val SUCCESS_LOW = 60
        const val SUCCESS_HIGH = 105
        const val PAYDIRT_XP = 60.0
        const val PAYDIRT_SOUND = 3600

        val VEIN_LOCS =
            listOf(
                "loc.motherlode_ore_single",
                "loc.motherlode_ore_left",
                "loc.motherlode_ore_middle",
                "loc.motherlode_ore_right",
            )
    }
}
