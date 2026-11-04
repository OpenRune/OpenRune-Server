package org.rsmod.content.minigames.motherlodemine.scripts

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.minigames.motherlodemine.MotherlodeRockfalls
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants.pickaxeSwingAnim
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MotherlodeRockfallScript
@Inject
constructor(
    private val rockfalls: MotherlodeRockfalls,
    private val xpMods: XpModifiers,
) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.LateCycle> { rockfalls.tick() }
        onOpContentLoc1(MotherlodeConstants.CONTENT_ROCKFALL) { mine(it.loc) }
    }

    internal suspend fun ProtectedAccess.mine(rockfall: BoundLocInfo) {
        arriveDelay()

        val pickaxe = Mining.findPickaxe(player)
        if (pickaxe == null) {
            mes("You need a pickaxe to clear this rockfall.")
            return
        }

        anim(swingAnim(pickaxe))
        try {
            delay(1)
        } finally {
            resetAnim()
        }
        if (!rockfalls.clear(rockfall)) return

        soundSynth("synth.mine_ore")
        val xp = MotherlodeConstants.ROCKFALL_XP * xpMods.get(player, "stat.mining")
        statAdvance("stat.mining", xp)
        spam("You clear the rockfall.")
    }

    private fun swingAnim(pickaxe: InvObj): String {
        val type = getInvObj(pickaxe)
        return RSCM.getReverseMapping(RSCMType.SEQ, type.pickaxeSwingAnim.id)
    }
}
