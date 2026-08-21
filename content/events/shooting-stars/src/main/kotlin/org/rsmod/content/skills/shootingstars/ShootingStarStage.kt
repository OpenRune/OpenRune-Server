package org.rsmod.content.skills.shootingstars

data class ShootingStarStage(
    val loc: String,
    val size: Int,
    val miningLevel: Int,
)

object ShootingStarStages {
    val ALL =
        listOf(
            ShootingStarStage("loc.star_size_nine_star", size = 9, miningLevel = 90),
            ShootingStarStage("loc.star_size_eight_star", size = 8, miningLevel = 80),
            ShootingStarStage("loc.star_size_seven_star", size = 7, miningLevel = 70),
            ShootingStarStage("loc.star_size_six_star", size = 6, miningLevel = 60),
            ShootingStarStage("loc.star_size_five_star", size = 5, miningLevel = 50),
            ShootingStarStage("loc.star_size_four_star", size = 4, miningLevel = 40),
            ShootingStarStage("loc.star_size_three_star", size = 3, miningLevel = 30),
            ShootingStarStage("loc.star_size_two_star", size = 2, miningLevel = 20),
            ShootingStarStage("loc.star_size_one_star", size = 1, miningLevel = 10),
        )

    fun rollInitialStageIndex(random: kotlin.random.Random = kotlin.random.Random.Default): Int {
        val roll = random.nextInt(100)
        return when {
            roll < 20 -> 0
            roll < 45 -> 1
            roll < 75 -> 2
            else -> 3
        }
    }

    val LOC_NAMES: List<String> = ALL.map { it.loc }

    const val XP_MEMBERS = 32.0
    const val XP_F2P = 16.0
    const val LAYER_DURATION_CYCLES = 700
}
