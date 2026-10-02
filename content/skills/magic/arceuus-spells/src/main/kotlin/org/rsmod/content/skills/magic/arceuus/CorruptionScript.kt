package org.rsmod.content.skills.magic.arceuus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.death.PvPPlayerHitHook
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hitmark
import org.rsmod.plugin.module.PluginModule
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal var Player.corruptionPrimed by intVarBit("varbit.arceuus_corruption")

internal var Player.corruptionCooldown by boolVarBit("varbit.arceuus_corruption_cooldown")

internal var Player.corruptionMarked by boolVarBit("varbit.corruption_marked")

internal var Player.corruptionFast by boolVarBit("varbit.corruption_fast")

internal var Player.corruptionBaseDrain by intVarBit("varbit.corruption_base_damage")

internal var Player.corruptionIteration by intVarBit("varbit.corruption_current_iterations")

internal var Player.corruptionTargetIterations by intVarBit("varbit.corruption_target_iterations")

internal val Player.isCorrupted: Boolean
    get() = corruptionTargetIterations > 0

internal enum class CorruptionSpell(
    val obj: String,
    val component: String,
    val primedValue: Int,
    val baseDrain: Int,
    val spotanim: String,
    val buffStruct: Int,
) {
    Lesser(
        obj = "obj.zanarischoir_dummy",
        component = "component.magic_spellbook:lesser_corruption",
        primedValue = 1,
        baseDrain = 1,
        spotanim = "spotanim.lesser_corruption_cast_spotanim",
        buffStruct = 3118,
    ),
    Greater(
        obj = "obj.skillcape_ardy_hood_firecape",
        component = "component.magic_spellbook:greater_corruption",
        primedValue = 2,
        baseDrain = 2,
        spotanim = "spotanim.greater_corruption_cast_spotanim",
        buffStruct = 4156,
    ),
}

internal object Corruption {
    const val CAST_ANIM = "seq.human_spellcast_demonbane"
    const val HITMARK = "hitmark.corruption"
    const val BUFF_BAR_START_CLIENTSCRIPT = 5931
    const val COOLDOWN_QUEUE = "queue.corruption_cooldown"
    const val TIMER = "timer.corruption"
    const val COOLDOWN_TICKS = 50
    const val DRAIN_STEPS = 3
    const val BASE_CHANCE = 50
    const val MARKED_CHANCE = 100
    const val SLOW_INTERVAL = 10
    const val FAST_INTERVAL = 5

    fun drainAt(baseDrain: Int, step: Int): Int = baseDrain * step

    fun chance(marked: Boolean): Int = if (marked) MARKED_CHANCE else BASE_CHANCE

    fun interval(marked: Boolean): Int = if (marked) FAST_INTERVAL else SLOW_INTERVAL
}

class CorruptionModule : PluginModule() {
    override fun bind() {
        addSetBinding<PvPPlayerHitHook>(CorruptionHitHook::class.java)
    }
}

class CorruptionScript
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueue(Corruption.COOLDOWN_QUEUE) { player.corruptionCooldown = false }
        onPlayerTimer(Corruption.TIMER) { drain() }
        for (spell in CorruptionSpell.entries) {
            onIfOverlayButton(spell.component) { cast(spell) }
        }
    }

    private fun ProtectedAccess.cast(spell: CorruptionSpell) {
        if (player.corruptionCooldown) {
            mes("You can only cast corruption spells every 30 seconds.")
            return
        }

        val spellObj = ServerCacheManager.getItem(spell.obj.asRSCM(RSCMType.OBJ)) ?: return
        val magicSpell = spells.getObjSpell(spellObj) ?: return

        if (runes.attemptCast(player, magicSpell).isFailure()) {
            return
        }

        statAdvance("stat.magic", magicSpell.castXp)
        anim(Corruption.CAST_ANIM)
        spotanim(spell.spotanim)

        player.corruptionPrimed = spell.primedValue
        player.corruptionMarked = player.markOfDarknessActive
        player.corruptionCooldown = true
        clearQueue(Corruption.COOLDOWN_QUEUE)
        queue(Corruption.COOLDOWN_QUEUE, Corruption.COOLDOWN_TICKS)
        runClientScript(Corruption.BUFF_BAR_START_CLIENTSCRIPT, spell.buffStruct, mapClock)
    }

    private fun ProtectedAccess.drain() {
        val next = player.corruptionIteration + 1
        val amount = Corruption.drainAt(player.corruptionBaseDrain, next - 1)

        if (amount > 0) {
            player.statSub("stat.prayer", constant = amount, percent = 0)
            val hitmark = Corruption.HITMARK.asRSCM(RSCMType.HITMARK)
            player.showHitmark(
                Hitmark.fromNoSource(
                    self = hitmark,
                    source = hitmark,
                    public = hitmark,
                    damage = amount,
                    delay = 0,
                )
            )
        }

        if (next > Corruption.DRAIN_STEPS) {
            player.clearCorruption()
            mes("You are no longer afflicted with corruption.")
            return
        }

        player.corruptionIteration = next
        player.timer(Corruption.TIMER, Corruption.interval(player.corruptionFast))
    }

    private fun Player.clearCorruption() {
        corruptionIteration = 0
        corruptionTargetIterations = 0
        corruptionBaseDrain = 0
        corruptionFast = false
    }
}

internal class CorruptionHitHook @Inject constructor(private val random: GameRandom) :
    PvPPlayerHitHook {
    override fun onPlayerHit(attacker: Player, target: Player, damage: Int) {
        val type = CorruptionSpell.entries.firstOrNull { it.primedValue == attacker.corruptionPrimed }
        if (type == null || damage <= 0) {
            return
        }
        if (target.wardOfArceuusActive || target.isCorrupted) {
            return
        }

        val marked = attacker.corruptionMarked
        if (random.of(100) >= Corruption.chance(marked)) {
            return
        }

        attacker.corruptionPrimed = 0
        attacker.corruptionMarked = false
        attacker.mes("<col=ff289d>Your target has been corrupted!</col>")
        target.mes("<col=ef0083>You have been corrupted!</col>")

        target.corruptionBaseDrain = type.baseDrain
        target.corruptionIteration = 1
        target.corruptionTargetIterations = Corruption.DRAIN_STEPS
        target.corruptionFast = marked
        target.timer(Corruption.TIMER, Corruption.interval(marked))
    }
}
