package org.rsmod.content.other.travelcoverage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DoorPairingTest {
    private fun closed(symbol: String, name: String = "Door", group: Int = -1) =
        DoorType(symbol, name, listOf("Open", "", "", "", ""), group)

    private fun opened(symbol: String, name: String = "Door", group: Int = -1) =
        DoorType(symbol, name, listOf("Close", "", "", "", ""), group)

    private fun pair(vararg types: DoorType): List<DoorSuggestion> {
        val bySymbol = types.associateBy { it.symbol }
        val closedSymbols = types.filter { it.ops.first() == "Open" }.map { it.symbol }
        return DoorPairing.pair(closedSymbols, bySymbol).sortedBy { it.closed }
    }

    @Test
    fun `a door pairs with its open twin named with open added`() {
        assertEquals(
            listOf(DoorSuggestion("loc.poordoor", "loc.poordooropen", DoorKind.Single)),
            pair(closed("loc.poordoor"), opened("loc.poordooropen")),
        )
        assertEquals(
            DoorSuggestion("loc.bankdoor", "loc.openbankdoor", DoorKind.Single),
            pair(closed("loc.bankdoor"), opened("loc.openbankdoor")).single(),
        )
    }

    @Test
    fun `closed and open words swap`() {
        assertEquals(
            "loc.metalgateopen",
            pair(closed("loc.metalgateclosed", "Gate"), opened("loc.metalgateopen", "Gate")).single().opened,
        )
    }

    @Test
    fun `double door halves are told apart by a matching twin`() {
        val result =
            pair(
                closed("loc.palacedoor_l"),
                closed("loc.palacedoor_r"),
                opened("loc.openpalacedoor_l"),
                opened("loc.openpalacedoor_r"),
            )
        assertEquals(listOf(DoorKind.DoorLeft, DoorKind.DoorRight), result.map { it.kind })
    }

    @Test
    fun `double gates use the picket gate kinds`() {
        val result =
            pair(
                closed("loc.metalgateclosedl", "Gate"),
                closed("loc.metalgateclosedr", "Gate"),
                opened("loc.metalgateopenl", "Gate"),
                opened("loc.metalgateopenr", "Gate"),
            )
        assertEquals(listOf(DoorKind.GateLeft, DoorKind.GateRight), result.map { it.kind })
    }

    @Test
    fun `a trailing l with no right twin is a single door`() {
        assertEquals(
            DoorKind.Single,
            pair(closed("loc.hall"), opened("loc.hallopen")).single().kind,
        )
    }

    @Test
    fun `a door with no open twin or an already tagged one is skipped`() {
        assertTrue(pair(closed("loc.lockeddoor")).isEmpty())
        assertTrue(pair(closed("loc.poordoor"), opened("loc.poordooropen", group = 1)).isEmpty())
        assertTrue(pair(closed("loc.poordoor", group = 0), opened("loc.poordooropen")).isEmpty())
    }

    @Test
    fun `an open model claimed by two closed doors is left alone`() {
        val shared = pair(closed("loc.door"), closed("loc.door_"), opened("loc.door_open"))
        assertTrue(shared.isEmpty(), "$shared")
    }

    @Test
    fun `the cache's no-group marker counts as untagged`() {
        assertEquals(
            1,
            pair(closed("loc.poordoor", group = 0xFFFF), opened("loc.poordooropen", group = 0xFFFF)).size,
        )
    }
}
