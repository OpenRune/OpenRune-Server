package org.rsmod.content.quest.area.asgarnia.icemountain

import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

internal object BimStage {
    const val NOT_STARTED = 0
    const val STARTED = 5
    const val HEARD_OF_CREW = 7
    const val RECRUITING = 10
    const val ONE_RECRUITED = 15
    const val ALL_RECRUITED = 20
    const val ENTRANCE_CUTSCENE = 25
    const val INSIDE_CUTSCENE = 30
    const val GUARDIAN_FIGHT = 35
    const val GUARDIAN_DEAD = 40
    const val COMPLETE = 120
}

internal object BimCheckal {
    const val NOT_STARTED = 0
    const val SENT_TO_ATLAS = 5
    const val TRAINING = 10
    const val TRAINED = 15
    const val FLEX_PROMPT = 20
    const val RECRUITED = 40
    const val AT_ENTRANCE = 45
    const val INSIDE = 50
}

internal object BimMarley {
    const val NOT_STARTED = 0
    const val SANDWICH_REQUESTED = 5
    const val RECIPE_KNOWN = 10
    const val SANDWICH_GIVEN = 35
    const val RECRUITED = 40
    const val AT_ENTRANCE = 45
    const val INSIDE = 50
}

internal object BimBurntof {
    const val NOT_STARTED = 0
    const val DRINK_REQUESTED = 5
    const val DRINK_GIVEN = 10
    const val RPS_ACCEPTED = 15
    const val RECRUITED = 40
    const val AT_ENTRANCE = 45
    const val INSIDE = 50
}

internal object BimRamarno {
    const val AT_ENTRANCE = 0
    const val DEPARTED = 1
    const val WORKSHOP = 2
}

internal var Player.bimStage: Int by intVarBit("varbit.bim")
internal var Player.bimCheckal: Int by intVarBit("varbit.bim_checkal")
internal var Player.bimMarley: Int by intVarBit("varbit.bim_marley")
internal var Player.bimBurntof: Int by intVarBit("varbit.bim_burntof")
internal var Player.bimClaimedReward: Int by intVarBit("varbit.bim_claimed_reward")
internal var Player.bimWorkoutCount: Int by intVarBit("varbit.bim_workout_counter")
internal var Player.bimRamarnoIntro: Int by intVarBit("varbit.camdozaal_ramarno_intro")
internal var Player.emoteFlex: Int by intVarBit("varbit.emote_flex")
