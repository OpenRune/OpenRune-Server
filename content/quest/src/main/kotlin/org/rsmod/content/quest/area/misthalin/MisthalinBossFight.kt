package org.rsmod.content.quest.area.misthalin

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MesAnimType
import dev.openrune.types.NpcServerType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.util.Wearpos
import kotlin.math.abs
import org.rsmod.api.config.constants
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onApNpc2
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal class MisthalinBossFight(
    private val quest: Quest,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val locRepo: LocRepository,
    private val regionRepo: RegionRepository,
    private val worldRepo: WorldRepository,
    private val playerRepo: PlayerRepository,
    private val protectedAccess: ProtectedAccessLauncher,
    private val collision: CollisionFlagMap,
) {

    // Death queue dispatch keys on the spawned NPC type, not the visible multinpc leaf.
    fun register(ctx: ScriptContext): Unit =
        with(ctx) {
            onPlayerLogout {
                if (player.uuid in rooms) protectedAccess.launch(player) { leaveBlueRoom() }
            }
            onOpLoc1("loc.mistmyst_door_sapphire") { sapphireDoor(it.loc) }

            onOpNpc1("npc.mistmyst_mirror") { pushMirror(it.npc) }

            onOpNpc1(AbigaleBody) { abigaleBodyOp(it.npc) }
            onOpNpc1(AbigaleKiller) { abigaleBodyOp(it.npc) }

            for (ornament in Unattackable) {
                onOpNpc2(ornament) {}
                onApNpc2(ornament) { player.abortRoute() }
            }
            onOpNpc1("npc.mistmyst_hewey_killer_unmasked") { talkToCorpse("he") }
            onOpNpc1(HeweyKiller) { heweyCorpseOp(it.npc) }

            onAiTimer("npc.mistmyst_invisible_npc") { mirrorCycleTick(npc) }
        }

    private fun ProtectedAccess.restoreRoomCast() {
        val r = room(player)
        when (quest.getQuestStage(player)) {
            MisthalinStage.BossArmed -> {
                val proxy = spawn(ProxyNpc, r[MisthalinCoords.BossProxy])
                armMirrorCycle(player, proxy)

                spawn(MirrorNpc, r[MisthalinCoords.MirrorStart])
                locRepo.add(
                    r[MisthalinCoords.MirrorBlockerSentinel],
                    MirrorBlocker,
                    Int.MAX_VALUE,
                    LocAngle.West,
                    LocShape.CentrepieceStraight,
                )
            }
            MisthalinStage.HeweyDead -> {
                spawnPersistent(AbigaleBody, r[MirrorKillerTile]).faceSquare(r[HeweyTile])

                val corpse = spawnPersistent(HeweyUnmasked, r[HeweyTile], facing = Direction.West)
                corpse.anim("seq.human_death")
                corpse.faceSquare(r[MirrorKillerTile])
            }
            else -> Unit
        }
    }

    private suspend fun ProtectedAccess.sapphireDoor(door: BoundLocInfo) {
        val stage = quest.getQuestStage(player)

        if (player.uuid?.let(rooms::containsKey) == true) {
            if (stage == MisthalinStage.AbigaleKilled) {
                rescue()
            }
            misthalinDoorCreak()
            leaveBlueRoom()
            return
        }

        if (stage < MisthalinStage.SapphireRoomOpen) {
            if (invTotal(inv, SapphireKey) <= 0) {
                mesbox("The door is securely locked.")
                soundSynth("synth.locked")
                return
            }
            if (invDel(inv, SapphireKey, 1).failure) {
                return
            }
            mes("You use the sapphire key to unlock the door.")
            soundSynth("synth.unlock")
            quest.setQuestStage(this, MisthalinStage.SapphireRoomOpen)
        }

        val route = misthalinCrossingRoute(door)
        openMisthalinLeaf(
            locRepo,
            SapphireLeaf,
            misthalinCrossingOpenTicks(misthalinCrossingTiles(route)),
        )
        misthalinDoorCreak()
        misthalinCrossDoorway(route)
        if (enterBlueRoom() == null) {
            telejump(SapphireDoorTile)
            return
        }

        restoreRoomCast()

        if (quest.getQuestStage(player) == MisthalinStage.SapphireRoomOpen) {
            confrontation()
        }
    }

    private suspend fun ProtectedAccess.confrontation() {
        val r = room(player)
        var killer: Npc? = null
        var completed = false

        lockedScene(
            vantage = r[ConfrontationVantage],
            faceAt = r[MirrorKillerTile],
            camera = r.map(UnmaskCamera),
            returnTo = r[UnmaskVantage],
            teardown = { killer?.let { despawn(it) } },
        ) {
            forcedWalk(listOf(r[UnmaskVantage]), crossTiles = 4)

            faceSquare(r[MirrorKillerTile])
            startDialogue { chatPlayer(angry, "Show yourself you coward!") }

            val entrance = MisthalinCoords.BossWardrobes[0]
            locRepo.add(
                r[entrance.coords],
                WardrobeOpen,
                WardrobeOpenTicks,
                entrance.angle,
                LocShape.CentrepieceStraight,
            )
            soundSynth("synth.creakydoor_open")
            delay(1)

            val entering = spawn(AbigaleKiller, r[ConfrontationEntry])
            killer = entering
            entering.faceSquare(r[entrance.coords])
            delay(1)
            entering.walk(r[MirrorKillerTile])
            delay(4)
            entering.faceSquare(player.coords)

            confrontationLines()

            entering.walk(r[ConfrontationExit])
            delay(3)
            entering.faceSquare(r[MisthalinCoords.BossWardrobes[3].coords])
            entering.anim("seq.human_openbigcupboard")
            delay(1)
            entering.anim("seq.human_reachforladder")
            openWardrobe(
                r[MisthalinCoords.BossWardrobes[3].coords],
                MisthalinCoords.BossWardrobes[3].angle,
            )
            delay(1)

            completed = true
        }

        if (!completed) {
            return
        }

        quest.setQuestStage(this, MisthalinStage.BossArmed)
        player.misthalinReflections = 0
        player.misthalinMirrorBlocker = r[MisthalinCoords.MirrorBlockerSentinel].packed

        spawn(MirrorNpc, r[MisthalinCoords.MirrorStart])

        val proxy = spawn(ProxyNpc, r[MisthalinCoords.BossProxy])
        locRepo.add(
            r[MisthalinCoords.MirrorBlockerSentinel],
            MirrorBlocker,
            Int.MAX_VALUE,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )

        armMirrorCycle(player, proxy)
    }

    private fun armMirrorCycle(player: Player, proxy: Npc) {
        val room = rooms[player.uuid] ?: return
        if (room.cycle != null) {
            return
        }
        room.cycle = MirrorCycle(proxy)
        proxy.aiTimer(1)
    }

    private suspend fun ProtectedAccess.confrontationLines() {
        startDialogue {
            killerSays(
                neutral,
                "So we meet at last. I have to say, I'm impressed you worked out my riddles.",
            )
            chatPlayer(neutral, "Well maybe you aren't as smart as you think you are.")
            killerSays(
                neutral,
                "Oh really? All the dead bodies suggest otherwise. And what did you do to stop them?",
            )
            killerSays(laugh, "NOTHING!")
            killerSays(
                neutral,
                "People think you are some big hero but you're not. All you adventurers are the " +
                    "same. Nothing but opportunists!",
            )
            chatPlayer(neutral, "What are you going on about?")
            killerSays(
                bored,
                "Well, I did have this big speech planned out but quite frankly I don't think you " +
                    "are worth the effort it would take to deliver it.",
            )
            killerSays(
                neutral,
                "So, this is the end of the line for you. It is time for the final act. The " +
                    "showdown. The finale. Let's see if you are up to the challenge!",
            )
        }
    }

    // Reset the push animation after one tick; otherwise its remaining frames play after the shove.
    private suspend fun ProtectedAccess.pushMirror(mirror: Npc) {
        val r = room(player)
        if (quest.getQuestStage(player) != MisthalinStage.BossArmed) {
            mes(constants.dm_default)
            return
        }

        val from = mirror.coords
        val dx = from.x - player.coords.x
        val dz = from.z - player.coords.z
        val direction = PushDirections.indexOfFirst { it.dx == dx && it.dz == dz }
        if (direction == -1) {
            mes(constants.dm_default)
            return
        }

        val push = PushDirections[direction]
        val to = CoordGrid(from.x + push.dx, from.z + push.dz, from.level)

        val boxMin =
            r[CoordGrid(MisthalinCoords.MirrorBoxX.first, MisthalinCoords.MirrorBoxZ.first)]
        val boxMax = r[CoordGrid(MisthalinCoords.MirrorBoxX.last, MisthalinCoords.MirrorBoxZ.last)]
        if (to.x !in boxMin.x..boxMax.x || to.z !in boxMin.z..boxMax.z) {
            mes("The mirror can't go any further in that direction.")
            return
        }

        anim("seq.agility_pyramid_block_push_1")
        exactMove(player.coords, from, delay1 = 0, delay2 = 29, dir = push.angle)

        PathingEntityCommon.teleport(mirror, collision, to)

        locRepo.add(
            CoordGrid(player.misthalinMirrorBlocker),
            MirrorUnblocker,
            Int.MAX_VALUE,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )
        locRepo.add(to, MirrorBlocker, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
        player.misthalinMirrorBlocker = to.packed
        soundArea(worldRepo, from, "synth.pyramid_block", radius = 4)

        rooms[player.uuid]?.cycle?.let {
            it.pushDx = push.dx
            it.pushDz = push.dz
        }

        delay(1)

        resetAnim()
    }

    private class BlueRoom(val region: Region) {
        val scene = SceneRegion(region)
        var cycle: MirrorCycle? = null
    }

    private val rooms = HashMap<Long, BlueRoom>()

    private fun room(player: Player): SceneRegion =
        player.uuid?.let { rooms[it]?.scene } ?: SceneRegion(null)

    private class MirrorCycle(val proxy: Npc) {
        var leadIn: Int = MirrorLeadIn

        var phase: Int = 0

        var wardrobe: Int = -1

        var pushDx: Int = 0
        var pushDz: Int = 0

        var pendingScore: Boolean = false

        var awaitingUnmask: Boolean = false
    }

    // Persist the exit before logout; a login-handler teleport is too late for RebuildLogin.
    private fun ProtectedAccess.enterBlueRoom(): SceneRegion? {
        val uuid = player.uuid ?: return null
        rooms[uuid]?.let {
            return it.scene
        }

        val region = regionRepo.add(MisthalinIslandTemplate)
        if (region == null) {
            mes("The room is unavailable at the moment. Please try again shortly.")
            return null
        }
        regionRepo.protect(region)

        val room = BlueRoom(region)
        rooms[uuid] = room

        player.misthalinReturnTile = SapphireDoorTile.packed
        telejump(room.scene[SapphireInsideTile])
        rebuildAppearance()
        return room.scene
    }

    // Unprotect before unregistering; unregisterSmall alone leaves the UID pinned.
    private fun ProtectedAccess.leaveBlueRoom() {
        if (!leaveMurderWeapon()) return
        val room = player.uuid?.let(rooms::remove)
        room?.cycle?.let { stopCycle(it.proxy) }
        player.misthalinReturnTile = 0
        telejump(SapphireDoorTile)
        rebuildAppearance()
        if (room != null) {
            regionRepo.unprotect(room.region)
        }
        rewindAbandonedRoom()
    }

    private fun ProtectedAccess.leaveMurderWeapon(): Boolean {
        val heldCount = inv.count(KillerKnife)
        val wornCount = worn.count(KillerKnife)
        if (heldCount == 0 && wornCount == 0) return true
        val equipped = worn[Wearpos.RightHand.slot]?.takeIf { it.id == KillerKnife.asRSCM() }
        val removed = player.invTransaction(inv, worn) {
            if (heldCount > 0) delete(select(inv), KillerKnife.asRSCM(), heldCount)
            if (wornCount > 0) delete(select(worn), KillerKnife.asRSCM(), wornCount)
        }
        if (removed.failure) return false
        if (equipped != null) {
            val type = checkNotNull(ServerCacheManager.getItem(equipped.id))
            publish(HeldEquipEvents.WearposChange(player, Wearpos.RightHand, type))
            publish(HeldEquipEvents.Unequip(player, Wearpos.RightHand, type))
            rebuildAppearance()
        }
        mes("You decide it's wise to leave the murder weapon behind.")
        return true
    }

    private fun ProtectedAccess.rewindAbandonedRoom() {
        val stage = quest.getQuestStage(player)
        if (stage < MisthalinStage.BossArmed || stage >= MisthalinStage.AbigaleKilled) {
            return
        }
        quest.setQuestStage(this, MisthalinStage.SapphireRoomOpen)
        player.misthalinReflections = 0
    }

    private fun mirrorCycleTick(npc: Npc) {
        val entry = rooms.entries.firstOrNull { it.value.cycle?.proxy === npc } ?: return
        val owner = entry.key
        val r = entry.value.scene
        val active = entry.value.cycle ?: return

        val viewers =
            playerRepo
                .findAll(ZoneKey.from(r[MisthalinCoords.BossProxy]), zoneRadius = 1)
                .filter { quest.getQuestStage(it) == MisthalinStage.BossArmed }
                .toList()

        if (viewers.isEmpty() && !active.awaitingUnmask) {
            return
        }

        if (active.awaitingUnmask) {
            retryUnmask(r, npc)
            return
        }

        if (active.leadIn > 0) {
            active.leadIn--
            return
        }

        when (active.phase) {
            0 -> {
                flushPendingHit(r, active, viewers)
                if (rooms[owner]?.cycle == null || active.awaitingUnmask) {
                    return
                }
                telegraph(r, active)
            }
            ThrowPhase -> throwFromWardrobe(r, active, viewers)
            CryPhase -> cryOut(r, active)
        }
        active.phase = (active.phase + 1) % MirrorCyclePeriod
    }

    private fun telegraph(r: SceneRegion, active: MirrorCycle) {
        active.wardrobe = (active.wardrobe + 1) % ThrowingWardrobes
        val target = MisthalinCoords.BossWardrobes[active.wardrobe]
        worldRepo.spotanimMap(
            SpotanimType(TelegraphSpotanim.asRSCM(RSCMType.SPOTANIM)),
            r[target.coords],
            height = 1,
        )
    }

    private fun throwFromWardrobe(r: SceneRegion, active: MirrorCycle, viewers: List<Player>) {
        val wardrobe = MisthalinCoords.BossWardrobes[active.wardrobe]
        val mirror = findMirror(r)
        val at = mirror?.coords

        val wardrobeAt = r[wardrobe.coords]

        locRepo.add(
            wardrobeAt,
            WardrobeOpen,
            WardrobeOpenTicks,
            wardrobe.angle,
            LocShape.CentrepieceStraight,
        )

        val inLine = at != null && (wardrobeAt.x == at.x || wardrobeAt.z == at.z)
        val scores =
            inLine &&
                at != null &&
                when {
                    active.pushDx != 0 ->
                        wardrobeAt.z == at.z && (wardrobeAt.x - at.x).sameSign(active.pushDx)
                    active.pushDz != 0 ->
                        wardrobeAt.x == at.x && (wardrobeAt.z - at.z).sameSign(active.pushDz)
                    else -> false
                }

        val outbound = if (inLine && at != null) at else r[wardrobe.wallEnd]
        throwKnife(wardrobeAt, outbound, mirror.takeIf { inLine })
        if (inLine && at != null) {
            val bounce = if (scores) wardrobeAt else r[MisthalinCoords.StrayReflection]
            reflectKnife(at, bounce)
        }

        for (viewer in viewers) {
            viewer.soundSynth("synth.throwingknife")
        }

        active.pendingScore = scores
    }

    private fun cryOut(r: SceneRegion, active: MirrorCycle) {
        if (!active.pendingScore) {
            return
        }
        val wardrobe = r[MisthalinCoords.BossWardrobes[active.wardrobe].coords]
        val proxy = active.proxy

        PathingEntityCommon.telejump(proxy, collision, wardrobe)

        proxy.say(if (reflectionsSoFar(r) % 2 == 0) "Ow!" else "Aaaargh!")

        worldRepo.soundArea(wardrobe, "synth.female_hit_2", radius = 10)
    }

    private fun flushPendingHit(r: SceneRegion, active: MirrorCycle, viewers: List<Player>) {
        if (!active.pendingScore) {
            return
        }
        active.pendingScore = false

        var won = false
        for (viewer in viewers) {
            val reflected = viewer.misthalinReflections + 1
            viewer.misthalinReflections = reflected
            if (reflected >= ReflectionsToWin) {
                won = true
            }
        }
        active.proxy.showReflectionDamage(reflectionsSoFar(r))

        if (won) {
            active.awaitingUnmask = true
            retryUnmask(r, active.proxy)
        }
    }

    private fun retryUnmask(r: SceneRegion, proxy: Npc) {
        val winner =
            playerRepo
                .findAll(ZoneKey.from(r[MisthalinCoords.BossProxy]), zoneRadius = 1)
                .firstOrNull {
                    quest.getQuestStage(it) == MisthalinStage.BossArmed &&
                        it.misthalinReflections >= ReflectionsToWin
                }
        if (winner == null) {
            stopCycle(proxy)
            return
        }
        val started =
            protectedAccess.launch(winner) {
                quest.setQuestStage(this, MisthalinStage.BossBeaten)

                resetMirror()
                unmasking()
            }
        if (started) {
            stopCycle(proxy)
        }
    }

    private fun stopCycle(proxy: Npc) {
        proxy.aiTimer(0)
        rooms.values.firstOrNull { it.cycle?.proxy === proxy }?.cycle = null
    }

    private fun reflectionsSoFar(r: SceneRegion): Int =
        playerRepo.findAll(ZoneKey.from(r[MisthalinCoords.BossProxy]), zoneRadius = 1).maxOfOrNull {
            it.misthalinReflections
        } ?: 0

    private fun findMirror(r: SceneRegion): Npc? =
        npcRepo.findAll(ZoneKey.from(r[MisthalinCoords.MirrorStart]), zoneRadius = 1).firstOrNull {
            it.isType(MirrorNpc)
        }

    // Projectile timing fields are client cycles, with launch delay separate from flight duration.
    private fun throwKnife(from: CoordGrid, to: CoordGrid, bindTo: Npc?) {
        val distance = from.axisDistance(to)
        val endTime =
            if (bindTo != null) {
                KnifeDelay + KnifeTrackedAdjust + KnifeStep * distance
            } else {
                KnifeDelay + KnifeStep * distance
            }
        worldRepo.projAnim(
            ProjAnim(
                spotanim = ThrowingKnifeSpotanim.asRSCM(RSCMType.SPOTANIM),
                startHeight = KnifeStartHeight,
                endHeight = KnifeEndHeight,
                startTime = KnifeDelay,
                endTime = endTime,
                angle = KnifeAngle,
                progress = KnifeProgress,
                sourceIndex = 0,
                targetIndex = bindTo?.let { it.slotId + 1 } ?: 0,
                startCoord = from,
                endCoord = to,
            )
        )
    }

    private fun reflectKnife(from: CoordGrid, to: CoordGrid) {
        worldRepo.projAnim(
            ProjAnim(
                spotanim = ThrowingKnifeSpotanim.asRSCM(RSCMType.SPOTANIM),
                startHeight = KnifeStartHeight,
                endHeight = KnifeEndHeight,
                startTime = ReflectDelay,
                endTime = ReflectDelay + ReflectAdjust + ReflectStep * from.axisDistance(to),
                angle = KnifeAngle,
                progress = KnifeProgress,
                sourceIndex = 0,
                targetIndex = 0,
                startCoord = from,
                endCoord = to,
            )
        )
    }

    private fun CoordGrid.axisDistance(other: CoordGrid): Int =
        maxOf(abs(x - other.x), abs(z - other.z))

    // Keep cutscene_status set through the reveal so the client cannot offer Walk here midway.
    private suspend fun ProtectedAccess.unmasking() =
        lockedScene(
            vantage = room(player)[UnmaskVantage],
            faceAt = room(player)[MirrorKillerTile],
            camera = room(player).map(UnmaskCamera),
        ) {
            unmaskingBody()
        }

    private suspend fun ProtectedAccess.unmaskingBody() {
        val r = room(player)
        val entrance = MisthalinCoords.BossWardrobes[3]
        locRepo.add(
            r[entrance.coords],
            WardrobeOpen,
            WardrobeOpenTicks,
            entrance.angle,
            LocShape.CentrepieceStraight,
        )
        soundSynth("synth.creakydoor_open")
        delay(1)

        var masked: Npc? = spawn(AbigaleKiller, r[ConfrontationExit])
        var hewey: Npc? = null
        var abigale: Npc? = null
        var corpse: Npc? = null
        var knifeDropped = false
        try {
            masked?.walk(r[MirrorKillerTile])
            delay(4)
            masked?.faceSquare(player.coords)
            startDialogue { killerSays(shocked, "Urgh...I think I may have underestimated you.") }

            faceSquare(r[MirrorKillerTile])
            anim("seq.reach_forward_high")
            delay(1)

            abigale = masked?.also { it.transmog(abigaleUnmaskedType, Int.MAX_VALUE) }
            masked = null

            abigale?.facePlayer(player)

            startDialogue {
                chatPlayer(
                    neutral,
                    "You've lost at your own game. And now it is time to see who you really are " +
                        "under that mask!",
                )
                chatPlayer(
                    shocked,
                    "No! You!? But why!? Why on earth would you kill all your friends!? Why beg me to " +
                        "come here to help them!?",
                )
                abigaleSays(quiz, "You just don't get it do you?")
                abigaleSays(
                    neutral,
                    "So big headed. So used to it all being about you. About you saving the day.",
                )
                abigaleSays(angry, "Well I'm sick of it!")
                chatPlayer(confused, "What do you mean?")
                abigaleSays(
                    neutral,
                    "Adventurers! You're all the same. Everything always has to be about you.",
                )
                chatPlayer(confused, "I don't understand?")
                abigaleSays(angry, "Of course you wouldn't! Because you can't see past yourself!")
                abigaleSays(sad, "Believe it or not... I used to be like you.")
                abigaleSays(
                    sad,
                    "I'd hear about some damsel in distress and go to help. To have my moment. For me " +
                        "to be the hero, just for once.",
                )
                abigaleSays(
                    angry,
                    "But YOU! You and every other darned adventurer! Every time I get there to help, " +
                        "it's too late. Crisis solved. Disaster averted. Reward claimed. Every time " +
                        "you and those other adventurers steal my moment!",
                )
                abigaleSays(angry, "But not this time!")
                abigaleSays(
                    angry,
                    "This time, it is all about me! I'm the star of the show! ME ME ME ME ME!",
                )
                chatPlayer(
                    neutral,
                    "Well not any longer. The show is over, and there will be no curtain call.",
                )
                abigaleSays(neutral, "Oh, but there is always a curtain call.")
                abigaleSays(evilLaugh, "Hahahaha!")
            }

            val entering = heweyEnters()
            hewey = entering

            startDialogue {
                chatPlayer(shocked, "What!? How!? There was another killer!?")
                abigaleSays(
                    neutral,
                    "Of course. Every good show needs a good supporting actor. Now, take off your " +
                        "mask darling. Take a bow. You have earned it.",
                )

                heweyMasked(neutral, "Of course, my love.")
                access.delay(1)
            }

            entering.anim("seq.hween16_remove_mask")
            delay(2)

            // Transform the entering actor into its corpse; delete-and-add would change its identity.
            val unmaskedHewey = entering
            unmaskedHewey.transmog(heweyUnmaskedType, Int.MAX_VALUE)
            unmaskedHewey.lifecycleDelCycle = -1
            unmaskedHewey.respawns = false
            hewey = null
            corpse = unmaskedHewey
            unmaskedHewey.anim("seq.trail_bow_emote")

            unmaskedHewey.facePlayer(player)

            startDialogue {
                chatPlayer(
                    shocked,
                    "You!? But how!? You were almost dead when I saw you! I saw your wounds!",
                )
                heweySays(
                    neutral,
                    "No, what you watched was what we wanted you to watch. A masterful performance if " +
                        "I do say myself.",
                )
                chatPlayer(quiz, "But what is in this for you?")
                heweySays(
                    neutral,
                    "Everyone loves the limelight, and who better to share it with than my beloved " +
                        "Abigale.",
                )
                chatPlayer(confused, "But your friends?")
                chatPlayer(angry, "They didn't deserve to die!")
                abigaleSays(
                    neutral,
                    "Perhaps not, but neither did I deserve to be cast back into the shadows when I " +
                        "deserve the spotlight!",
                )
                heweySays(neutral, "Yes, while we deserve the spotlight.")
                abigaleSays(evilLaugh, "And now I have my starring role!")
                heweySays(neutral, "OUR starring role.")
                abigaleSays(
                    neutral,
                    "Gielinor will forever remember this night and what I have done here.",
                )
                unmaskedHewey.faceSquare(r[MirrorKillerTile])
                heweySays(angry, "No, what WE have done!")
                abigale?.faceSquare(r[HeweyTile])
                abigaleSays(angry, "Quiet Hewey! This is my moment!")
                heweySays(
                    angry,
                    "No, this is supposed to be our moment! It isn't just all about you either! We " +
                        "were in this together!",
                )
                abigaleSays(
                    angry,
                    "Stop ruining this Hewey! I have worked so hard for this and I'm not going to let " +
                        "you take it away from me!",
                )
                heweySays(
                    angry,
                    "Well I'm not going to be pushed aside either, we were both in this as equals!",
                )
                abigaleSays(angry, "We were never equals! How could you possibly compare to me!?")
                heweySays(angry, "You egotistical, selfish...")
            }

            abigale?.anim("seq.hween16_slash")
            soundSynth("synth.scythe_double")

            startDialogue {
                soundSynth("synth.human_hit")
                heweySays(confused, "No...my love...")
            }

            unmaskedHewey.anim("seq.human_death")
            soundSynth("synth.human_death")

            objRepo.add(KillerKnife, r[KnifeDropTile], KnifeDespawnTicks, receiver = player)
            knifeDropped = true

            startDialogue {
                abigaleSays(sad, "No....no...what have I done!?")
                abigale?.anim("seq.emote_cry")
                abigaleSays(
                    sad,
                    "It wasn't supposed to be like this...but you just couldn't let me have my " +
                        "moment. WHY HEWEY WHY!? I loved you...",
                )
                abigale?.anim("seq.emote_cry")
                chatPlayer(shifty, "This could be my chance to take advantage of the situation...")
            }
        } finally {
            armFinale(masked, abigale, hewey, corpse, knifeDropped)
        }
    }

    private suspend fun ProtectedAccess.heweyEnters(): Npc {
        val r = room(player)
        val entrance = MisthalinCoords.BossWardrobes[0]
        locRepo.add(
            r[entrance.coords],
            WardrobeOpen,
            WardrobeOpenTicks,
            entrance.angle,
            LocShape.CentrepieceStraight,
        )
        soundSynth("synth.creakydoor_open")
        delay(1)

        val hewey = spawn(HeweyKiller, r[ConfrontationEntry])
        hewey.faceSquare(r[entrance.coords])

        camMoveToV3(
            r[HeweyEntranceEye],
            height = 300,
            rate = SceneCreepRate,
            rate2 = SceneCreepRate,
        )
        camLookAtV3(r[entrance.coords], height = 275, rate = SceneCreepRate, rate2 = SceneCreepRate)

        delay(1)
        hewey.walk(r[HeweyTile])
        camLookAtV3(r[HeweyTile], height = 275, rate = SceneCreepRate, rate2 = SceneCreepRate)
        delay(3)
        hewey.faceSquare(player.coords)
        return hewey
    }

    private fun ProtectedAccess.armFinale(
        maskedAbigale: Npc?,
        abigale: Npc?,
        maskedHewey: Npc?,
        corpse: Npc?,
        knifeDropped: Boolean,
    ) {
        val r = room(player)
        maskedHewey?.let { despawn(it) }

        if (corpse == null) {
            val recovered = spawnPersistent(HeweyUnmasked, r[HeweyTile], facing = Direction.West)
            recovered.anim("seq.human_death")
            recovered.faceSquare(r[MirrorKillerTile])
        }
        if (!knifeDropped) {
            objRepo.add(KillerKnife, r[KnifeDropTile], KnifeDespawnTicks, receiver = player)
        }

        if (quest.getQuestStage(player) < MisthalinStage.HeweyDead) {
            quest.setQuestStage(this, MisthalinStage.HeweyDead)
        }

        val survivor = (abigale ?: maskedAbigale)?.takeIf { it.isSlotAssigned }
        val alreadyPresent = npcRepo.findAll(r[MirrorKillerTile]).any { it.isType(AbigaleBody) }
        when {
            alreadyPresent -> {
                survivor?.let { despawn(it) }
            }
            survivor != null -> {
                survivor.transmog(abigaleBodyType, Int.MAX_VALUE)
                survivor.lifecycleDelCycle = -1
                survivor.respawns = false
                survivor.faceSquare(r[HeweyTile])
            }
            else -> {
                spawnPersistent(AbigaleBody, r[MirrorKillerTile]).faceSquare(r[HeweyTile])
            }
        }
    }

    private suspend fun ProtectedAccess.talkToCorpse(pronoun: String) {
        startDialogue { chatPlayer(neutral, "I don't think $pronoun's in a very talkative mood.") }
    }

    private suspend fun ProtectedAccess.heweyCorpseOp(npc: Npc) {
        if (npc.visType.id != heweyUnmaskedType.id) {
            mes(constants.dm_default, ChatType.Engine)
            return
        }
        talkToCorpse("he")
    }

    private suspend fun ProtectedAccess.abigaleBodyOp(body: Npc) {
        when (quest.getQuestStage(player)) {
            MisthalinStage.HeweyDead -> strikeAbigale(body)
            MisthalinStage.AbigaleKilled -> talkToCorpse("she")
            else -> mes(constants.dm_default, ChatType.Engine)
        }
    }

    private suspend fun ProtectedAccess.strikeAbigale(killer: Npc) {
        if (quest.getQuestStage(player) != MisthalinStage.HeweyDead) {
            return
        }

        if (invTotal(worn, KillerKnife) <= 0) {
            startDialogue { chatPlayer(worried, "I'm not going at her empty-handed!") }
            return
        }

        faceSquare(killer.coords)
        anim("seq.hween16_slash")
        soundSynth("synth.scythe_double")
        soundSynth("synth.scream2")

        try {
            delay(2)
            killer.anim("seq.human_death")
            soundSynth("synth.human_death")
            delay(2)

            startDialogue {
                chatPlayer(neutral, "Well thank Saradomin that's over! I should get out of here.")
            }
        } finally {
            quest.setQuestStage(this, MisthalinStage.AbigaleKilled)
        }
    }

    private suspend fun ProtectedAccess.rescue() {
        val r = room(player)
        var abigale: Npc? = null
        var hewey: Npc? = null
        var mandy: Npc? = null
        var godsword: Npc? = null

        lockedScene(
            vantage = r[RescueVantage],
            faceAt = r[MirrorKillerTile],
            camera = r.map(RescueCamera),
            underFade = {
                npcRepo
                    .findAll(r[MirrorKillerTile])
                    .filter { it.isVisType(AbigaleBody) || it.isType(AbigaleKiller) }
                    .toList()
                    .forEach { despawn(it) }

                abigale = spawn(AbigaleUnmasked, r[MirrorKillerTile]).also { it.anim(DeathAnim) }

                hewey =
                    reuseOrSpawn(HeweyUnmasked, r[HeweyTile], facing = Direction.West).also {
                        it.anim(DeathAnim)
                        it.faceSquare(r[MirrorKillerTile])
                    }

                mandy = spawn(MandyAlive, r[MandyKitchenStart])

                objRepo.add(BgsProp, r[GodswordProp], RescuePropTicks)

                // Let both death sequences finish before lifting the fade.
                delay(2)
            },
            teardown = {
                listOfNotNull(abigale, hewey, mandy, godsword).forEach { despawn(it) }
                findProxy(r)?.let { despawn(it) }
                resetMirror()
            },
        ) {
            rescueBody(
                abigale = checkNotNull(abigale) { "Rescue cast was never spawned." },
                mandy = checkNotNull(mandy) { "Rescue cast was never spawned." },
                onArmed = { godsword = it },
            )
        }

        quest.setQuestStage(this, MisthalinStage.MandyWaiting)
    }

    private suspend fun ProtectedAccess.rescueBody(
        abigale: Npc,
        mandy: Npc,
        onArmed: (Npc) -> Unit,
    ) {
        val r = room(player)
        delay(2)

        abigale.anim("seq.human_death_reverse")

        abigale.faceSquare(player.coords)

        run {
            // Use teleport with jump=false for interpolated scene movement; telejump snaps the actor.
            startDialogue { abigaleSays(angry, "You'll never escape me!") }
            mandy.walkTo(r[MandyKitchenMid])

            startDialogue { abigaleSays(angry, "You dare try and ruin my moment!") }
            mandy.walkTo(r[MandyGodswordTile])

            startDialogue { abigaleSays(angry, "Hewey is dead because of you!") }
            mandy.faceSquare(r[GodswordProp])
            delay(1)
            mandy.anim("seq.human_pickuptable")
            delay(2)

            takeGodswordProp(r)

            mandy.transmog(mandyGodswordType, Int.MAX_VALUE)
            mandy.anim("seq.human_reachforladder")
            openWardrobe(r[MandyKitchenWardrobe], LocAngle.North)
            delay(1)
            despawn(mandy)

            startDialogue { abigaleSays(angry, "Now it is time for you to join him!") }

            val entrance = MisthalinCoords.BossWardrobes[3]
            openWardrobe(r[entrance.coords], entrance.angle)
            delay(1)

            val godsword = spawn(MandyGodsword, r[ConfrontationExit])
            onArmed(godsword)
            godsword.faceSquare(r[ConfrontationExit.translateZ(-1)])
            delay(2)

            startDialogue {
                mandySays(r[ConfrontationExit], angry, "You're not killing anyone else!")
            }

            godsword.walk(r[MandyStrikePost])
            delay(2)

            godsword.faceSquare(r[MirrorKillerTile])
            godsword.anim("seq.bgs_special_player")
            godsword.spotanim("spotanim.dh_sword_update_bandos_special_spotanim")
            soundSynth("synth.godwars_godsword_special_attack")
            delay(2)

            startDialogue {
                soundSynth("synth.scream2")
                abigaleSays(sad, "No....this can't be it....")
            }
            abigale.anim("seq.human_death")
            soundSynth("synth.human_death")

            godsword.faceSquare(player.coords)

            startDialogue {
                chatPlayer(shocked, "Wow, that was close!")
                chatPlayer(
                    confused,
                    "Mandy, how are you alive!? I watched you get stabbed through the heart!",
                )
                mandySays(
                    r[MandyStrikePost],
                    neutral,
                    "I did get stabbed, but I was born with a rare condition called Dextrocardia.",
                )
                chatPlayer(confused, "Dextrowhat?")
                mandySays(
                    r[MandyStrikePost],
                    neutral,
                    "Dextrocardia. It means that my heart is on the right side of my chest, not the " +
                        "left like most people.",
                )
                chatPlayer(
                    confused,
                    "So when the killer thought they stabbed you in the heart, they actually missed?",
                )
                mandySays(r[MandyStrikePost], neutral, "Well, missed the heart yes.")
                mandySays(
                    r[MandyStrikePost],
                    sad,
                    "I did still get stabbed though, and am actually feeling pretty woozy...",
                )
                chatPlayer(shocked, "Oh, yes, right! You should go sort yourself out!")
                mandySays(
                    r[MandyStrikePost],
                    neutral,
                    "I will wait outside the house after seeing to this stab wound. You should come " +
                        "talk to me there.",
                )
            }
        }
    }

    // Reset shared mirror state and blockers on teardown so the next player starts a fresh fight.
    private fun ProtectedAccess.resetMirror() {
        val r = room(player)
        val mirror =
            npcRepo
                .findAll(ZoneKey.from(r[MisthalinCoords.MirrorStart]), zoneRadius = 1)
                .firstOrNull { it.isType("npc.mistmyst_mirror") }
        if (mirror != null) {
            PathingEntityCommon.telejump(mirror, collision, mirror.spawnCoords)
        }
        val trail = setOf(MirrorBlocker.asRSCM(RSCMType.LOC), MirrorUnblocker.asRSCM(RSCMType.LOC))
        val tiles =
            MisthalinCoords.MirrorBoxX.flatMap { x ->
                MisthalinCoords.MirrorBoxZ.map { z -> r[CoordGrid(x, z, 0)] }
            } + r[MisthalinCoords.MirrorBlockerSentinel]
        for (tile in tiles) {
            val loc = locRepo.findExact(tile, LocShape.CentrepieceStraight) ?: continue
            if (loc.id in trail) {
                locRepo.del(loc, Int.MAX_VALUE)
            }
        }
    }

    private fun npcType(type: String): NpcServerType =
        ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC)) ?: error("Missing npc type: $type")

    private val abigaleUnmaskedType: NpcServerType by lazy { npcType(AbigaleUnmasked) }

    private val abigaleBodyType: NpcServerType by lazy { npcType(AbigaleBody) }

    private val heweyUnmaskedType: NpcServerType by lazy { npcType(HeweyUnmasked) }

    private val mandyGodswordType: NpcServerType by lazy { npcType(MandyGodsword) }

    private fun spawn(type: String, coords: CoordGrid): Npc {
        val npc = Npc(type, coords)
        npcRepo.add(npc, SceneLifespan)
        npc.noneMode()
        return npc
    }

    private fun spawnPersistent(type: String, coords: CoordGrid, facing: Direction? = null): Npc {
        val npc = Npc(type, coords)
        if (facing != null) {
            npc.respawnDir = facing
        }
        npcRepo.add(npc, Int.MAX_VALUE)
        npc.noneMode()
        return npc
    }

    private fun takeGodswordProp(r: SceneRegion) {
        val propType = BgsProp.asRSCM(RSCMType.OBJ)
        objRepo
            .findAll(r[GodswordProp])
            .filter { it.type == propType }
            .toList()
            .forEach { objRepo.del(it, Int.MAX_VALUE) }
    }

    private fun ProtectedAccess.openWardrobe(tile: CoordGrid, angle: LocAngle) {
        locRepo.add(tile, WardrobeOpen, WardrobeOpenTicks, angle, LocShape.CentrepieceStraight)
        soundSynth("synth.creakydoor_open")
    }

    private fun Npc.walkTo(coords: CoordGrid) {
        PathingEntityCommon.teleport(this, collision, coords)
    }

    private fun reuseOrSpawn(type: String, coords: CoordGrid, facing: Direction? = null): Npc =
        npcRepo.findAll(coords).firstOrNull { it.isVisType(type) }
            ?: spawnPersistent(type, coords, facing)

    private fun despawn(npc: Npc) {
        if (!npc.isSlotAssigned) {
            return
        }
        npcRepo.del(npc, Int.MAX_VALUE)
    }

    private fun findProxy(r: SceneRegion): Npc? =
        npcRepo.findAll(ZoneKey.from(r[MisthalinCoords.BossProxy]), zoneRadius = 1).firstOrNull {
            it.isType(ProxyNpc)
        }

    private fun Npc.showReflectionDamage(reflection: Int) {
        val hitmarkId = ReflectionHitmark.asRSCM(RSCMType.HITMARK)
        showHitmark(
            Hitmark.fromNoSource(
                self = hitmarkId,
                source = hitmarkId,
                public = hitmarkId,
                damage = ReflectionDamage,
                delay = 0,
            )
        )

        val fill = HeadbarFills.getOrElse(reflection - 1) { HeadbarFills.last() }
        showHeadbar(
            Headbar.fromNoSource(
                self = ReflectionHeadbar.asRSCM(RSCMType.HEADBAR),
                public = ReflectionHeadbar.asRSCM(RSCMType.HEADBAR),
                startFill = fill,
                endFill = fill,
                startTime = 0,
                endTime = 0,
            )
        )
    }

    private suspend fun Dialogue.killerSays(mesanim: MesAnimType, text: String) {
        access.faceSquare(room(access.player)[MirrorKillerTile])
        chatNpcSpecific("Killer", AbigaleKiller, mesanim, text)
    }

    private suspend fun Dialogue.abigaleSays(mesanim: MesAnimType, text: String) {
        access.faceSquare(room(access.player)[MirrorKillerTile])
        chatNpcSpecific("Abigale", AbigaleUnmasked, mesanim, text)
    }

    private suspend fun Dialogue.heweyMasked(mesanim: MesAnimType, text: String) {
        access.faceSquare(room(access.player)[HeweyTile])
        chatNpcSpecific("Killer", HeweyKiller, mesanim, text)
    }

    private suspend fun Dialogue.heweySays(mesanim: MesAnimType, text: String) {
        access.faceSquare(room(access.player)[HeweyTile])
        chatNpcSpecific("Hewey", HeweyUnmasked, mesanim, text)
    }

    private suspend fun Dialogue.mandySays(at: CoordGrid, mesanim: MesAnimType, text: String) {
        access.faceSquare(at)
        chatNpcSpecific("Mandy", MandyGodsword, mesanim, text)
    }

    private val Dialogue.evilLaugh: MesAnimType
        get() = madlaugh

    private class PushDirection(val dx: Int, val dz: Int, val angle: Int)

    private companion object {
        private const val SapphireKey = "obj.mistmyst_sapphire_key"
        private const val KillerKnife = "obj.mistmyst_cutscene_knife"

        private const val AbigaleKiller = "npc.mistmyst_abigale_killer"

        private const val AbigaleBody = "npc.mistmyst_abigale_cutscene_multi"

        private const val AbigaleUnmasked = "npc.mistmyst_abigale_killer_unmasked"
        private const val HeweyKiller = "npc.mistmyst_hewey_killer"
        private const val HeweyUnmasked = "npc.mistmyst_hewey_killer_unmasked"
        private const val MandyGodsword = "npc.mistmyst_mandy_godsword"

        private const val ThrowingKnifeSpotanim = "spotanim.steel_tknife_travel"

        private const val KnifeStartHeight = 163
        private const val KnifeEndHeight = 146
        private const val KnifeAngle = 15
        private const val KnifeProgress = 11
        private const val KnifeDelay = 41
        private const val KnifeTrackedAdjust = 5
        private const val KnifeStep = 5
        private const val ReflectDelay = 82
        private const val ReflectAdjust = 10
        private const val ReflectStep = 10

        private const val MirrorBlocker = "loc.mistmyst_mirror_blocker"
        private const val MirrorUnblocker = "loc.mistmyst_mirror_unblocker"
        private const val WardrobeOpen = "loc.mistmyst_boss_wardrobe_open"

        private const val WardrobeOpenTicks = 5

        private const val MirrorCyclePeriod = 19
        private const val ThrowPhase = 14
        private const val CryPhase = 18

        // NPCs process before players; player-armed timers start counting on the next NPC pass.
        private const val MirrorLeadIn = 11

        private const val ThrowingWardrobes = 3

        private const val TelegraphSpotanim = "spotanim.dark_spec_spot"

        private const val ReflectionsToWin = 3

        private const val ReflectionHitmark = "hitmark.regular_damage_tint"
        private const val ReflectionDamage = 10

        private const val ReflectionHeadbar = "headbar.health_30"
        private val HeadbarFills = intArrayOf(22, 15, 8)

        private const val SceneLifespan = 3000

        private const val KnifeDespawnTicks = 500

        private val SapphireDoorTile = CoordGrid(1628, 4829, 0)

        private val SapphireInsideTile = CoordGrid(1627, 4829, 0)

        private const val MirrorNpc = "npc.mistmyst_mirror"
        private const val ProxyNpc = "npc.mistmyst_invisible_npc"

        private val SapphireLeaf =
            MisthalinDoorLeaf(
                tile = SapphireDoorTile,
                closed = "loc.mistmyst_door_sapphire",
                openLeaf = "loc.mistmyst_door_sapphire_inactive",
                openAngle = LocAngle.North,
                closedAngle = LocAngle.West,
            )

        private val MirrorKillerTile = CoordGrid(1623, 4831, 0)

        private val HeweyTile = CoordGrid(1624, 4831, 0)

        private val ConfrontationEntry = CoordGrid(1626, 4831, 0)
        private val ConfrontationExit = CoordGrid(1622, 4833, 0)

        private val HeweyEntranceEye = CoordGrid(1622, 4828, 0)

        private val ConfrontationVantage = CoordGrid(1627, 4829, 0)

        private val UnmaskVantage = CoordGrid(1623, 4830, 0)
        private val UnmaskCamera =
            SceneCamera(
                eye = CoordGrid(1619, 4824, 0),
                eyeHeight = 1250,
                lookAt = CoordGrid(1623, 4830, 0),
                lookAtHeight = 300,
            )

        private const val MandyAlive = "npc.mistmyst_mandy_vis"
        private val MandyKitchenStart = CoordGrid(1627, 4839, 0)
        private val MandyKitchenMid = CoordGrid(1627, 4841, 0)
        private val MandyGodswordTile = CoordGrid(1627, 4843, 0)
        private val GodswordProp = CoordGrid(1626, 4843, 0)

        private const val RescuePropTicks = 400
        private const val BgsProp = "obj.mistmyst_bgs_prop"
        private val MandyKitchenWardrobe = CoordGrid(1627, 4844, 0)

        private const val DeathAnim = "seq.human_death"

        private val RescueVantage = CoordGrid(1627, 4829, 0)
        private val RescueCamera =
            SceneCamera(
                eye = CoordGrid(1620, 4826, 0),
                eyeHeight = 1300,
                lookAt = CoordGrid(1626, 4833, 0),
                lookAtHeight = 150,
            )

        private val MandyStrikePost = CoordGrid(1623, 4832, 0)

        private val KnifeDropTile = CoordGrid(1624, 4830, 0)

        private val PushDirections =
            listOf(
                PushDirection(dx = 1, dz = 0, angle = constants.em_face_east),
                PushDirection(dx = -1, dz = 0, angle = constants.em_face_west),
                PushDirection(dx = 0, dz = 1, angle = constants.em_face_north),
                PushDirection(dx = 0, dz = -1, angle = constants.em_face_south),
            )

        private val Unattackable =
            listOf(
                "npc.mistmyst_mirror",
                "npc.mistmyst_mirror_fixed",
                "npc.mistmyst_mirror_movable",
                "npc.mistmyst_invisible_npc",
                "npc.mistmyst_abigale_killer",
                "npc.mistmyst_hewey_killer",
                "npc.mistmyst_abigale_cutscene_multi",
                "npc.mistmyst_abigale_killer_unmasked",
                "npc.mistmyst_abigale_killer_attackable",
                "npc.mistmyst_hewey_killer_unmasked",
            )
    }
}

private fun Int.sameSign(other: Int): Boolean = (this > 0 && other > 0) || (this < 0 && other < 0)
