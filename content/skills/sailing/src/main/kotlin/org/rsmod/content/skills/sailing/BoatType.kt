package org.rsmod.content.skills.sailing

data class BoatType(
    val key: String,
    val worldEntityType: Int,
    val templateZoneX: Int,
    val templateZoneZ: Int,
    val sizeZonesX: Int,
    val sizeZonesZ: Int,
    val deckLevel: Int,
    val boardDestDx: Int,
    val boardDestDz: Int,
    val dockFineDx: Int,
    val dockFineDz: Int,
    val baseSpeed: Int,
    val speedCap: Int,
    val acceleration: Int,
    val helmPlayerGrab: String,
    val helmSynthGrab: Int,
    val helmSynthRelease: Int,
    val deckLocs: List<DeckLoc>,
)

object BoatTypes {
    val RAFT =
        BoatType(
            key = "raft",
            worldEntityType = 1,
            templateZoneX = 480,
            templateZoneZ = 807,
            sizeZonesX = 1,
            sizeZonesZ = 1,
            deckLevel = 1,
            boardDestDx = 3,
            boardDestDz = 4,
            dockFineDx = 0,
            dockFineDz = 0,
            baseSpeed = 192,
            speedCap = 320,
            acceleration = 64,
            helmPlayerGrab = "seq.human_sailing_alpha_helm_raft01_active01",
            helmSynthGrab = 10792,
            helmSynthRelease = 10793,
            deckLocs = DeckLocs.RAFT,
        )

    val SKIFF =
        BoatType(
            key = "skiff",
            worldEntityType = 2,
            templateZoneX = 480,
            templateZoneZ = 806,
            sizeZonesX = 1,
            sizeZonesZ = 1,
            deckLevel = 1,
            boardDestDx = 4,
            boardDestDz = 4,
            dockFineDx = 128,
            dockFineDz = 0,
            baseSpeed = 192,
            speedCap = 384,
            acceleration = 64,
            helmPlayerGrab = "seq.human_sailing_alpha_helm_small01_active01",
            helmSynthGrab = 10807,
            helmSynthRelease = 10808,
            deckLocs = DeckLocs.SKIFF,
        )

    val SLOOP =
        BoatType(
            key = "sloop",
            worldEntityType = 3,
            templateZoneX = 480,
            templateZoneZ = 804,
            sizeZonesX = 1,
            sizeZonesZ = 2,
            deckLevel = 1,
            boardDestDx = 4,
            boardDestDz = 10,
            dockFineDx = 192,
            dockFineDz = 0,
            baseSpeed = 192,
            speedCap = 448,
            acceleration = 64,
            helmPlayerGrab = "seq.human_sailing_helm_3x8_active01",
            helmSynthGrab = 10807,
            helmSynthRelease = 10808,
            deckLocs = DeckLocs.SLOOP,
        )

    val all = listOf(RAFT, SKIFF, SLOOP)

    fun byKey(key: String): BoatType? = all.firstOrNull { it.key.equals(key, ignoreCase = true) }
}
