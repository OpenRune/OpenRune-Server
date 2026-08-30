package org.rsmod.content.bosses.demonicgorilla

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val PHASE_MELEE = "melee"
private const val PHASE_RANGED = "ranged"
private const val PHASE_MAGIC = "magic"
private val PHASES = listOf(PHASE_MELEE, PHASE_RANGED, PHASE_MAGIC)

private val GORILLA_TYPE_NAMES: List<String> =
    listOf(
        "npc.mm2_demon_gorilla_1_melee",
        "npc.mm2_demon_gorilla_1_ranged",
        "npc.mm2_demon_gorilla_1_magic",
        "npc.mm2_demon_gorilla_2_melee",
        "npc.mm2_demon_gorilla_2_ranged",
        "npc.mm2_demon_gorilla_2_magic",
    )

class DemonicGorilla @Inject constructor(private val deps: BossDeps, private val npcList: NpcList) :
    PluginScript() {

    /** Npc type id → its own RSCM name, for every gorilla variant. */
    private val nameByTypeId: Map<Int, String> =
        GORILLA_TYPE_NAMES.associateBy {
            requireNotNull(it.npcTypeId()) { "Missing npc type: $it" }
        }

    val spec: BossSpec =
        boss(*GORILLA_TYPE_NAMES.toTypedArray()) {
            stats(attackRate = ATTACK_RATE, aggressionRadius = 8)

            val meleeAttack =
                ability("melee_attack") {
                    anim(MELEE_ATTACK_SEQ)
                    hit {
                        damage(Accuracy(Roll(0..MAX_HIT), meleeAttackType = MeleeAttackType.Crush))
                        type(Melee)
                        delay = MELEE_HIT_DELAY
                    }
                }
            val rangedAttack =
                ability("ranged_attack") {
                    anim(RANGED_ATTACK_SEQ)
                    projectile {
                        spotanim = RANGED_PROJECTILE_SPOT
                        hit {
                            damage(Accuracy(Roll(0..MAX_HIT)))
                            type(Ranged)
                            spotanim(RANGED_IMPACT_SPOT)
                        }
                    }
                }
            val magicAttack =
                ability("magic_attack") {
                    anim(MAGIC_ATTACK_SEQ)
                    projectile {
                        spotanim = MAGIC_PROJECTILE_SPOT
                        hit {
                            damage(Accuracy(Roll(0..MAX_HIT)))
                            type(Magic)
                            spotanim(MAGIC_IMPACT_SPOT)
                        }
                    }
                }
            val boulder =
                ability("boulder") {
                    anim(BOULDER_SEQ)
                    debris(
                        telegraph = BOULDER_TELEGRAPH_SPOT,
                        impact = BOULDER_IMPACT_SPOT,
                        damage = DamageExpr.PercentOfTargetHp(BOULDER_DAMAGE_FRACTION),
                        type = Typeless,
                        windup = BOULDER_WINDUP_TICKS,
                        targetRadius = 0,
                        scatterRadius = 0,
                        count = 1..1,
                        center = CurrentTargetTile,
                    )
                }

            // dump: no gorilla observed attacking with melee (demonic_gorilla_punch) ever threw a
            // boulder; the one boulder came from a gorilla in its ranged attack phase (see below).
            phase(PHASE_MELEE) { weightedSelectorRandom { +random(meleeAttack, weight = 1) } }
            phase(PHASE_RANGED) {
                weightedSelectorRandom {
                    +random(rangedAttack, weight = 3)
                    +random(boulder, weight = 1)
                }
            }
            phase(PHASE_MAGIC) {
                weightedSelectorRandom {
                    +random(magicAttack, weight = 3)
                    +random(boulder, weight = 1)
                }
            }
        }

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onModifyHit = { onModifyProtectionHit(this) })

        val bossIds = spec.npcTypes.mapNotNullTo(mutableSetOf()) { it.npcTypeId() }
        onEvent<NpcStateEvents.Create> { if (npc.type.id in bossIds) resetGorilla(npc) }
        onEvent<NpcStateEvents.Respawn> { if (npc.type.id in bossIds) resetGorilla(npc) }
        onEvent<PlayerHitEvents.Impact> { onAttackImpact(bossIds, hit) }
    }

    /**
     * Initialises a freshly (re)spawned gorilla:
     * - Protection prayer is taken from its spawn identity (`npc.type`, which keeps its original
     *   gameval suffix through later [transmogToStyle] calls), not randomized - wiki: a gorilla's
     *   overhead is fixed by which variant spawned.
     * - Attack style (encounter phase) IS randomized - wiki: "start with a random attack style".
     *   Attack style and protection are independent, so this can differ from the protection style.
     *
     * Runs from the `Create`/`Respawn` handlers, which are registered after [BossCombat]'s own
     * `Respawn` reset, so the encounter created here survives that reset.
     */
    private fun resetGorilla(npc: Npc) {
        val protectStyle = nameByTypeId[npc.type.id]?.substringAfterLast('_') ?: return
        npc.vars["varn.gorilla_protect_damage"] = 0
        npc.vars["varn.gorilla_protect_hits"] = 0
        npc.vars["varn.gorilla_miss_streak"] = 0
        applyImmunity(npc, protectStyle)

        val startStyle = PHASES[deps.random.of(PHASES.size)]
        deps.encounter(npc).transitionTo(startStyle, deps.mapClock.cycle)
    }

    /** Tracks the outgoing damage/hit counters that drive the gorilla's protection prayer switch. */
    private fun onModifyProtectionHit(event: NpcHitEvents.Modify) {
        if (!event.hit.isFromPlayer) return
        val style =
            when (event.hit.type) {
                HitType.Melee -> PHASE_MELEE
                HitType.Ranged -> PHASE_RANGED
                HitType.Magic -> PHASE_MAGIC
                else -> return
            }
        val npc = event.npc

        if (style == currentProtectStyle(npc)) return

        val damage = npc.vars["varn.gorilla_protect_damage"] + event.hit.damage
        val hits = npc.vars["varn.gorilla_protect_hits"] + 1
        if (hits >= PROTECT_HIT_THRESHOLD && damage >= PROTECT_DAMAGE_THRESHOLD) {
            npc.vars["varn.gorilla_protect_damage"] = 0
            npc.vars["varn.gorilla_protect_hits"] = 0
            applyImmunity(npc, style)
            transmogToStyle(npc, style)
        } else {
            npc.vars["varn.gorilla_protect_damage"] = damage
            npc.vars["varn.gorilla_protect_hits"] = hits
        }
    }

    private fun currentProtectStyle(npc: Npc): String =
        when {
            npc.vars["varn.immune_melee"] == 1 -> PHASE_MELEE
            npc.vars["varn.immune_ranged"] == 1 -> PHASE_RANGED
            else -> PHASE_MAGIC
        }

    private fun applyImmunity(npc: Npc, style: String) {
        npc.vars["varn.immune_melee"] = if (style == PHASE_MELEE) 1 else 0
        npc.vars["varn.immune_ranged"] = if (style == PHASE_RANGED) 1 else 0
        npc.vars["varn.immune_magic"] = if (style == PHASE_MAGIC) 1 else 0
    }

    /**
     * Transmogs [npc] into the sibling gorilla type sharing its spawn family (the RSCM name minus
     * its style suffix) with [style]'s suffix.
     */
    @OptIn(InternalApi::class)
    private fun transmogToStyle(npc: Npc, style: String) {
        val spawnName = nameByTypeId[npc.type.id] ?: return
        val newName = spawnName.substringBeforeLast('_') + "_" + style
        val npcType = newName.npcTypeId()?.let(ServerCacheManager::getNpc) ?: return
        npc.transmog(npcType, Int.MAX_VALUE)
        npc.assignUid()
    }

    /** Tracks the outgoing miss streak that drives the gorilla's style-switch trigger. */
    private fun onAttackImpact(bossIds: Set<Int>, hit: Hit) {
        if (!hit.isFromNpc || hit.type == HitType.Typeless) return
        val npc = hit.resolveNpcSource(npcList) ?: return
        if (npc.type.id !in bossIds) return

        if (hit.damage > 0) {
            npc.vars["varn.gorilla_miss_streak"] = 0
            return
        }

        val encounter = deps.encounter(npc)
        val style = encounter.currentPhaseName
        val missStreak = npc.vars["varn.gorilla_miss_streak"] + 1
        if (missStreak >= MISS_STREAK_THRESHOLD) {
            // TODO: the wiki documents a "bone-chilling scream" / `Rhaaaaaaa!` cue on the style
            // switch. It is NOT a `say` packet (the rev235 dump rsprox-3342-rev235 has zero gorilla
            // `say` lines across many switches) nor an attributable area sound in that capture, so
            // it is most likely a client-side synth sound - id still unknown. Re-add once identified.
            val others = PHASES.filter { it != style }
            val next = others[deps.random.of(others.size)]
            encounter.transitionTo(next, deps.mapClock.cycle)
            npc.vars["varn.gorilla_miss_streak"] = 0
        } else {
            npc.vars["varn.gorilla_miss_streak"] = missStreak
        }
    }

    private fun String.npcTypeId(): Int? = ServerCacheManager.getNpc(this.asRSCM(RSCMType.NPC))?.id

    private companion object {
        // "dump:" = cross-checked against proxy capture rsprox-3342-rev235 (rev235, ~25 kills, one
        // boulder at tick 2876). "wiki:" = https://oldschool.runescape.wiki/w/Demonic_gorilla/Strategies
        private const val ATTACK_RATE = 5 // dump: confirmed - same gorilla re-attacks exactly 5t apart
        // wiki: "each one hitting up to 30 damage". Dump only saw 21 land (through a prayer lapse),
        // which is consistent. Old value of 31 was slightly over.
        private const val MAX_HIT = 30
        private const val MELEE_HIT_DELAY = 1

        // wiki: switches attack style after 3 missed hits (prayer-blocked counts; boulder does not).
        private const val MISS_STREAK_THRESHOLD = 3
        // wiki: switches protection prayer after >=4 hits AND >=70 total damage in the unprotected
        // style(s) - e.g. 49 melee then 26 ranged -> immediately Protect from Missiles.
        private const val PROTECT_HIT_THRESHOLD = 4
        private const val PROTECT_DAMAGE_THRESHOLD = 70

        // dump: rock projanim launches on the cast tick and the splash map-anim lands on the target
        // tile 5 ticks later (cast t2876 -> impact t2881); projanim heights 600 -> 10.
        private const val BOULDER_WINDUP_TICKS = 5
        // wiki: "Players will take 33% of their health as damage if they don't move away" - i.e. a
        // fraction of *current* hp, so PercentOfTargetHp(0.33) is the right model.
        private const val BOULDER_DAMAGE_FRACTION = 0.33

        // dump: the windup anim on the casting gorilla is demonic_gorilla_smash_chest (also its idle
        // taunt), so this is right - there is no dedicated boulder sequence.
        private const val BOULDER_SEQ = "seq.demonic_gorilla_smash_chest"
        // dump: the boulder uses the falling-rock projanim `myarm_rock_roc_travel` (id 856), NOT the
        // ranged attack's `mm2_gorilla_stone`. Impact is the map-anim `castlewars_catapult_splash`
        // (id 305) plus area sound 1444. `mm2_gorilla_stone*` are the ordinary ranged attack only.
        // The lone boulder (tick 2876) came from npc 15186, which was in its ranged attack phase
        // (its prior attacks at t2782/t2787 were demonic_gorilla_range) though its npc id stayed
        // mm2_demon_gorilla_2_melee throughout - i.e. attack phase drove it, protection id was
        // independent and unchanged.
        private const val BOULDER_TELEGRAPH_SPOT = "spotanim.myarm_rock_roc_travel"
        private const val BOULDER_IMPACT_SPOT = "spotanim.castlewars_catapult_splash"

        private const val MELEE_ATTACK_SEQ = "seq.demonic_gorilla_punch"
        private const val RANGED_ATTACK_SEQ = "seq.demonic_gorilla_range"
        private const val MAGIC_ATTACK_SEQ = "seq.demonic_gorilla_magic"

        private const val RANGED_IMPACT_SPOT = "spotanim.mm2_gorilla_stonesplat"
        private const val MAGIC_IMPACT_SPOT = "spotanim.mm2_gorilla_spellsplat"

        private const val RANGED_PROJECTILE_SPOT = "spotanim.mm2_gorilla_stone"
        private const val MAGIC_PROJECTILE_SPOT = "spotanim.mm2_gorilla_spell"
    }
}
