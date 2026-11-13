package org.rsmod.content.quest.area.asgarnia.icemountain

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.music.MusicPlayer
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.stopAmbience
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.table.MusicRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

@Singleton
class BimCutscene
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val musicPlayer: MusicPlayer,
) {
    fun ensureEntrance(stage: Int) {
        val entrance = CoordGrid(ENTRANCE_DOOR_X, ENTRANCE_DOOR_Z)
        locRepo.findAll(entrance).toList().forEach { loc ->
            when (loc.id) {
                CUTSCENE_DOOR_ID,
                RUBBLE_ID,
                ENTRANCE_ID,
                DOOR_ID,
                -> locRepo.del(loc, Int.MAX_VALUE)
            }
        }
        if (stage >= BimStage.INSIDE_CUTSCENE) {
            locRepo.add(entrance, DOOR, Int.MAX_VALUE, ENTRANCE_ANGLE, ENTRANCE_SHAPE)
        } else {
            locRepo.add(entrance, ENTRANCE, Int.MAX_VALUE, ENTRANCE_ANGLE, ENTRANCE_SHAPE)
        }
    }

    suspend fun playEntrance(access: ProtectedAccess, quest: BelowIceMountain) {
        access.beginCutscene()
        val player = access.player
        player.bimStage = BimStage.ENTRANCE_CUTSCENE
        removeBaseCrew()
        access.telejump(ENTRANCE_PLAYER_TILE)
        access.faceSquare(ENTRANCE_DOOR_TILE)
        val willow = access.spawn(WILLOW_CUTSCENE, ENTRANCE_WILLOW_TILE, Direction.West)
        val burntof = access.spawn(BURNOTOF_CUTSCENE, ENTRANCE_BURNOTOF_TILE, Direction.NorthEast)
        val checkal = access.spawn(CHECKAL_CUTSCENE, ENTRANCE_CHECKAL_TILE, Direction.East)
        val marley = access.spawn(MARLEY_CUTSCENE, ENTRANCE_MARLEY_TILE, Direction.SouthEast)

        access.camMoveToV3(CoordGrid(2990, 3490), height = 600, rate = SNAP, rate2 = SNAP)
        access.camLookAtV3(CoordGrid(2997, 3494), height = 400, rate = SNAP, rate2 = SNAP)
        playEntranceMusic(player)
        access.fadeOverlay(0, 0, 0, 255, 50)
        access.delay(2)
        access.closeFadeOverlay(cycles = 2)

        access.startDialogue {
            chatNpcSpecific("Willow", WILLOW_CUTSCENE, happy, "Right, that's everyone gathered. " +
                "Thank you, ${player.displayName}, for all your help.")
            chatPlayer(neutral, "Oh it was no pro...")
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "Okay everyone, we've wasted enough time. Let's crack this door open. Burntof, " +
                    "you're up!",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "Try not to blow the entire mountain up this time...",
            )
            chatNpcSpecific(
                "Burntof",
                BURNOTOF_CUTSCENE,
                drunk,
                "Yesh ma'am, let'sh get this rock out the way.",
            )
        }

        access.fadeOverlay(0, 255, 0, 0, 15)
        access.delay(2)
        val fuse = access.spawn(FUSE, ENTRANCE_FUSE_TILE, Direction.South)
        fuse.say("Tsssss...")
        access.camMoveToV3(CoordGrid(2989, 3501), height = 700, rate = SNAP, rate2 = SNAP)
        access.camLookAtV3(CoordGrid(2997, 3494), height = 350, rate = SNAP, rate2 = SNAP)

        access.startDialogue {
            access.soundSynth("synth.bim_dynamite_preparation_01")
            chatNpcSpecific(
                "Burntof",
                BURNOTOF_CUTSCENE,
                drunk,
                "Jusht got to plashe thish here...",
            )
            access.soundSynth("synth.bim_dynamite_preparation_02")
            fuse.say("Tsssss...")
            chatNpcSpecific(
                "Burntof",
                BURNOTOF_CUTSCENE,
                drunk,
                "... little bit of dyn-*hic*-amite here...",
            )
            access.soundSynth("synth.bim_dynamite_preparation_03")
            fuse.say("Tsssss...")
            chatNpcSpecific("Burntof", BURNOTOF_CUTSCENE, drunk, "... and there we go!")
            access.soundSynth("synth.bim_dynamite_lighting_fuse_02")
            fuse.say("Tsssss...")
            chatNpcSpecific("Burntof", BURNOTOF_CUTSCENE, drunk, "Now we light the fushe!")
        }

        access.soundSynth("synth.bim_dynamite_hiss")
        access.delay(2)
        access.fadeOverlay(0, 0, 0, 255, 50)
        access.delay(2)
        access.closeFadeOverlay(cycles = 2)

        access.startDialogue {
            fuse.say("Tsssss...")
            chatNpcSpecific(
                "Burntof",
                BURNOTOF_CUTSCENE,
                worried,
                "What are you doin' standin' there?!",
            )
            chatPlayer(quiz, "What do you mean?")
            chatNpcSpecific(
                "Burntof",
                BURNOTOF_CUTSCENE,
                worried,
                "Everyone'sh ran for cover ${if (access.isBodyTypeA()) "lad" else "lass"}. " +
                    "You'd best do the shame.",
            )
            chatPlayer(confused, "Wait, what?!")
            fuse.say("Tsssss...")
            chatNpcSpecific(
                "Burntof",
                BURNOTOF_CUTSCENE,
                worried,
                "Clock'sh ticking ${if (access.isBodyTypeA()) "lad" else "lass"}!",
            )
        }

        access.soundSynth("synth.bim_dynamite_timer")
        fuse.say("Tsssss...")
        access.say("Aaah!")
        access.cinematicStep(CoordGrid(2993, 3496))
        access.cinematicStep(CoordGrid(2993, 3498))
        access.cinematicStep(CoordGrid(2992, 3500))
        access.cinematicStep(CoordGrid(2991, 3501))

        access.camMoveToV3(CoordGrid(2991, 3500), height = 800, rate = 1, rate2 = 1)
        access.camLookAtV3(CoordGrid(2997, 3494), height = 400, rate = SNAP, rate2 = SNAP)
        access.fadeOverlay(0, 255, 0, 0, 15)
        access.delay(2)
        addExplosionLoc(0)
        worldRepo.soundArea(
            CoordGrid(2996, 3493),
            "synth.bim_wall_dynamite_explosions",
            delay = 5,
            radius = 15,
        )
        access.camShake(CamShakeAxis.LEFT_RIGHT, random = 75, amplitude = 0, rate = 0)
        access.delay(5)
        addExplosionLoc(1)
        worldRepo.soundArea(
            CoordGrid(2995, 3492),
            "synth.bim_wall_dynamite_explosions_p2",
            delay = 5,
            radius = 15,
        )
        access.delay(5)
        addExplosionLoc(2)
        access.delay(3)
        despawn(listOf(fuse))
        access.telejump(CoordGrid(2996, 3496))
        swapEntranceToCutsceneDoor()
        access.camReset()
        access.camMoveToV3(CoordGrid(2990, 3490), height = 600, rate = SNAP, rate2 = SNAP)
        access.camLookAtV3(CoordGrid(2997, 3494), height = 400, rate = SNAP, rate2 = SNAP)
        access.fadeOverlay(0, 0, 0, 255, 50)
        access.delay(2)
        access.closeFadeOverlay(cycles = 2)

        access.startDialogue {
            chatNpcSpecific("Marley", MARLEY_CUTSCENE, shocked, "Bloomin' 'eck, Burntof!")
            chatNpcSpecific(
                "Checkal",
                CHECKAL_CUTSCENE,
                worried,
                "That was a tad too explosive I think.",
            )
            chatPlayer(worried, "My ears are still ringing!")
            chatNpcSpecific(
                "Burntof",
                BURNOTOF_CUTSCENE,
                drunk,
                "Quit yer moanin', it did the job didnae it?",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "He's right, the rock's been broken up. Checkal, it's your turn.",
            )
            chatNpcSpecific(
                "Checkal",
                CHECKAL_CUTSCENE,
                neutral,
                "Of course ma'am, I will get to work.",
            )
        }

        access.fadeOverlay(0, 255, 0, 0, 15)
        access.delay(2)
        removeRubble()
        access.fadeOverlay(0, 0, 0, 255, 50)
        access.delay(2)
        access.closeFadeOverlay(cycles = 2)

        access.startDialogue {
            chatNpcSpecific(
                "Checkal",
                CHECKAL_CUTSCENE,
                happy,
                "The work is done, Willow. My apologies if it took too long.",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                happy,
                "Don't be sorry, Checkal. Gotta put those muscles to use somehow.",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "Alright Marley, in you go. Let us know if you find anything.",
            )
            chatNpcSpecific(
                "Marley",
                MARLEY_CUTSCENE,
                neutral,
                "Right, in I go. Wish me luck ${player.displayName}.",
            )
            chatPlayer(worried, "Try not to die...")
        }

        // Marley disarms the traps inside while the player watches from the corridor.
        access.telejump(CoordGrid(2952, 5763))
        despawn(listOf(willow, burntof, checkal, marley))
        locRepo
            .findAll(CoordGrid(2950, 5769))
            .filter { it.id == BAG_CUTSCENE_ID }
            .toList()
            .forEach { locRepo.del(it, Int.MAX_VALUE) }
        ensureLoc(
            CoordGrid(2951, 5761),
            CUTSCENE_EXIT,
            LocAngle[2],
            LocShape.CentrepieceStraight,
        )
        access.addTripwire()
        access.camMoveToV3(CoordGrid(2954, 5771), height = 500, rate = SNAP, rate2 = SNAP)
        access.camLookAtV3(CoordGrid(2952, 5764), height = 250, rate = SNAP, rate2 = SNAP)
        access.cinematicStep(CoordGrid(2952, 5764))
        access.camLookAtV3(CoordGrid(2952, 5764), height = 250, rate = 1, rate2 = 1)
        access.camMoveToV3(CoordGrid(2954, 5772), height = 500, rate = 1, rate2 = 1)
        access.cinematicStep(CoordGrid(2952, 5765))
        access.camLookAtV3(CoordGrid(2952, 5766), height = 250, rate = 1, rate2 = 1)
        access.camMoveToV3(CoordGrid(2954, 5773), height = 500, rate = 1, rate2 = 1)
        access.cinematicStep(CoordGrid(2952, 5766))
        access.cinematicStep(CoordGrid(2952, 5767))
        access.startDialogue {
            access.soundSynth("synth.bim_disarm_trap")
            chatNpcSpecific(
                "Marley",
                MARLEY_CUTSCENE,
                neutral,
                "Right, let's see what hidden surprises you have in store for me.",
            )
            access.soundSynth("synth.bim_disarm_trap_failure_01")
            chatNpcSpecific("Marley", MARLEY_CUTSCENE, neutral, "Hmm...")
            chatNpcSpecific(
                "Marley",
                MARLEY_CUTSCENE,
                happy,
                "That would have been nasty.",
            )
            chatNpcSpecific(
                "Marley",
                MARLEY_CUTSCENE,
                neutral,
                "Is that it? I thought this place would be better protected.",
            )
            chatNpcSpecific("Marley", MARLEY_CUTSCENE, neutral, "Now to disarm it...")
            access.soundSynth("synth.bim_disarm_trap_failure_02")
            access.soundSynth("synth.bim_disarm_trap_failure_03")
            chatNpcSpecific("Marley", MARLEY_CUTSCENE, neutral, "Maybe this will work?")
            access.soundSynth("synth.bim_disarm_trap")
            chatNpcSpecific("Marley", MARLEY_CUTSCENE, happy, "That got it!")
            chatNpcSpecific(
                "Marley",
                MARLEY_CUTSCENE,
                happy,
                "Alright guys! It's safe to come in! I think...",
            )
        }
        access.removeTripwire()

        access.fadeOverlay(0, 255, 0, 0, 15)
        access.delay(2)
        access.telejump(INSIDE_PLAYER_TILE)
        access.faceSquare(INSIDE_CENTER)
        despawn(listOf(willow, burntof, checkal, marley))
        access.camReset()
        player.bimStage = BimStage.INSIDE_CUTSCENE
        player.bimCheckal = BimCheckal.AT_ENTRANCE
        player.bimMarley = BimMarley.AT_ENTRANCE
        player.bimBurntof = BimBurntof.AT_ENTRANCE
        access.fadeOverlay(0, 0, 0, 255, 50)
        access.delay(2)
        access.closeFadeOverlay(cycles = 2)
        access.endCutscene()
        ensureEntrance(BimStage.INSIDE_CUTSCENE)
    }

    suspend fun playReveal(
        access: ProtectedAccess,
        quest: BelowIceMountain,
        spawnBoss: () -> Unit,
    ) {
        access.beginCutscene()
        removeEntranceRamarno()
        val player = access.player
        access.telejump(REVEAL_PLAYER_TILE)
        access.faceSquare(INSIDE_CENTER)

        val willow = access.spawn(WILLOW_CUTSCENE, INSIDE_WILLOW_TILE, Direction.South)
        val checkal = access.spawn(CHECKAL_CUTSCENE, INSIDE_CHECKAL_TILE, Direction.West)
        val marley = access.spawn(MARLEY_CUTSCENE, INSIDE_MARLEY_TILE, Direction.SouthEast)
        val burntof = access.spawn(BURNOTOF_CUTSCENE, INSIDE_BURNOTOF_TILE, Direction.SouthEast)
        val rubble = access.spawn(RUBBLE_CUTSCENE, BOSS_TILE, Direction.South)

        addRevealProps()
        access.camMoveToV3(CoordGrid(2952, 5761), height = 200, rate = SNAP, rate2 = SNAP)
        access.camLookAtV3(CoordGrid(2952, 5768), height = 50, rate = SNAP, rate2 = SNAP)
        playRuinsMusic(player)
        access.fadeOverlay(0, 0, 0, 255, 50)
        access.delay(2)
        access.closeFadeOverlay(cycles = 2)
        access.camMoveToV3(CoordGrid(2952, 5763), height = 800, rate = 2, rate2 = 2)
        access.camLookAtV3(CoordGrid(2952, 5771), height = 50, rate = 1, rate2 = 1)

        access.startDialogue {
            chatPlayer(neutral, "Wow, this place is amazing. I can't believe this was hidden away.")
            chatPlayer(neutral, "I wonder how long it's been left untouched for.")
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "You're impressed? There's nothing here but dust and fallen rocks.",
            )
            burntof.say("Yaaa*hic*aaawn")
        }

        access.camMoveToV3(CoordGrid(2959, 5768), height = 800, rate = 2, rate2 = 2)

        access.startDialogue {
            chatPlayer(
                quiz,
                "You don't find it exciting? As an archaeologist, I thought you would be " +
                    "thrilled at this discovery?",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                angry,
                "Thrilled? What's there to be excited about? I don't see any gold, diamonds, " +
                    "rare gems or piles of lost treasure!",
            )
            burntof.say("Hmm...")
            chatPlayer(
                neutral,
                "It's not about the money though, right? We've potentially discovered the long " +
                    "lost ruins of an ancient civilisation!",
            )
        }

        access.camMoveToV3(CoordGrid(2952, 5763), height = 800, rate = 2, rate2 = 2)
        access.camLookAtV3(CoordGrid(2952, 5773), height = 70, rate = 1, rate2 = 1)

        access.startDialogue {
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                angry,
                "Of course it's about the money. It's always been about money, you fool.",
            )
            chatPlayer(confused, "I don't understand...")
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                angry,
                "We're not a team of archaeologist, you idiot. We're grave robbers looking for " +
                    "their next score.",
            )
            burntof.say("Theshe markings look familiar...")
            chatPlayer(
                sad,
                "So this entire time, you've just been using me to help break into these ruins " +
                    "in hopes of finding someone else's treasures.",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                happy,
                "Yep, and you were surprisingly easy to convince. I'll admit, you have been a " +
                    "tremendous help, ${player.displayName}.",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "But your usefulness has come to an end I'm afraid. I can't simply let you leave here.",
            )
            chatPlayer(quiz, "Why not?")
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                neutral,
                "I can't risk you running to the authorities and telling them about our little " +
                    "gang. We're wanted in quite a few large cities across Gielinor.",
            )
            burntof.say("I've sheen thish before...")
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                angry,
                "I'm afraid this is the end of the road for you, ${player.displayName}. This " +
                    "ruin is about to become your tomb.",
            )
            chatNpcSpecific("Burntof", BURNOTOF_CUTSCENE, drunk, "*hic* Woops...")
            chatNpcSpecific("Willow", WILLOW_CUTSCENE, confused, "What was that noise?")
            chatNpcSpecific("Burntof", BURNOTOF_CUTSCENE, worried, "Uh... it wasn't me!")
        }

        access.camMoveToV3(CoordGrid(2946, 5768), height = 700, rate = 5, rate2 = 5)
        access.camLookAtV3(CoordGrid(2953, 5774), height = 100, rate = 5, rate2 = 5)
        access.camShake(CamShakeAxis.LEFT_RIGHT, random = 50, amplitude = 0, rate = 0)
        access.soundSynth("synth.bim_dungeon_rumble")
        access.mapAnim(QUAKE_1, QUAKE_COORD_1, 160)
        access.mapAnim(QUAKE_2, QUAKE_COORD_2, 0)
        access.mapAnim(QUAKE_3, QUAKE_COORD_3, 180)
        access.mapAnim(QUAKE_4, QUAKE_COORD_4, 50)
        access.mapAnim(QUAKE_5, QUAKE_COORD_5, 90)
        access.delay(3)
        access.startDialogue {
            chatNpcSpecific("Willow", WILLOW_CUTSCENE, worried, "Whats happening?!")
        }
        access.mapAnim(QUAKE_2, QUAKE_COORD_2, 0)
        access.mapAnim(QUAKE_5, QUAKE_COORD_5, 50)
        access.mapAnim(QUAKE_4, QUAKE_COORD_4, 90)
        access.camShake(CamShakeAxis.LEFT_RIGHT, random = 20, amplitude = 0, rate = 0)
        access.delay(3)
        access.startDialogue {
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                worried,
                "It feels like the quaking has finally stopped...",
            )
        }
        access.mapAnim(QUAKE_1, QUAKE_COORD_1, 50)
        access.mapAnim(QUAKE_3, QUAKE_COORD_3, 0)
        access.camShake(CamShakeAxis.LEFT_RIGHT, random = 5, amplitude = 0, rate = 0)
        access.camReset()
        access.camMoveToV3(CoordGrid(2946, 5768), height = 700, rate = SNAP, rate2 = SNAP)
        access.camLookAtV3(CoordGrid(2953, 5774), height = 100, rate = SNAP, rate2 = SNAP)
        access.camMoveToV3(CoordGrid(2948, 5771), height = 700, rate = 1, rate2 = 1)
        access.camLookAtV3(CoordGrid(2953, 5778), height = 100, rate = 1, rate2 = 1)

        despawn(listOf(rubble))
        val guardian = access.spawn(GUARDIAN_CUTSCENE, BOSS_TILE, Direction.South)
        guardian.anim("seq.ancient_guardian_awaken")
        access.camShake(CamShakeAxis.LEFT_RIGHT, random = 5, amplitude = 0, rate = 0)
        access.soundSynth("synth.bim_ancient_guardian_awaken")
        access.camMoveToV3(CoordGrid(2948, 5771), height = 700, rate = SNAP, rate2 = SNAP)
        access.camLookAtV3(CoordGrid(2952, 5778), height = 150, rate = 2, rate2 = 2)

        access.startDialogue {
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                shocked,
                "Holy Zamorak! What is that thing?! Checkal, take it down!",
            )
            chatNpcSpecific(
                "Checkal",
                CHECKAL_CUTSCENE,
                worried,
                "I'm a muscle man, not a fighter.",
            )
            chatNpcSpecific(
                "Marley",
                MARLEY_CUTSCENE,
                worried,
                "I think we should get out of here, Willow!",
            )
            chatNpcSpecific(
                "Willow",
                WILLOW_CUTSCENE,
                worried,
                "You're right, Marley. Burntof, we're leaving!",
            )
        }
        access.fadeOverlay(0, 255, 0, 0, 15)
        access.delay(2)
        despawn(listOf(willow, checkal, marley, burntof, guardian))
        player.bimStage = BimStage.GUARDIAN_FIGHT
        player.bimCheckal = BimCheckal.INSIDE
        player.bimMarley = BimMarley.INSIDE
        player.bimBurntof = BimBurntof.INSIDE
        spawnBoss()
        access.camReset()
        access.fadeOverlay(0, 0, 0, 255, 50)
        access.delay(2)
        access.closeFadeOverlay(cycles = 2)
        access.endCutscene()
        access.startDialogue {
            chatPlayer(
                neutral,
                "I can't believe they ran so quickly. I suppose I should take care of this thing.",
            )
        }
    }

    private suspend fun ProtectedAccess.beginCutscene() {
        fadeOverlay(0, 255, 0, 0, 50)
        delay(2)
        hideEntityOps()
        hideTopLevel()
        hideHealthHud()
        clearHealthHud()
        minimapHideMap()
        camModeClose()
        maxDrawDistance(true)
        tempDisableAcceptAid()
        closeTopLevelTabsLenient()
        syncDrawDistance()
        camShakeResetAll()
        player.stopAmbience()
    }

    private fun ProtectedAccess.endCutscene() {
        showEntityOps()
        restoreLastAcceptAid()
        maxDrawDistance(false)
        minimapReset()
        camModeReset()
        showTopLevel()
        showHealthHud()
        camReset()
        openTopLevelTabs()
        syncDrawDistance()
    }

    private fun ProtectedAccess.mapAnim(spotanim: String, coords: CoordGrid, delay: Int) {
        worldRepo.spotanimMap(
            SpotanimType(spotanim.asRSCM(RSCMType.SPOTANIM)),
            coords,
            height = 0,
            delay = delay,
        )
    }

    private fun playRuinsMusic(player: org.rsmod.game.entity.Player) {
        val row =
            MusicRow.all().firstOrNull { it.displayname == RUINS_MUSIC } ?: return
        musicPlayer.unlockAndPlay(player, row)
    }

    private fun playEntranceMusic(player: org.rsmod.game.entity.Player) {
        val row = MusicRow.all().firstOrNull { it.midi.id == ENTRANCE_MUSIC_ID } ?: return
        musicPlayer.unlockAndPlay(player, row)
    }

    private fun ProtectedAccess.cinematicStep(dest: CoordGrid) {
        val start = coords
        val dx = dest.x - start.x
        val dz = dest.z - start.z
        val dir =
            when {
                dx > 0 && dz > 0 -> 1280
                dx > 0 && dz < 0 -> 1792
                dx < 0 && dz > 0 -> 768
                dx < 0 && dz < 0 -> 256
                dx > 0 -> 1536
                dx < 0 -> 512
                dz > 0 -> 1024
                else -> 0
            }
        exactMove(start, dest, delay1 = 0, delay2 = 1, dir = dir, teleportType = TeleportType.Exempt)
    }

    private fun addExplosionLoc(index: Int) {
        locRepo.add(
            EXPLOSION_TILES[index],
            "loc.barbassault_egg_explosion",
            3,
            LocAngle[0],
            LocShape.CentrepieceStraight,
        )
    }

    private fun removeBaseCrew() {
        for (type in BASE_CREW) {
            for (tile in BASE_CREW_TILES) {
                npcRepo
                    .findAll(tile)
                    .filter { it.isType(type) }
                    .toList()
                    .forEach { npcRepo.del(it, Int.MAX_VALUE) }
            }
        }
    }

    private fun removeEntranceRamarno() {
        npcRepo
            .findAll(CoordGrid(2951, 5779))
            .filter { it.isType("npc.camdozaal_ramarno_entrance_multi") }
            .toList()
            .forEach { npcRepo.del(it, Int.MAX_VALUE) }
    }

    private fun swapEntranceToCutsceneDoor() {
        val entrance = CoordGrid(ENTRANCE_DOOR_X, ENTRANCE_DOOR_Z)
        locRepo.findAll(entrance).toList().forEach { locRepo.del(it, Int.MAX_VALUE) }
        locRepo.add(
            entrance,
            CUTSCENE_DOOR,
            Int.MAX_VALUE,
            ENTRANCE_ANGLE,
            ENTRANCE_SHAPE,
        )
        locRepo.add(
            CoordGrid(2998, 3493),
            RUBBLE,
            Int.MAX_VALUE,
            LocAngle[1],
            LocShape.CentrepieceStraight,
        )
    }

    private fun removeRubble() {
        locRepo.findAll(CoordGrid(2998, 3493)).filter { it.id == RUBBLE_ID }.toList().forEach {
            locRepo.del(it, Int.MAX_VALUE)
        }
    }

    private fun addRevealProps() {
        ensureLoc(
            CoordGrid(2951, 5780),
            CLOSED_DOOR,
            LocAngle[0],
            LocShape.CentrepieceStraight,
        )
        for ((coords, angle) in REVEAL_PILLARS) {
            ensureLoc(coords, PILLAR_CUTSCENE, angle, LocShape.CentrepieceStraight)
        }
        ensureLoc(
            CoordGrid(2950, 5769),
            "loc.bim_willows_bag_cutscene",
            LocAngle[3],
            LocShape.CentrepieceDiagonal,
        )
    }

    private fun ensureLoc(coords: CoordGrid, loc: String, angle: LocAngle, shape: LocShape) {
        if (locRepo.findAll(coords).any { it.id == loc.asRSCM(RSCMType.LOC) }) {
            return
        }
        locRepo.add(coords, loc, Int.MAX_VALUE, angle, shape)
    }

    private fun ProtectedAccess.addTripwire() {
        locRepo.add(
            CoordGrid(2952, 5766),
            TRIPWIRE,
            Int.MAX_VALUE,
            LocAngle[0],
            LocShape.CentrepieceStraight,
        )
        locRepo.add(
            CoordGrid(2953, 5766),
            TRIPWIRE,
            Int.MAX_VALUE,
            LocAngle[0],
            LocShape.CentrepieceStraight,
        )
    }

    private fun ProtectedAccess.removeTripwire() {
        for (coords in listOf(CoordGrid(2952, 5766), CoordGrid(2953, 5766))) {
            locRepo.findAll(coords).filter { it.id == TRIPWIRE_ID }.toList().forEach {
                locRepo.del(it, Int.MAX_VALUE)
            }
        }
    }

    private fun ProtectedAccess.spawn(type: String, coords: CoordGrid, facing: Direction): Npc {
        val npc = Npc(type, coords)
        npc.respawnDir = facing
        npc.mode = NpcMode.None
        npcRepo.add(npc, CUTSCENE_LIFESPAN)
        return npc
    }

    private fun despawn(npcs: List<Npc>) {
        for (npc in npcs) {
            if (npc.isSlotAssigned) {
                npcRepo.del(npc, Int.MAX_VALUE)
            }
        }
    }

    private companion object {
        const val SNAP = 232
        const val CUTSCENE_LIFESPAN = 500
        const val ENTRANCE_DOOR_X = 2999
        const val ENTRANCE_DOOR_Z = 3493
        val ENTRANCE_ANGLE = LocAngle[1]
        val ENTRANCE_SHAPE = LocShape.CentrepieceStraight

        const val WILLOW_CUTSCENE = "npc.bim_cutscene_willow"
        const val CHECKAL_CUTSCENE = "npc.bim_cutscene_checkal"
        const val MARLEY_CUTSCENE = "npc.bim_cutscene_marley"
        const val BURNOTOF_CUTSCENE = "npc.bim_cutscene_burntof"
        const val RUBBLE_CUTSCENE = "npc.bim_golem_boss_inactive_cutscene"
        const val GUARDIAN_CUTSCENE = "npc.bim_golem_guardian_cutscene"

        const val ENTRANCE = "loc.bim_entrance"
        const val DOOR = "loc.bim_door"
        const val CUTSCENE_DOOR = "loc.bim_door_cutscene"
        const val RUBBLE = "loc.bim_rubble_3"
        const val TRIPWIRE = "loc.bim_tripwire"
        const val CUTSCENE_EXIT = "loc.bim_exit_cutscene"
        const val BAG_CUTSCENE = "loc.bim_willows_bag_cutscene"
        const val CLOSED_DOOR = "loc.bim_ruins_door_closed"
        const val PILLAR_CUTSCENE = "loc.bim_boss_rock_cutscene"

        val ENTRANCE_ID = ENTRANCE.asRSCM(RSCMType.LOC)
        val DOOR_ID = DOOR.asRSCM(RSCMType.LOC)
        val CUTSCENE_DOOR_ID = CUTSCENE_DOOR.asRSCM(RSCMType.LOC)
        val RUBBLE_ID = RUBBLE.asRSCM(RSCMType.LOC)
        val TRIPWIRE_ID = TRIPWIRE.asRSCM(RSCMType.LOC)
        val BAG_CUTSCENE_ID = BAG_CUTSCENE.asRSCM(RSCMType.LOC)

        val ENTRANCE_DOOR_TILE = CoordGrid(2999, 3493)
        val ENTRANCE_PLAYER_TILE = CoordGrid(2995, 3495)
        val ENTRANCE_FUSE_TILE = CoordGrid(2999, 3494)
        val EXPLOSION_TILES =
            listOf(
                CoordGrid(2998, 3492),
                CoordGrid(2998, 3493),
                CoordGrid(2998, 3494),
            )
        val ENTRANCE_WILLOW_TILE = CoordGrid(2997, 3494)
        val ENTRANCE_BURNOTOF_TILE = CoordGrid(2996, 3492)
        val ENTRANCE_CHECKAL_TILE = CoordGrid(2994, 3494)
        val ENTRANCE_MARLEY_TILE = CoordGrid(2996, 3496)

        val INSIDE_PLAYER_TILE = CoordGrid(2953, 5769)
        val REVEAL_PLAYER_TILE = CoordGrid(2953, 5769)
        val INSIDE_CENTER = CoordGrid(2953, 5772)
        val INSIDE_WILLOW_TILE = CoordGrid(2953, 5771)
        val INSIDE_CHECKAL_TILE = CoordGrid(2956, 5770)
        val INSIDE_MARLEY_TILE = CoordGrid(2952, 5772)
        val INSIDE_BURNOTOF_TILE = CoordGrid(2950, 5772)
        val BOSS_TILE = CoordGrid(2952, 5778)

        val BASE_CREW =
            listOf(
                "npc.bim_willow_outside_entrace",
                "npc.bim_checkal_temple_outside",
                "npc.bim_marley_temple_outside",
                "npc.bim_burntof_temple_outside",
            )

        val BASE_CREW_TILES =
            listOf(
                CoordGrid(2997, 3494),
                CoordGrid(2994, 3494),
                CoordGrid(2992, 3497),
                CoordGrid(2997, 3489),
            )

        val QUAKE_1 = "spotanim.rockfall"
        val QUAKE_2 = "spotanim.rockfall"
        val QUAKE_3 = "spotanim.rockfall"
        val QUAKE_4 = "spotanim.rockfall"
        val QUAKE_5 = "spotanim.rockfall"
        val QUAKE_COORD_1 = CoordGrid(2957, 5772)
        val QUAKE_COORD_2 = CoordGrid(2956, 5776)
        val QUAKE_COORD_3 = CoordGrid(2953, 5777)
        val QUAKE_COORD_4 = CoordGrid(2950, 5775)
        val QUAKE_COORD_5 = CoordGrid(2950, 5772)
        val REVEAL_PILLARS =
            listOf(
                CoordGrid(2947, 5771) to LocAngle[3],
                CoordGrid(2958, 5771) to LocAngle[1],
                CoordGrid(2947, 5778) to LocAngle[3],
                CoordGrid(2958, 5778) to LocAngle[1],
            )
        const val RUINS_MUSIC = "The Ruins of Camdozaal"
        const val ENTRANCE_MUSIC_ID = 102
        const val FUSE = "npc.bim_invisnpc"
    }
}
