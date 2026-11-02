package org.rsmod.content.quest.area.corsaircove

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.combat.commons.npc.attackRate
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.isInCombat
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftQueue
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext

internal class CorsairCurseFight(
    private val quest: Quest,
    private val npcRepo: NpcRepository,
    private val random: GameRandom,
    private val playerHitModifier: PlayerHitModifier,
    private val worldRepo: WorldRepository,
    private val accuracy: AccuracyFormulae,
    private val maxHits: MaxHitFormulae,
) {
    private val spellProjectile by lazy {
        ServerCacheManager.getProjectile(SpellProjectile.asRSCM(RSCMType.PROJANIM))
            ?: error("Missing projectile type: $SpellProjectile")
    }

    fun ScriptContext.register() {
        val ithoi =
            ServerCacheManager.getNpc(CorsairCurseRefs.NavigatorCombat.asRSCM(RSCMType.NPC))
                ?: error("Missing npc type: ${CorsairCurseRefs.NavigatorCombat}")

        onAiOpPlayer2(ithoi) { castAtTarget(it.target) }
        onAiApPlayer2(ithoi) { castAtTarget(it.target) }

        onPlayerSoftQueue(DrainQueue) { player.curseDrain() }

        onPlayerLogin { placeIthoi(player) }
        onPlayerCoordsChanged {
            if (player.coords != lastKnownCoords && player.onIthoiPlateau()) {
                placeIthoi(player)
            }
        }
    }

    fun placeIthoi(player: Player) {
        if (quest.getQuestStage(player) != CorsairCurseStage.KillOrdered) {
            return
        }
        if (findIthoi() != null) {
            return
        }
        val ithoi = Npc(CorsairCurseRefs.NavigatorCombat, CorsairCurseRefs.IthoiHut)
        npcRepo.add(ithoi, IthoiLifespanTicks)
    }

    private fun Player.onIthoiPlateau(): Boolean {
        val hut = CorsairCurseRefs.IthoiHut
        if (coords.level != hut.level) {
            return false
        }
        return abs(coords.x - hut.x) <= IthoiHutArrivalRadius &&
            abs(coords.z - hut.z) <= IthoiHutArrivalRadius
    }

    private fun findIthoi(): Npc? =
        npcRepo.findAll(ZoneKey.from(CorsairCurseRefs.IthoiHut), zoneRadius = 1).firstOrNull {
            it.isType(CorsairCurseRefs.NavigatorCombat)
        }

    private fun StandardNpcAccess.castAtTarget(target: Player) {
        if (!target.isValidTarget()) {
            resetMode()
            return
        }
        if (actionDelay > mapClock) {
            return
        }
        if (!npc.isInCombat()) {
            resetMode()
            return
        }
        actionDelay = mapClock + npc.attackRate()

        val spell = rollSpell()

        anim(spell.castAnim)
        spotanim(spell.castSpotanim, height = CastSpotanimHeight)
        say(random.pick(Taunts))
        target.soundSynth(spell.castSound)

        val projectile =
            ProjAnim.fromNpcToPlayer(
                npc,
                target,
                spell.travelSpotanim.asRSCM(RSCMType.SPOTANIM),
                spellProjectile,
            )
        worldRepo.projAnim(projectile)

        val landed = accuracy.rollMagicAccuracy(npc, target, random)
        val impact = if (landed) spell.impactSpotanim else FailedSpellSpotanim

        target.spotanim(
            impact,
            delay = projectile.clientCycles,
            height = ImpactSpotanimHeight,
            slot = ImpactSpotanimSlot,
        )
        target.soundSynth(spell.hitSound, delay = projectile.clientCycles)
        target.queueCombatRetaliate(npc)
        target.combatPlayDefendAnim(clientDelay = projectile.clientCycles)

        if (spell == CorsairCurseSpell.Curse) {
            if (landed) {
                target.softQueue(DrainQueue, projectile.serverCycles)
            }
            return
        }

        val damage = if (landed) random.of(maxHits.getMagicMaxHit(npc, target) + 1) else 0
        target.queueHit(npc, projectile.serverCycles, HitType.Magic, damage, modifier = playerHitModifier)
    }

    private fun rollSpell(): CorsairCurseSpell =
        if (random.randomBoolean(CurseOdds)) {
            CorsairCurseSpell.Curse
        } else {
            random.pick(CorsairCurseSpell.Strikes)
        }

    private fun Player.curseDrain() {
        val base = statBase(Defence)
        val drain = base - (base * DrainNumerator) / DrainDenominator
        if (drain <= 0) {
            return
        }
        statSub(Defence, constant = drain, percent = 0)
        mes("You feel weakened.")
    }

    private companion object {
        private const val Defence = "stat.defence"

        // The fight queue must run while the player is protected by eating or another action.
        private const val DrainQueue = "queue.corscurs_curse_drain"

        private const val SpellProjectile = "projanim.magic_spell"

        private const val CastSpotanimHeight = 92

        private const val ImpactSpotanimHeight = 124

        private const val ImpactSpotanimSlot = 2

        private const val FailedSpellSpotanim = "spotanim.failedspell_impact"

        private const val CurseOdds = 10

        private val Taunts =
            arrayOf("Curse 'em all!", "Curse you!", "Arrr, land-lubber!", "Ye'll pay!")

        private const val DrainNumerator = 95

        private const val DrainDenominator = 100

        private const val IthoiLifespanTicks = 3000

        private const val IthoiHutArrivalRadius = 8
    }
}

