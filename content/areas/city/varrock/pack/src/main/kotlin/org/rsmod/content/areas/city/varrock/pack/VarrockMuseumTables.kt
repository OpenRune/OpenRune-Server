package org.rsmod.content.areas.city.varrock.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object VarrockMuseumTables {
    const val DISPLAY = 0
    const val QUESTION = 1
    const val ANSWER_1 = 2
    const val ANSWER_2 = 3
    const val ANSWER_3 = 4
    const val CORRECT = 5

    const val DISPLAY_NUMBER = 0
    const val DISPLAY_PLAQUE = 1
    const val DISPLAY_VARBIT = 2
    const val DISPLAY_TITLE = 3
    const val DISPLAY_ROTATES = 4

    const val MODEL_DISPLAY = 0
    const val MODEL_COMPONENT = 1
    const val MODEL_MODEL = 2

    fun nathisQuestions() =
        dbTable("dbtable.vm_nathis_questions", serverOnly = true) {
            column("display", DISPLAY, VarType.INT)
            column("question", QUESTION, VarType.STRING)
            column("answer_1", ANSWER_1, VarType.STRING)
            column("answer_2", ANSWER_2, VarType.STRING)
            column("answer_3", ANSWER_3, VarType.STRING)
            column("correct", CORRECT, VarType.INT)
            row("dbrow.vm_nathis_dragon_1") {
                column(DISPLAY, 1)
                column(QUESTION, "What is considered a delicacy by dragons?")
                column(ANSWER_1, "Adventurers.")
                column(ANSWER_2, "Runite.")
                column(ANSWER_3, "Cheese.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_dragon_2") {
                column(DISPLAY, 1)
                column(QUESTION, "What is the best defence against a dragon's attack?")
                column(ANSWER_1, "Anti dragon-breath shield.")
                column(ANSWER_2, "Rune plate.")
                column(ANSWER_3, "Cats.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_dragon_3") {
                column(DISPLAY, 1)
                column(QUESTION, "How long do dragons live?")
                column(ANSWER_1, "Fifty years.")
                column(ANSWER_2, "Forever.")
                column(ANSWER_3, "Unknown.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_dragon_4") {
                column(DISPLAY, 1)
                column(QUESTION, "Which of these is not a type of dragon?")
                column(ANSWER_1, "Coloured.")
                column(ANSWER_2, "Elemental.")
                column(ANSWER_3, "Ferrous.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_dragon_5") {
                column(DISPLAY, 1)
                column(QUESTION, "What is the favoured territory of a dragon?")
                column(ANSWER_1, "Swamps.")
                column(ANSWER_2, "Old battle sites.")
                column(ANSWER_3, "Lumbridge Castle.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_dragon_6") {
                column(DISPLAY, 1)
                column(QUESTION, "Approximately how many feet tall do dragons stand?")
                column(ANSWER_1, "Twelve.")
                column(ANSWER_2, "Six.")
                column(ANSWER_3, "Ten.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_wyvern_1") {
                column(DISPLAY, 2)
                column(QUESTION, "How did the wyverns die out?")
                column(ANSWER_1, "Climate change.")
                column(ANSWER_2, "Disease.")
                column(ANSWER_3, "Marauding adventurers.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_wyvern_2") {
                column(DISPLAY, 2)
                column(QUESTION, "How many legs does a wyvern have?")
                column(ANSWER_1, "Two.")
                column(ANSWER_2, "Five.")
                column(ANSWER_3, "Six.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_wyvern_3") {
                column(DISPLAY, 2)
                column(QUESTION, "Where have wyvern bones been found?")
                column(ANSWER_1, "Ape Atoll.")
                column(ANSWER_2, "Al Kharid.")
                column(ANSWER_3, "Asgarnia.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_wyvern_4") {
                column(DISPLAY, 2)
                column(QUESTION, "Which genus does the wyvern theoretically belong to?")
                column(ANSWER_1, "Invertebrates.")
                column(ANSWER_2, "Reptiles.")
                column(ANSWER_3, "Mammals.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_wyvern_5") {
                column(DISPLAY, 2)
                column(QUESTION, "What are the wyverns' closest relations?")
                column(ANSWER_1, "Lizards.")
                column(ANSWER_2, "Snakes.")
                column(ANSWER_3, "Dragons.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_wyvern_6") {
                column(DISPLAY, 2)
                column(QUESTION, "What is the ambient temperature of wyvern bones?")
                column(ANSWER_1, "Room temperature.")
                column(ANSWER_2, "Below room temperature.")
                column(ANSWER_3, "Above room temperature.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_tortoise_1") {
                column(DISPLAY, 3)
                column(QUESTION, "What is the name of the oldest tortoise ever recorded?")
                column(ANSWER_1, "Healthorg.")
                column(ANSWER_2, "Mibbiwocket.")
                column(ANSWER_3, "Snookums.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_tortoise_2") {
                column(DISPLAY, 3)
                column(QUESTION, "What is a tortoise's favourite food?")
                column(ANSWER_1, "Sharks.")
                column(ANSWER_2, "Kebabs.")
                column(ANSWER_3, "Vegetables.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_tortoise_3") {
                column(DISPLAY, 3)
                column(QUESTION, "Name the explorer who discovered the world's oldest tortoise.")
                column(ANSWER_1, "Admiral Bake.")
                column(ANSWER_2, "Captain Cook.")
                column(ANSWER_3, "Ensign Chef.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_tortoise_4") {
                column(DISPLAY, 3)
                column(QUESTION, "How does the tortoise protect itself?")
                column(ANSWER_1, "Thick skin.")
                column(ANSWER_2, "Magic.")
                column(ANSWER_3, "Hard shell.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_tortoise_5") {
                column(DISPLAY, 3)
                column(QUESTION, "If a tortoise had twenty rings on its shell, how old would it be?")
                column(ANSWER_1, "Ten years.")
                column(ANSWER_2, "Twenty years.")
                column(ANSWER_3, "Sixty years.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_tortoise_6") {
                column(DISPLAY, 3)
                column(QUESTION, "Which race breeds tortoises for battle?")
                column(ANSWER_1, "Gnomes.")
                column(ANSWER_2, "Orks.")
                column(ANSWER_3, "Elves.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_lizard_1") {
                column(DISPLAY, 4)
                column(QUESTION, "How does a lizard regulate body heat?")
                column(ANSWER_1, "Chilli sauce.")
                column(ANSWER_2, "Fire.")
                column(ANSWER_3, "Sunlight.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_lizard_2") {
                column(DISPLAY, 4)
                column(QUESTION, "Who discovered how to kill lizards?")
                column(ANSWER_1, "Admiral Bake.")
                column(ANSWER_2, "The Slayer Masters.")
                column(ANSWER_3, "The Wise Old Man.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_lizard_3") {
                column(DISPLAY, 4)
                column(QUESTION, "How many eyes does a lizard have?")
                column(ANSWER_1, "Two.")
                column(ANSWER_2, "Three.")
                column(ANSWER_3, "Six.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_lizard_4") {
                column(DISPLAY, 4)
                column(QUESTION, "What order do lizards belong to?")
                column(ANSWER_1, "Mammal.")
                column(ANSWER_2, "Squamata.")
                column(ANSWER_3, "Insecta.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_lizard_5") {
                column(DISPLAY, 4)
                column(QUESTION, "What happens when a lizard becomes cold?")
                column(ANSWER_1, "It becomes sleepy.")
                column(ANSWER_2, "It becomes aggressive.")
                column(ANSWER_3, "It explodes.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_lizard_6") {
                column(DISPLAY, 4)
                column(QUESTION, "Lizard skin is made of the same substance as?")
                column(ANSWER_1, "Hair.")
                column(ANSWER_2, "Bone.")
                column(ANSWER_3, "Wood.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_leech_1") {
                column(DISPLAY, 5)
                column(QUESTION, "What is the favoured habitat of leeches?")
                column(ANSWER_1, "Water.")
                column(ANSWER_2, "Desert.")
                column(ANSWER_3, "Trees.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_leech_2") {
                column(DISPLAY, 5)
                column(QUESTION, "What shape is the inside of a leech's mouth?")
                column(ANSWER_1, "Star-shaped.")
                column(ANSWER_2, "'X'-shaped.")
                column(ANSWER_3, "'Y'-shaped.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_leech_3") {
                column(DISPLAY, 5)
                column(QUESTION, "Which of these is not eaten by leeches?")
                column(ANSWER_1, "Meat.")
                column(ANSWER_2, "Apples.")
                column(ANSWER_3, "Blood.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_leech_4") {
                column(DISPLAY, 5)
                column(QUESTION, "What contributed to the giant growth of Morytanian leeches?")
                column(ANSWER_1, "Environment.")
                column(ANSWER_2, "Healthy living.")
                column(ANSWER_3, "Magic.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_leech_5") {
                column(DISPLAY, 5)
                column(QUESTION, "What is special about Morytanian leeches?")
                column(ANSWER_1, "They sing.")
                column(ANSWER_2, "They attack by jumping.")
                column(ANSWER_3, "They have legs.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_leech_6") {
                column(DISPLAY, 5)
                column(QUESTION, "How does a leech change when it feeds?")
                column(ANSWER_1, "It turns blue.")
                column(ANSWER_2, "It starts singing.")
                column(ANSWER_3, "It doubles in size.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_camel_1") {
                column(DISPLAY, 6)
                column(QUESTION, "What is produced by feeding chilli to a camel?")
                column(ANSWER_1, "An irate camel.")
                column(ANSWER_2, "Toxic dung.")
                column(ANSWER_3, "An undead bactrian.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_camel_2") {
                column(DISPLAY, 6)
                column(QUESTION, "If an ugthanki has one, how many does a bactrian have?")
                column(ANSWER_1, "One.")
                column(ANSWER_2, "Two.")
                column(ANSWER_3, "Four.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_camel_3") {
                column(DISPLAY, 6)
                column(QUESTION, "Camels: herbivore, carnivore or omnivore?")
                column(ANSWER_1, "Herbivore.")
                column(ANSWER_2, "Carnivore.")
                column(ANSWER_3, "Omnivore.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_camel_4") {
                column(DISPLAY, 6)
                column(QUESTION, "What is the usual mood for a camel?")
                column(ANSWER_1, "Annoyed.")
                column(ANSWER_2, "Bemused.")
                column(ANSWER_3, "Cheerful.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_camel_5") {
                column(DISPLAY, 6)
                column(QUESTION, "Where would you find an ugthanki?")
                column(ANSWER_1, "Al Kharid.")
                column(ANSWER_2, "Morytania.")
                column(ANSWER_3, "Nowhere.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_camel_6") {
                column(DISPLAY, 6)
                column(QUESTION, "Which camel byproduct is known to be very nutritious?")
                column(ANSWER_1, "Milk.")
                column(ANSWER_2, "Dung.")
                column(ANSWER_3, "Meat.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_mole_1") {
                column(DISPLAY, 7)
                column(QUESTION, "What habitat do moles prefer?")
                column(ANSWER_1, "Subterranean.")
                column(ANSWER_2, "Any.")
                column(ANSWER_3, "Subaquatic.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_mole_2") {
                column(DISPLAY, 7)
                column(QUESTION, "Why are moles considered to be an agricultural pest?")
                column(ANSWER_1, "They dig holes.")
                column(ANSWER_2, "They eat food meant for livestock.")
                column(ANSWER_3, "They attack farmers.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_mole_3") {
                column(DISPLAY, 7)
                column(QUESTION, "Who discovered giant moles?")
                column(ANSWER_1, "Sir Tiffy Cashien.")
                column(ANSWER_2, "Ak-Haranu.")
                column(ANSWER_3, "Wyson the Gardener.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_mole_4") {
                column(DISPLAY, 7)
                column(QUESTION, "What would you call a group of young moles?")
                column(ANSWER_1, "A litter.")
                column(ANSWER_2, "A mess.")
                column(ANSWER_3, "A labour.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_mole_5") {
                column(DISPLAY, 7)
                column(QUESTION, "What is a mole's favourite food?")
                column(ANSWER_1, "Insects and other invertebrates.")
                column(ANSWER_2, "Humans and gnomes.")
                column(ANSWER_3, "Seeds and plant roots.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_mole_6") {
                column(DISPLAY, 7)
                column(QUESTION, "Which family do moles belong to?")
                column(ANSWER_1, "The Malpidae family.")
                column(ANSWER_2, "The Talpidae family.")
                column(ANSWER_3, "The Falpidae family.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_penguin_1") {
                column(DISPLAY, 8)
                column(QUESTION, "Which sense do penguins rely on when hunting?")
                column(ANSWER_1, "Smell.")
                column(ANSWER_2, "Taste.")
                column(ANSWER_3, "Sight.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_penguin_2") {
                column(DISPLAY, 8)
                column(QUESTION, "Which skill seems unusual for the penguins to possess?")
                column(ANSWER_1, "Planning.")
                column(ANSWER_2, "Fishing.")
                column(ANSWER_3, "Sleeping.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_penguin_3") {
                column(DISPLAY, 8)
                column(QUESTION, "How do penguins keep warm?")
                column(ANSWER_1, "A layer of fat.")
                column(ANSWER_2, "Vigorous exercise.")
                column(ANSWER_3, "Magic.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_penguin_4") {
                column(DISPLAY, 8)
                column(QUESTION, "What is the preferred climate for penguins?")
                column(ANSWER_1, "Cold.")
                column(ANSWER_2, "Hot.")
                column(ANSWER_3, "Damp.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_penguin_5") {
                column(DISPLAY, 8)
                column(QUESTION, "Describe the behaviour of penguins?")
                column(ANSWER_1, "Social.")
                column(ANSWER_2, "Asocial.")
                column(ANSWER_3, "Hive.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_penguin_6") {
                column(DISPLAY, 8)
                column(QUESTION, "When do penguins fast?")
                column(ANSWER_1, "Just before dawn.")
                column(ANSWER_2, "During breeding.")
                column(ANSWER_3, "During the night.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_terrorbird_1") {
                column(DISPLAY, 9)
                column(QUESTION, "What is a terrorbird's preferred food?")
                column(ANSWER_1, "Anything.")
                column(ANSWER_2, "Pizza.")
                column(ANSWER_3, "Vegetables.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_terrorbird_2") {
                column(DISPLAY, 9)
                column(QUESTION, "Who use terrorbirds as mounts?")
                column(ANSWER_1, "Dwarves.")
                column(ANSWER_2, "Gnomes.")
                column(ANSWER_3, "Goblins.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_terrorbird_3") {
                column(DISPLAY, 9)
                column(QUESTION, "Where do terrorbirds get most of their water?")
                column(ANSWER_1, "Streams and rivers.")
                column(ANSWER_2, "Licking trees.")
                column(ANSWER_3, "Eating plants.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_terrorbird_4") {
                column(DISPLAY, 9)
                column(QUESTION, "How many claws do terrorbirds have?")
                column(ANSWER_1, "Two.")
                column(ANSWER_2, "Four.")
                column(ANSWER_3, "Six.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_terrorbird_5") {
                column(DISPLAY, 9)
                column(QUESTION, "What do terrorbirds eat to aid digestion?")
                column(ANSWER_1, "Blurite.")
                column(ANSWER_2, "Stones.")
                column(ANSWER_3, "Paper.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_terrorbird_6") {
                column(DISPLAY, 9)
                column(QUESTION, "How many teeth do terrorbirds have?")
                column(ANSWER_1, "28.")
                column(ANSWER_2, "14.")
                column(ANSWER_3, "0.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_kalphite_1") {
                column(DISPLAY, 10)
                column(QUESTION, "Kalphites are ruled by a...?")
                column(ANSWER_1, "King.")
                column(ANSWER_2, "Pasha.")
                column(ANSWER_3, "Caliph.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_kalphite_2") {
                column(DISPLAY, 10)
                column(QUESTION, "What is the lowest caste in kalphite society?")
                column(ANSWER_1, "Prince.")
                column(ANSWER_2, "Soldier.")
                column(ANSWER_3, "Worker.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_kalphite_3") {
                column(DISPLAY, 10)
                column(QUESTION, "What are the armoured plates on a kalphite called?")
                column(ANSWER_1, "Hauberk.")
                column(ANSWER_2, "Lamellae.")
                column(ANSWER_3, "Shield.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_kalphite_4") {
                column(DISPLAY, 10)
                column(QUESTION, "Are kalphites carnivores, herbivores or omnivores?")
                column(ANSWER_1, "Omnivores.")
                column(ANSWER_2, "Herbivores.")
                column(ANSWER_3, "Carnivores.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_kalphite_5") {
                column(DISPLAY, 10)
                column(QUESTION, "What are kalphites assumed to have evolved from?")
                column(ANSWER_1, "Scarab beetles.")
                column(ANSWER_2, "Penguins.")
                column(ANSWER_3, "Dragons.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_kalphite_6") {
                column(DISPLAY, 10)
                column(QUESTION, "Name the prominent figure in kalphite mythology?")
                column(ANSWER_1, "Postie Pete.")
                column(ANSWER_2, "Scabaras.")
                column(ANSWER_3, "Jorral the Historian.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_monkey_1") {
                column(DISPLAY, 11)
                column(QUESTION, "Which type of primates do monkeys belong to?")
                column(ANSWER_1, "Simian.")
                column(ANSWER_2, "Hominid.")
                column(ANSWER_3, "Lagomorpha.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_monkey_2") {
                column(DISPLAY, 11)
                column(QUESTION, "Which have the lighter colour: Karamjan or Harmless monkeys?")
                column(ANSWER_1, "They are the same.")
                column(ANSWER_2, "Karamjan.")
                column(ANSWER_3, "Harmless.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_monkey_3") {
                column(DISPLAY, 11)
                column(QUESTION, "Monkeys love bananas. What else do they like to eat?")
                column(ANSWER_1, "Chilli.")
                column(ANSWER_2, "Seaweed.")
                column(ANSWER_3, "Bitternuts.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_monkey_4") {
                column(DISPLAY, 11)
                column(QUESTION, "There are two known families of monkeys. One is Karamjan, the other is...?")
                column(ANSWER_1, "Pointless.")
                column(ANSWER_2, "Harmless.")
                column(ANSWER_3, "Hairless.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_monkey_5") {
                column(DISPLAY, 11)
                column(QUESTION, "What colour mohawk do Karamjan monkeys have?")
                column(ANSWER_1, "Red.")
                column(ANSWER_2, "Blue.")
                column(ANSWER_3, "Green.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_monkey_6") {
                column(DISPLAY, 11)
                column(QUESTION, "What have Karamjan monkeys taken a deep dislike to?")
                column(ANSWER_1, "Seaweed.")
                column(ANSWER_2, "Chilli.")
                column(ANSWER_3, "Taxes.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_seaslug_1") {
                column(DISPLAY, 12)
                column(QUESTION, "We assume that sea slugs have a stinging organ on their soft skin - what is it called?")
                column(ANSWER_1, "Nematocysts.")
                column(ANSWER_2, "Hematocysts.")
                column(ANSWER_3, "Pematocysts.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_seaslug_2") {
                column(DISPLAY, 12)
                column(QUESTION, "Why has the museum never examined a live sea slug?")
                column(ANSWER_1, "The sea slugs are shy.")
                column(ANSWER_2, "The researchers keep vanishing.")
                column(ANSWER_3, "Sea slugs are extinct.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_seaslug_3") {
                column(DISPLAY, 12)
                column(QUESTION, "What do we think the sea slug feeds upon?")
                column(ANSWER_1, "Blood.")
                column(ANSWER_2, "Seaweed.")
                column(ANSWER_3, "Insects.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_seaslug_4") {
                column(DISPLAY, 12)
                column(QUESTION, "What are the two fangs presumed to be used for?")
                column(ANSWER_1, "Defense or display.")
                column(ANSWER_2, "Attacking prey.")
                column(ANSWER_3, "Latching on to objects.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_seaslug_5") {
                column(DISPLAY, 12)
                column(QUESTION, "Off of which coastline would you find sea slugs?")
                column(ANSWER_1, "Karamja.")
                column(ANSWER_2, "Miscellania.")
                column(ANSWER_3, "Ardougne.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_seaslug_6") {
                column(DISPLAY, 12)
                column(QUESTION, "In what way are sea slugs similar to snails?")
                column(ANSWER_1, "They eat lettuce.")
                column(ANSWER_2, "They move very slowly.")
                column(ANSWER_3, "They have a hard shell.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_snake_1") {
                column(DISPLAY, 13)
                column(QUESTION, "What is snake venom adapted from?")
                column(ANSWER_1, "Ear wax.")
                column(ANSWER_2, "Stomach acid.")
                column(ANSWER_3, "Saliva.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_snake_2") {
                column(DISPLAY, 13)
                column(QUESTION, "Aside from their noses, what do snakes use to smell?")
                column(ANSWER_1, "Tongue.")
                column(ANSWER_2, "Eyes.")
                column(ANSWER_3, "Ears.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_snake_3") {
                column(DISPLAY, 13)
                column(QUESTION, "If a snake sticks its tongue out at you, what is it doing?")
                column(ANSWER_1, "Being rude.")
                column(ANSWER_2, "Seeing how you taste.")
                column(ANSWER_3, "Seeing how you smell.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_snake_4") {
                column(DISPLAY, 13)
                column(QUESTION, "If some snakes use venom to kill their prey, what do other snakes use?")
                column(ANSWER_1, "Fire.")
                column(ANSWER_2, "Constriction.")
                column(ANSWER_3, "Magic.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_snake_5") {
                column(DISPLAY, 13)
                column(QUESTION, "Lizards and snakes belong to the same order - what is it?")
                column(ANSWER_1, "Squamata.")
                column(ANSWER_2, "Insecta.")
                column(ANSWER_3, "Sarcopterghii.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_snake_6") {
                column(DISPLAY, 13)
                column(QUESTION, "Which habitat do snakes prefer?")
                column(ANSWER_1, "The desert.")
                column(ANSWER_2, "The ocean.")
                column(ANSWER_3, "Anywhere.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_snail_1") {
                column(DISPLAY, 14)
                column(QUESTION, "What is special about the shell of the giant Morytanian snail?")
                column(ANSWER_1, "It glows in the dark.")
                column(ANSWER_2, "It is very tasty.")
                column(ANSWER_3, "It is resistant to acid.")
                column(CORRECT, 2)
            }
            row("dbrow.vm_nathis_snail_2") {
                column(DISPLAY, 14)
                column(QUESTION, "How do Morytanian snails capture their prey?")
                column(ANSWER_1, "Aggressive marketing.")
                column(ANSWER_2, "Spitting acid.")
                column(ANSWER_3, "Hex them until they glow.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_snail_3") {
                column(DISPLAY, 14)
                column(QUESTION, "Which of these is a snail byproduct?")
                column(ANSWER_1, "Fireproof oil.")
                column(ANSWER_2, "A musical instrument")
                column(ANSWER_3, "Snail-a-cola.")
                column(CORRECT, 0)
            }
            row("dbrow.vm_nathis_snail_4") {
                column(DISPLAY, 14)
                column(QUESTION, "What does 'Achatina Acidia' mean?")
                column(ANSWER_1, "Giant acid-spitting snail.")
                column(ANSWER_2, "Acid-spitting snail.")
                column(ANSWER_3, "Giant cheese-spitting snail.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_snail_5") {
                column(DISPLAY, 14)
                column(QUESTION, "How do snails move?")
                column(ANSWER_1, "Flapping their wings.")
                column(ANSWER_2, "Contracting and stretching.")
                column(ANSWER_3, "Hopping about.")
                column(CORRECT, 1)
            }
            row("dbrow.vm_nathis_snail_6") {
                column(DISPLAY, 14)
                column(QUESTION, "What is the 'trapdoor', which snails use to cover the entrance to their shells called?")
                column(ANSWER_1, "An operculum.")
                column(ANSWER_2, "A caldarium.")
                column(ANSWER_3, "A tavernium.")
                column(CORRECT, 0)
            }
        }

    fun nathisDisplays() =
        dbTable("dbtable.vm_nathis_displays", serverOnly = true) {
            column("number", DISPLAY_NUMBER, VarType.INT)
            column("plaque", DISPLAY_PLAQUE, VarType.LOC)
            column("varbit", DISPLAY_VARBIT, VarType.STRING)
            column("title", DISPLAY_TITLE, VarType.STRING)
            column("rotates", DISPLAY_ROTATES, VarType.BOOLEAN)
            row("dbrow.vm_nathis_display_dragon") {
                column(DISPLAY_NUMBER, 1)
                columnRSCM(DISPLAY_PLAQUE, "loc.vm_plaque_dragon")
                column(DISPLAY_VARBIT, "varbit.vm_nathis_dragon_display")
                column(DISPLAY_TITLE, "The Dragon (Draconis Rex)")
                column(DISPLAY_ROTATES, false)
            }
            row("dbrow.vm_nathis_display_wyvern") {
                column(DISPLAY_NUMBER, 2)
                columnRSCM(DISPLAY_PLAQUE, "loc.vm_plaque_wyvern")
                column(DISPLAY_VARBIT, "varbit.vm_nathis_wyvern_display")
                column(DISPLAY_TITLE, "The Wyvern (Draconis Ossis)")
                column(DISPLAY_ROTATES, true)
            }
            row("dbrow.vm_nathis_display_tortoise") {
                column(DISPLAY_NUMBER, 3)
                columnRSCM(DISPLAY_PLAQUE, "loc.vm_plaque_battle_tortoise")
                column(DISPLAY_VARBIT, "varbit.vm_nathis_tortoise_display")
                column(DISPLAY_TITLE, "The Giant Tortoise (Testudines Giganteus)")
                column(DISPLAY_ROTATES, true)
            }
            row("dbrow.vm_nathis_display_lizard") {
                column(DISPLAY_NUMBER, 4)
                columnRSCM(DISPLAY_PLAQUE, "loc.vm_plaque_lizard")
                column(DISPLAY_VARBIT, "varbit.vm_nathis_lizard_display")
                column(DISPLAY_TITLE, "The Giant Lizard (Lacertilia Giganteus)")
                column(DISPLAY_ROTATES, true)
            }
            row("dbrow.vm_nathis_display_leech") {
                column(DISPLAY_NUMBER, 5)
                columnRSCM(DISPLAY_PLAQUE, "loc.vm_plaque_leech")
                column(DISPLAY_VARBIT, "varbit.vm_nathis_leech_display")
                column(DISPLAY_TITLE, "The Giant Leech (Hirudinea Acidia)")
                column(DISPLAY_ROTATES, true)
            }
            row("dbrow.vm_nathis_display_camel") {
                column(DISPLAY_NUMBER, 6)
                columnRSCM(DISPLAY_PLAQUE, "loc.vm_plaque_camel")
                column(DISPLAY_VARBIT, "varbit.vm_nathis_camel_display")
                column(DISPLAY_TITLE, "The Camel (Camelus Bactrian)")
                column(DISPLAY_ROTATES, true)
            }
            row("dbrow.vm_nathis_display_mole") {
                column(DISPLAY_NUMBER, 7)
                columnRSCM(DISPLAY_PLAQUE, "loc.vm_plaque_mole")
                column(DISPLAY_VARBIT, "varbit.vm_nathis_mole_display")
                column(DISPLAY_TITLE, "The Giant Mole (Talpidae Wysonian)")
                column(DISPLAY_ROTATES, true)
            }
            row("dbrow.vm_nathis_display_penguin") {
                column(DISPLAY_NUMBER, 8)
                columnRSCM(DISPLAY_PLAQUE, "loc.vm_plaque_penguine")
                column(DISPLAY_VARBIT, "varbit.vm_nathis_penguin_display")
                column(DISPLAY_TITLE, "The Penguin (Spheniscidae Callidus)")
                column(DISPLAY_ROTATES, true)
            }
            row("dbrow.vm_nathis_display_terrorbird") {
                column(DISPLAY_NUMBER, 9)
                columnRSCM(DISPLAY_PLAQUE, "loc.vm_plaque_terrorbird")
                column(DISPLAY_VARBIT, "varbit.vm_nathis_terrorbird_display")
                column(DISPLAY_TITLE, "The Terrorbird (Aves Terror)")
                column(DISPLAY_ROTATES, true)
            }
            row("dbrow.vm_nathis_display_kalphite") {
                column(DISPLAY_NUMBER, 10)
                columnRSCM(DISPLAY_PLAQUE, "loc.vm_plaque_kalphite_queen")
                column(DISPLAY_VARBIT, "varbit.vm_nathis_kalphite_display")
                column(DISPLAY_TITLE, "The Kalphite Queen (Kalphiscarabeinae Pasha)")
                column(DISPLAY_ROTATES, true)
            }
            row("dbrow.vm_nathis_display_monkey") {
                column(DISPLAY_NUMBER, 11)
                columnRSCM(DISPLAY_PLAQUE, "loc.vm_plaque_monkey")
                column(DISPLAY_VARBIT, "varbit.vm_nathis_monkey_display")
                column(DISPLAY_TITLE, "The Monkey (Simiiformes Karamjan)")
                column(DISPLAY_ROTATES, true)
            }
            row("dbrow.vm_nathis_display_seaslug") {
                column(DISPLAY_NUMBER, 12)
                columnRSCM(DISPLAY_PLAQUE, "loc.vm_plaque_seaslug")
                column(DISPLAY_VARBIT, "varbit.vm_nathis_seaslug_display")
                column(DISPLAY_TITLE, "The Sea Slug (Opisthobranchia Alucinor)")
                column(DISPLAY_ROTATES, false)
            }
            row("dbrow.vm_nathis_display_snake") {
                column(DISPLAY_NUMBER, 13)
                columnRSCM(DISPLAY_PLAQUE, "loc.vm_plaque_snake")
                column(DISPLAY_VARBIT, "varbit.vm_nathis_snake_display")
                column(DISPLAY_TITLE, "The Snake (Serpentes Fellis)")
                column(DISPLAY_ROTATES, true)
            }
            row("dbrow.vm_nathis_display_snail") {
                column(DISPLAY_NUMBER, 14)
                columnRSCM(DISPLAY_PLAQUE, "loc.vm_plaque_snail")
                column(DISPLAY_VARBIT, "varbit.vm_nathis_snail_display")
                column(DISPLAY_TITLE, "The Giant Snail (Achatina Acidia Giganteus)")
                column(DISPLAY_ROTATES, true)
            }
        }

    fun nathisDisplayModels() =
        dbTable("dbtable.vm_nathis_display_models", serverOnly = true) {
            column("display", MODEL_DISPLAY, VarType.INT)
            column("component", MODEL_COMPONENT, VarType.STRING)
            column("model", MODEL_MODEL, VarType.STRING)
            row("dbrow.vm_nathis_model_dragon_01") {
                column(MODEL_DISPLAY, 1)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_dragon_01")
                column(MODEL_MODEL, "models.vm_nathis_dragon_01")
            }
            row("dbrow.vm_nathis_model_dragon_02") {
                column(MODEL_DISPLAY, 1)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_dragon_02")
                column(MODEL_MODEL, "models.vm_nathis_dragon_02")
            }
            row("dbrow.vm_nathis_model_dragon_03") {
                column(MODEL_DISPLAY, 1)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_dragon_03")
                column(MODEL_MODEL, "models.vm_nathis_dragon_03")
            }
            row("dbrow.vm_nathis_model_wyvern") {
                column(MODEL_DISPLAY, 2)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_wyvern")
                column(MODEL_MODEL, "models.vm_nathis_wyvern")
            }
            row("dbrow.vm_nathis_model_battle_tortoise") {
                column(MODEL_DISPLAY, 3)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_battle_tortoise")
                column(MODEL_MODEL, "models.vm_nathis_battle_tortoise")
            }
            row("dbrow.vm_nathis_model_lizard") {
                column(MODEL_DISPLAY, 4)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_lizard")
                column(MODEL_MODEL, "models.vm_nathis_lizard")
            }
            row("dbrow.vm_nathis_model_leech") {
                column(MODEL_DISPLAY, 5)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_leech")
                column(MODEL_MODEL, "models.vm_nathis_leech")
            }
            row("dbrow.vm_nathis_model_camel") {
                column(MODEL_DISPLAY, 6)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_camel")
                column(MODEL_MODEL, "models.vm_nathis_camel")
            }
            row("dbrow.vm_nathis_model_mole") {
                column(MODEL_DISPLAY, 7)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_mole")
                column(MODEL_MODEL, "models.vm_nathis_mole")
            }
            row("dbrow.vm_nathis_model_penguine") {
                column(MODEL_DISPLAY, 8)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_penguine")
                column(MODEL_MODEL, "models.vm_nathis_penguine")
            }
            row("dbrow.vm_nathis_model_terrorbird") {
                column(MODEL_DISPLAY, 9)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_terrorbird")
                column(MODEL_MODEL, "models.vm_nathis_terrorbird")
            }
            row("dbrow.vm_nathis_model_kalphite_queen") {
                column(MODEL_DISPLAY, 10)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_kalphite_queen")
                column(MODEL_MODEL, "models.vm_nathis_kalphite_queen")
            }
            row("dbrow.vm_nathis_model_monkey") {
                column(MODEL_DISPLAY, 11)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_monkey")
                column(MODEL_MODEL, "models.vm_nathis_monkey")
            }
            row("dbrow.vm_nathis_model_seaslugs_1") {
                column(MODEL_DISPLAY, 12)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_seaslugs_1")
                column(MODEL_MODEL, "models.vm_nathis_seaslugs_1")
            }
            row("dbrow.vm_nathis_model_seaslugs_2") {
                column(MODEL_DISPLAY, 12)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_seaslugs_2")
                column(MODEL_MODEL, "models.vm_nathis_seaslugs_2")
            }
            row("dbrow.vm_nathis_model_seaslugs_3") {
                column(MODEL_DISPLAY, 12)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_seaslugs_3")
                column(MODEL_MODEL, "models.vm_nathis_seaslugs_3")
            }
            row("dbrow.vm_nathis_model_seaslugs_4") {
                column(MODEL_DISPLAY, 12)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_seaslugs_4")
                column(MODEL_MODEL, "models.vm_nathis_seaslugs_4")
            }
            row("dbrow.vm_nathis_model_snake") {
                column(MODEL_DISPLAY, 13)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_snake")
                column(MODEL_MODEL, "models.vm_nathis_snake")
            }
            row("dbrow.vm_nathis_model_snail") {
                column(MODEL_DISPLAY, 14)
                column(MODEL_COMPONENT, "component.vm_natural_history:vm_nathis_snail")
                column(MODEL_MODEL, "models.vm_nathis_snail")
            }
        }
}
