package org.rsmod.content.skills.fishing.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class InfernalHarpoonScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(SMOULDERING_STONE, DRAGON_HARPOON) { createHarpoon() }
    }

    private fun ProtectedAccess.createHarpoon() {
        if (player.statBase("stat.fishing") < FISHING_REQ) {
            mes("You need a Fishing level of $FISHING_REQ to make an infernal harpoon.")
            return
        }
        if (player.statBase("stat.cooking") < COOKING_REQ) {
            mes("You need a Cooking level of $COOKING_REQ to make an infernal harpoon.")
            return
        }
        invDel(inv, SMOULDERING_STONE, 1)
        invDel(inv, DRAGON_HARPOON, 1)
        invAdd(inv, INFERNAL_HARPOON, 1)
        statAdvance("stat.cooking", COOKING_XP)
        statAdvance("stat.fishing", FISHING_XP)
        mes("You combine the smouldering stone with your dragon harpoon.")
    }

    private companion object {
        private const val SMOULDERING_STONE = "obj.smouldering_stone"
        private const val DRAGON_HARPOON = "obj.dragon_harpoon"
        private const val INFERNAL_HARPOON = "obj.infernal_harpoon"
        private const val FISHING_REQ = 75
        private const val COOKING_REQ = 85
        private const val COOKING_XP = 350.0
        private const val FISHING_XP = 200.0
    }
}
