package org.rsmod.content.areas.city.yanille.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.spam
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class DragonInn @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.dragon_bartender") { startDialogue(it.npc) { bartender() } }
        onOpNpc1(GUARD_CAPTAIN) { startDialogue(it.npc) { guardCaptain() } }
        onOpNpcU(GUARD_CAPTAIN) {
            when (it.objType.internalName) {
                BEER -> startDialogue(it.npc) { guardCaptain() }
                GREENMANS_ALE,
                DRAGON_BITTER -> startDialogue(it.npc) { guardCaptainAle() }
                else -> mes("Nothing interesting happens.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bartender_(Dragon_Inn)
    // TODO(content): [miniquest:alfred_grimhands_barcrawl] barcrawl drink
    // TODO(content): [quest:zogre_flesh_eaters] showing the bartender the dragon inn tankard
    private suspend fun Dialogue.bartender() {
        chatNpc(happy, "What can I get you?")
        chatPlayer(quiz, "What's on the menu?")
        chatNpc(happy, "Dragon Bitter and Greenman's Ale, oh and some cheap beer.")
        val option =
            choice4(
                "I'll give it a miss I think.",
                1,
                "I'll try the Dragon Bitter.",
                2,
                "Can I have some Greenman's Ale?",
                3,
                "One cheap beer please!",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "I'll give it a miss I think.")
                chatNpc(neutral, "Come back when you're a little thirstier.")
            }
            2 -> {
                chatPlayer(happy, "I'll try the Dragon Bitter.")
                chatNpc(happy, "Ok, that'll be two coins.")
                if (buy(DRAGON_BITTER, 2)) {
                    player.spam("You buy a pint of Dragon Bitter.")
                }
            }
            3 -> {
                chatPlayer(happy, "Can I have some Greenman's Ale?")
                chatNpc(happy, "Ok, that'll be ten coins.")
                if (buy(GREENMANS_ALE, 10)) {
                    player.spam("You buy a pint of Greenman's Ale.")
                }
            }
            4 -> {
                chatPlayer(happy, "One cheap beer please!")
                chatNpc(happy, "That'll be 2 gold coins please!")
                if (buy(BEER, 2)) {
                    player.spam("You buy a pint of cheap beer.")
                    chatNpc(happy, "Have a super day!")
                }
            }
        }
    }

    private suspend fun Dialogue.buy(obj: String, price: Int): Boolean {
        if (!player.invTakeFee(price)) {
            chatPlayer(sad, "Oh dear. I don't seem to have enough money.")
            return false
        }
        player.invAddOrDrop(objRepo, obj)
        return true
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_Captain
    // TODO(content): [quest:the_hand_in_the_sand] quest stages and using beer on the captain
    private suspend fun Dialogue.guardCaptain() {
        chatPlayer(quiz, "Excuse me...")
        chatNpc(drunk, "I don' need a hand drinkin me beer, go 'way!")
        chatPlayer(confused, "But...")
        chatNpc(drunk, "Talk to tha' hand coz thish face ain't lishtnin'!")
    }

    private suspend fun Dialogue.guardCaptainAle() {
        chatNpc(drunk, "Yeeeuuuch! I hatesh that shtuff, jusht bring ush a beer! Mmmmm beer!")
    }

    private companion object {
        const val GUARD_CAPTAIN = "npc.handsand_guard_captain"
        const val BEER = "obj.beer"
        const val DRAGON_BITTER = "obj.dragon_bitter"
        const val GREENMANS_ALE = "obj.greenmans_ale"
    }
}
