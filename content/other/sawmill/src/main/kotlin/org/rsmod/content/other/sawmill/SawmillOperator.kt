package org.rsmod.content.other.sawmill

private const val PLANK_OFFER =
    "Do you want me to make some planks for you? I can make planks from wood, oak, teak and " +
        "mahogany logs. Or would you like to buy some other housing supplies?"

enum class SawmillOperator(
    val npc: String,
    val greeting: String,
    val explainsPlanks: Boolean,
    val acknowledgesSupplies: Boolean,
    val declineOption: String,
    val farewell: String,
) {
    LumberYard(
        npc = "npc.poh_sawmill_opp",
        greeting = "Hello there. $PLANK_OFFER",
        explainsPlanks = false,
        acknowledgesSupplies = true,
        declineOption = "I'm good, thanks.",
        farewell =
            "Well come back when you want some. You'll struggle to find quality planks " +
                "anywhere but here!",
    ),
    Prifddinas(
        npc = "npc.prif_sawmill_operator",
        greeting = PLANK_OFFER,
        explainsPlanks = true,
        acknowledgesSupplies = false,
        declineOption = "Nothing, thanks",
        farewell =
            "Well come back when you want some. You can't get good quality planks anywhere " +
                "but here!",
    ),
    Auburnvale(
        npc = "npc.auburn_sawmill_operator",
        greeting = "Nilsal. $PLANK_OFFER",
        explainsPlanks = false,
        acknowledgesSupplies = true,
        declineOption = "I'm good, thanks",
        farewell =
            "Well come back when you want some. You'll struggle to find quality planks " +
                "anywhere but here!",
    ),
}