private const val StrikeAnim = "seq.human_caststrike_walkmerge"

internal enum class CorsairCurseSpell(
    val castAnim: String,
    val castSpotanim: String,
    val travelSpotanim: String,
    val impactSpotanim: String,
    val castSound: String,
    val hitSound: String,
) {
    WindStrike(
        castAnim = StrikeAnim,
        castSpotanim = "spotanim.windstrike_casting",
        travelSpotanim = "spotanim.windstrike_travel",
        impactSpotanim = "spotanim.windstrike_impact",
        castSound = "synth.windstrike_cast_and_fire",
        hitSound = "synth.windstrike_hit",
    ),
    WaterStrike(
        castAnim = StrikeAnim,
        castSpotanim = "spotanim.waterstrike_casting",
        travelSpotanim = "spotanim.waterstrike_travel",
        impactSpotanim = "spotanim.waterstrike_impact",
        castSound = "synth.waterstrike_cast_and_fire",
        hitSound = "synth.waterstrike_hit",
    ),
    EarthStrike(
        castAnim = StrikeAnim,
        castSpotanim = "spotanim.earthstrike_casting",
        travelSpotanim = "spotanim.earthstrike_travel",
        impactSpotanim = "spotanim.earthstrike_impact",
        castSound = "synth.earthstrike_cast_and_fire",
        hitSound = "synth.earthstrike_hit",
    ),
    FireStrike(
        castAnim = StrikeAnim,
        castSpotanim = "spotanim.firestrike_casting",
        travelSpotanim = "spotanim.firestrike_travel",
        impactSpotanim = "spotanim.firestrike_impact",
        castSound = "synth.firestrike_cast_and_fire",
        hitSound = "synth.firestrike_hit",
    ),

    Curse(
        castAnim = "seq.human_castcurse_walkmerge",
        castSpotanim = "spotanim.curse_casting",
        travelSpotanim = "spotanim.curse_travel",
        impactSpotanim = "spotanim.curse_impact",
        castSound = "synth.curse_cast_and_fire",
        hitSound = "synth.curse_hit",
    );

    companion object {
        val Strikes: Array<CorsairCurseSpell> =
            arrayOf(WindStrike, WaterStrike, EarthStrike, FireStrike)
    }
}

internal class CorsairCurseIthoiKillHook
@Inject
constructor(private val protectedAccess: ProtectedAccessLauncher) : NpcDeathKillHook {
    private val ithoiId by lazy { CorsairCurseRefs.NavigatorCombat.asRSCM(RSCMType.NPC) }

    override fun onKill(context: NpcDeathKillContext) {
        if (context.npc.id != ithoiId) {
            return
        }

        val player = context.hero
        val quest = Quest.get(CorsairCurseRefs.QuestKey) ?: return
        if (quest.getQuestStage(player) != CorsairCurseStage.KillOrdered) {
            return
        }

        protectedAccess.launch(player) { quest.setQuestStage(this, CorsairCurseStage.IthoiSlain) }
    }
}
