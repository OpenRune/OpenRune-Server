package org.rsmod.content.skills.magic.spell.teleports

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

/** Native inventory options for every real teleport scroll in the accepted revision-240 cache. */
class TeleportScrollScript @Inject constructor(
    private val teleports: PlayerTeleportValidator,
    private val areas: AreaChecker,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (scroll in TeleportScroll.entries) {
            if (scroll == TeleportScroll.Revenants) {
                onOpHeld3(scroll.item) { teleport(scroll, it.inventory, it.slot, it.obj) }
                onOpHeld4(scroll.item) {
                    val skip = choice2("Show Wilderness warning", false, "Disable Wilderness warning", true)
                    ifCloseChat()
                    VarPlayerIntMapSetter.set(player, "varp.revenant_scroll_nowarn", if (skip) 1 else 0)
                    mes("Revenant teleport warnings ${if (skip) "disabled" else "enabled"}.")
                }
            } else {
                onOpHeld1(scroll.item) { teleport(scroll, it.inventory, it.slot, it.obj) }
            }
        }
    }

    private suspend fun ProtectedAccess.teleport(scroll: TeleportScroll, inventory: Inventory, slot: Int, item: InvObj) {
        TeleportScrollTravel(teleports, areas, collision).travel(this, scroll,
            valid = { inventory[slot] === item },
            consume = { player.invDel(inventory, scroll.item, 1, slot = slot).success })
    }

    internal companion object {
        val REVENANT_MIDDLE = CoordGrid(3069, 3740)
        val REVENANT_SOUTH = CoordGrid(3075, 3653)
    }
}

enum class TeleportScroll(val item: String, val destination: CoordGrid) {
    Nardah("obj.teleportscroll_nardah", CoordGrid(3421, 2917)),
    Digsite("obj.teleportscroll_digsite", CoordGrid(3324, 3412)),
    FeldipHills("obj.teleportscroll_feldip", CoordGrid(2542, 2925)),
    LunarIsle("obj.teleportscroll_lunarisle", CoordGrid(2093, 3912)),
    Mortton("obj.teleportscroll_mortton", CoordGrid(3489, 3288)),
    PestControl("obj.teleportscroll_pestcontrol", CoordGrid(2657, 2660)),
    Piscatoris("obj.teleportscroll_piscatoris", CoordGrid(2339, 3648)),
    TaiBwoWannai("obj.teleportscroll_taibwo", CoordGrid(2788, 3066)),
    IorwerthCamp("obj.teleportscroll_elf", CoordGrid(2193, 3257)),
    MosLeHarmless("obj.teleportscroll_mosles", CoordGrid(3701, 2996)),
    Lumberyard("obj.teleportscroll_lumberyard", CoordGrid(3303, 3487)),
    ZulAndra("obj.teleportscroll_zulandra", CoordGrid(2197, 3056)),
    KeyMaster("obj.teleportscroll_cerberus", CoordGrid(1310, 1250)),
    Revenants("obj.teleportscroll_revenants", CoordGrid(3127, 3833)),
    Watson("obj.teleportscroll_watson", CoordGrid(1645, 3579)),

    // Preserve the owner's accepted TD cave arrival, instead of the OSRS outside entrance.
    GuthixianTemple("obj.teleportscroll_guthixian_temple", CoordGrid(4061, 4464)),
    SpiderCave("obj.teleportscroll_spidercave", CoordGrid(3658, 3403)),
    ColossalWyrm("obj.teleportscroll_colossal_wyrm", CoordGrid(1641, 2921)),
    ChasmOfFire("obj.teleportscroll_chasmoffire", CoordGrid(1439, 10076)),
    Ardeaglais("obj.teleportscroll_ardeaglais", CoordGrid(2543, 2216)),
}
