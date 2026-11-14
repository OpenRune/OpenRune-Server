package org.rsmod.content.quest.area.asgarnia.icemountain

import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SynthType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceRegionRotation
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.instances.RegionLocal
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.table.MusicRow
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Singleton
class BimRps
@Inject
constructor(
    private val instanceManager: InstanceManager,
    private val mapClock: MapClock,
) {
    suspend fun play(dialogue: Dialogue, quest: BelowIceMountain) =
        with(dialogue) {
            val player = player
            if (instanceManager.sessionForPlayer(player) != null) {
                access.mes("You are already inside an instance.")
                return@with
            }
            instanceManager.cancelPendingEntry(player, mapClock.cycle)
            val session: InstanceSession
            val enter: CoordGrid
            when (
                val result =
                    instanceManager.create(
                        player,
                        RPS_INSTANCE_KEY,
                        RPS_SPEC,
                        InstanceAccess.Private,
                        mapClock.cycle,
                    )
            ) {
                is InstanceManager.Result.Created -> {
                    session = result.session
                    enter = result.enter
                }
                is InstanceManager.Result.Joined -> {
                    session = result.session
                    enter = result.enter
                }
                is InstanceManager.Result.Failed -> {
                    access.mes(result.reason)
                    return@with
                }
            }
            access.hideEntityOps()
            access.telejump(enter, TeleportType.Exempt)
            instanceManager.finalizeEntry(player, session, mapClock.cycle)
            access.resetAnim()
            access.faceAngle(1024)

            val instanceNpcs = instanceManager.npcsForInstance(session.id)
            val burntof = instanceNpcs.firstOrNull { it.name == "Burntof" }
            val tina = instanceNpcs.firstOrNull { it.name == "Tina" }
            if (burntof == null || tina == null) {
                access.mes("The pub is unavailable at the moment. Please try again shortly.")
                val exit = instanceManager.leave(player, session, mapClock.cycle)
                access.telejump(exit, TeleportType.Exempt)
                return@with
            }
            burntof.mode = NpcMode.None
            tina.mode = NpcMode.None
            playMusic(72)
            access.delay(1)
            val camMove =
                checkNotNull(
                    instanceManager.resolveCoord(session, CoordGrid(2961, 3369, PUB_LEVEL)),
                )
            val camLook =
                checkNotNull(
                    instanceManager.resolveCoord(session, CoordGrid(2955, 3369, PUB_LEVEL)),
                )
            access.camMoveToV3(camMove, 400, SNAP, SNAP)
            access.camLookAtV3(camLook, 180, SNAP, SNAP)
            access.fadeOverlay(0, FADE_BLACK, 0, FADE_CLEAR, 50)
            access.delay(1)
            access.closeFadeOverlay(cycles = 2)

            chatNpcSpecific(
                "Burntof",
                BURNOTOF,
                drunk,
                "The rulesh are ash followsh. We both pick a shign to show and then shay " +
                    "'rock, paper, shcissors.' After schissors, we show our shigns.",
            )
            chatNpcSpecific(
                "Burntof",
                BURNOTOF,
                drunk,
                "Thish ish a besht of three game. Nowsh, pick what you are going to shhow.",
            )

            val first = chooseSign(1)
            playSigns(player, burntof)
            access.mesboxEmpty()
            access.delay(5)
            val firstActual = first.beatsMe()
            mesbox("Burntof chose ${firstActual.label.lowercase()}.")
            chatNpcSpecific("Burntof", BURNOTOF, drunk, "Looksh like you win the firsht round.")
            chatPlayer(quiz, "But...? I put out ${first.label.lowercase()}?")
            chatNpcSpecific(
                "Burntof",
                BURNOTOF,
                drunk,
                "Yesh, and I put out *hic* ${first.losesTo().label.lowercase()}... I think.",
            )
            chatPlayer(quiz, "Okay then... I guess the first round goes to me.")

            val second = chooseSign(2)
            playSigns(player, burntof)
            access.mesboxEmpty()
            access.delay(5)
            val secondActual = second.beatsMe()
            mesbox("Burntof chose ${secondActual.label.lowercase()}.")
            chatPlayer(happy, "I won! That's two rounds to me, so you have to help.")
            chatNpcSpecific(
                "Burntof",
                BURNOTOF,
                drunk,
                "Hooold it right there *hic* bud, I thinksh I won that round...",
            )
            chatPlayer(
                quiz,
                "But ${secondActual.beatsMe().label.lowercase()} beats " +
                    "${secondActual.label.lowercase()}?",
            )
            chatNpcSpecific(
                "Burntof",
                BURNOTOF,
                drunk,
                "Yesh, that is correct, but you chose ${second.label.lowercase()}. So I win.",
            )
            chatPlayer(angry, "Whatever, let's just play the last round.")

            val third = chooseSign(3)
            playSigns(player, burntof)
            access.mesboxEmpty()
            access.delay(5)
            val thirdActual = third.beatsMe()
            mesbox("Burntof chose ${thirdActual.label.lowercase()}.")
            chatNpcSpecific(
                "Burntof",
                BURNOTOF,
                drunk,
                "Well, it looksh like I win, sho I don't have to help you out.",
            )
            val changed = thirdActual.beatsMe()
            mesbox("You quickly change your sign to ${changed.label.lowercase()}.")
            chatPlayer(shifty, "Are you okay Burntof? I clearly beat you. Check my hand again.")
            chatNpcSpecific(
                "Burntof",
                BURNOTOF,
                shocked,
                "Merlinsh beard! You did. A deal'sh a deal, I'll help you out.",
            )
            chatNpcSpecific(
                "Burntof",
                BURNOTOF,
                drunk,
                "But I think I'll need one more drink before I hitsh the road.",
            )
            chatNpcSpecific("Burntof", BURNOTOF, drunk, "Tina! Bring me another beer pleash.")
            chatNpcSpecific(
                "Tina",
                TINA,
                happy,
                "Sure thing Burntof, I'll be right there!",
            )
            tina.mode = NpcMode.None
            tina.clearInteraction()
            tina.walk(
                listOf(
                    checkNotNull(
                        instanceManager.resolveCoord(session, CoordGrid(2955, 3373, PUB_LEVEL)),
                    ),
                    checkNotNull(
                        instanceManager.resolveCoord(session, CoordGrid(2955, 3372, PUB_LEVEL)),
                    ),
                    checkNotNull(
                        instanceManager.resolveCoord(session, CoordGrid(2955, 3371, PUB_LEVEL)),
                    ),
                    checkNotNull(
                        instanceManager.resolveCoord(session, CoordGrid(2956, 3371, PUB_LEVEL)),
                    ),
                )
            )
            access.delay(3)
            burntof.faceSquare(tina.coords)
            tina.faceSquare(burntof.coords)
            chatNpcSpecific("Tina", TINA, happy, "Here you go.")
            chatNpcSpecific("Burntof", BURNOTOF, drunk, "Thanksh Tina.")

            access.mesboxEmpty()
            access.soundSynth(SynthType(2401))
            access.delay(3)
            access.fadeOverlay(0, FADE_CLEAR, 0, FADE_BLACK, 50)
            access.delay(2)
            val exit = instanceManager.leave(player, session, mapClock.cycle)
            access.telejump(exit, TeleportType.Exempt)
            access.resetAnim()
            access.camReset()
            access.showEntityOps()
            player.bimBurntof = BimBurntof.RECRUITED
            quest.markOneRecruited(player)
            access.delay(3)
            access.fadeOverlay(0, FADE_BLACK, 0, FADE_CLEAR, 50)
            access.closeFadeOverlay(cycles = 2)

            if (quest.hasAllRecruits(player)) {
                quest.markAllRecruited(player)
                chatPlayer(
                    happy,
                    "Well, that's everyone. I guess I should head off the entrance on the west side of " +
                        "Ice Mountain.",
                )
            } else {
                chatPlayer(
                    neutral,
                    "That was interesting, but that's Burntof recruited. On to the next person.",
                )
            }
        }

    private suspend fun Dialogue.chooseSign(round: Int): RpsSign =
        when (
            choice3(
                "Rock.",
                1,
                "Paper.",
                2,
                "Scissors.",
                3,
                title = "Round $round: What sign will you show?",
            )
        ) {
            1 -> RpsSign.Rock
            2 -> RpsSign.Paper
            else -> RpsSign.Scissors
        }

    private fun playSigns(player: Player, burntof: Npc) {
        player.anim("seq.rps_human")
        player.say("Rock, paper, scissors!")
        burntof.say("Rock, paper, uhm... scissors!")
        burntof.anim("seq.dwarf_rps")
    }

    private fun RpsSign.losesTo(): RpsSign =
        when (this) {
            RpsSign.Rock -> RpsSign.Scissors
            RpsSign.Paper -> RpsSign.Rock
            RpsSign.Scissors -> RpsSign.Paper
        }

    private fun RpsSign.beatsMe(): RpsSign =
        when (this) {
            RpsSign.Rock -> RpsSign.Paper
            RpsSign.Paper -> RpsSign.Scissors
            RpsSign.Scissors -> RpsSign.Rock
        }

    private fun Dialogue.playMusic(midi: Int) {
        val row = MusicRow.all().firstOrNull { it.midi.id == midi } ?: return
        access.musicPlay(row)
    }

    private enum class RpsSign(val label: String) {
        Rock("Rock"),
        Paper("Paper"),
        Scissors("Scissors"),
    }

    private companion object {
        const val SNAP = 232
        const val FADE_CLEAR = 255
        const val FADE_BLACK = 0
        const val PUB_X = 2956
        const val PUB_Z = 3369
        const val PUB_LEVEL = 1
        const val BURNOTOF_Z = 3370
        const val TINA_Z = 3373
        const val RETURN_Z = 3368
        const val BURNOTOF = "npc.bim_cutscene_burntof"
        const val TINA = "npc.bim_cutscene_tina"
        const val RPS_INSTANCE_KEY = "bim_rps"

        val RPS_AREA =
            InstanceArea.copyRegions(
                centerRegionId = 11828,
                gridSize = 3,
                level = PUB_LEVEL,
                rotation = InstanceRegionRotation.NONE,
                enterCoord = RegionLocal(PUB_LEVEL, 46, 52, 12, 41),
                exitCoord = CoordGrid(PUB_X, RETURN_Z, 0),
                npcSpawns =
                    listOf(
                        InstanceNpc(BURNOTOF, CoordGrid(PUB_X, BURNOTOF_Z, PUB_LEVEL)),
                        InstanceNpc(TINA, CoordGrid(PUB_X, TINA_Z, PUB_LEVEL)),
                    ),
            )

        val RPS_SPEC =
            InstanceSpec(
                fee = 0,
                maxPlayers = 1,
                reclaimTicks = 20,
                graceTicks = 0,
                destroyWhenEmpty = true,
                area = RPS_AREA,
                settingsRowId = 0,
                bossName = "Below Ice Mountain",
                recordsKillTime = { false },
            )
    }
}
