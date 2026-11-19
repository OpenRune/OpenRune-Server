package org.rsmod.content.events.demonicpacts

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.config.constants
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdd
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PactCombatScript
@Inject
constructor(
    private val playerList: PlayerList,
    private val npcList: NpcList,
    private val npcHitModifier: NpcHitModifier,
    private val random: GameRandom,
) : PluginScript() {
    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    override fun ScriptContext.startup() {
        onEvent<NpcHitEvents.ModifyAny> { onPlayerHitsNpc(npc, hit) }
        onEvent<PlayerHitEvents.Modify> { onPlayerHit(player, hit) }
        onPlayerSoftTimer(PRAYER_REGEN_TIMER) { player.regenPrayer() }
    }

    private fun onPlayerHitsNpc(npc: Npc, hit: HitBuilder) {
        if (!hit.isFromPlayer || hit.type == HitType.Typeless) {
            return
        }
        val attacker = hit.sourceUid?.let { PlayerUid(it).resolve(playerList) } ?: return
        if (!attacker.isDemonicPactsLeague) {
            return
        }
        val effects = attacker.pactEffects()
        val distance = attacker.coords.chebyshevDistance(npc.coords)

        if (hit.type == HitType.Melee && hit.damage > 0 && effects.meleeMinHit > 0) {
            val minHit = effects.meleeMinHit * (1 + max(0, distance - 1))
            hit.damage = max(hit.damage, minHit)
        }
        if (hit.type == HitType.Melee && hit.damage > 0 && effects.specOnMeleeHit > 0) {
            attacker.restoreSpecialEnergy(effects.specOnMeleeHit)
        }
        if (distance >= 2 && effects.specFromDistance > 0) {
            attacker.restoreSpecialEnergy(effects.specFromDistance)
        }
    }

    private fun onPlayerHit(target: Player, hit: HitBuilder) {
        if (hit.isFromPlayer) {
            applyPenetration(hit)
            return
        }
        if (!hit.isFromNpc || !target.isDemonicPactsLeague) {
            return
        }
        val effects = target.pactEffects()
        val hasShield = target.worn[Wearpos.LeftHand.slot] != null
        val attacker = hit.sourceUid?.let { NpcUid(it).resolve(npcList) } ?: return

        if (effects.reflect && hasShield && hit.damage > 0) {
            val chance = target.statBase("stat.defence")
            if (random.of(REFLECT_ROLL) < chance) {
                attacker.queueHit(target, 1, HitType.Typeless, hit.damage, npcHitModifier)
                hit.damage = 0
            }
        }

        val thorns = if (hasShield) effects.thorns else 0
        if (thorns > 0) {
            attacker.queueHit(target, 1, HitType.Typeless, thorns, npcHitModifier)
            if (effects.thornsDouble) {
                attacker.queueHit(target, 2, HitType.Typeless, max(1, thorns / 2), npcHitModifier)
            }
        }
    }

    private fun applyPenetration(hit: HitBuilder) {
        val attacker = hit.sourceUid?.let { PlayerUid(it).resolve(playerList) } ?: return
        if (!attacker.isDemonicPactsLeague) {
            return
        }
        val penetration = attacker.pactEffects().prayerPenetration
        if (penetration > hit.penetration) {
            hit.penetration = penetration
        }
    }

    private fun Player.restoreSpecialEnergy(amount: Int) {
        val current = vars[SPEC_ENERGY_VARP]
        val restored = min(constants.sa_max_energy, current + amount)
        if (restored > current) {
            VarPlayerIntMapSetter.set(this, SPEC_ENERGY_VARP, restored)
        }
    }

    private fun Player.regenPrayer() {
        if (!isDemonicPactsLeague || pactEffects().prayerRegenTicks <= 0) {
            return
        }
        val protecting =
            vars["varbit.prayer_protectfrommelee"] == 1 ||
                vars["varbit.prayer_protectfrommissiles"] == 1 ||
                vars["varbit.prayer_protectfrommagic"] == 1
        if (!protecting && stat("stat.prayer") < statBase("stat.prayer")) {
            statAdd("stat.prayer", 1, 0)
        }
    }

    private companion object {
        const val PRAYER_REGEN_TIMER = "timer.dp_prayer_regen"
        const val SPEC_ENERGY_VARP = "varp.sa_energy"
        const val REFLECT_ROLL = 1000
    }
}
