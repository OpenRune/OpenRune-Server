package org.rsmod.content.raids.toa.raid.encounter.het.akkha

import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.ToaBook
import org.rsmod.content.raids.toa.raid.encounter.ToaBossEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaBossLoot
import org.rsmod.content.raids.toa.raid.encounter.ToaCombatant
import org.rsmod.content.raids.toa.raid.encounter.ToaDeath
import org.rsmod.content.raids.toa.raid.encounter.ToaFight
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

class AkkhaEncounter(
    raid: ToaRaid,
    room: ToaRoom,
    session: InstanceSession,
    controllerId: Int,
) : ToaBossEncounter(raid, room, session, controllerId) {

    enum class Style(val npc: String, val protectedFrom: HitType) {
        MELEE(AkkhaNpcs.MELEE, HitType.Melee),
        RANGED(AkkhaNpcs.RANGE, HitType.Ranged),
        MAGIC(AkkhaNpcs.MAGE, HitType.Magic);

        fun next(): Style = entries[(ordinal + 1) % entries.size]
    }

    internal var akkha: Npc? = null
        private set

    internal var style = Style.MELEE
        private set

    internal val quadrants = AkkhaQuadrants(this)
    internal val effects = AkkhaEffects(this)
    internal val memory = AkkhaMemory(this)
    internal val shadows = AkkhaShadows(this)
    internal val finalStand = AkkhaFinalStand(this)

    private var capped = false
    private var lastAttacked = 0
    internal val trail = AkkhaTrail(this)
    internal val detonate = AkkhaDetonate(this)

    enum class Special {
        MEMORY,
        TRAIL,
        DETONATE,
    }

    internal var picked = Special.MEMORY
        private set

    private var lastSpecial: Special? = null
    private var withTrail = false

    private var hiddenHitpoints = 0

    private var autosSinceSpecial = 0
    private var autoSinceLastSpecial = false

    override val fight =
        ToaFight(
            boss = { akkha },
            combatants = listOf(ToaCombatant.Spec(AkkhaNpcs.SPAWN)),
            music = AkkhaMidis.FIGHT,
            attackRate = { ATTACK_RATE },
            death =
                ToaDeath(
                    anim = AkkhaSeqs.DEATH,
                    dead = null,
                    modelDelay = DEATH_MODEL_DELAY,
                ),
        )

    override val loot =
        ToaBossLoot(
            trackerNpc = AkkhaNpcs.ENRAGE,
            heroObj = AkkhaObjs.ASHES,
            book = ToaBook(AkkhaObjs.BOOK, AkkhaVarbits.BOOK_OWNED),
        )

    internal val specialCadence: Int
        get() = if (raid.isActive("dbrow.toa_invocation_stay_vigilant")) VIGILANT_CADENCE else CADENCE

    internal val specialDue: Boolean
        get() = autosSinceSpecial >= specialCadence

    override fun onBuilt() {
        spawnAkkha()
    }

    override fun onReset() {
        spawnAkkha()
    }

    override fun onFightStart(boss: Npc) {
        resetCounters()
        setStyle(boss, Style.MELEE)
    }

    internal fun fightingStyle(npc: Npc): Style? = if (npc === akkha) style else null

    internal fun countAuto() {
        autosSinceSpecial++
        autoSinceLastSpecial = true
    }

    internal fun beginSpecial(boss: Npc) {
        autosSinceSpecial = 0
        if (autoSinceLastSpecial) swapStyle(boss)
        autoSinceLastSpecial = false
    }

    internal fun akkhaHit(boss: Npc, hit: Hit) {
        if (stage != ToaStage.STARTED || boss !== akkha) return
        if (hit.isFromPlayer) shadows.disrupt()
        hpBar?.update()
        if (capped) {
            memory.clear()
            trail.clear()
            finalStand.begin(boss)
        } else if (boss.hitpoints > 0) {
            shadows.onAkkhaHit(boss)
        }
    }

    override fun canEngage(boss: Npc): Boolean = finalStand.phase == AkkhaFinalStand.Phase.NONE

    override fun onFightTick(boss: Npc, targets: List<Player>) {
        shadows.tick()
        idleSwap(boss)
    }

    override fun validateAttack(player: Player, npc: Npc): NpcAttackValidateResult {
        if (npc === akkha) lastAttacked = deps.mapClock.cycle
        return NpcAttackValidateResult.Pass
    }

    private fun idleSwap(boss: Npc) {
        if (finalStand.phase != AkkhaFinalStand.Phase.NONE || memory.active) return
        val cycle = deps.mapClock.cycle
        if (cycle - lastAttacked < IDLE_SWAP_TICKS) return
        lastAttacked = cycle
        swapStyle(boss)
    }

    internal fun modifyAkkhaHit(boss: Npc, hit: HitBuilder) {
        if (boss !== akkha || !hit.isFromPlayer) return
        val attacker = hit.sourceUid?.let { PlayerUid(it).resolve(deps.bossDeps.playerList) }
        if (attacker != null && effects.isFrozen(attacker)) attacker.actionDelay += 1
        if (shadows.shielded(boss)) {
            hit.damage = 0
            attacker?.mes(SHIELDED_TEXT)
        } else if (capped || hit.type == style.protectedFrom) {
            hit.damage = 0
        } else if (hit.damage >= boss.hitpoints) {
            hit.damage = boss.hitpoints - 1
            capped = true
        }
    }

    internal fun modifyFinalHit(npc: Npc, hit: HitBuilder) {
        finalStand.modifyHit(npc, hit)
    }

    internal fun finalHit(npc: Npc, hit: Hit) {
        if (npc !== akkha || stage != ToaStage.STARTED) return
        hpBar?.update()
        finalStand.onRealHit(hit)
    }

    override fun onFightComplete() {
        finalStand.complete()
    }

    internal fun modifyShadowHit(shadow: Npc, hit: HitBuilder) {
        if (!shadows.isImmune(shadow)) return
        hit.damage = 0
        if (!hit.isFromPlayer) return
        hit.sourceUid?.let { PlayerUid(it).resolve(deps.bossDeps.playerList) }?.mes(IMMUNE_TEXT)
    }

    internal fun shadowDeath(shadow: Npc) {
        shadows.dispel(shadow)
    }

    internal fun messageAll(text: String) {
        for (player in targets()) player.mes(text)
    }

    internal fun akkhaDeath(boss: Npc) {
        if (boss === akkha) complete()
    }

    private fun swapStyle(boss: Npc) {
        deps.worldRepo.soundArea(boss.coords, AkkhaSynths.STYLE_SWAP, radius = STYLE_SOUND_RADIUS)
        setStyle(boss, style.next())
    }

    private fun setStyle(boss: Npc, next: Style) {
        style = next
        boss.transmog(npcType(next.npc), Int.MAX_VALUE)
    }

    internal fun debugBlast(quadrant: Quadrant, slam: Boolean) {
        if (slam) quadrants.shadowSlam(quadrant) else quadrants.memoryBlast(quadrant)
    }

    private fun resetCounters() {
        effects.clear()
        memory.clear()
        trail.clear()
        shadows.clear()
        finalStand.clear()
        capped = false
        lastAttacked = deps.mapClock.cycle
        lastSpecial = null
        withTrail = false
        autosSinceSpecial = 0
        autoSinceLastSpecial = false
        style = Style.MELEE
    }

    internal fun pickSpecial() {
        val doubleTrouble = raid.isActive("dbrow.toa_invocation_double_trouble")
        var pool = if (doubleTrouble) listOf(Special.MEMORY, Special.DETONATE) else Special.entries
        if (targets().size < 2) pool = pool - Special.DETONATE
        if (pool.size > 1) pool = pool.filter { it != lastSpecial }
        picked = pool[deps.random.of(0, pool.lastIndex)]
        lastSpecial = picked
        withTrail = doubleTrouble
    }

    internal fun startMemory(boss: Npc) {
        memory.start(boss)
        if (withTrail) trail.start()
    }

    internal fun startTrail() {
        trail.start()
    }

    internal fun startDetonate() {
        detonate.start()
        if (withTrail) trail.start()
    }

    internal fun hideAkkha() {
        val boss = akkha ?: return
        hiddenHitpoints = boss.hitpoints
        deps.bossDeps.encounterRegistry.remove(boss)
        despawn(boss)
        akkha = null
    }

    internal fun showAkkha() {
        if (stage != ToaStage.STARTED) return
        val boss = createAkkha()
        boss.hitpoints = hiddenHitpoints
        setStyle(boss, style)
        boss.ignoreCombatInteractions = false
        deps.bossDeps.encounter(boss).attackRateOverride = ATTACK_RATE
        deps.bossDeps.suppressAttacks(boss, ATTACK_RATE)
        boss.anim(AkkhaSeqs.APPEAR)
        hpBar?.update()
    }

    internal fun debugFinalStand(): String? {
        val boss = akkha
        if (stage != ToaStage.STARTED || boss == null) return "The fight isn't running."
        capped = true
        memory.clear()
        trail.clear()
        finalStand.begin(boss)
        return null
    }

    internal fun debugMemory(): String? {
        val boss = akkha
        if (stage != ToaStage.STARTED || boss == null) return "The fight isn't running."
        if (memory.active) return "A memory blast is already running."
        boss.anim(AkkhaSeqs.MEMORY)
        memory.start(boss)
        return null
    }

    internal fun debugSpecial(special: Special): String? {
        val boss = akkha
        if (stage != ToaStage.STARTED || boss == null) return "The fight isn't running."
        when (special) {
            Special.MEMORY -> return debugMemory()
            Special.TRAIL -> {
                boss.anim(AkkhaSeqs.TRAIL)
                trail.start()
            }
            Special.DETONATE -> {
                boss.anim(AkkhaSeqs.DETONATE)
                detonate.start()
            }
        }
        return null
    }

    private fun createAkkha(): Npc {
        val boss = spawn(AkkhaNpcs.SPAWN, coords(AkkhaCoords.AKKHA), Direction.West)
        boss.apRequiresLineOfSight = false
        akkha = boss
        return boss
    }

    private fun spawnAkkha() {
        akkha?.let(::despawn)
        createAkkha().ignoreCombatInteractions = true
        resetCounters()
    }

    private companion object {
        const val ATTACK_RATE = 6
        const val STYLE_SOUND_RADIUS = 14
        const val IDLE_SWAP_TICKS = 100
        const val DEATH_MODEL_DELAY = 2
        const val CADENCE = 7
        const val VIGILANT_CADENCE = 5
        const val SHIELDED_TEXT =
            "Akkha is currently immune to your attacks, but his shadows are still disrupted."
        const val IMMUNE_TEXT = "This shadow is currently immune to your attacks."
    }
}

internal object AkkhaCoords {
    val AKKHA = CoordGrid(3680, 5407, 1)
}
