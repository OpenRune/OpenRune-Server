package org.rsmod.content.quest.area.asgarnia.blackknights

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.instances.RegionLocal
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.content.quest.area.misthalin.SceneCamera
import org.rsmod.content.quest.area.misthalin.lockedScene
import org.rsmod.game.MapClock
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid

class BlackKnightsScene @Inject constructor(
    private val instances: InstanceManager,
    private val worldRepo: WorldRepository,
    private val clock: MapClock,
) {
    suspend fun sabotage(access: ProtectedAccess, item: String): Boolean = with(access) {
        if (!BlackKnightsProgress.canSabotage(this, item)) return false
        val returnTo = coords
        val area = InstanceArea.copyRegions(
            regionIds = listOf((47 shl 8) or 54),
            enterCoord = RegionLocal(0, VANTAGE.mx, VANTAGE.mz, VANTAGE.lx, VANTAGE.lz),
            exitCoord = returnTo,
            npcSpawns = listOf(
                InstanceNpc("npc.fortwitch", WITCH),
                InstanceNpc("npc.grillknight", CAPTAIN),
                InstanceNpc("npc.bkf_cat", CAT),
            ),
        )
        val spec = InstanceSpec(
            fee = 0,
            maxPlayers = 1,
            reclaimTicks = 100,
            graceTicks = 100,
            destroyWhenEmpty = true,
            area = area,
            settingsRowId = -1,
        )
        val created = instances.create(player, SCENE_KEY, spec, InstanceAccess.Private, mapClock)
        if (created !is InstanceManager.Result.Created) {
            mes("The scene is unavailable just now. Please try again.")
            return true
        }
        val session = created.session
        var entered = false
        try {
            if (!BlackKnightsProgress.throwCabbage(this, item)) return false
            VarPlayerIntMapSetter.set(player, "varbit.spy_cauldron_multi", 0)
            anim("seq.bkf_throw_cabbage")
            spotanim("spotanim.bkfortress_cabbage_thrown", height = 96)
            cabbageFlight(coords, CoordGrid(3031, 3507, 1), 130, 0, 96, 196, 55, 60)
            soundSynth("synth.bk_throw_cabbage")
            delay(5)
            fun at(source: CoordGrid) = checkNotNull(instances.resolveCoord(session, source))
            val actors = instances.npcsForInstance(session.id)
            val witch = actors.single { it.isType("npc.fortwitch") }
            val captain = actors.single { it.isType("npc.grillknight") }
            actors.forEach { it.defaultMode() }
            lockedScene(
                vantage = created.enter,
                faceAt = at(WITCH),
                camera = SceneCamera(
                    eye = at(CoordGrid(3035, 3499)),
                    eyeHeight = 1500,
                    lookAt = at(CoordGrid(3032, 3505)),
                    lookAtHeight = 300,
                    rate = 100,
                    rate2 = 100,
                ),
                returnTo = returnTo,
                exitCut = true,
                afterPositioning = {
                    instances.finalizeEntry(player, session, mapClock)
                    entered = true
                    midiSong("midi.silence")
                    midiJingle("jingle.bkf_cutscene")
                    delay(2)
                },
                teardown = {
                    if (instances.sessionForPlayer(player)?.id == session.id) {
                        instances.leave(player, session, mapClock)
                    }
                },
            ) {
                delay(11)
                startDialogue {
                    chatNpcSpecificNp("Witch", "npc.fortwitch", angry,
                        "Where has Greldo got to with that magic cabbage!")
                    soundSynth("synth.bk_cabbage_hit")
                    delay(3)
                    chatNpcSpecificNp("Black Knight Captain", "npc.grillknight", confused,
                        "What's that noise?")
                    chatNpcSpecificNp("Witch", "npc.fortwitch", shocked,
                        "Hopefully Greldo with the cabbage... yes, look here it co....NOOOOOoooo!")
                    delay(1)
                    cabbageFlight(at(CoordGrid(3032, 3507)), at(CAULDRON), 500, 50, 0, 100, 192, 128)
                    spotanimMap(worldRepo, "spotanim.bkfortress_cauldron_cabbage_bubbles", at(CAULDRON), delay = 100)
                    soundSynth("synth.bk_cabbage_land")
                    delay(5)
                    VarPlayerIntMapSetter.set(player, "varbit.spy_cauldron_multi", 1)
                    midiSong("midi.knightmare")
                    chatNpcSpecificNp("Witch", "npc.fortwitch", shocked, "My potion!")
                    witch.anim("seq.emote_cry")
                    delay(1)
                    chatNpcSpecificNp("Black Knight Captain", "npc.grillknight", worried,
                        "Oh boy, this doesn't look good!")
                    captain.anim("seq.emote_panic")
                    chatNpcSpecificNp("Black Cat", "npc.bkf_cat", neutral, "Meow!")
                }
            }
            BlackKnightsProgress.sabotage(this)
            startDialogue {
                chatPlayer(neutral,
                    "Looks like my work here is done. Seems like that's successfully sabotaged their little secret weapon plan.")
            }
            return true
        } finally {
            if (BlackKnightsProgress.stage(player) == 2) {
                VarPlayerIntMapSetter.set(player, "varbit.spy_cauldron_multi", 0)
            }
            if (instances.sessionForPlayer(player)?.id == session.id) {
                instances.leave(player, session, clock.cycle)
                telejump(returnTo)
            } else if (!entered) {
                instances.cancelPendingEntry(player, clock.cycle)
            }
        }
    }

    private fun cabbageFlight(
        from: CoordGrid, to: CoordGrid, startHeight: Int, endHeight: Int,
        startTime: Int, endTime: Int, angle: Int, progress: Int,
    ) {
        worldRepo.projAnim(ProjAnim(
            spotanim = "spotanim.bkfortress_cabbage_travel".asRSCM(RSCMType.SPOTANIM),
            startHeight = startHeight, endHeight = endHeight,
            startTime = startTime, endTime = endTime, angle = angle, progress = progress,
            sourceIndex = 0, targetIndex = 0, startCoord = from, endCoord = to,
        ))
    }

    private companion object {
        const val SCENE_KEY = "quest_blackknights_sabotage"
        val VANTAGE = CoordGrid(3017, 3497)
        val WITCH = CoordGrid(3030, 3509)
        val CAPTAIN = CoordGrid(3028, 3509)
        val CAT = CoordGrid(3027, 3507)
        val CAULDRON = CoordGrid(3031, 3507)
    }
}
