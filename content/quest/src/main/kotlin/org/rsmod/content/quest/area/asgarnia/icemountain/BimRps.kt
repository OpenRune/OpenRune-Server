package org.rsmod.content.quest.area.asgarnia.icemountain

import dev.openrune.types.aconverted.SynthType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.table.MusicRow
import org.rsmod.content.quest.area.misthalin.SceneCamera
import org.rsmod.content.quest.area.misthalin.SceneRegion
import org.rsmod.content.quest.area.misthalin.lockedScene
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

@Singleton
class BimRps
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val npcRepo: NpcRepository,
) {
    suspend fun play(dialogue: Dialogue, quest: BelowIceMountain) =
        with(dialogue) {
            val player = player
            val region = regionRepo.add(BIM_RISING_SUN)
            region?.let(regionRepo::protect)
            val scene = SceneRegion(region)
            val cast = mutableListOf<Npc>()

            try {
                access.lockedScene(
                    vantage = scene[RpsScene.Vantage],
                    faceAt = scene[RpsScene.Burntof],
                    camera = scene.map(RpsScene.Camera),
                    returnTo = RpsScene.Exit,
                    underFade = {
                        cast += spawn(BURNOTOF, scene[RpsScene.Burntof])
                        cast += spawn(TINA, scene[RpsScene.Tina])
                    },
                    teardown = { cast.forEach(::despawn) },
                ) {
                    startDialogue {
                        playMusic(72)
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

                        val burntof = cast.first()
                        val round1 = playRound(burntof, 1)
                        val hisFirst = round1.beatenBy
                        mesbox("Burntof chose ${hisFirst.label}.")
                        chatNpcSpecific(
                            "Burntof",
                            BURNOTOF,
                            drunk,
                            "Looksh like you win the firsht round.",
                        )
                        chatPlayer(quiz, "But...? I put out ${round1.label}?")
                        chatNpcSpecific(
                            "Burntof",
                            BURNOTOF,
                            drunk,
                            "Yesh, and I put out *hic* ${round1.beats.label}... I think.",
                        )
                        chatPlayer(quiz, "Okay then... I guess the first round goes to me.")

                        val round2 = playRound(burntof, 2)
                        val hisSecond = round2.beats
                        mesbox("Burntof chose ${hisSecond.label}.")
                        chatPlayer(happy, "I won! That's two rounds to me, so you have to help.")
                        chatNpcSpecific(
                            "Burntof",
                            BURNOTOF,
                            drunk,
                            "Hooold it right there *hic* bud, I thinksh I won that round...",
                        )
                        chatPlayer(
                            quiz,
                            "But ${round2.label} beats ${hisSecond.label}?",
                        )
                        chatNpcSpecific(
                            "Burntof",
                            BURNOTOF,
                            drunk,
                            "Yesh, that is correct, but you chose ${hisSecond.beats.label}. So I win.",
                        )
                        chatPlayer(angry, "Whatever, let's just play the last round.")

                        val round3 = playRound(burntof, 3)
                        val hisThird = round3.beatenBy
                        mesbox("Burntof chose ${hisThird.label}.")
                        chatNpcSpecific(
                            "Burntof",
                            BURNOTOF,
                            drunk,
                            "Well, it looksh like I win, sho I don't have to help you out.",
                        )
                        mesbox("You quickly change your sign to ${hisThird.beatenBy.label}.")
                        chatPlayer(
                            shifty,
                            "Are you okay Burntof? I clearly beat you. Check my hand again.",
                        )

                        player.bimBurntof = BimBurntof.RECRUITED
                        quest.markOneRecruited(player)

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
                        chatNpcSpecific(
                            "Burntof",
                            BURNOTOF,
                            drunk,
                            "Tina! Bring me another beer pleash.",
                        )

                        val tina = cast.first { it.isType(TINA) }
                        chatNpcSpecific(
                            "Tina",
                            TINA,
                            happy,
                            "Sure thing Burntof, I'll be right there!",
                        )
                        tina.walk(RpsScene.TinaServePath.map { scene[it] })
                        access.delay(TINA_SERVE_TICKS)
                        tina.faceSquare(scene[RpsScene.Burntof])
                        burntof.faceSquare(scene[RpsScene.TinaServe])
                        chatNpcSpecific("Tina", TINA, happy, "Here you go.")
                        chatNpcSpecific("Burntof", BURNOTOF, drunk, "Thanksh Tina.")
                        access.soundSynth(SynthType(2401))
                    }

                    startDialogue {
                        if (quest.hasAllRecruits(player)) {
                            quest.markAllRecruited(player)
                            chatPlayer(
                                happy,
                                "Well, that's everyone. I guess I should head off the entrance on " +
                                    "the west side of Ice Mountain.",
                            )
                        } else {
                            chatPlayer(
                                neutral,
                                "That was interesting, but that's Burntof recruited. On to the " +
                                    "next person.",
                            )
                        }
                    }
                }
            } finally {
                region?.let(regionRepo::unprotect)
            }
        }

    private suspend fun Dialogue.playRound(burntof: Npc, round: Int): RpsSign {
        val sign =
            choice3(
                "Rock.",
                RpsSign.Rock,
                "Paper.",
                RpsSign.Paper,
                "Scissors.",
                RpsSign.Scissors,
                title = "Round $round: What sign will you show?",
            )
        access.anim("seq.rps_human")
        access.say("Rock, paper, scissors!")
        burntof.anim("seq.dwarf_rps")
        burntof.say("Rock, paper, uhm... scissors!")
        access.mesboxEmpty()
        access.delay(RPS_THROW_TICKS)
        return sign
    }

    private fun spawn(type: String, coords: CoordGrid): Npc {
        val npc = Npc(type, coords)
        npcRepo.add(npc, Int.MAX_VALUE)
        npc.noneMode()
        return npc
    }

    private fun despawn(npc: Npc) {
        if (npc.isSlotAssigned) {
            npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    private fun Dialogue.playMusic(midi: Int) {
        val row = MusicRow.all().firstOrNull { it.midi.id == midi } ?: return
        access.musicPlay(row)
    }

    private companion object {
        const val BURNOTOF = "npc.bim_cutscene_burntof"
        const val TINA = "npc.bim_cutscene_tina"
        const val RPS_THROW_TICKS = 5
        const val TINA_SERVE_TICKS = 8

        val BIM_RISING_SUN: RegionTemplate =
            RegionTemplate.create {
                copyAllLevels(368, 420) {
                    regionZoneX = 6
                    regionZoneZ = 6
                    zoneWidth = 4
                    zoneLength = 3
                }
            }
    }
}

private object RpsScene {
    val Vantage: CoordGrid = CoordGrid(2956, 3369, 1)
    val Burntof: CoordGrid = CoordGrid(2956, 3370, 1)
    val Tina: CoordGrid = CoordGrid(2956, 3373, 1)

    val TinaServePath: List<CoordGrid> =
        listOf(
            CoordGrid(2955, 3373, 1),
            CoordGrid(2955, 3371, 1),
            CoordGrid(2956, 3371, 1),
        )

    val TinaServe: CoordGrid = CoordGrid(2956, 3371, 1)

    val Camera: SceneCamera =
        SceneCamera(
            eye = CoordGrid(2961, 3369, 1),
            eyeHeight = 400,
            lookAt = CoordGrid(2955, 3369, 1),
            lookAtHeight = 180,
        )

    val Exit: CoordGrid = CoordGrid(2956, 3368, 0)
}

private enum class RpsSign(val label: String) {
    Rock("rock"),
    Paper("paper"),
    Scissors("scissors");

    val beats: RpsSign
        get() =
            when (this) {
                Rock -> Scissors
                Paper -> Rock
                Scissors -> Paper
            }

    val beatenBy: RpsSign
        get() =
            when (this) {
                Rock -> Paper
                Paper -> Scissors
                Scissors -> Rock
            }
}
