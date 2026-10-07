package org.rsmod.content.quest.area.varrock.childrenofthesun

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.quest.area.misthalin.SceneCamera
import org.rsmod.content.quest.area.misthalin.lockedScene
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

internal interface CotsScenes {
    suspend fun delegation(access: ProtectedAccess)

    suspend fun eavesdrop(access: ProtectedAccess)

    suspend fun toRoof(access: ProtectedAccess)

    suspend fun interrogation(access: ProtectedAccess)
}

internal object CotsPlaces {
    val King = CoordGrid(3213, 3431)
    val Advisor = CoordGrid(3214, 3432)
    val Guards = listOf(CoordGrid(3212, 3433), CoordGrid(3215, 3433))
    val DelegationStart =
        listOf(
            CoordGrid(3222, 3430),
            CoordGrid(3223, 3431),
            CoordGrid(3223, 3429),
            CoordGrid(3224, 3430),
        )
    val DelegationStop =
        listOf(
            CoordGrid(3216, 3430),
            CoordGrid(3217, 3431),
            CoordGrid(3217, 3429),
            CoordGrid(3218, 3430),
        )
    val KnightsStart =
        listOf(
            CoordGrid(3224, 3432),
            CoordGrid(3224, 3428),
            CoordGrid(3225, 3431),
            CoordGrid(3225, 3429),
            CoordGrid(3226, 3430),
            CoordGrid(3226, 3429),
        )
    val KnightsStop =
        listOf(
            CoordGrid(3218, 3432),
            CoordGrid(3218, 3428),
            CoordGrid(3219, 3431),
            CoordGrid(3219, 3429),
            CoordGrid(3220, 3430),
            CoordGrid(3220, 3428),
        )
    val TobynScene = CoordGrid(3213, 3438)
    val BagGuardStart = CoordGrid(3212, 3441)
    val BagGuardStop = CoordGrid(3219, 3434)

    val DelegationCamera =
        SceneCamera(
            eye = CoordGrid(3223, 3425),
            eyeHeight = 450,
            lookAt = CoordGrid(3215, 3431),
            lookAtHeight = 100,
        )

    val EavesdropVantage = CoordGrid(3259, 3401)
    val EavesdropWindow = CoordGrid(3260, 3401)
    val HideoutTable = CoordGrid(3261, 3399)
    const val HideoutTableSize = 2
    val HideoutGang =
        listOf(
            "npc.vmq1_bag_guard_varrock",
            "npc.vmq1_bandit_1_varrock",
            "npc.vmq1_bandit_2_varrock",
            "npc.vmq1_bandit_3_varrock",
            "npc.vmq1_bandit_4_varrock",
        )

    // The client hides roofs only for a camera on a roofed tile under 800 units high.
    val EavesdropCamera =
        SceneCamera(
            eye = CoordGrid(3261, 3403),
            eyeHeight = 600,
            lookAt = CoordGrid(3261, 3399),
            lookAtHeight = 100,
        )

    val RoofArrival = CoordGrid(3204, 3473, 2)
    val RoofTobyn = CoordGrid(3204, 3474, 2)
    val RoofItzla = CoordGrid(3202, 3474, 2)
    val RoofCell = CoordGrid(3203, 3471, 2)
    val CellPlayer = CoordGrid(3204, 3470, 2)
    val CellItzla = CoordGrid(3202, 3470, 2)
    val CellBanditFacing = CoordGrid(3203, 3470, 2)
    val RoofCamera =
        SceneCamera(
            eye = CoordGrid(3204, 3478, 2),
            eyeHeight = 350,
            lookAt = CoordGrid(3203, 3473, 2),
            lookAtHeight = 80,
        )
    val CellCamera =
        SceneCamera(
            eye = CoordGrid(3206, 3471, 2),
            eyeHeight = 420,
            lookAt = CoordGrid(3203, 3470, 2),
            lookAtHeight = 110,
        )
}

