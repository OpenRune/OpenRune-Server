package org.rsmod.content.quest.area.draynor.vampyreslayer

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import java.util.Collections
import java.util.IdentityHashMap
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.heal
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.npc.stat.statDrain
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.content.quest.area.draynor.vampyreslayer.VampyreSlayerQuest.Companion.GARLIC
import org.rsmod.content.quest.area.draynor.vampyreslayer.VampyreSlayerQuest.Companion.HAMMER
import org.rsmod.content.quest.area.draynor.vampyreslayer.VampyreSlayerQuest.Companion.STAGE_COMPLETE
import org.rsmod.content.quest.area.draynor.vampyreslayer.VampyreSlayerQuest.Companion.STAGE_STARTED
import org.rsmod.content.quest.area.draynor.vampyreslayer.VampyreSlayerQuest.Companion.STAKE
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Morgan's garlic cupboard, the coffin in Draynor Manor's crypt and the Count himself. */
class CountDraynor
@Inject
constructor(
    private val quest: VampyreSlayerQuest,
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val playerList: PlayerList,
    private val launcher: ProtectedAccessLauncher,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(CUPBOARD_SHUT) { openCupboard(it.loc) }
        onOpLoc1(CUPBOARD_OPEN) { searchCupboard() }
        onOpLoc2(CUPBOARD_OPEN) { shutCupboard(it.loc) }

        onOpLoc1(COFFIN) { openCoffin(it.loc) }

        val count = ServerCacheManager.getNpc(COUNT_DRAYNOR.asRSCM(RSCMType.NPC))!!
        onModifyNpcHit(count) { onCountHit(npc, hit) }
        onNpcHit(count) { finishRegeneration(npc) }
    }

    private suspend fun ProtectedAccess.openCupboard(cupboard: BoundLocInfo) {
        arriveDelay()
        anim(OPEN_SEQ)
        delay(1)
        player.searchedCupboard = 0
        locRepo.change(cupboard, CUPBOARD_OPEN, CUPBOARD_DURATION)
    }

    private suspend fun ProtectedAccess.shutCupboard(cupboard: BoundLocInfo) {
        arriveDelay()
        anim(OPEN_SEQ)
        delay(1)
        locRepo.change(cupboard, CUPBOARD_SHUT, CUPBOARD_DURATION)
    }

    private suspend fun ProtectedAccess.searchCupboard() {
        arriveDelay()
        if (player.searchedCupboard == 0) {
            player.searchedCupboard = 1
            mes("The cupboard contains garlic. You take a clove.")
        } else {
            mes("You take a clove of garlic.")
        }
        invAddOrDrop(objRepo, GARLIC)
    }

    private suspend fun ProtectedAccess.openCoffin(coffin: BoundLocInfo) {
        arriveDelay()
        if (quest.stage(player) !in STAGE_STARTED until STAGE_COMPLETE) {
            mes("The coffin is sealed shut.")
            return
        }
        if (countFor(player) != null) {
            return
        }
        anim(OPEN_SEQ)
        locRepo.change(coffin, COFFIN_OPEN, COFFIN_OPEN_DURATION)
        delay(COFFIN_OPEN_TICKS)
        val rising = Npc(COUNT_IN_COFFIN, coffin.coords)
        npcRepo.add(rising, COUNT_ARISE_TICKS)
        rising.anim(COUNT_ARISE_SEQ)
        delay(COUNT_ARISE_TICKS)
        val count = Npc(COUNT_DRAYNOR, COUNT_SPAWN)
        npcRepo.add(count, COUNT_LIFESPAN)
        count.opPlayer2(player, aiPlayerInteractions)
    }

    private fun countFor(player: Player): Npc? =
        npcRepo.findAll(ZoneKey.from(COUNT_SPAWN), zoneRadius = 1).firstOrNull {
            (it.isType(COUNT_DRAYNOR) || it.isType(COUNT_IN_COFFIN)) &&
                it.coords.level == player.coords.level
        }

    private fun onCountHit(count: Npc, hit: HitBuilder) {
        val attacker = hit.attacker() ?: return
        if (attacker.inv.count(GARLIC) > 0 && isAutoAttack(attacker, hit)) {
            hit.damage += GARLIC_DAMAGE_BONUS
            weakenWithGarlic(count, attacker, hit)
        }
        if (hit.damage < count.hitpoints) {
            return
        }
        val onQuest = quest.stage(attacker) in STAGE_STARTED until STAGE_COMPLETE
        val hasStake = attacker.inv.count(STAKE) > 0
        val hasHammer = attacker.inv.count(HAMMER) > 0
        if (onQuest && hasStake && hasHammer && stakeTheCount(attacker)) {
            return
        }
        hit.damage = count.hitpoints - 1
        if (hasStake && !hasHammer) {
            attacker.mes(NO_HAMMER_MESSAGE)
        }
        attacker.mes(REGENERATE_MESSAGE)
        regenerating += count
    }

    private fun finishRegeneration(count: Npc) {
        if (regenerating.remove(count)) {
            count.heal(count.baseHitpointsLvl, showHitsplat = true)
        }
    }

    /** Garlic acts on the auto-attack timer, so magic only counts while autocasting. */
    private fun isAutoAttack(attacker: Player, hit: HitBuilder): Boolean =
        when (hit.type) {
            HitType.Melee,
            HitType.Ranged -> true
            HitType.Magic -> attacker.autocasting == 1
            HitType.Typeless -> false
        }

    /** Garlic only bites when the Count is at full health, as on the wiki. */
    private fun weakenWithGarlic(count: Npc, attacker: Player, hit: HitBuilder) {
        if (count.hitpoints < count.baseHitpointsLvl) {
            return
        }
        hit.damage += GARLIC_FULL_HEALTH_DAMAGE
        val attackBefore = count.attackLvl
        count.statDrain("stat.attack", GARLIC_ATTACK_DRAIN, 0)
        count.statDrain("stat.strength", GARLIC_STRENGTH_DRAIN, 0)
        count.statDrain("stat.defence", GARLIC_DEFENCE_DRAIN, 0)
        if (count.attackLvl < attackBefore) {
            attacker.mes(GARLIC_MESSAGE)
        }
    }

    private fun stakeTheCount(player: Player): Boolean =
        launcher.launch(player) {
            if (takeStake(player)) {
                mes(STAKE_MESSAGE)
                with(quest) { completeBySlaying() }
            }
        }

    private fun takeStake(player: Player): Boolean =
        player.invTransaction(player.inv) {
            val inv = select(player.inv)
            delete {
                from = inv
                obj = STAKE.asRSCM(RSCMType.OBJ)
                strictCount = 1
            }
        }.success

    private val regenerating: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap())

    private var Player.searchedCupboard by intVarBit("varbit.vampyre_slayer_cupboard_searched")
    private val Player.autocasting by intVarBit("varbit.autocast_set")

    private fun HitBuilder.attacker(): Player? =
        if (isFromPlayer) sourceUid?.let { PlayerUid(it).resolve(playerList) } else null

    private companion object {
        const val CUPBOARD_SHUT = "loc.garliccupboardshut"
        const val CUPBOARD_OPEN = "loc.garliccupboardopen"
        const val CUPBOARD_DURATION = 500
        const val OPEN_SEQ = "seq.human_openchest"

        const val COFFIN = "loc.vampcoffin"
        const val COFFIN_OPEN = "loc.vampcoffin_animated"
        const val COFFIN_OPEN_DURATION = 100
        const val COFFIN_OPEN_TICKS = 3

        const val COUNT_DRAYNOR = "npc.count_draynor"
        const val COUNT_IN_COFFIN = "npc.count_draynor_coffin"
        const val COUNT_ARISE_SEQ = "seq.vampireslayer_arise"
        const val COUNT_ARISE_TICKS = 6
        const val COUNT_LIFESPAN = 500
        val COUNT_SPAWN = CoordGrid(3079, 9775, 0)

        const val GARLIC_DAMAGE_BONUS = 1
        const val GARLIC_FULL_HEALTH_DAMAGE = 10
        const val GARLIC_ATTACK_DRAIN = 10
        const val GARLIC_STRENGTH_DRAIN = 10
        const val GARLIC_DEFENCE_DRAIN = 40

        const val GARLIC_MESSAGE = "The vampyre seems to be weakened by the garlic you're carrying."
        const val REGENERATE_MESSAGE = "<col=ef1020>The vampyre seems to regenerate!</col>"
        const val NO_HAMMER_MESSAGE =
            "<col=ef1020>You're unable to push the stake far enough in!</col>"
        const val STAKE_MESSAGE = "<col=ef1020>You hammer the stake into the vampyre's chest!</col>"
    }
}
