package org.alter.areas.lumbridge.npcs

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onNpcOption

class CookingTutorPlugin() : PluginEvent() {

    override fun init() {
        spawnNpc("npcs.aide_tutor_cooking", x = 3233, z = 3195, walkRadius = 3, height = 0)

        onNpcOption("npcs.aide_tutor_cooking", "talk-to") {
            player.queue { menu(player) }
        }
    }

    private suspend fun QueueTask.menu(player: Player) {
        when (options(player, "Can you teach me the basics of cooking please?", "Tell me about different food I can make.", "Goodbye.")) {
            1 -> {
                chatPlayer(player, "Can you teach me the basics of cooking please?", animation = "sequences.chathap1")
                chatNpc(player, "The simplest thing to cook is raw meat or fish.", animation = "sequences.chatneu1")
                chatNpc(player, "Fish can be caught, speak to the fishing tutor south of<br>here in the swamp. Killing cows or chickens will yield<br>raw meat to cook too.", animation = "sequences.chathap3",)
                itemMessageBox(player, "When you have a full inventory of meat or fish, find a<br>range. Look for this icon on your minimap.", item = "items.range_icon_dummy", amountOrZoom = 400,)
                chatNpc(player, "You can use your own fire... but it's not as effective<br>and you'll burn more. To build a fire use a tinderbox<br>on logs.", animation = "sequences.chathap3",)
                chatNpc(player, "Once you've found your range, click on it. This will<br>bring up a menu of the food you can cook.", animation = "sequences.chatneu2")
                itemMessageBox(player, "When you have a full inventory of cooked food, drop<br>the useless burnt food and find a bank. Look for this<br>symbol on your minimap after climbing the stairs of the<br>Lumbridge Castle to the top. There are numerous", item = "items.bank_icon_dummy", amountOrZoom = 400,)
                itemMessageBox(player, "banks around the world, all marked with that symbol.", item = "items.bank_icon_dummy", amountOrZoom = 400)
                chatNpc(player, "If you're interested in quests, I heard my friend the<br>cook in Lumbridge Castle is in need of a hand. Just<br>talk to him and he'll set you off.",)
                menu(player)
            }

            2 -> {
                chatPlayer(player, "Tell me about different foods.", animation = "sequences.chathap1")
                foodStuffs(player)
            }

            3 -> chatPlayer(player, "Goodbye.", animation = "sequences.chatneu1")
        }
    }

    private suspend fun QueueTask.foodStuffs(player: Player) {
        when (options(player, "Fish and Meat", "Pies and Pizza", "Go back to teaching")) {
            1 -> {
                doubleItemMessageBox(player, "Fish and meat of most varieties can be cooked very<br>simply on either a fire or range, experiment which one<br>works for you.", item1 = "items.raw_beef", item2 = "items.cooked_meat", amount1 = 400, amount2 = 400,)
                foodStuffs(player)
            }

            2 -> {
                doubleItemMessageBox(player, "Use a pot of flour with a bucket of water. You will then<br>get an option to make bread dough, pitta bread dough,<br>pastry dough, or pizza dough. Select pizza or pastry<br>dough.", item1 = "items.bucket_water", item2 = "items.pot_flour", amount1 = 400, amount2 = 400,)
                doubleItemMessageBox(player, "Use the pastry dough with a pie dish then add your<br>filling such as apple or red berries.", item1 = "items.piedish", item2 = "items.pastry_dough", amount1 = 400, amount2 = 400,)
                chatNpc(player, "Finally cook your pie by using the unbaked pie on a<br>cooking range. Mmmm...pie.", animation = "sequences.chathap2")
                chatNpc(player, "There's pizza too! Find yourself some tomato and<br>cheese, use on the Pizza dough. Cook the pizza on a<br>range then add any other toppings you want, such as<br>anchovies.", animation = "sequences.chathap4",)
                foodStuffs(player)
            }

            3 -> menu(player)
        }
    }
}