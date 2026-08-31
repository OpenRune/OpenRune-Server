package org.rsmod.content.skills.fishing.scripts

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.skilling.SkillingAwardResult
import org.rsmod.api.player.skilling.awardSkillingProduct
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.script.onOpContentNpc1
import org.rsmod.api.script.onOpContentNpcU
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.fishing.HeronPet.rollHeron
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class KarambwanFishing
@Inject
constructor(
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
) : PluginScript() {

    private val karambwanType: ItemServerType by lazy {
        ItemServerType(RAW_KARAMBWAN.asRSCM(RSCMType.OBJ))
    }

    override fun ScriptContext.startup() {
        onOpHeldU(KARAMBWANJI, EMPTY_VESSEL) { loadVessel() }
        onOpContentNpcU("content.karambwan_fishing_spot", LOADED_VESSEL) { opNpc1(it.npc) }
        onOpContentNpc1("content.karambwan_fishing_spot") { fish(it.npc) }
    }

    private fun ProtectedAccess.loadVessel() {
        invReplace(inv, EMPTY_VESSEL, 1, LOADED_VESSEL)
        invDel(inv, KARAMBWANJI, 1)
        mes("You fill the karambwan vessel with karambwanji.")
    }

    private fun ProtectedAccess.fish(spot: Npc) {
        if (player.fishingLvl < 65) {
            mes("You need a Fishing level of 65 to fish here.")
            return
        }

        if (invTotal(inv, LOADED_VESSEL) == 0 &&
            invTotal(inv, EMPTY_VESSEL) > 0 &&
            invTotal(inv, KARAMBWANJI) > 0
        ) {
            invReplace(inv, EMPTY_VESSEL, 1, LOADED_VESSEL)
            invDel(inv, KARAMBWANJI, 1)
        }

        if (invTotal(inv, LOADED_VESSEL) == 0) {
            mes("You need a karambwan vessel filled with karambwanji to fish here.")
            return
        }

        if (inv.isFull()) {
            mes("Your inventory is too full to hold any more fish.")
            soundSynth("synth.pillory_wrong")
            return
        }

        if (skillAnimDelay <= mapClock) {
            skillAnimDelay = mapClock + 4
            anim("seq.human_fish_onspot")
        }

        var caught = false
        if (actionDelay < mapClock) {
            actionDelay = mapClock + 3
        } else if (actionDelay == mapClock) {
            caught = statRandom("stat.fishing", 5, 160, invisibleLvls)
        }

        if (caught) {
            invReplace(inv, LOADED_VESSEL, 1, EMPTY_VESSEL)
            val xp = 50.0 * xpMods.get(player, "stat.fishing")
            val product =
                SkillingProduct(
                    player = player,
                    skill = "stat.fishing",
                    item = RAW_KARAMBWAN,
                    count = 1,
                    experience = xp,
                    grantsExperience = true,
                    source = SkillingProductSource.Fishing(karambwanType),
                )
            rollHeron(RAW_KARAMBWAN)
            if (awardSkillingProduct(product) == SkillingAwardResult.Success) {
                spam("You catch a karambwan!")
            }
        }

        opNpc1(spot)
    }

    private companion object {
        const val KARAMBWANJI = "obj.tbwt_raw_karambwanji"
        const val EMPTY_VESSEL = "obj.tbwt_karambwan_vessel"
        const val LOADED_VESSEL = "obj.tbwt_karambwan_vessel_loaded_with_karambwanji"
        const val RAW_KARAMBWAN = "obj.tbwt_raw_karambwan"
    }
}
