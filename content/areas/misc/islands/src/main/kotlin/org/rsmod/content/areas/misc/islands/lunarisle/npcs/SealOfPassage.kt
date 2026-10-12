package org.rsmod.content.areas.misc.islands.lunarisle.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess

private const val SEAL_OF_PASSAGE = "obj.lunar_seal_of_passage"

// https://oldschool.runescape.wiki/w/Lunar_Isle: every islander except 'Birds-Eye' Jack asks for
// the seal, worn or carried.
// TODO(content): [diary:fremennik] the elite diary removes the need for the seal
internal fun ProtectedAccess.hasSealOfPassage(): Boolean =
    SEAL_OF_PASSAGE in player.inv || SEAL_OF_PASSAGE in player.worn

// https://oldschool.runescape.wiki/w/Transcript:Selene (without the Seal of Passage)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] the player is teleported to Rellekka before the last line; the
// wiki gives no landing tile
internal suspend fun Dialogue.refuseWithoutSeal() {
    chatPlayer(happy, "Hi, I...")
    chatNpc(angry, "What are you doing here, Fremennik?!")
    chatPlayer(worried, "I have a seal of pass...")
    chatNpc(angry, "No you do not! Begone!")
    chatPlayer(sad, "Ooops. Suppose I need a seal of passage when I'm walking around that island.")
}
