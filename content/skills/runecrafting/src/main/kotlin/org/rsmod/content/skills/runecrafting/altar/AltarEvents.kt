package org.rsmod.content.skills.runecrafting.altar

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.advanced.onWearposChange
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.runecrafting.RunecraftingAltarsRow
import org.rsmod.content.skills.runecrafting.action.RunecraftAction.craftOurania
import org.rsmod.content.skills.runecrafting.action.RunecraftAction.craftRune
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AltarEvents @Inject constructor(
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        RunecraftingAltarsRow.all().forEach { altar ->
            registerRuins(altar)
            registerAltar(altar)
            registerExitPortal(altar)
            registerTiaraVarbits(altar)
            registerTalismanLocate(altar)
        }

        registerOuraniaAltar()
        registerElementalCatalyticAccess()
    }

    private fun ScriptContext.registerOuraniaAltar() {
        onOpLoc1("loc.rc_zmi_dungeon_cracked_center_altar") {
            craftOurania(xpMods)
        }
    }

    private fun ScriptContext.registerElementalCatalyticAccess() {
        val elementalEntrances =
            RunecraftingAltarsRow.all()
                .filter { it.altarObject.internalName in elementalAltarLocs }
                .mapNotNull { row -> row.entrance?.let { row.altarObject.internalName to it } }
                .toMap()

        val catalyticEntrances =
            RunecraftingAltarsRow.all()
                .filter { it.altarObject.internalName in catalyticAltarLocs }
                .mapNotNull { row -> row.entrance?.let { row.altarObject.internalName to it } }
                .toMap()

        listOf(
            "loc.airtemple_ruined_old",
            "loc.airtemple_ruined_new",
            "loc.watertemple_ruined_old",
            "loc.watertemple_ruined_new",
            "loc.earthtemple_ruined_old",
            "loc.earthtemple_ruined_new",
            "loc.firetemple_ruined_old",
            "loc.firetemple_ruined_new"
        ).forEach { ruin ->
            onOpLocU(ruin, "obj.elemental_talisman") {
                val entrance = nearestEntrance(elementalEntrances) ?: return@onOpLocU
                telejump(entrance)
            }
        }

        listOf(
            "loc.mindtemple_ruined_old",
            "loc.mindtemple_ruined_new",
            "loc.bodytemple_ruined_old",
            "loc.bodytemple_ruined_new",
            "loc.cosmictemple_ruined_old",
            "loc.cosmictemple_ruined_new",
            "loc.chaostemple_ruined_old",
            "loc.chaostemple_ruined_new",
            "loc.naturetemple_ruined_old",
            "loc.naturetemple_ruined_new",
            "loc.lawtemple_ruined_old",
            "loc.lawtemple_ruined_new",
            "loc.deathtemple_ruined_old",
            "loc.deathtemple_ruined_new",
            "loc.wrathtemple_ruined_0op",
            "loc.wrathtemple_ruined_1op"
        ).forEach { ruin ->
            onOpLocU(ruin, "obj.catalytic_talisman") {
                val entrance = nearestEntrance(catalyticEntrances) ?: return@onOpLocU
                telejump(entrance)
            }
        }

        onOpHeld4("obj.elemental_talisman") {
            val entrance = nearestEntrance(elementalEntrances) ?: return@onOpHeld4
            locateAltar(entrance)
        }

        onOpHeld4("obj.catalytic_talisman") {
            val entrance = nearestEntrance(catalyticEntrances) ?: return@onOpHeld4
            locateAltar(entrance)
        }
    }

    private fun ScriptContext.registerRuins(altar: RunecraftingAltarsRow) {
        val entrance = altar.entrance ?: return
        for (ruin in altar.ruins) {
            onOpLoc1(ruin.internalName) {
                if (canEnterRuinsWithoutTalisman(altar)) {
                    telejump(entrance)
                    return@onOpLoc1
                }
                mes("You need a talisman to enter these ruins.")
            }

            altar.talisman?.let { talisman ->
                onOpLocU(ruin.internalName, talisman.internalName) {
                    telejump(entrance)
                }
            }

            altar.tiara?.let { tiaraDef ->
                onOpLocU(ruin.internalName, tiaraDef.item.internalName) {
                    telejump(entrance)
                }
            }
        }
    }

    private fun ScriptContext.registerAltar(altar: RunecraftingAltarsRow) {
        if (altar.altarObject.internalName == "loc.rc_zmi_dungeon_cracked_center_altar") {
            return
        }

        onOpLoc1(altar.altarObject.internalName) {
            craftRune(altar.rune, xpMods)
        }
    }

    private fun ScriptContext.registerExitPortal(altar: RunecraftingAltarsRow) {
        val exitPortal = altar.exitPortal ?: return
        val exit = altar.exit ?: return
        onOpLoc1(exitPortal.internalName) {
            telejump(exit)
        }
    }

    private fun ScriptContext.registerTiaraVarbits(altar: RunecraftingAltarsRow) {
        val varbitId = altar.varbit ?: return
        val tiaraDef = altar.tiara ?: return
        val tiaraItem = tiaraDef.item.internalName
        val varbitName = RSCM.getReverseMapping(RSCMType.VARBIT, varbitId)

        onWearposChange {
            if (wearpos == Wearpos.Hat) {
                val equipped = tiaraItem in player.worn
                VarPlayerIntMapSetter.set(player, varbitName, if (equipped) 1 else 0)
            }
        }

        onPlayerLogin {
            val equipped = tiaraItem in player.worn
            VarPlayerIntMapSetter.set(player, varbitName, if (equipped) 1 else 0)
        }
    }

    private fun ScriptContext.registerTalismanLocate(altar: RunecraftingAltarsRow) {
        val entrance = altar.exit ?: return
        val talisman = altar.talisman ?: return
        onOpHeld4(talisman.internalName) {
            locateAltar(entrance)
        }
    }

    private fun ProtectedAccess.locateAltar(altarCoords: CoordGrid) {
        val dx = altarCoords.x - player.coords.x
        val dz = altarCoords.z - player.coords.z

        val direction = when {
            dx > 0 && dz > 0 -> "north-east"
            dx < 0 && dz > 0 -> "north-west"
            dx > 0 && dz < 0 -> "south-east"
            dx < 0 && dz < 0 -> "south-west"
            dx == 0 && dz > 0 -> "north"
            dx == 0 && dz < 0 -> "south"
            dz == 0 && dx > 0 -> "east"
            dz == 0 && dx < 0 -> "west"
            else -> "unknown"
        }

        mes("The talisman pulls towards the $direction.")
    }

    private fun ProtectedAccess.canEnterRuinsWithoutTalisman(altar: RunecraftingAltarsRow): Boolean {
        if (playerHasRunecraftCape()) {
            return true
        }

        val varbitId = altar.varbit ?: return false
        val varbitName = RSCM.getReverseMapping(RSCMType.VARBIT, varbitId)
        return vars[varbitName] == 1
    }

    private fun ProtectedAccess.playerHasRunecraftCape(): Boolean =
        "obj.skillcape_runecrafting" in player.worn ||
            "obj.skillcape_runecrafting_trimmed" in player.worn ||
            "obj.skillcape_max" in player.worn

    private fun ProtectedAccess.nearestEntrance(entrances: Map<String, CoordGrid>): CoordGrid? {
        if (entrances.isEmpty()) {
            return null
        }
        return entrances.values.minByOrNull { entrance ->
            val dx = entrance.x - player.coords.x
            val dz = entrance.z - player.coords.z
            dx * dx + dz * dz
        }
    }

    private companion object {
        val elementalAltarLocs =
            setOf(
                "loc.air_altar",
                "loc.water_altar",
                "loc.earth_altar",
                "loc.fire_altar",
            )

        val catalyticAltarLocs =
            setOf(
                "loc.mind_altar",
                "loc.body_altar",
                "loc.cosmic_altar",
                "loc.chaos_altar",
                "loc.nature_altar",
                "loc.law_altar",
                "loc.death_altar",
                "loc.wrath_altar",
            )
    }
}
