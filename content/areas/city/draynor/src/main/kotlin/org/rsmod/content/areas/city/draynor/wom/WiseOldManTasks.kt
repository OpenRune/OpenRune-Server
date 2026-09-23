package org.rsmod.content.areas.city.draynor.wom

internal enum class WomTask(
    val id: Int,
    val obj: String,
    val hard: Boolean,
    val request: String,
    val where: String,
) {
    BronzeBar(10, "obj.bronze_bar", false, "I need some bronze bars.", MINE_BRONZE),
    BronzeDagger(27, "obj.bronze_dagger", false, "I need a few bronze daggers to keep the goblins at bay.", bronze("daggers")),
    BronzeMace(11, "obj.bronze_mace", false, "I'd like some maces made of bronze.", bronze("maces")),
    BronzeArrowtips(12, "obj.bronze_arrowheads", true, "I need a few bronze arrowheads to complete some arrows I was making.", bronze("arrowheads")),
    BronzeKnife(13, "obj.bronze_knife", true, "I'd like some bronze knives to throw at goblins.", bronze("throwing knives")),
    BronzeSword(14, "obj.bronze_sword", false, "My weapons collection isn't all it used to be. Some short swords made of bronze would be much appreciated.", bronze("short swords")),
    BronzeAxe(15, "obj.bronze_axe", false, "The head fell off my axe when I was chopping wood last week, so I need some more bronze axes.", bronze("axes")),
    BronzeWarhammer(16, "obj.bronze_warhammer", true, "Could you fetch me some big warhammers made of bronze?", bronze("warhammers")),
    BronzeMedHelm(17, "obj.bronze_med_helm", false, "I could do with some medium-sized helmets. I'd like bronze ones.", bronze("helmets")),
    BronzeWire(18, "obj.bronzecraftwire", true, "I need some lengths of bronze wire to repair something.", bronze("wire")),
    IronBar(19, "obj.iron_bar", false, "I need some iron bars.", "Mine some iron ore. The furnace in Lumbridge can smelt them into bars of iron."),
    IronArrowtips(20, "obj.iron_arrowheads", true, "I need some iron arrowheads to put on the arrows I was making.", iron("arrowtips")),
    IronKnife(21, "obj.iron_knife", true, "I could do with some iron throwing knives to keep the goblins away from my house.", iron("throwing knives")),
    IronWarhammer(22, "obj.iron_warhammer", true, "I'd like some chunky iron warhammers.", iron("warhammers")),
    IronMace(23, "obj.iron_mace", false, "Some iron maces would be useful.", iron("maces")),
    IronOre(24, "obj.iron_ore", false, "A few lumps of iron ore would be nice.", "It should be easy enough for you to mine some iron ore."),
    CadavaBerries(25, "obj.cadavaberries", false, "I've found a use for cadava berries.", "Just look around the woods south-west of Varrock."),
    LeatherCowl(26, "obj.leather_cowl", true, "The hail can be very heavy here, so I'd like a few leather cowls.", "If you get the tanner in Al Kharid to turn some cowhides into leather, you'll be able to make the cowls yourself. Buy needles & thread from the crafting shop in Al Kharid if you need any."),
    Beer(28, "obj.beer", false, "Strange as it may seem, I want a lot of beer!", "There's a pub not far from here that will sell you plenty of that. Go west to Port Sarim."),
    Bread(29, "obj.bread", true, "I don't have a decent larder here, so I can't store food very well. Now I'm out of loaves of bread.", "Mix some flour and water to make bread dough, then bake it on a cooking range."),
    SoftLeather(30, "obj.leather", false, "I'll be needing a few pieces of soft leather.", "There's a tanner in Al Kharid who will turn cowhides into leather."),
    BallOfWool(31, "obj.ball_of_wool", true, "I saw an interesting bed in Karamja called a 'hammock'. It seemed to be made out of string, and I'd like some balls of wool so I can make my own.", "You can buy shears from the general store east of here. Shear some sheep, then spin the wool into balls on the spinning wheel in Lumbridge Castle."),
    LeatherGloves(32, "obj.leather_gloves", false, "It's a bit nippy in this house, and my hands are cold. I need some leather gloves.", "If you get the tanner in Al Kharid to turn some cowhides into leather, you'll be able to sew the gloves yourself. Buy a needle & thread from the crafting shop in Al Kharid."),
    Egg(33, "obj.egg", false, "I'm going to make an omelette, so I'll need some eggs.", "Eggs are usually found where chickens are farmed."),
    Silk(34, "obj.silk", false, "My undergarments are getting a bit worn out. I'll be needing some sheets of silk to patch them.", "There's a man in Al Kharid who sells it very cheaply. If you ever take any to Ardougne you'll get a good profit."),
    SoftClay(35, "obj.softclay", false, "I'll need some clay that's been softened so I can craft it.", "If you mine some clay, you can use containers of water on it to soften it."),
    UnfiredPot(36, "obj.pot_unfired", true, "Believe it or not, I need a few unfired clay pots.", "Mine some clay. Then use a container full of water to soften it. In the Barbarian Village you'll be able to form this into pots. Just don't bake them."),
    SwampPaste(37, "obj.swamppaste", true, "My roof is leaking, so I need some swamp paste to fix it.", "Swamp tar is found south-east of here. Add it to a pot of flour and cook it on an open fire to make the paste."),
    Potato(38, "obj.potato", false, "I need some potatoes, if you'd be so kind.", "There's a field of those north of Lumbridge."),
    HeadlessArrow(39, "obj.headless_arrow", true, "I want to make some arrows, so I'll need some headless arrows that have the feathers attached.", "Use a knife to chop some logs into arrowshafts. Then add a feather to each arrowshaft."),
    BronzeSpear(40, "obj.bronze_spear", false, "I need some bronze spears.", "If you kill some of those pesky goblins, you're bound to get some bronze spears."),
    BeerGlass(41, "obj.beer_glass", false, "My glassware got damaged when I moved here, so I need some new beer glasses.", "If you get some seaweed and a bucket of sand, you'll be able to mix them at a furnace to make molten glass. Then use a glassblowing pipe to make beer glasses. Most of what you need can be found on Entrana."),
    Bones(42, "obj.bones", false, "My plans for today require some sets of bones, the normal-sized sort you get from goblins.", "You'll find bones easily enough if you fight creatures."),
    UnfiredPieDish(43, "obj.piedish_unfired", true, "Strange as this may seem, I need some unfired pie dishes.", "Mine some clay. Then use a container full of water to soften it. In the Barbarian Village you'll be able to form this into pie dishes. Just don't bake them."),
    RawRatMeat(44, "obj.raw_rat_meat", false, "I hear pet cats are getting popular these days. I'd like some raw rat in case I get one.", "You should find some big rats south-east of here in the swamp."),
    CopperOre(45, "obj.copper_ore", false, "I need a few lumps of copper ore.", "It should be easy enough for you to mine some copper ore."),
    PotOfFlour(46, "obj.pot_flour", true, "I'm out of flour, so I could do with a few pots of that.", "There's a windmill north of here. You can use it to grind grain into flour. Then use an empty pot to collect the flour."),
    Feather(47, "obj.feather", false, "I need a handful of feathers to stick in my beard.", "The cheapest way to get feathers is to kill chickens."),
    Logs(48, "obj.logs", false, "This house is a bit cold, so I could do with some normal logs to burn.", "I suggest you take an axe and chop down some standard trees."),
    CookedChicken(49, "obj.cooked_chicken", false, "I'm running short of food, so I'd like some cooked chickens.", "You could try killing some chickens, then cooking their meat."),
    LeatherBoots(50, "obj.leather_boots", true, "My footwear is getting a bit worn out, so I need a few pairs of leather boots.", "If you get the tanner in Al Kharid to turn some cowhides into leather, you'll be able to make the boots yourself. Buy needles & thread from the crafting shop in Al Kharid if you need any."),
    Cowhide(51, "obj.cow_hide", false, "I'd like a few cowhides.", "If you slaughter some cows, you'll get cowhides easily enough."),
    BowString(52, "obj.bow_string", true, "My shortbow's string is getting a bit worn out, so could you fetch me some bow strings?", "If you pick some flax, you can use the spinning wheel in Lumbridge Castle to spin it into bowstrings."),
    CookedMeat(53, "obj.cooked_meat", false, "I'm a bit hungry. I'd like some cooked meat, not chicken or any sort of bird. And definitely not camel or rabbit either!", "Just cook some beef, rat or bear."),
    BronzeArrow(54, "obj.bronze_arrow", false, "I'm short of ammunition, so I could do with some bronze arrows. That way I can shoot at goblins through my window.", "You can make them by chopping a log into arrowshafts and adding feathers to them, then smithing a bronze bar into arrowheads to complete the arrows."),
    Grain(55, "obj.grain", false, "I'd like a bit of grain.", "There's a field full of it north of here."),
    TinOre(56, "obj.tin_ore", false, "I need a few lumps of tin ore.", "You shouldn't have any trouble mining tin ore."),
    Anchovies(57, "obj.anchovies", false, "I could do with some freshly cooked anchovies for a salad I'm planning.", "Use a small fishing net to get anchovies from the sea south of here. Then cook them on a fire or range."),
    Shrimps(58, "obj.shrimp", false, "I could do with some freshly cooked shrimps for a salad I'm planning.", "Use a small fishing net to get shrimps from the sea south of here. Then cook them on a fire or range."),
    RuneEssence(59, "obj.blankrune", false, "I'd like to study the rune essence that the wizards have been talking about recently, so I need a few pieces.", "Ask the Archmage in the tower south of here to teleport you to the Rune Essence mine. Then you can mine me some pieces."),
    MoltenGlass(60, "obj.molten_glass", false, "Some chunks of molten glass would be the ideal patch for my cracked window.", "Use a bucket of sand with some soda ash on a furnace.");

    companion object {
        fun byId(id: Int): WomTask? = entries.firstOrNull { it.id == id }
    }
}

private const val MINE_BRONZE =
    "Mine some copper ore and tin ore. The furnace in Lumbridge can smelt them into bars of bronze."

private fun bronze(items: String): String =
    "Try smelting some copper and tin ore to make bronze. Then hammer the bronze on an anvil to " +
        "make $items."

private fun iron(items: String): String =
    "Try smelting some iron. Then hammer the bronze on an anvil to make $items."
