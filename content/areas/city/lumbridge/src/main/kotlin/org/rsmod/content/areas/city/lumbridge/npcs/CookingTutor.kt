package org.rsmod.content.areas.city.lumbridge.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CookingTutor : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.aide_tutor_cooking") { startDialogue(it.npc) { cookingTutor() } }
    }

    private suspend fun Dialogue.cookingTutor() {
        chatPlayer(neutral, "Hello.")
        chatNpc(happy, "Hello there! Are you interested in hearing all about cooking?")
        var asked = false
        while (true) {
            val topic =
                menu(
                    buildList {
                        if (!asked) add("How can I train my cooking?" to 1)
                        add("What kinds of things can I cook?" to 2)
                        add("No, thank you." to 3)
                    }
                )
            when (topic) {
                1 -> {
                    trainCooking()
                    asked = true
                }
                2 -> foodTypes()
                else -> {
                    chatPlayer(neutral, "No, thank you.")
                    chatNpc(happy, "Well, just come back any time you want to know how to cook up a storm!")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.trainCooking() {
        chatPlayer(quiz, "How can I train my cooking?")
        if (access.statBase("stat.cooking") >= EXPERIENCED_LEVEL) {
            chatNpc(
                happy,
                "Now you have gained some experience in cooking, you can look forward to creating many " +
                    "more dishes.",
            )
            chatNpc(
                happy,
                "If you feel up to the task, you could try your hand at making pizza. A pizza is " +
                    "comprised of flour with water. Put some tomato on there, sprinkle on some cheese " +
                    "then cook it. You might consider adding a topping too.",
            )
            chatNpc(
                happy,
                "Potatoes are another great food, you can put all sorts of toppings on them if you so " +
                    "desire. Such as eggs and tomato.",
            )
            chatNpc(shifty, "But if you are looking for something with a bit of a kick, I suggest curry.")
            chatNpc(quiz, "Is there anything else you want to know?")
            return
        }
        chatNpc(
            happy,
            "The simplest thing to cook is raw meat or fish. Shrimp can be caught south of here, to the " +
                "east of Lumbridge Swamp. You could ask the fishing tutor for a net if you have not got " +
                "one.",
        )
        chatNpc(
            happy,
            "Alternatively, you could acquire some raw beef or chicken. You can find cows and chickens " +
                "north of here. Go over the bridge then follow the path north, you can't miss them!",
        )
        objbox(
            RANGE_ICON,
            "When you have a full inventory of meat or fish, find a range. Look for this icon on your " +
                "minimap.",
        )
        chatNpc(happy, "You could use my range here if you like.")
        chatNpc(
            happy,
            "If you wanted to, my friend the cook in Lumbridge Castle has a range you could use. You " +
                "might find you burn even less on that range as its top notch.",
        )
        chatNpc(
            happy,
            "Alternatively, you can use your own fire, but it's not as effective and the food will burn " +
                "more frequently. To make a fire, use a tinderbox on some logs.",
        )
        chatNpc(
            happy,
            "Once you've found your range or fire, use your raw food on it. This will bring up a menu of " +
                "the food you can cook. Then select the food you want.",
        )
        chatNpc(happy, "When you have a full inventory of cooked food, drop the useless burnt food and find a bank.")
        objbox(
            BANK_ICON,
            "Look for this symbol on your minimap after climbing the stairs of the Lumbridge Castle to " +
                "the top. There are numerous banks around the world, all marked with that symbol.",
        )
        if (!QuestRequirements.hasCompleted(player, "quest_cooksassistant")) {
            chatNpc(
                happy,
                "If you're interested in quests, I heard my friend the cook in Lumbridge Castle is in need " +
                    "of a hand. Just talk to him and he'll set you off.",
            )
        }
        chatNpc(quiz, "Is there anything else you want to know?")
    }

    private suspend fun Dialogue.foodTypes() {
        chatPlayer(quiz, "What kinds of things can I cook?")
        chatNpc(
            happy,
            "Many things! You can cook anything from fish to poultry. You could try your hand at making " +
                "baked potatoes or even brew some of the good stuff.",
        )
        chatNpc(quiz, "What would you like to hear about?")
        while (true) {
            val topic =
                menu(
                    "Tell me about Fish and Meat." to 1,
                    "Tell me about Brewing." to 2,
                    "Tell me about Vegetables." to 3,
                    "Tell me about Pies and Pizzas." to 4,
                    "Go back to teaching." to 5,
                )
            when (topic) {
                1 -> {
                    chatPlayer(quiz, "Tell me about Fish and Meat.")
                    doubleobjbox(
                        "obj.raw_beef",
                        "obj.cooked_meat",
                        "Fish and meat of most varieties can be cooked very simply on either a fire or " +
                            "range, experiment which one works for you.",
                    )
                    chatNpc(happy, "You might find that when killing monsters, they drop some raw meat for you to cook.")
                    chatNpc(
                        happy,
                        "Some of them might have hidden properties. Such as anglerfish, but you will find it " +
                            "hard getting hold of any of them at this time.",
                    )
                    chatNpc(
                        happy,
                        "Most of the time, cooking meat and fish requires nothing else but a range or fire. " +
                            "More complicated dishes might require specialized equipment.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about Brewing.")
                    doubleobjbox(
                        "obj.dragon_bitter",
                        "obj.beer",
                        "You can brew your own beers using the fermenting vats in either Keldagrim or Port " +
                            "Phasmatys.",
                    )
                    chatNpc(
                        happy,
                        "You might find it hard getting to these places but you can always buy beer at any " +
                            "pub. Some pubs sell unique beers too.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Tell me about Vegetables.")
                    objbox("obj.tuna_potato", "Baked potatoes are a more advanced food and are healthy too!")
                    chatNpc(
                        happy,
                        "They require many different ingredients to make, such as butter and vegetables, but " +
                            "you will find it hard to acquire the ingredients needed.",
                    )
                }
                4 -> {
                    chatPlayer(quiz, "Tell me about Pies and Pizzas.")
                    doubleobjbox(
                        "obj.bucket_water",
                        "obj.pot_flour",
                        "Use a pot of flour with a bucket of water. You will then need to make some pastry " +
                            "dough.",
                    )
                    doubleobjbox(
                        "obj.piedish",
                        "obj.pastry_dough",
                        "Use the pastry dough with a pie dish to get a pie shell, then add your choice of " +
                            "filling such as apples or red berries.",
                    )
                    chatNpc(happy, "Finally cook your pie by using the unbaked pie on a cooking range. Mmmm...pie.")
                    chatNpc(
                        happy,
                        "Next, we have pizza. You will first need to make yourself a pizza base, use a pot of " +
                            "flour with a bucket of water and make a pizza base.",
                    )
                    doubleobjbox(
                        "obj.tomato",
                        "obj.cheese",
                        "Now you will need to get yourself a tomato and some cheese. Use the tomato on the " +
                            "pizza base and then use the cheese. Cook the uncooked pizza base and there you " +
                            "have yourself a plain pizza.",
                    )
                    chatNpc(happy, "If you want to, you could add yourself a topping to the pizza too. Why not try some anchovies?")
                }
                else -> return
            }
            chatNpc(quiz, "Would you like to hear about anything else I mentioned?")
        }
    }

    private companion object {
        const val EXPERIENCED_LEVEL = 20
        const val RANGE_ICON = "obj.range_icon_dummy"
        const val BANK_ICON = "obj.bank_icon_dummy"
    }
}
