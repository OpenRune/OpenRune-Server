package org.rsmod.content.quest.mudfoot

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ProjAnimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.npc.owner.assignSpawnOwner
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

/** One of the three thieves Mudfoot and the player fight together. */
internal enum class MudfootFight(
    val area: MudfootArea,
    val fightStage: Int,
    val deadStage: Int,
    val outlawType: String,
    val mudfootType: String,
    val playerTile: CoordGrid,
    val outlawTile: CoordGrid,
    val mudfootTile: CoordGrid,
    val entryVis: Int,
    val exitVis: List<Int>,
    val rocks: Boolean,
    val autoAttack: Boolean,
    val loot: List<Pair<String, Int>>,
) {
    Ranger(
        area = MudfootAreas.WINDMILL,
        fightStage = MudfootStage.RANGER_FIGHT,
        deadStage = MudfootStage.RANGER_DEAD,
        outlawType = "npc.mba_outlaw_ranged_combat",
        mudfootType = "npc.mba_mudfoot_spear_combat",
        playerTile = CoordGrid(3170, 3285, 0),
        outlawTile = CoordGrid(3166, 3288, 0),
        mudfootTile = CoordGrid(3171, 3287, 0),
        entryVis = MudfootVis.WINDMILL,
        exitVis = listOf(MudfootVis.FOLLOWING),
        rocks = false,
        autoAttack = true,
        loot = listOf("obj.bones" to 1, "obj.shortbow" to 1, "obj.bronze_arrow" to 80),
    ),
    Mage(
        area = MudfootAreas.WOODS,
        fightStage = MudfootStage.MAGE_FIGHT,
        deadStage = MudfootStage.MAGE_DEAD,
        outlawType = "npc.mba_outlaw_magic_combat",
        mudfootType = "npc.mba_mudfoot_rocks_combat",
        playerTile = CoordGrid(3173, 3240, 0),
        outlawTile = CoordGrid(3171, 3241, 0),
        mudfootTile = CoordGrid(3172, 3238, 0),
        entryVis = MudfootVis.WINDMILL,
        exitVis = listOf(MudfootVis.WOODS, MudfootVis.FOLLOWING),
        rocks = true,
        autoAttack = false,
        loot =
            listOf(
                "obj.bones" to 1,
                "obj.plainstaff" to 1,
                "obj.airrune" to 80,
                "obj.mindrune" to 80,
            ),
    ),
    Warrior(
        area = MudfootAreas.GRAVEYARD,
        fightStage = MudfootStage.WARRIOR_FIGHT,
        deadStage = MudfootStage.WARRIOR_DEAD,
        outlawType = "npc.mba_outlaw_melee_combat",
        mudfootType = "npc.mba_mudfoot_spear_combat",
        playerTile = CoordGrid(3244, 3200, 0),
        outlawTile = CoordGrid(3244, 3197, 0),
        mudfootTile = CoordGrid(3242, 3199, 0),
        entryVis = MudfootVis.WOODS,
        exitVis = listOf(MudfootVis.GRAVEYARD, MudfootVis.FOLLOWING),
        rocks = false,
        autoAttack = false,
        loot = listOf("obj.bones" to 1, "obj.iron_kiteshield" to 1, "obj.iron_scimitar" to 1),
    ),
}

private class ActiveFight(
    val player: Player,
    val fight: MudfootFight,
    val scene: MudfootScene,
    val outlaw: Npc,
    val mudfoot: Npc,
) {
    var ticks = 0
    var nextMudfootAttack = FIRST_MUDFOOT_ATTACK_TICK
    var warnedLowHealth = false
    var outlawProvoked = false
    var outlawOnMudfoot = false
    var startCycle = 0
    var nextOutlawAttack = 0
    var deathTile: CoordGrid? = null

    private companion object {
        const val FIRST_MUDFOOT_ATTACK_TICK = 4
    }
}