internal class WorldScenes @Inject constructor(private val npcRepo: NpcRepository) : CotsScenes {
    override suspend fun delegation(access: ProtectedAccess) {
        access.ifCloseChat()
        val actors = mutableListOf<Npc>()
        lateinit var delegates: List<Npc>
        lateinit var knights: List<Npc>
        lateinit var tobynActor: Npc
        lateinit var guard: Npc
        access.lockedScene(
            vantage = access.player.coords,
            faceAt = CotsPlaces.King,
            camera = CotsPlaces.DelegationCamera,
            underFade = {
                val east = CotsPlaces.DelegationCamera.eye
                actors += spawn("npc.vmq1_king_roald_cutscene", CotsPlaces.King, east)
                actors += spawn("npc.vmq1_aeonisig_raispher_cutscene", CotsPlaces.Advisor, east)
                for (post in CotsPlaces.Guards) {
                    actors += spawn("npc.fai_varrock_guard02", post, east)
                }
                delegates =
                    listOf(
                        spawn(CotsNpc.ItzlaCutscene, CotsPlaces.DelegationStart[0]),
                        spawn("npc.vmq1_servius_cutscene", CotsPlaces.DelegationStart[1]),
                        spawn("npc.vmq1_furia_cutscene", CotsPlaces.DelegationStart[2]),
                        spawn("npc.vmq1_ennius_cutscene", CotsPlaces.DelegationStart[3]),
                    )
                knights =
                    CotsPlaces.KnightsStart.mapIndexed { index, start ->
                        spawn("npc.vmq1_knight_${index + 1}", start)
                    }
                tobynActor = spawn("npc.vmq1_guard_sergeant_cutscene", CotsPlaces.TobynScene)
                guard = spawn("npc.vmq1_bag_guard", CotsPlaces.BagGuardStart)
                actors += delegates + knights + tobynActor + guard
            },
            teardown = { actors.forEach(::despawn) },
        ) {
            startDialogue {
                delegates.forEachIndexed { i, npc -> npc.walk(CotsPlaces.DelegationStop[i]) }
                knights.forEachIndexed { i, npc -> npc.walk(CotsPlaces.KnightsStop[i]) }
                delay(9)
                for (npc in delegates + knights) npc.faceSquare(CotsPlaces.King)
                delegationArrives()
                guard.walk(CotsPlaces.BagGuardStop)
                delay(1)
                tobynActor.faceSquare(CotsPlaces.BagGuardStart)
                guardLeaves()
                delay(1)
            }
        }
    }

    override suspend fun eavesdrop(access: ProtectedAccess) {
        access.ifCloseChat()
        val back = access.player.coords
        access.lockedScene(
            vantage = CotsPlaces.EavesdropVantage,
            faceAt = CotsPlaces.EavesdropWindow,
            camera = CotsPlaces.EavesdropCamera,
            returnTo = back,
            underFade = {
                val size = CotsPlaces.HideoutTableSize
                for (npc in hideoutGang()) npc.faceSquare(CotsPlaces.HideoutTable, size, size)
            },
        ) {
            startDialogue { banditsScheme() }
        }
    }

    private fun hideoutGang(): List<Npc> =
        npcRepo
            .findAll(ZoneKey.from(CotsPlaces.HideoutTable), zoneRadius = 1)
            .filter { npc -> CotsPlaces.HideoutGang.any(npc::isType) }
            .toList()

    override suspend fun toRoof(access: ProtectedAccess) {
        access.ifCloseChat()
        access.lockedScene(
            vantage = CotsPlaces.RoofArrival,
            faceAt = CotsPlaces.RoofTobyn,
            camera = CotsPlaces.RoofCamera,
        ) {
            delay(2)
        }
    }

    override suspend fun interrogation(access: ProtectedAccess) {
        access.ifCloseChat()
        val back = access.player.coords
        var itzla: Npc? = null
        access.lockedScene(
            vantage = CotsPlaces.CellPlayer,
            faceAt = CotsPlaces.RoofCell,
            camera = CotsPlaces.CellCamera,
            returnTo = back,
            underFade = {
                cellBandit()?.faceSquare(CotsPlaces.CellBanditFacing)
                itzla = spawn(CotsNpc.ItzlaCutscene, CotsPlaces.CellItzla, CotsPlaces.RoofCell)
            },
            teardown = { itzla?.let(::despawn) },
        ) {
            startDialogue { interrogationInCell() }
        }
    }

    private fun cellBandit(): Npc? =
        npcRepo.findAll(CotsPlaces.RoofCell).firstOrNull { it.isType(CotsNpc.CellBandit) }

    private fun spawn(type: String, coords: CoordGrid, facing: CoordGrid? = null): Npc {
        val npc = Npc(type, coords)
        npcRepo.add(npc, SceneLifespan)
        npc.noneMode()
        facing?.let(npc::faceSquare)
        return npc
    }

    private fun despawn(npc: Npc) {
        if (npc.isSlotAssigned) {
            npcRepo.del(npc, Int.MAX_VALUE)
        }
    }

    private companion object {
        const val SceneLifespan = 400
    }
}

internal suspend fun Dialogue.delegationArrives() {
    alina(
        happy,
        "There they are! The children of the sun! That's Itzla Arkan, heir to the throne of " +
            "Varlamore, and next to him is Servius, the Teokan of Ralos!",
    )
    noah(confused, "A Teokan? What on earth is a Teokan?")
    alina(neutral, "It means High Priest. He leads the religion over in Varlamore.")
    noah(bored, "Huh.")
}

