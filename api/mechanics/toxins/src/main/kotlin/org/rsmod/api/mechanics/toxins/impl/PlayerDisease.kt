package org.rsmod.api.mechanics.toxins.impl

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.StatType
import kotlin.random.Random
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.config.refs.params
import org.rsmod.api.mechanics.toxins.Toxin
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.processor.InstantPlayerHitProcessor
import org.rsmod.api.player.hit.takeInstantHit
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.queueDeath
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.getInvObj

public object PlayerDisease {

    public const val TICK_INTERVAL: Int = PlayerPoison.TICK_INTERVAL

    private object DiseaseSplatOnlyProcessor : InstantPlayerHitProcessor {
        override fun Player.process(hit: Hit) {
            showHitmark(hit.hitmark)
        }
    }

    private object DiseaseDamageOnlyProcessor : InstantPlayerHitProcessor {
        override fun Player.process(hit: Hit) {
            val damage = minOf(hitpoints, hit.damage)
            if (damage > 0) {
                statSub("stat.hitpoints", constant = damage, percent = 0)
            }
            val queueDeath = hitpoints == 0 && "queue.death" !in queueList
            if (queueDeath) {
                queueDeath()
            }
            showHitmark(hit.hitmark)
        }
    }

    public fun isDiseased(player: Player): Boolean = player.vars["varp.disease_drain"] > 0

    public fun hasWornDiseaseMitigation(player: Player): Boolean {
        for (obj in player.worn) {
            if (obj == null) continue
            val type = getInvObj(obj)
            if ((type.paramOrNull(params.worn_disease_mitigation) ?: 0) > 0) {
                return true
            }
        }
        return false
    }

    public fun tryDisease(player: Player, drainPerTick: Int): Boolean {
        if (drainPerTick <= 0) {
            return false
        }
        return applyDisease(player, drainPerTick)
    }

    private fun applyDisease(player: Player, drainPerTick: Int): Boolean {
        if (eligibleDiseaseStats().isEmpty()) {
            return false
        }
        VarPlayerIntMapSetter.set(player, "varp.disease_drain", drainPerTick)
        player.timer("timer.player_disease", TICK_INTERVAL)
        player.mes("You feel slightly ill.", ChatType.Spam)
        Toxin.syncStatusOrbs(player)
        return true
    }

    public fun clear(player: Player) {
        VarPlayerIntMapSetter.set(player, "varp.disease_drain", 0)
        player.clearTimer("timer.player_disease")
        Toxin.syncStatusOrbs(player)
    }

    public fun onDiseaseTimerTick(player: Player) {
        if (!isDiseased(player)) {
            clear(player)
            return
        }
        val drain = player.vars["varp.disease_drain"]
        val targetStat = pickDiseaseStat() ?: run {
            clear(player)
            return
        }

        if (hasWornDiseaseMitigation(player)) {
            player.takeInstantHit(
                type = HitType.Typeless,
                damage = 0,
                processor = DiseaseSplatOnlyProcessor,
                hitmark = hitmark_groups.disease,
                modifier = NoopPlayerHitModifier,
            )
        } else {
            applyDiseaseDrain(player, RSCM.getReverseMapping(RSCMType.STAT,targetStat.id), drain)
        }

        player.timer("timer.player_disease", TICK_INTERVAL)
    }

    public fun rearmTimerAfterLogin(player: Player) {
        if (isDiseased(player)) {
            player.timer("timer.player_disease", TICK_INTERVAL)
        }
    }

    private fun applyDiseaseDrain(player: Player, targetStat: String, drain: Int) {
        val current = player.stat(targetStat)
        if (current >= 2) {
            val loss = minOf(drain, current - 1)
            player.statSub(targetStat, loss, percent = 0)
            player.takeInstantHit(
                type = HitType.Typeless,
                damage = loss,
                processor = DiseaseSplatOnlyProcessor,
                hitmark = hitmark_groups.disease,
                modifier = NoopPlayerHitModifier,
            )
        } else {
            val hpLoss = minOf(drain, player.hitpoints)
            if (hpLoss > 0) {
                player.takeInstantHit(
                    type = HitType.Typeless,
                    damage = hpLoss,
                    processor = DiseaseDamageOnlyProcessor,
                    hitmark = hitmark_groups.disease,
                    modifier = NoopPlayerHitModifier,
                )
            } else {
                player.takeInstantHit(
                    type = HitType.Typeless,
                    damage = 0,
                    processor = DiseaseSplatOnlyProcessor,
                    hitmark = hitmark_groups.disease,
                    modifier = NoopPlayerHitModifier,
                )
            }
        }
    }

    private fun eligibleDiseaseStats(): List<StatType> = ServerCacheManager.getStats().values.filter { st ->
        !st.isType("stat.hitpoints") && !st.isType("stat.prayer")
    }

    private fun pickDiseaseStat(): StatType? {
        val pool = eligibleDiseaseStats()
        if (pool.isEmpty()) {
            return null
        }
        return pool.random(Random.Default)
    }
}
