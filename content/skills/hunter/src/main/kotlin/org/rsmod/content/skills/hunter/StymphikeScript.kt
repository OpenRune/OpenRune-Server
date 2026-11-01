package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.baseVar
import dev.openrune.types.varp.bits
import jakarta.inject.Inject
import org.rsmod.api.enums.HunterEnums.hunter_stymphike_loot
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.VarpSync
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.table.QuestRow
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class StymphikeScript
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
    private val hitModifier: NpcHitModifier,
) : PluginScript() {
    private class Bird(val npc: Npc, val tree: Int)

    private val birds = HashMap<Player, Bird>()
    private val trees = HashMap<Int, CoordGrid>()

    override fun ScriptContext.startup() {
        for (tree in 0 until TREE_COUNT) {
            onOpLoc1("loc.hunting_stymphike_tree_$tree") {
                trees[tree] = it.loc.coords
                bait(tree)
            }
        }
        onOpLoc1(BUSH) { if (vars[HIDING] == 0) hide(it.loc) else exit() }
        onOpLoc1(SPEARS_PILE) { invAdd(inv, MAKESHIFT_SPEAR, PILE_TAKE) }
        onOpNpc1(STYMPHIKE) { spear(it.npc) }
        onArea(AREA) { showQuestOps() }
    }

    private fun ProtectedAccess.showQuestOps() {
        if (!QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES)) return
        val varbit = checkNotNull(ServerCacheManager.getVarbit(QUEST_VARBIT.asRSCM(RSCMType.VARBIT)))
        val end = QuestRow.getRow("dbrow.$BLOOD_MOON_RISES".asRSCM()).endstate
        if (vars[QUEST_VARBIT] >= end) return
        val mask = ((1 shl (varbit.bits.last - varbit.bits.first + 1)) - 1) shl varbit.bits.first
        val packed = (player.vars[varbit.baseVar] and mask.inv()) or ((end shl varbit.bits.first) and mask)
        VarpSync.writeVarp(player, varbit.baseVar, packed)
    }

    private fun ProtectedAccess.canHunt(): Boolean {
        if (!QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES)) {
            mes("You need to complete The Blood Moon Rises to hunt stymphikes.")
            return false
        }
        if (stat(HUNTER) < LEVEL) {
            mes("You need a Hunter level of at least $LEVEL to hunt stymphikes.")
            return false
        }
        if (spear() == null) {
            mes("You need a spear to hunt stymphikes.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.spear(): String? =
        listOf(HUNTERS_SPEAR, MAKESHIFT_SPEAR).firstOrNull { it in inv || it in player.worn }

    private fun ProtectedAccess.bait(tree: Int) {
        val status = "varbit.hunting_stymphike_trap_status_$tree"
        if (vars[status] != 0) return
        if (!canHunt()) return
        if (LETVEK !in inv) {
            mes("You need some letvek to bait the tree.")
            return
        }
        val used = minOf(inv.count(LETVEK), 1 + random.of(BAIT_EXTRA_DEN))
        if (invDel(inv, LETVEK, used).failure) return
        anim(HunterTrapScript.BAIT_SEQ)
        VarPlayerIntMapSetter.set(player, status, 1)
        when (used) {
            2 -> mes("An additional letvek scurries out of your pack as you pull one out to bait the tree.", ChatType.Spam)
            3 -> mes("A few letvek scurry out of your pack as you pull one out to bait the tree.", ChatType.Spam)
        }
    }

    private suspend fun ProtectedAccess.hide(bush: BoundLocInfo) {
        if (!canHunt()) return
        teleport(bush.coords)
        anim(HIDE_SEQ)
        VarPlayerIntMapSetter.set(player, HIDING, 1)
        delay(1)
        anim(IDLE_START_SEQ)
        var hidden = 0
        while (vars[HIDING] != 0) {
            delay(1)
            hidden++
            val bird = birds[player]
            if (bird == null && hidden >= SPAWN_TICKS) spawnBird()
            if (hidden % PEEK_TICKS != 0) continue
            anim(RUSTLE_SEQ)
            worldRepo.locAnim(bush, BUSH_SHAKE_SEQ)
            val current = birds[player]
            if (current != null && current.npc.isSlotAssigned) {
                throwSpear(current, alerted = false)
                return
            }
            if (baitedTree() != null) statAdvance(HUNTER, PEEK_XP)
        }
    }

    private fun ProtectedAccess.exit() {
        VarPlayerIntMapSetter.set(player, HIDING, 0)
        resetAnim()
    }

    private fun ProtectedAccess.baitedTree(): Int? =
        (0 until TREE_COUNT).firstOrNull { vars["varbit.hunting_stymphike_trap_status_$it"] != 0 }

    private fun ProtectedAccess.spawnBird() {
        val tree = baitedTree() ?: return
        val type = ServerCacheManager.getNpc(STYMPHIKE.asRSCM(RSCMType.NPC)) ?: return
        val npc = Npc(type, trees[tree] ?: return)
        npcRepo.add(npc, BIRD_TICKS)
        npc.anim(LANDING_SEQ)
        birds[player] = Bird(npc, tree)
    }

    private suspend fun ProtectedAccess.spear(npc: Npc) {
        val bird = birds[player]?.takeIf { it.npc === npc } ?: return
        if (spear() == null) {
            mes("You need a spear to hunt stymphikes.")
            return
        }
        throwSpear(bird, alerted = vars[HIDING] == 0)
    }

    private suspend fun ProtectedAccess.throwSpear(bird: Bird, alerted: Boolean) {
        val spear = spear() ?: return
        if (spear == MAKESHIFT_SPEAR && invDel(if (spear in inv) inv else player.worn, spear).failure) return
        faceEntitySquare(bird.npc)
        spotanim(LAUNCH_SPOTANIM)
        worldRepo.projAnim(
            ProjAnim(TRAVEL_SPOTANIM.asRSCM(RSCMType.SPOTANIM), 40, 36, 20, 50, 10, 0, 0, 0, coords, bird.npc.coords),
        )
        delay(2)
        birds.remove(player)
        VarPlayerIntMapSetter.set(player, "varbit.hunting_stymphike_trap_status_${bird.tree}", 0)
        VarPlayerIntMapSetter.set(player, HIDING, 0)
        resetAnim()
        if (!bird.npc.isSlotAssigned) return
        if (alerted) {
            bird.npc.queueHit(player, 1, HitType.Typeless, ALERTED_DAMAGE, hitModifier)
            bird.npc.anim(TAKEOFF_SEQ)
            statAdvance(HUNTER, ALERTED_XP)
            delay(1)
            if (bird.npc.isSlotAssigned) npcRepo.del(bird.npc, 1)
            return
        }
        bird.npc.anim(DEATH_SEQ)
        npcRepo.del(bird.npc, 2)
        statAdvance(HUNTER, KILL_XP)
        val double = HerbiboarScript.HORN_OF_PLENTY in player.worn && vars[HerbiboarScript.HORN_CHARGES] > 0 &&
            random.of(HerbiboarScript.HORN_CHANCE) == 0
        if (double) VarPlayerIntMapSetter.set(player, HerbiboarScript.HORN_CHARGES, vars[HerbiboarScript.HORN_CHARGES] - 1)
        for ((type, count) in hunter_stymphike_loot) {
            val obj = type.internalName
            val total = (count ?: continue) * if (double && obj != BONES) 2 else 1
            objRepo.add(obj, coords, LOOT_DURATION, receiver = player, count = total)
        }
    }

    companion object {
        const val HUNTER = "stat.hunter"
        const val LEVEL = 82
        const val BLOOD_MOON_RISES = "quest_bloodmoonrises"
        const val QUEST_VARBIT = "varbit.myq6"
        const val AREA = "area.vampyrium_stymphikes"
        const val BUSH = "loc.hunting_stymphike_bush"
        const val SPEARS_PILE = "loc.hunting_stymphike_spears"
        const val STYMPHIKE = "npc.hunting_stymphike"
        const val LETVEK = "obj.letvek"
        const val MAKESHIFT_SPEAR = "obj.hunting_stymphike_spear"
        const val HUNTERS_SPEAR = "obj.hg_hunter_spear"
        const val HIDING = "varbit.hunting_stymphike_hiding"
        const val HIDE_SEQ = "seq.stymphike_hunting_hide"
        const val IDLE_START_SEQ = "seq.human_stymphike_idle_start"
        const val RUSTLE_SEQ = "seq.human_stymphike_rustle"
        const val BUSH_SHAKE_SEQ = "seq.bush_stymphike_shake"
        const val LANDING_SEQ = "seq.stymphike_landing"
        const val TAKEOFF_SEQ = "seq.stymphike_takeoff"
        const val DEATH_SEQ = "seq.stymphike_death"
        const val LAUNCH_SPOTANIM = "spotanim.stymphike_spear_launch"
        const val TRAVEL_SPOTANIM = "spotanim.stymphike_spear_travel"
        const val PILE_TAKE = 10
        const val BAIT_EXTRA_DEN = 3
        const val SPAWN_TICKS = 65
        const val PEEK_TICKS = 20
        const val BIRD_TICKS = 100
        const val PEEK_XP = 125.0
        const val KILL_XP = 1350.0
        const val ALERTED_XP = 891.0
        const val ALERTED_DAMAGE = 6
        const val BONES = "obj.bones"
        const val LOOT_DURATION = 200
        const val TREE_COUNT = 5
    }
}
