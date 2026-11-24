package org.rsmod.content.quest.mudfoot

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

/** The first, failed confrontation with the ranger outside the General Store. */
@Singleton
class MudfootStoreCutscene
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val npcRepo: NpcRepository,
    private val follower: MudfootFollower,
    private val npcHitModifier: NpcHitModifier,
) {
    internal lateinit var quest: Quest

    /** Plays the scene and returns once the player is back beside Captain Harlan. */
    suspend fun ProtectedAccess.play(): Boolean {
        val shout = "Look, that him there!"
        follower.follower(player)?.say(shout)
        player.mes("$MUDFOOT_TITLE|$shout", ChatType.NpcSay)
        delay(1)
        fadeToBlack()
        delay(2)

        val region = regionRepo.add(MudfootAreas.GENERAL_STORE.template)
        if (region == null) {
            fadeFromBlack()
            return false
        }
        regionRepo.protect(region)
        val scene = MudfootScene(MudfootAreas.GENERAL_STORE, region)
        val spawned = ArrayList<Npc>()
        var finished = false

        player.mudfootReturnTile = STORE_RETURN.packed
        try {
            player.mudfootVis = MudfootVis.GOBLIN_HOUSE
            follower.stop(player)
            hideScene()
            telejump(scene[PLAYER_START])
            faceSquare(scene[OUTLAW_TILE])

            val outlaw = spawn(OUTLAW, scene[OUTLAW_TILE], spawned)
            val mudfoot = spawn(MUDFOOT_VIS, scene[MUDFOOT_START], spawned)
            outlaw.faceSquare(scene[OUTLAW_TILE].translate(1, 0))
            mudfoot.faceSquare(scene[MUDFOOT_START].translate(-1, 0))
            delay(1)

            camMoveToV3(scene[CAMERA_EYE], height = EYE_HEIGHT, rate = CAMERA_RATE, rate2 = CAMERA_RATE2)
            camLookAtV3(
                scene[CAMERA_LOOK_AT],
                height = LOOK_AT_HEIGHT,
                rate = CAMERA_RATE,
                rate2 = CAMERA_RATE2,
            )
            delay(1)

            walk(scene[PLAYER_STAND])
            mudfoot.walk(scene[MUDFOOT_STAND])
            fadeFromBlack()
            delay(1)
            faceSquare(scene[PLAYER_FACE])
            mudfoot.faceNpc(outlaw)
            delay(1)

            startDialogue {
                chatNpcSpecific(OUTLAW_TITLE, OUTLAW, confused, "Er... What is this?")
                outlaw.faceNpc(mudfoot)
                chatNpcSpecific(MUDFOOT_TITLE, MUDFOOT_VIS, angry, "Evil human! Mudfoot kill you now!")
            }

            mudfoot.walk(scene[MUDFOOT_SWING])
            delay(1)
            mudfoot.anim(HAMMER_ANIM)
            soundSynth(HAMMER_SOUND)
            outlaw.anim(OUTLAW_BLOCK_ANIM)
            outlaw.queueHit(mudfoot, 1, HitType.Melee, 0, npcHitModifier)
            delay(1)
            soundSynth(BLOCK_SOUND)
            delay(3)

            startDialogue {
                chatNpcSpecific(OUTLAW_TITLE, OUTLAW, confusedIdle, "...")
                chatNpcSpecific(OUTLAW_TITLE, OUTLAW, confused, "Okay... Good luck with that...")
            }

            outlaw.resetFaceEntity()
            outlaw.walk(scene[OUTLAW_EXIT])
            delay(3)
            val harlan = spawn(HARLAN_CUTSCENE, scene[HARLAN_START], spawned)
            harlan.faceSquare(scene[HARLAN_START].translate(0, 1))
            mudfoot.faceSquare(scene[PLAYER_STAND])

            startDialogue {
                chatNpcSpecific(MUDFOOT_TITLE, MUDFOOT_VIS, confused, "Me no understand. Why no work?")
            }

            harlan.walk(scene[HARLAN_STAND])
            delay(2)
            faceSquare(scene[HARLAN_STAND])
            mudfoot.faceSquare(scene[HARLAN_STAND])
            harlan.faceSquare(scene[PLAYER_STAND].translate(-1, 0))

            startDialogue { commotion() }

            delay(3)
            quest.setQuestStage(this, MudfootStage.MET_HARLAN)
            fadeToBlack()
            delay(2)
            finished = true
        } finally {
            for (npc in spawned) {
                npcRepo.del(npc, Int.MAX_VALUE)
            }
            regionRepo.unprotect(region)
            if (player.loggingOut) {
                follower.resumeOnLogin(player, spear = false)
            } else {
                player.mudfootReturnTile = 0
                restoreScene()
                telejump(STORE_RETURN)
                follower.start(player, spear = false)
            }
        }
        if (finished) {
            faceSquare(HARLAN_TILE)
            delay(2)
            fadeFromBlack()
        }
        return finished
    }

    private suspend fun org.rsmod.api.player.dialogue.Dialogue.commotion() {
        chatNpcSpecific(
            HARLAN_TITLE,
            HARLAN_CUTSCENE,
            confused,
            "What's happening here? What's all the commotion?",
        )
        chatNpcSpecific(
            HARLAN_TITLE,
            HARLAN_CUTSCENE,
            confused,
            "A goblin? Is this the one responsible for the robbery, adventurer?",
        )
        chatPlayer(
            neutral,
            "As it turns out, no. Mudfoot here is actually trying to deal with the thieves as well.",
        )
        chatNpcSpecific(HARLAN_TITLE, HARLAN_CUTSCENE, confused, "He is?")
        chatNpcSpecific(
            MUDFOOT_TITLE,
            MUDFOOT_VIS,
            disgruntled,
            "Yes. Nasty humans try to frame innocent goblins. Me here to stop them. Me make them " +
                "go dead.",
        )
        chatNpcSpecific(HARLAN_TITLE, HARLAN_CUTSCENE, confused, "You killed them?")
        chatPlayer(
            confused,
            "Not yet. Mudfoot seems to be having some... difficulties with that.",
        )
        chatNpcSpecific(
            HARLAN_TITLE,
            HARLAN_CUTSCENE,
            confused,
            "I see... In that case, come. Let us discuss this further.",
        )
    }

    private fun ProtectedAccess.hideScene() {
        hideEntityOps()
        hideTopLevel()
        hideHealthHud()
        clearHealthHud()
        minimapHideMap()
        camModeClose()
        maxDrawDistance(enabled = true)
        tempDisableAcceptAid()
        closeTopLevelTabsLenient()
        syncDrawDistance()
        camShakeResetAll()
    }

    private fun ProtectedAccess.restoreScene() {
        ifClose()
        showEntityOps()
        restoreLastAcceptAid()
        maxDrawDistance(enabled = false)
        minimapReset()
        camModeReset()
        showTopLevel()
        showHealthHud()
        camReset()
        openTopLevelTabs()
        syncDrawDistance()
    }

    private fun spawn(type: String, coords: CoordGrid, spawned: MutableList<Npc>): Npc {
        val npc = Npc(type, coords)
        npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        npc.noneMode()
        spawned += npc
        return npc
    }

    private companion object {
        const val OUTLAW = "npc.mba_outlaw_ranged_vis"

        val PLAYER_START = CoordGrid(3222, 3246, 0)
        val PLAYER_STAND = CoordGrid(3219, 3246, 0)
        val PLAYER_FACE = CoordGrid(3218, 3245, 0)
        val OUTLAW_TILE = CoordGrid(3217, 3245, 0)
        val OUTLAW_EXIT = CoordGrid(3217, 3262, 0)
        val MUDFOOT_START = CoordGrid(3222, 3244, 0)
        val MUDFOOT_STAND = CoordGrid(3219, 3244, 0)
        val MUDFOOT_SWING = CoordGrid(3218, 3245, 0)
        val HARLAN_START = CoordGrid(3220, 3240, 0)
        val HARLAN_STAND = CoordGrid(3220, 3244, 0)
        val CAMERA_EYE = CoordGrid(3221, 3239, 0)
        val CAMERA_LOOK_AT = CoordGrid(3215, 3253, 0)

        val STORE_RETURN = CoordGrid(3218, 3238, 0)
        val HARLAN_TILE = CoordGrid(3217, 3239, 0)

        const val EYE_HEIGHT = 480
        const val LOOK_AT_HEIGHT = 230
        const val CAMERA_RATE = 232
        const val CAMERA_RATE2 = 100

        const val HAMMER_ANIM = "seq.slice_surface_goblin_armed_attack"
        const val HAMMER_SOUND = "synth.goblin_warhammer_attack"
        const val OUTLAW_BLOCK_ANIM = "seq.human_unarmed_def"
        const val BLOCK_SOUND = "synth.leather_hit_3"
    }
}
