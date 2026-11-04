package org.rsmod.content.quest.area.corsaircove

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfSubType
import org.rsmod.api.config.constants
import org.rsmod.api.instances.InstanceAttributes
import org.rsmod.api.player.cinematic.SceneCamera
import org.rsmod.api.player.cinematic.SceneFadeDuration
import org.rsmod.api.player.cinematic.SceneFadeTicks
import org.rsmod.api.player.cinematic.SceneRegion
import org.rsmod.api.player.cinematic.SceneSnapRate
import org.rsmod.api.player.cinematic.lockedScene
import org.rsmod.api.player.cinematic.revealScene
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal class CorsairCurseCutscene(
    private val regionRepo: RegionRepository,
    private val npcRepo: NpcRepository,
    private val collision: CollisionFlagMap,
    private val worldRepo: WorldRepository,
) {

    private val sceneRegions = HashMap<Long, Region>()

    private fun discardLeakedRegion(player: Player) {
        val region = player.uuid?.let(sceneRegions::remove) ?: return
        regionRepo.unprotect(region)
    }

    fun ScriptContext.register() {
        onAiTimer(CorsairCurseRefs.CookFishing) { npc.anim(LobsterAnim) }

        onPlayerLogin { discardLeakedRegion(player) }

        cookDiggingType

        checkNotNull(
            ServerCacheManager.getInterface(SpyglassInterface.asRSCM(RSCMType.INTERFACE))
        ) {
            "Missing interface type: $SpyglassInterface"
        }
    }

    private val cookDiggingType by lazy {
        ServerCacheManager.getNpc(CorsairCurseRefs.CookDigging.asRSCM(RSCMType.NPC))
            ?: error("Missing npc type: ${CorsairCurseRefs.CookDigging}")
    }

    suspend fun rimmingtonFlyby(access: ProtectedAccess) {
        access.lockedScene(
            vantage = RimmingtonFlyby.Vantage,
            faceAt = RimmingtonFlyby.FaceAt,
            camera = RimmingtonFlyby.Opening.toCamera(),
            returnTo = access.player.coords,
            fadeOutDuration = FlybyFadeOutDuration,
            underFade = { access.hideFromShot() },
            teardown = { access.showInShot() },
        ) {
            val dolly = RimmingtonFlyby.Dolly
            camMoveToV3(
                dolly.eye,
                height = dolly.eyeHeight,
                rate = FlybyDollyRate,
                rate2 = FlybyRate2,
            )
            camLookAtV3(
                dolly.lookAt,
                height = dolly.lookAtHeight,
                rate = FlybyDollyRate,
                rate2 = FlybyRate2,
            )
            delay(FlybyDollyTicks)

            val pan = RimmingtonFlyby.Pan
            camLookAtV3(
                pan.lookAt,
                height = pan.lookAtHeight,
                rate = FlybyPanRate,
                rate2 = FlybyRate2,
            )
            delay(FlybyPanTicks)
        }
    }

    suspend fun colinFlashback(access: ProtectedAccess) {
        val cast = mutableListOf<Npc>()
        access.scene(
            template = CorsairCurseRegions.CoveHut,
            vantage = ColinScene.Vantage,
            faceAt = ColinScene.Anchor,
            camera = ColinScene.Opening.toCamera(),
            teardown = {
                cast.forEach(::despawn)
                access.showInShot()
            },
            underFade = { r ->
                spawn(CorsairCurseRefs.CabinBoyTelescope, r[ColinScene.Anchor]).also(cast::add)
                hideFromShot()
            },
        ) { r ->
            var colin = cast.first()

            startDialogue {
                access.dolly(r, ColinScene.OpeningDolly)
                access.player.midiJingle(SceneOpenJingle)
                colinLine(happy, "Such a beautiful night!")

                colin.anim(CorsairCurseRefs.TelescopeAnim)
                colinLine(happy, "What lands lie beyond the dim horizon, I wonder...")

                access.frame(r, ColinScene.Startled)
                colinLine(confused, "Eh?")

                colin.anim(ThinkAnim)
                colinLine(confused, "What was that? I'd better have another look.")

                access.dolly(r, ColinScene.StartledDolly)
                colin =
                    retype(cast, colin, CorsairCurseRefs.CabinBoyTelescope, r[ColinScene.Anchor])
                colin.anim(TelescopeStartAnim)
                colinLine(shocked, "Is that what I think it is?")

                colinLine(
                    shocked,
                    "It's so dark out there, but I think... ... yes, I'm quite sure...",
                )

                access.dolly(r, ColinScene.StartledReturn)
                colin = retype(cast, colin, CorsairCurseRefs.CabinBoy, r[ColinScene.Anchor])
                colin.anim(TelescopeEndAnim)
                access.player.midiJingle(ColinMermaidJingle)
                colinLine(happy, "... yes, it's a mermaid! She's beautiful!")

                access.frame(r, ColinScene.CloseUp)
                colin.anim(WaveAnim)
                colinLine(quiz, "Hello?")

                colin.anim(BeckonAnim)
                colinLine(quiz, "Can you hear me?")

                colin.anim(CheerAnim)
                access.player.midiJingle(ColinCheerJingle)
                colinLine(happy, "You're the most lovely person I've ever seen!")

                access.frame(r, ColinScene.Startled)
                colin.anim(CorsairCurseRefs.TelescopeAnim)
                colinLine(
                    happy,
                    "I can see you moving your arms, but it's very dark. Are you waving to me?",
                )

                access.frame(r, ColinScene.CloseUp)
                colin.anim(WaveAnim)

                access.delay(ColinKissGapTicks)
                colin.resetAnim()
                colin.anim(BlowKissAnim)
                access.soundSynth(KissSound, delay = KissSoundDelay)
                colinLine(happy, "<col=0000ff>*kiss*</col>")

                access.frame(r, ColinScene.Startled)
                colin =
                    retype(cast, colin, CorsairCurseRefs.CabinBoyTelescope, r[ColinScene.Anchor])
                colin.anim(TelescopeStartAnim)
                colinLine(quiz, "Hello?")

                colinLine(sad, "Oh, no, I think she's gone away. Did I put her off?")

                colinLine(
                    confused,
                    "Maybe she doesn't like strangers shouting about her appearance and blowing " +
                        "kisses at her. That might have been rude of me.",
                )

                access.dolly(r, ColinScene.Closing, ColinClosingRate)
                access.player.midiJingle(SceneCloseJingle)
                colinLine(sad, "I hope she's not angry. Ithoi says mermaids can be dangerous.")
            }
        }
    }

    suspend fun gnocciFlashback(access: ProtectedAccess) {
        val cast = mutableListOf<Npc>()
        access.scene(
            template = CorsairCurseRegions.CoveShore,
            vantage = GnocciScene.Vantage,
            faceAt = GnocciScene.Anchor,
            camera = GnocciScene.Opening.toCamera(),
            teardown = {
                cast.forEach(::despawn)
                access.showInShot()
            },
            underFade = { r ->
                val gnocci =
                    spawn(CorsairCurseRefs.CookFishing, r[GnocciScene.Anchor]).also(cast::add)
                gnocci.anim(LobsterAnim)
                gnocci.aiTimer(LobsterLoopTicks)
                hideFromShot()
            },
        ) { r ->
            var gnocci = cast.first()

            startDialogue {
                access.dolly(r, GnocciScene.OpeningDolly, GnocciOpeningRate)
                access.player.midiJingle(SceneOpenJingle)
                gnocciLine(happy, "Let's see what I can find for the boys tonight.")

                gnocciLine(happy, "C'mon little lobsters, come to Gnocci!")

                gnocciLine(chant, "Obby-lobby obby-lobby oi oi oi!")

                access.fadeOverlay(
                    startColour = 0,
                    startTransparency = 255,
                    endColour = 0,
                    endTransparency = 0,
                    clientDuration = FlybyFadeOutDuration,
                )

                access.delay(1)
                access.showInShot()
                chatPlayer(shifty, "'obby-lobby' - what?")

                access.hideFromShot()
                gnocciLine(
                    angry,
                    "It's a traditional lobster-fishing chant handed down by my ancestors. Don't " +
                        "interrupt.",
                )

                access.player.midiJingle(SceneOpenJingle)
                access.fadeOverlay(
                    startColour = 0,
                    startTransparency = 0,
                    endColour = 0,
                    endTransparency = 255,
                    clientDuration = SceneFadeDuration,
                )
                access.delay(SceneFadeTicks)
                access.closeFadeOverlayNow()

                gnocciLine(
                    sad,
                    "C'mon little lobsters. We don't want to keep the Captain waiting! He'll get " +
                        "angry with me again.",
                )

                access.frame(r, GnocciScene.Reveal)
                gnocci = retype(cast, gnocci, CorsairCurseRefs.Cook, r[GnocciScene.Anchor])
                gnocciLine(quiz, "Ooh, what's this? This isn't a lobster.")

                access.dolly(r, GnocciScene.RevealDolly)
                gnocciLine(confused, "Looks more like a child's toy. But - no, it can't be...")

                gnocciLine(shocked, "Is it moving?")

                // NPC-side queues own the scene beats independently of dialogue continuation.
                gnocci.faceSquare(r[GnocciScene.PanicFacing])
                access.delay(1)

                gnocci.anim(PanicAnim)
                access.player.midiJingle(GnocciPanicJingle)
                access.soundSynth(
                    CorsairCurseRefs.ClockworkSound,
                    loops = SceneClockworkLoops,
                    delay = SceneClockworkDelay,
                )
                access.delay(1)

                access.runClientScript(DollJumpscareScript.asRSCM(RSCMType.CLIENTSCRIPT))

                gnocciLine(worried, "She's moving! SHE'S POSSESSED! HELP!")

                access.frame(r, GnocciScene.Burial)
                gnocci.telejump(collision, r[GnocciScene.BurialMark])
                gnocci.transmog(cookDiggingType, Int.MAX_VALUE)

                access.delay(GnocciDigTransformTicks)
                gnocci.anim(DigAnim)
                access.soundSynth(CorsairCurseRefs.DigSound)
                access.delay(GnocciBurialDigTicks)

                access.player.midiJingle(SceneCloseJingle)
                gnocciLine(sad, GnocciBuriedIt)
            }
        }
    }

    suspend fun telescopeView(access: ProtectedAccess) {
        val cast = mutableListOf<Npc>()
        access.blankChatbox()
        access.scene(
            template = CorsairCurseRegions.CoveHut,
            vantage = TelescopeScene.Vantage,
            faceAt = TelescopeScene.Ogre,
            camera = TelescopeScene.Opening.toCamera(rate2 = TelescopeOpeningRate2),
            teardown = { cast.forEach(::despawn) },
            onEntered = {
                player.midiSong(CorsairCurseRefs.StopMusic)
                player.midiJingle(SceneOpenJingle)
            },
            underFade = { r ->
                spawn(CorsairCurseRefs.Ogre, r[TelescopeScene.Ogre]).also(cast::add)
            },
            reveal = { ifOpenSub(SpyglassInterface, FadeOverlaySlot, IfSubType.Overlay) },
        ) { r ->
            dolly(r, TelescopeScene.PushIn)

            delay(TelescopeHoldTicks)

            player.midiSong(CorsairCurseRefs.StopMusic)
            player.midiJingle(TelescopeRevealJingle)

            delay(TelescopeRevealLineTicks)
            chatPlayerNoPause(
                OgreRevealLine,
                OgreRevealLineWrapped,
                OgreRevealMesanim,
                OgreRevealLineHeight,
            )

            delay(TelescopeRevealTicks - TelescopeRevealLineTicks)
        }
    }

    private fun ProtectedAccess.blankChatbox() {
        ifOpenSub(MessageBoxInterface, ChatModalSlot, IfSubType.Modal)
        ifSetText(MessageBoxText, "")
        ClientScripts.ifSetTextAlign(player, MessageBoxText, alignH = 1, alignV = 1, 0)
        ifSetEvents(MessageBoxContinue, -1..-1)
        ifSetText(MessageBoxContinue, "")
        runClientScript(ChatboxBackgroundScript, "")
    }

    private fun ProtectedAccess.chatPlayerNoPause(
        text: String,
        wrapped: String,
        mesanim: String,
        lineHeight: Int,
    ) {
        val title = player.displayName
        mes("$title|$text", ChatType.Dialogue)
        chatModalUnclamp = constants.modal_fixedwidthandheight
        ClientScripts.topLevelChatboxResetBackground(player)
        runClientScript(ChatboxBackgroundScript, "0")
        ifOpenSub(ChatRightInterface, ChatModalSlot, IfSubType.Modal)
        ifSetPlayerHead(ChatRightHead)
        ifSetAnim(ChatRightHead, ServerCacheManager.getAnim(mesanim.asRSCM(RSCMType.SEQ)))
        ifSetText(ChatRightName, title)
        ifSetText(ChatRightText, wrapped)
        ClientScripts.ifSetTextAlign(player, ChatRightText, alignH = 1, alignV = 1, lineHeight)
        ifSetEvents(ChatRightContinue, -1..-1)
        ifSetText(ChatRightContinue, "")
    }

    suspend fun driftwoodScene(access: ProtectedAccess, onDouse: () -> Unit) {
        val cast = mutableListOf<Npc>()
        access.scene(
            template = CorsairCurseRegions.CoveHut,
            vantage = DriftwoodScene.Vantage,
            faceAt = DriftwoodScene.BedMark,
            camera = DriftwoodScene.Opening.toCamera(),
            fadeOutDuration = FlybyFadeOutDuration,
            teardown = {
                cast.forEach(::despawn)
                access.showInShot()
            },
            underFade = { r ->
                spawn(CorsairCurseRefs.NavigatorSick, r[DriftwoodScene.BedMark]).also(cast::add)
                hideFromShot()
            },
        ) { r ->
            soundSynth(CorsairCurseRefs.FireCracklingSound)
            dolly(r, DriftwoodScene.OpeningDolly)

            delay(DriftwoodNoticeTicks)
            cast.first().say(IthoiNoticeLine)
            delay(DriftwoodBurnTicks)

            fadeOverlay(
                startColour = 0,
                startTransparency = 255,
                endColour = 0,
                endTransparency = 0,
                clientDuration = FlybyFadeOutDuration,
            )
            delay(SceneFadeTicks)

            cast.forEach(::despawn)
            cast.clear()
            showInShot()
            telejump(r[DriftwoodScene.BeachVantage])
            delay(1)

            frame(r, DriftwoodScene.Reverse)
            delay(1)

            val ithoi =
                spawn(CorsairCurseRefs.NavigatorRunning, r[DriftwoodScene.HutDoor]).also(cast::add)
            fadeOverlay(
                startColour = 0,
                startTransparency = 0,
                endColour = 0,
                endTransparency = 255,
                clientDuration = SceneFadeDuration,
            )
            delay(SceneFadeTicks)
            closeFadeOverlayNow()

            for (leg in DriftwoodScene.RunRoute) {
                ithoi.teleport(collision, r[leg])
                delay(1)
            }

            delay(DriftwoodArrivalPauseTicks)

            ithoi.say(IthoiArrivalLine)
            delay(DriftwoodSayTicks)

            ithoi.faceSquare(r[DriftwoodScene.ProjectileEnd])
            ithoi.anim(CastAnim)
            ithoi.spotanim(WaterBoltSpotanim, height = CastSpotanimHeight)
            soundSynth(CorsairCurseRefs.WaterBoltCastSound)

            worldRepo.projAnim(waterBolt(from = ithoi.coords, to = r[DriftwoodScene.ProjectileEnd]))
            delay(DriftwoodCastTicks)

            onDouse()
            spotanimMap(
                worldRepo,
                DouseSpotanim,
                r[DriftwoodScene.DouseCoords],
                height = DouseSpotanimHeight,
            )
            soundSynth(CorsairCurseRefs.WaterBoltHitSound)
            soundSynth(CorsairCurseRefs.SteamSound, delay = SteamSoundDelay)

            // Lock facing during movement; ordinary face-square processing otherwise yields to movement.
            ithoi.lockFacing(r[DriftwoodScene.HutDoor])

            delay(1)
            dolly(r, DriftwoodScene.ReverseDolly)

            for (leg in DriftwoodScene.ExitRoute) {
                ithoi.teleport(collision, r[leg])
                delay(1)
            }
        }
    }

    private fun waterBolt(from: CoordGrid, to: CoordGrid): ProjAnim =
        ProjAnim(
            spotanim = WaterBoltTravelSpotanim.asRSCM(RSCMType.SPOTANIM),
            startHeight = WaterBoltStartHeight,
            endHeight = WaterBoltEndHeight,
            startTime = WaterBoltStartTime,
            endTime = WaterBoltEndTime,
            angle = WaterBoltAngle,
            progress = WaterBoltProgress,
            sourceIndex = 0,
            targetIndex = 0,
            startCoord = from,
            endCoord = to,
        )

    private suspend fun ProtectedAccess.scene(
        template: RegionTemplate,
        vantage: CoordGrid,
        faceAt: CoordGrid,
        camera: SceneCamera,
        teardown: () -> Unit = {},
        underFade: suspend ProtectedAccess.(SceneRegion) -> Unit = {},
        onEntered: suspend ProtectedAccess.() -> Unit = {},
        reveal: (suspend ProtectedAccess.() -> Unit)? = null,
        exitCut: Boolean = false,
        fadeOutDuration: Int = SceneFadeDuration,
        body: suspend ProtectedAccess.(SceneRegion) -> Unit,
    ) {
        val access = this
        val returnTo = player.coords
        val previousNpcViewDistance = player.npcViewDistance
        val region = regionRepo.add(template)
        region?.let(regionRepo::protect)
        val scene = SceneRegion(region)

        player.npcViewDistance = SceneNpcViewDistance
        player.attr[InstanceAttributes.LOGIN_EXIT_COORD] = returnTo.packed
        player.uuid?.let { uuid -> region?.let { sceneRegions[uuid] = it } }

        try {
            if (reveal != null) {
                revealScene(
                    vantage = scene[vantage],
                    faceAt = scene[faceAt],
                    camera = scene.map(camera),
                    returnTo = returnTo,
                    onEntered = onEntered,
                    underFade = { access.underFade(scene) },
                    teardown = teardown,
                    reveal = reveal,
                    fadeOutDuration = fadeOutDuration,
                ) {
                    access.body(scene)
                }
            } else {
                lockedScene(
                    vantage = scene[vantage],
                    faceAt = scene[faceAt],
                    camera = scene.map(camera),
                    returnTo = returnTo,
                    underFade = { access.underFade(scene) },
                    teardown = teardown,
                    exitCut = exitCut,
                    fadeOutDuration = fadeOutDuration,
                ) {
                    access.body(scene)
                }
            }
        } finally {
            player.npcViewDistance = previousNpcViewDistance
            player.attr.remove(InstanceAttributes.LOGIN_EXIT_COORD)
            player.uuid?.let(sceneRegions::remove)

            region?.let(regionRepo::unprotect)
        }
    }

    private fun ProtectedAccess.frame(r: SceneRegion, frame: SceneFrame) {
        val mapped = r.map(frame.toCamera())
        camMoveToV3(mapped.eye, height = mapped.eyeHeight, rate = SceneSnapRate, rate2 = SnapRate2)
        camLookAtV3(
            mapped.lookAt,
            height = mapped.lookAtHeight,
            rate = SceneSnapRate,
            rate2 = SnapRate2,
        )
    }

    private fun ProtectedAccess.dolly(
        r: SceneRegion,
        frame: SceneFrame,
        rate: Int = SceneDollyRate,
    ) {
        val mapped = r.map(frame.toCamera())
        camMoveToV3(mapped.eye, height = mapped.eyeHeight, rate = rate, rate2 = rate)
    }

    private fun ProtectedAccess.hideFromShot() {
        transmog(InvisibleActor)
        rebuildAppearance()
    }

    private fun ProtectedAccess.showInShot() {
        resetTransmog()
        rebuildAppearance()
    }

    private fun spawn(type: String, coords: CoordGrid): Npc {
        val npc = Npc(type, coords)
        npcRepo.add(npc, SceneLifespanTicks)
        npc.noneMode()
        return npc
    }

    private fun retype(cast: MutableList<Npc>, current: Npc, type: String, coords: CoordGrid): Npc {
        despawn(current)
        cast.remove(current)
        return spawn(type, coords).also(cast::add)
    }

    private fun despawn(npc: Npc) {
        if (!npc.isSlotAssigned) {
            return
        }
        npcRepo.del(npc, Int.MAX_VALUE)
    }

    private suspend fun org.rsmod.api.player.dialogue.Dialogue.colinLine(
        mesanim: dev.openrune.types.MesAnimType,
        text: String,
    ) = chatNpcSpecific("Cabin Boy Colin", CorsairCurseRefs.CabinBoy, mesanim, text)

    private suspend fun org.rsmod.api.player.dialogue.Dialogue.gnocciLine(
        mesanim: dev.openrune.types.MesAnimType,
        text: String,
    ) = chatNpcSpecific("Gnocci the Cook", CorsairCurseRefs.Cook, mesanim, text)

    private val org.rsmod.api.player.dialogue.Dialogue.chant
        get() = mesanim("mesanim.chant")

    private companion object {

        private const val SnapRate2 = 100

        private const val SceneDollyRate = 1

        private const val GnocciOpeningRate = 3

        private const val ColinClosingRate = 2

        private const val ColinKissGapTicks = 4

        private const val SceneOpenJingle = 244

        private const val SceneCloseJingle = 265

        private const val ColinMermaidJingle = 103

        private const val ColinCheerJingle = 165

        private const val GnocciPanicJingle = 243

        // The clientscript draws the doll; server-side redraws would duplicate it.
        private const val DollJumpscareScript = "clientscript.[clientscript,script2085]"

        private const val TelescopeRevealJingle = 138

        private const val KissSound = "synth.kiss"
        private const val KissSoundDelay = 5

        private const val SceneClockworkLoops = 7
        private const val SceneClockworkDelay = 15

        private const val SteamSoundDelay = 10

        private const val ThinkAnim = "seq.emote_think"

        private const val TelescopeStartAnim = "seq.human_use_wom_telecope_start"

        private const val TelescopeEndAnim = "seq.human_use_wom_telecope_end"

        private const val WaveAnim = "seq.emote_wave"

        private const val BeckonAnim = "seq.emote_beckon"

        private const val CheerAnim = "seq.emote_cheer"

        private const val BlowKissAnim = "seq.emote_blow_kiss"

        private const val LobsterAnim = "seq.human_lobster"

        private const val LobsterLoopTicks = 4

        private const val PanicAnim = "seq.emote_panic_flap"

        private const val DigAnim = "seq.human_dig"

        private const val CastAnim = "seq.human_caststrike"
        private const val WaterBoltSpotanim = "spotanim.waterbolt_casting"

        private const val CastSpotanimHeight = 92

        private const val IthoiArrivalLine = "Arrr!"

        private const val IthoiNoticeLine = "Arrr... uh-oh!"

        private const val WaterBoltTravelSpotanim = "spotanim.waterbolt_travel"
        private const val WaterBoltStartHeight = 172
        private const val WaterBoltEndHeight = 10
        private const val WaterBoltStartTime = 51
        private const val WaterBoltEndTime = 56
        private const val WaterBoltAngle = 8
        private const val WaterBoltProgress = 64

        private const val DouseSpotanim = "spotanim.waterbolt_impact"

        private const val DouseSpotanimHeight = 10

        private const val GnocciDigTransformTicks = 2

        private const val GnocciBurialDigTicks = 5

        private const val InvisibleActor = "npc.desert_treasure_invisible_npc"

        private const val TelescopeHoldTicks = 6

        private const val TelescopeRevealTicks = 9

        private const val TelescopeRevealLineTicks = 2

        private const val TelescopeOpeningRate2 = 100

        private const val SpyglassInterface = "interface.wom_telescope"

        private const val FadeOverlaySlot = "component.toplevel_osrs_stretch:overlay_atmosphere"

        private const val MessageBoxInterface = "interface.messagebox"
        private const val MessageBoxText = "component.messagebox:text"
        private const val MessageBoxContinue = "component.messagebox:continue"

        private const val ChatboxBackgroundScript = 1508

        private const val OgreRevealLine =
            "That doesn't look anything like a mermaid. That's an ogre!"

        private const val OgreRevealLineWrapped =
            "That doesn't look anything like a mermaid.<br>That's an ogre!"

        private const val OgreRevealMesanim = "seq.chatlaugh2"

        private const val OgreRevealLineHeight = 28

        private const val ChatRightInterface = "interface.chat_right"
        private const val ChatModalSlot = "component.chatbox:chatmodal"
        private const val ChatRightHead = "component.chat_right:head"
        private const val ChatRightName = "component.chat_right:name"
        private const val ChatRightText = "component.chat_right:text"
        private const val ChatRightContinue = "component.chat_right:continue"

        private const val DriftwoodNoticeTicks = 1

        private const val DriftwoodBurnTicks = 2

        private const val DriftwoodArrivalPauseTicks = 2

        private const val DriftwoodSayTicks = 1

        private const val DriftwoodCastTicks = 2

        private const val FlybyFadeOutDuration = 15

        private const val FlybyDollyRate = 6

        private const val FlybyPanRate = 1

        private const val FlybyRate2 = 0

        private const val FlybyDollyTicks = 9

        private const val FlybyPanTicks = 9

        private const val SceneLifespanTicks = 500

        private const val SceneNpcViewDistance = 48

        private const val GnocciBuriedIt =
            "I buried Her in the sand, by that tree, to the west. But I still feel Her eyes " +
                "staring into my soul. Please find Her, and kill the demon inside!"
    }
}

private var ProtectedAccess.chatModalUnclamp by intVarBit("varbit.chatmodal_unclamp")
