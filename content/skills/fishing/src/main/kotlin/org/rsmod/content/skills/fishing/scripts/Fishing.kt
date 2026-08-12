package org.rsmod.content.skills.fishing.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.skilling.SkillingAwardResult
import org.rsmod.api.player.skilling.awardSkillingProduct
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.script.onOpContentNpc1
import org.rsmod.api.script.onOpContentNpcU
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.fishing.configs.FishCatch
import org.rsmod.content.skills.fishing.configs.FishingRates
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * MVP Small Net fishing (Draynor `npc.0_48_50_saltfish` and any NPC tagged
 * `content.fishing_spot`).
 *
 * Spots do not deplete. Bait/lure and other tools are out of scope.
 */
class Fishing
@Inject
constructor(
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentNpc1("content.fishing_spot") { smallNet(it.npc) }
        onOpContentNpcU("content.fishing_spot", FishingRates.NET_OBJ) { smallNet(it.npc) }
    }

    private fun ProtectedAccess.smallNet(npc: Npc) {
        if (!hasNet()) {
            mes("You need a small fishing net to fish here.")
            return
        }

        if (!FishingRates.canFish(player.fishingLvl)) {
            mes("You need a Fishing level of ${FishingRates.shrimp.level} to fish here.")
            return
        }

        if (inv.isFull()) {
            mes("Your inventory is too full to hold any more fish.")
            soundSynth("synth.pillory_wrong")
            resetAnim()
            return
        }

        // Cold start: arm delays, play cast anim/spam once, then re-enter when ready.
        if (actionDelay < mapClock) {
            val coldStart = skillAnimDelay < mapClock
            actionDelay = mapClock + 3
            skillAnimDelay = mapClock + 3
            if (coldStart) {
                anim(FishingRates.NET_ANIM)
                spam("You cast out your net...")
            }
            opNpc1(npc)
            return
        }

        if (skillAnimDelay <= mapClock) {
            skillAnimDelay = mapClock + 4
            anim(FishingRates.NET_ANIM)
        }

        var caught: FishCatch? = null
        if (actionDelay == mapClock) {
            actionDelay = mapClock + FishingRates.ACTION_DELAY
            val (low, high) = FishingRates.spotSuccessRates()
            if (statRandom("stat.fishing", low, high, invisibleLvls)) {
                val wantAnchovies = player.fishingLvl >= FishingRates.anchovies.level && random.randomBoolean()
                caught = FishingRates.resolveCatch(player.fishingLvl, wantAnchovies)
            }
        }

        if (caught != null) {
            val xp = caught.xp * xpMods.get(player, "stat.fishing")
            val product =
                SkillingProduct(
                    player = player,
                    skill = "stat.fishing",
                    item = caught.item,
                    count = 1,
                    experience = xp,
                    grantsExperience = true,
                    source = SkillingProductSource.Fishing(npc, caught.item),
                    depletes = false,
                )
            when (awardSkillingProduct(product)) {
                SkillingAwardResult.InventoryFull -> {
                    mes("Your inventory is too full to hold any more fish.")
                    soundSynth("synth.pillory_wrong")
                    resetAnim()
                    return
                }
                SkillingAwardResult.Cancelled -> Unit
                SkillingAwardResult.Success -> {
                    val name = getInvObj(InvObj(caught.item)).name.lowercase()
                    spam("You catch some $name.")
                }
            }
        }

        if (inv.isFull()) {
            mes("Your inventory is too full to hold any more fish.")
            soundSynth("synth.pillory_wrong")
            resetAnim()
            return
        }

        opNpc1(npc)
    }

    private fun ProtectedAccess.hasNet(): Boolean =
        FishingRates.NET_OBJ in inv || "content.fishing_net" in inv
}