internal suspend fun Dialogue.guardLeaves() {
    tobyn(quiz, "You there! What are you doing with that bag?")
    bagGuard(worried, "Oh! Er... just taking some supplies out to the guards on the gates.")
    tobyn(neutral, "Then be quick about it. I want everybody at their posts.")
    noah(quiz, "That's an awfully big bag he's lugging about.")
    chatPlayer(quiz, "Hmm... Now where could he be taking that?")
}

internal suspend fun Dialogue.banditsScheme() {
    chatPlayer(quiz, "What are they up to in there...?")
    bandit(CotsNpc.RedHood, angry, "You're late.")
    bagGuard(
        angry,
        "I'm here, aren't I? If you wanted it done faster you should have stolen the uniforms " +
            "yourself!",
    )
    bandit(CotsNpc.Woman, verymad, "Enough! There's no time for this. Did you bring everything?")
    bagGuard(shifty, "Most of it.")
    bandit(CotsNpc.RedHood, confused, "Most of it?")
    bagGuard(worried, "I took what I could get my hands on!")
    bandit(
        CotsNpc.Bearded,
        neutral,
        "We'll have to make do. The delegation is already inside the palace, and we need to " +
            "be ready the moment they come out.",
    )
    bandit(
        CotsNpc.Tanned,
        neutral,
        "Quite. You all know who the target is. Change quickly and get to your posts.",
    )
    bagGuard(shifty, "Well, good luck. You're going to need it.")
    bandit(
        CotsNpc.Bearded,
        laugh,
        "You worry too much, the lot of you. It'll go smoothly. Now, let's move.",
    )
    chatPlayer(
        worried,
        "That doesn't sound good. I'd better let that sergeant know right away.",
    )
}

internal suspend fun Dialogue.interrogationInCell() {
    val name = access.player.displayName
    itzla(neutral, "Nilsal to you, iknami. I hear you've landed yourself in a spot of trouble.")
    bandit(CotsNpc.Bearded, angry, "I'm not telling you anything.")
    itzla(happy, "Ah, tetamo. What a shame. Never mind, we'll have you talking soon enough.")
    itzla(
        quiz,
        "You'll have to forgive me, I'm rather new around here and your customs are strange " +
            "to me. Tell me, iknami, what is the preferred method of interrogation in these " +
            "parts?",
    )
    bandit(CotsNpc.Bearded, confused, "You're asking me how you should interrogate me?")
    itzla(
        happy,
        "It seemed only polite to ask. Back home in Varlamore we have all sorts of " +
            "entertaining methods. We could try one or two of them, if you like?",
    )
    bandit(CotsNpc.Bearded, worried, "If I like?")
    itzla(happy, "Of course! Now, tell me, what do you think of chicken?")
    bandit(CotsNpc.Bearded, confused, "Chicken?")
    itzla(
        quiz,
        "$name, do you not have chickens here? Forgive me, all of this is quite new to me.",
    )
    chatPlayer(neutral, "We do have chickens. The farms around here are full of them.")
    itzla(
        quiz,
        "Then why the confusion? Have you never seen one? You poor sheltered thing. Did your " +
            "parents never show you the wonders of the countryside?",
    )
    bandit(CotsNpc.Bearded, angry, "I know what a chicken is!")
    itzla(laugh, "Then why didn't you say so?")
    bandit(
        CotsNpc.Bearded,
        verymad,
        "I don't know what game you're playing, but it won't work on me!",
    )
    itzla(
        neutral,
        "No game at all, iknami. You see, one of our favoured methods involves just a little " +
            "fire, and you would not believe the smell of human flesh once it gets going...",
    )
    bandit(CotsNpc.Bearded, shocked, "You're going to cook me?")
    itzla(
        laugh,
        "Cook you? Certainly not! That would mean I intended to eat you, and I am no " +
            "barbarian! Although I am curious whether it tastes like chicken as well...",
    )
    bandit(CotsNpc.Bearded, worried, "Alright, enough! I'll tell you whatever you want!")
    itzla(happy, "Kuaini!")
    itzla(quiz, "So, who is it you work for?")
    bandit(
        CotsNpc.Bearded,
        sad,
        "I never learned their name. They just paid us to attack the delegation. They didn't " +
            "look local either. I'd say it was one of your lot!",
    )
    itzla(quiz, "And why did they want the delegation attacked?")
    bandit(
        CotsNpc.Bearded,
        sad,
        "They didn't say, but it was the priest they were after. He was the target!",
    )
    itzla(confused, "So a Varlamorian paid you to murder the Teokan? That's not much to go on.")
    bandit(CotsNpc.Bearded, worried, "That's all I know, I swear!")
    itzla(neutral, "Hmm...")
    itzla(neutral, "Well, I think we're done here. Come along, $name.")
}