@Singleton
class MudfootEncounters
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
    private val follower: MudfootFollower,
    private val launcher: ProtectedAccessLauncher,
    private val npcInteractions: NpcInteractions,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val npcHitModifier: NpcHitModifier,
    private val accuracy: AccuracyFormulae,
    private val maxHits: MaxHitFormulae,
    private val random: GameRandom,
    private val mapClock: MapClock,
    private val collision: CollisionFlagMap,
) {
    private val fightsByPlayer = HashMap<Player, ActiveFight>()
    private val fightsByMudfoot = HashMap<Npc, ActiveFight>()

    internal lateinit var quest: Quest

    fun inFight(player: Player): Boolean = player in fightsByPlayer

    /** Called from Mudfoot's combat npc ai timer, once per tick. */
    fun tick(mudfoot: Npc) {
        val active = fightsByMudfoot[mudfoot]
        if (active == null) {
            npcRepo.del(mudfoot, Int.MAX_VALUE)
            return
        }
        active.ticks++
        val player = active.player
        if (active.deathTile != null) {
            launchFinish(active)
            return
        }
        if (player.loggingOut || !active.scene.contains(player.coords)) {
            abort(active)
            return
        }
        if (active.outlaw.hitpoints <= 0) {
            active.deathTile = active.outlaw.coords
            mudfoot.resetFaceEntity()
            launchFinish(active)
            return
        }
        scriptedLines(active)
        mudfootAttack(active)
        if (active.fight == MudfootFight.Warrior) {
            warriorTurnsOnMudfoot(active)
        }
    }

    /**
     * Once the warrior has gone [WARRIOR_GIVE_UP_TICKS] without attacking the player (they ran off
     * or he lost them) while Mudfoot stands beside him, he fights Mudfoot for the rest of the
     * encounter, as in the capture.
     */
    private fun warriorTurnsOnMudfoot(active: ActiveFight) {
        val outlaw = active.outlaw
        val mudfoot = active.mudfoot
        if (!active.outlawOnMudfoot) {
            val lastAttack = maxOf(outlaw.vars["varn.lastattack"], active.startCycle)
            val playerEscaped = mapClock.cycle - lastAttack >= WARRIOR_GIVE_UP_TICKS
            val besideMudfoot = outlaw.coords.chebyshevDistance(mudfoot.coords) <= 1
            if (!playerEscaped || !besideMudfoot) {
                return
            }
            active.outlawOnMudfoot = true
            active.nextOutlawAttack = active.ticks + WARRIOR_SWITCH_DELAY
        }
        outlaw.noneMode()
        outlaw.faceNpc(mudfoot)
        if (active.ticks < active.nextOutlawAttack) {
            return
        }
        active.nextOutlawAttack = active.ticks + OUTLAW_ATTACK_RATE
        outlaw.anim(WARRIOR_SLASH_ANIM)
        active.player.soundSynth(WARRIOR_SLASH_SOUND)
        mudfoot.anim(MUDFOOT_DEFEND_ANIM)
        val hit = accuracy.rollMeleeAccuracy(outlaw, mudfoot, MeleeAttackType.Slash, random)
        val rolled = if (hit) random.of(0..maxHits.getMeleeMaxHit(outlaw)) else 0
        val damage = rolled.coerceAtMost(mudfoot.hitpoints - 1).coerceAtLeast(0)
        mudfoot.queueHit(outlaw, HIT_DELAY, HitType.Melee, damage, npcHitModifier)
    }

    /** Ends the fight once the player has left it: died, teleported out or walked off the copy. */
    fun checkPlayer(player: Player) {
        val active = fightsByPlayer[player] ?: return
        if (active.deathTile != null || active.scene.contains(player.coords)) {
            return
        }
        abort(active)
    }

    fun onLogout(player: Player) {
        val active = fightsByPlayer[player] ?: return
        if (active.scene.contains(player.coords)) {
            player.mudfootReturnTile = active.scene.toStatic(player.coords).packed
        }
        abort(active)
    }

    internal suspend fun ProtectedAccess.startFight(fight: MudfootFight) {
        fadeToBlack()
        delay(1)
        minimapHideMap()
        delay(1)

        val region = regionRepo.add(fight.area.template)
        if (region == null) {
            minimapReset()
            fadeFromBlack()
            return
        }
        regionRepo.protect(region)
        val scene = MudfootScene(fight.area, region)

        follower.stop(player)
        quest.setQuestStage(this, fight.fightStage)
        player.mudfootVis = fight.entryVis
        player.mudfootReturnTile = scene.toStatic(scene[fight.playerTile]).packed

        telejump(scene[fight.playerTile])
        faceSquare(scene[fight.outlawTile])

        val outlaw = spawn(fight.outlawType, scene[fight.outlawTile])
        val mudfoot = spawn(fight.mudfootType, scene[fight.mudfootTile])
        outlaw.assignSpawnOwner(player, this@MudfootEncounters.mapClock.cycle)
        mudfoot.assignSpawnOwner(player, this@MudfootEncounters.mapClock.cycle)
        outlaw.facePlayer(player)
        mudfoot.faceNpc(outlaw)
        mudfoot.aiTimer(1)

        val active = ActiveFight(player, fight, scene, outlaw, mudfoot)
        active.startCycle = this@MudfootEncounters.mapClock.cycle
        fightsByPlayer[player] = active
        fightsByMudfoot[mudfoot] = active
        outlaw.opPlayer2(player, aiPlayerInteractions)

        delay(2)
        minimapReset()
        fadeFromBlack()

        if (fight.autoAttack) {
            npcInteractions.interact(player, outlaw, InteractionOp.Op2)
        }
    }

    private fun launchFinish(active: ActiveFight) {
        if (active.player !in fightsByPlayer) {
            return
        }
        launcher.launch(active.player) { finishFight(active) }
    }

    private suspend fun ProtectedAccess.finishFight(active: ActiveFight) {
        fightsByPlayer.remove(player)
        fightsByMudfoot.remove(active.mudfoot)
        stopAction()
        fadeToBlack()
        delay(1)
        minimapHideMap()
        delay(1)

        val scene = active.scene
        val fight = active.fight
        val deathTile = scene.toStatic(active.deathTile ?: active.outlaw.coords)
        val returnTile = scene.toStatic(player.coords)

        despawn(active.outlaw)
        despawn(active.mudfoot)
        regionRepo.unprotect(scene.region)

        quest.setQuestStage(this, fight.deadStage)
        for (vis in fight.exitVis) {
            player.mudfootVis = vis
        }
        player.mudfootReturnTile = 0
        telejump(returnTile)
        faceSquare(deathTile)
        follower.start(player, spear = true)
        dropLoot(player, fight, deathTile)

        delay(2)
        minimapReset()
        fadeFromBlack()

        when (fight) {
            MudfootFight.Ranger -> rangerDefeated(quest)
            MudfootFight.Mage -> mageDefeated(quest)
            MudfootFight.Warrior -> warriorDefeated(quest)
        }
    }

    private fun abort(active: ActiveFight) {
        val player = active.player
        fightsByPlayer.remove(player)
        fightsByMudfoot.remove(active.mudfoot)
        despawn(active.outlaw)
        despawn(active.mudfoot)
        regionRepo.unprotect(active.scene.region)
        player.mudfootVis = active.fight.entryVis
        if (!player.loggingOut) {
            player.mudfootReturnTile = 0
        }
    }

    private fun scriptedLines(active: ActiveFight) {
        val player = active.player
        when (active.fight) {
            MudfootFight.Mage -> {
                if (active.ticks == MAGE_TAUNT_TICK) {
                    outlawSay(active, "A human helping a goblin? You're a disgrace!")
                }
                if (!active.outlawProvoked && active.outlaw.hitpoints < active.outlaw.baseHitpointsLvl) {
                    active.outlawProvoked = true
                    outlawSay(active, "H.A.M. will get rid of people like you!")
                }
            }
            MudfootFight.Warrior -> {
                if (active.ticks == WARRIOR_PRAYER_TICK) {
                    outlawSay(active, "My faith protects me!")
                    active.outlaw.setHeadIcon(HEAD_ICON_SLOT, PRAYER_HEAD_ICONS, PROTECT_MELEE_ICON)
                    active.outlaw.vars["varn.immune_melee"] = 1
                }
                if (active.ticks == WARRIOR_TAUNT_TICK) {
                    mudfootSay(
                        active,
                        "Ha! Stupid thief pray to stop stabs, but he can't stop magic as well!",
                    )
                }
            }
            MudfootFight.Ranger -> Unit
        }
        if (!active.warnedLowHealth && player.hitpoints in 1..LOW_HEALTH) {
            active.warnedLowHealth = true
            mudfootSay(active, "Human going to die if human not careful. Human eat food quick.")
        }
    }

    private fun mudfootAttack(active: ActiveFight) {
        val mudfoot = active.mudfoot
        val outlaw = active.outlaw
        mudfoot.faceNpc(outlaw)
        if (active.fight.rocks) {
            if (active.ticks < active.nextMudfootAttack) {
                return
            }
            active.nextMudfootAttack = active.ticks + MUDFOOT_ATTACK_RATE
            mudfoot.anim(ROCK_ANIM)
            worldRepo.projAnim(rockProjectile(mudfoot, outlaw))
            val hit = accuracy.rollRangedAccuracy(mudfoot, outlaw, random)
            val damage = if (hit) random.of(0..maxHits.getRangedMaxHit(mudfoot)) else 0
            outlaw.queueHit(mudfoot, HIT_DELAY, HitType.Ranged, damage, npcHitModifier)
            return
        }
        val adjacent = adjacentTile(outlaw.coords, mudfoot.coords) ?: return
        if (mudfoot.coords != adjacent) {
            mudfoot.walk(adjacent)
            return
        }
        if (active.ticks < active.nextMudfootAttack) {
            return
        }
        active.nextMudfootAttack = active.ticks + MUDFOOT_ATTACK_RATE
        mudfoot.anim(SPEAR_ANIM)
        active.player.soundSynth(SPEAR_SOUND)
        val immune = outlaw.vars["varn.immune_melee"] == 1
        val hit = !immune && accuracy.rollMeleeAccuracy(mudfoot, outlaw, MeleeAttackType.Stab, random)
        val damage = if (hit) random.of(0..maxHits.getMeleeMaxHit(mudfoot)) else 0
        outlaw.queueHit(mudfoot, HIT_DELAY, HitType.Melee, damage, npcHitModifier)
    }

    private fun adjacentTile(target: CoordGrid, from: CoordGrid): CoordGrid? =
        CARDINAL_OFFSETS.map { (x, z) -> target.translate(x, z) }
            .filter { it == from || isWalkable(it) }
            .minByOrNull { it.chebyshevDistance(from) }

    private fun isWalkable(coords: CoordGrid): Boolean =
        collision[coords.x, coords.z, coords.level] and BLOCKED_FLAGS == 0

    private fun rockProjectile(source: Npc, target: Npc): ProjAnim =
        ProjAnim.fromNpcToNpc(source, target, ROCK_SPOTANIM.asRSCM(RSCMType.SPOTANIM), ROCK_PROJ)

    private fun outlawSay(active: ActiveFight, line: String) {
        active.outlaw.say(line)
        active.player.mes("$OUTLAW_TITLE|$line", ChatType.NpcSay)
    }

    private fun mudfootSay(active: ActiveFight, line: String) {
        active.mudfoot.say(line)
        active.player.mes("$MUDFOOT_TITLE|$line", ChatType.NpcSay)
    }

    private fun dropLoot(player: Player, fight: MudfootFight, tile: CoordGrid) {
        for ((obj, count) in fight.loot) {
            objRepo.add(obj, tile, LOOT_DURATION, receiver = player, count = count, reveal = LOOT_REVEAL)
        }
    }

    private fun spawn(type: String, coords: CoordGrid): Npc {
        val npc = Npc(type, coords)
        npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        return npc
    }

    private fun despawn(npc: Npc) {
        if (npc.isSlotAssigned) {
            npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    private companion object {
        const val MAGE_TAUNT_TICK = 3
        const val WARRIOR_PRAYER_TICK = 8
        const val WARRIOR_TAUNT_TICK = 11
        const val LOW_HEALTH = 3

        const val MUDFOOT_ATTACK_RATE = 4
        const val OUTLAW_ATTACK_RATE = 4
        const val WARRIOR_GIVE_UP_TICKS = 12
        const val WARRIOR_SWITCH_DELAY = 2
        const val WARRIOR_SLASH_ANIM = "seq.human_sword_slash"
        const val WARRIOR_SLASH_SOUND = "synth.hacksword_slash"
        const val MUDFOOT_DEFEND_ANIM = "seq.slice_surface_goblin_defend_spear"
        const val BLOCKED_FLAGS =
            CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_NPCS or CollisionFlag.LOC
        val CARDINAL_OFFSETS = listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)
        const val HIT_DELAY = 1
        const val SPEAR_ANIM = "seq.slice_surface_goblin_squat_attack_spear"
        const val SPEAR_SOUND = "synth.goblin_spear_attack"
        const val ROCK_ANIM = "seq.slice_surface_goblin_armed_attack"
        const val ROCK_SPOTANIM = "spotanim.thrown_rock"
        val ROCK_PROJ =
            ProjAnimType(
                startHeight = 112,
                endHeight = 144,
                delay = 15,
                angle = 15,
                lengthAdjustment = 0,
                progress = 4,
                stepMultiplier = 5,
            )

        const val HEAD_ICON_SLOT = 0
        const val PRAYER_HEAD_ICONS = 440
        const val PROTECT_MELEE_ICON = 0

        const val LOOT_DURATION = 1000
        const val LOOT_REVEAL = 100
    }
}

internal fun ProtectedAccess.fadeToBlack() {
    fadeOverlay(
        startColour = 0,
        startTransparency = 255,
        endColour = 0,
        endTransparency = 0,
        clientDuration = FADE_DURATION,
    )
}

internal suspend fun ProtectedAccess.fadeFromBlack() {
    fadeOverlay(
        startColour = 0,
        startTransparency = 0,
        endColour = 0,
        endTransparency = 255,
        clientDuration = FADE_DURATION,
    )
    delay(FADE_TICKS)
    closeFadeOverlayNow()
}

private const val FADE_DURATION = 50
private const val FADE_TICKS = 2
