package dtx.example

import dtx.impl.exhaustive.uniformExhaustiveTable
import dtx.impl.exhaustive.weightedExhaustiveTable


/**
 * Good for simple raffles or encounter table draws or something like that
 */
val uniformExhaustiveTableExample = uniformExhaustiveTable<Player, Item> {

    name("Goblin dungeon encounters")

    1 rolls {
        onRollCompleted { player, args, encounter ->
            println("You turn the corner and the head honcho is there, ready to beat you up.")
        }
        onExhaust {
            println("Well, there's usually only 1 head honcho so that's as hard as it'll get! Hopefully...")
        }
        result(Item("Goblin Head Honcho"))
    }

    3 rolls {
        onRollCompleted { player, args, encounter ->
            println("A... skeleton riding a goblin? What? That can't be common around here...")
        }
        onExhaust {
            println("You were right, that wasn't so common. All the... goblin jockeys... are dead.")
        }
        result(Item("Goblin Jockey"))
    }

    6 rolls {
        onRollCompleted { player, args, encounter ->
            println("You encounter a horde of goblins! Now this is a battle!")
        }
        onExhaust {
            println("That just might have been the last horde.")
        }
        result(Item("Goblin Horde"))
    }

    10 rolls {
        onRollCompleted { player, args, encounter ->
            println("You sneak up on a poor lone goblin!")
        }
        onExhaust {
            println("There can only be so many stray goblins, they usually stick together! That might be the last of them.")
        }
        result(Item("Lone goblin"))
    }

    onExhaust {
        println("Yikes, out of encounters to roll! Reset the table!")
    }
}


/**
 * Very good for spicy raffles tbh, weights are based on how many rolls each item has
 */
val weightedExhaustiveTableExample = weightedExhaustiveTable<Player, Item> {

    name("Card raffle")

    1 rolls Item("Jokette")

    2 rolls Item("Ace of Spades")
    2 rolls Item("Ace of Hearts")
    2 rolls Item("Ace of Diamonds")
    2 rolls Item("Ace of Clubs")

    4 rolls Item("King of Spades")
    4 rolls Item("King of Hearts")
    4 rolls Item("King of Diamonds")
    4 rolls Item("King of Clubs")

    onExhaust {
        println("Raffle is over, deck is empty!!")
    }
}

fun main() {
    repeat(21) {
        uniformExhaustiveTableExample.roll(examplePlayer)
    }

    repeat(26) {
        val res = weightedExhaustiveTableExample.roll(examplePlayer)
        println("Card Result: $res")
    }
}