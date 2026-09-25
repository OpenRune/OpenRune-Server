package org.rsmod.content.areas.city.ardougne.npcs

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.content.other.pets.cats.CatCare
import org.rsmod.content.other.pets.cats.Cats
import org.rsmod.content.other.pets.storesAnyObj
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Civilian @Inject constructor(private val care: CatCare) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(BLONDE) { talk(it.npc) { blonde() } }
        onOpNpc1(BROWN) { talk(it.npc) { brown() } }
        onOpNpc1(BALD) { talk(it.npc) { bald() } }
        for (npc in listOf(BLONDE, BROWN, BALD)) {
            onOpNpcU(npc) { useItem(it.npc, it.objType) }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc, intro: suspend Dialogue.() -> Unit) =
        startDialogue(npc) {
            intro()
            when {
                hasAdultCat(player) -> offerCat()
                WITCHS_CAT in player.inv -> witchsCat()
                else -> chatPlayer(neutral, "No, you're right, you don't see many around.")
            }
        }

    private suspend fun Dialogue.blonde() {
        chatPlayer(happy, "Hello there.")
        chatNpc(bored, "Oh hello, I'm sorry, I'm a bit worn out.")
        chatPlayer(quiz, "Busy day?")
        chatNpc(
            angry,
            "Oh, It's those mice! They're everywhere! What I really need is a cat. But they're hard to " +
                "come by nowadays.",
        )
    }

    private suspend fun Dialogue.brown() {
        chatPlayer(happy, "Hi there.")
        chatNpc(happy, "Good day to you traveller.")
        chatPlayer(quiz, "What are you up to?")
        chatNpc(bored, "Chasing mice as usual! It's all I seem to do nowadays.")
        chatPlayer(quiz, "You must waste a lot of time?")
        chatNpc(neutral, "Yes, but what can you do? It's not like there's many cats around here!")
    }

    private suspend fun Dialogue.bald() {
        chatPlayer(happy, "Hello there.")
        chatNpc(bored, "I'm a bit busy to talk right now, sorry.")
        chatPlayer(quiz, "Why? What are you doing?")
        chatNpc(angry, "Trying to kill these mice! What I really need is a cat!")
    }

    private suspend fun Dialogue.offerCat() {
        val sell = choice2("I have a cat that I could sell.", true, "Nope, they're not easy to get hold of.", false)
        if (!sell) {
            chatPlayer(neutral, "Nope, they're not easy to get hold of.")
            return
        }
        chatPlayer(happy, "I have a cat that I could sell.")
        chatNpc(quiz, "You don't say, is that it ?")
        chatPlayer(happy, "Say hello to a real mouse killer!")
        chatNpc(happy, "Hmmm, not bad, not bad at all. Looks like it's a lively one.")
        chatPlayer(worried, "Erm...kind of...")
        chatNpc(neutral, "I don't have much in the way of money. I do have these!")
        mesbox("The peasant shows you a sack of Death Runes.")
        chatNpc(
            happy,
            "The dwarves bring them from the mine for us. Tell you what, I'll give you ${runes(player)} " +
                "Death Runes for the cat.",
        )
        val deal = choice2("Nope, I'm not parting for that.", false, "Ok then, you've got a deal.", true)
        if (!deal) {
            chatPlayer(neutral, "Nope, I'm not parting for that.")
            chatNpc(angry, "Well, I'm not giving you anymore!")
            return
        }
        chatPlayer(happy, "Ok then, you've got a deal.")
        chatNpc(happy, "Great! Hand over the cat and I'll give you the runes.")
    }

    private suspend fun Dialogue.witchsCat() {
        chatPlayer(happy, "I have a cat...look!")
        chatNpc(bored, "Hmmm...doesn't look like it's seen daylight in years. That's not going to catch any mice!")
    }

    private suspend fun ProtectedAccess.useItem(npc: Npc, obj: ItemServerType) {
        if (obj.internalName == WITCHS_CAT) {
            startDialogue(npc) { witchsCat() }
            return
        }
        val cat = Cats.forObj(obj.id) ?: return
        if (cat.isKitten) {
            mes("Nothing interesting happens.")
            return
        }
        val runes = runes(player)
        if (!invDel(inv, cat.obj, 1).success) {
            return
        }
        invAdd(inv, DEATH_RUNE, runes)
        startDialogue(npc) {
            mesbox("You hand over the cat. You are given $runes Death Runes.")
            chatNpc(happy, "Great, thanks for that!")
            chatPlayer(happy, "That's ok, take care.")
        }
    }

    private fun hasAdultCat(player: Player): Boolean {
        val following = care.following(player)
        return following?.isKitten == false || player.storesAnyObj(Cats.adultObjs())
    }

    private fun runes(player: Player): Int = if (player.ardougneEasyDiary) DIARY_RUNES else RUNES

    private companion object {
        const val BLONDE = "npc.wantcat1"
        const val BROWN = "npc.wantcat2"
        const val BALD = "npc.wantcat3"
        const val DEATH_RUNE = "obj.deathrune"
        const val WITCHS_CAT = "obj.cavewitchcat"
        const val RUNES = 100
        const val DIARY_RUNES = 200

        val Player.ardougneEasyDiary: Boolean by boolVarBit("varbit.ardougne_diary_easy_complete")
    }
}
