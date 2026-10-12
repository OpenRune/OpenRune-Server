package org.rsmod.content.interfaces.emotes

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Test
import org.rsmod.api.enums.NamedEnums
import org.rsmod.api.testing.GameTestState

class EmotesScriptTest {
    @Test
    fun GameTestState.`validate all emotes`() =
        runGameTest(EmotesScript::class) {
            // Open the Emotes tab, otherwise button clicks would be discarded.
            player.ifOpenOverlay("interface.emote", "component.toplevel_osrs_stretch:side12")

            val emoteList = ServerCacheManager.fromComponent("component.emote:contents")
            val emoteSlots = NamedEnums.emote_names.keys
            for (slot in emoteSlots) {
                assertDoesNotThrow("Emote for slot is not implemented: slot=$slot") {
                    player.ifButton(emoteList, slot)
                    advance()
                }
            }
        }
}
