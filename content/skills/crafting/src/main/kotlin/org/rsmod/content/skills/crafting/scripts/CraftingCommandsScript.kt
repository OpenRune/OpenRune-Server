package org.rsmod.content.skills.crafting.scripts

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.or2.central.account.Rights
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.content.skills.crafting.CraftingRecipes
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CraftingCommandsScript : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("craftmat") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Spawn materials for a crafting recipe"
            invalidArgs =
                "Use as ::craftmat objDebugName [amount] (ex: ::craftmat red_dragonhide_body 6)"
            cheat { craftMaterials() }
        }
    }

    private fun Cheat.craftMaterials() {
        val typeName = args[0]
        val crafts = args.getOrNull(1)?.toInt()?.coerceAtLeast(1) ?: 1

        val output = "obj.$typeName"
        val materials = CraftingRecipes.materialsFor(output, crafts, player)
        if (materials == null) {
            player.mes("No crafting recipe produces: $output")
            return
        }

        for (material in materials) {
            if (material.tool && player.inv.contains(material.obj)) {
                continue
            }
            player.spawnMaterial(material.obj, material.count)
        }
        player.mes("Spawned materials for `$typeName` x $crafts")
    }

    private fun Player.spawnMaterial(internal: String, count: Int) {
        val type = ServerCacheManager.getItem(internal.asRSCM(RSCMType.OBJ)) ?: return
        val slots = if (type.isStackable) 1 else count
        val noted = type.certlink.takeIf { slots > inv.freeSpace() && type.canCert }
        val spawn = noted?.let { ServerCacheManager.getItem(it)?.internalName } ?: internal
        invAdd(inv, spawn, count, strict = false)
    }
}
