package org.rsmod.content.skills.runecrafting

import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

internal var Player.bloodEssenceCharges by intVarBit("varbit.blood_essence_charges")

internal var Player.bindingNecklaceCharges by intVarBit("varbit.binding_necklace_charges")

internal var Player.mediumPouchDegradeThreshold by intVarp("varp.rcu_pouch_degradation_med")

internal var Player.largePouchDegradeThreshold by intVarp("varp.rcu_pouch_degradation_large")

internal var Player.giantPouchDegradeThreshold by intVarp("varp.rcu_pouch_degradation_giant")

internal var Player.colossalPouchDegradeThreshold by intVarBit("varbit.rcu_pouch_degradation_colossal")

internal var Player.magicImbueActive by intVarBit("varbit.magic_imbue_active")

internal var Player.cordeliaPouchRepairUnlocked by intVarBit("varbit.cordelia_pouch_repair_unlocked")
