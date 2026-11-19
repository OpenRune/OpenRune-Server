package org.rsmod.content.quest.area.varrock.shieldofarrav

import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

internal var Player.phoenixGang by intVarp("varp.phoenixgang")
internal var Player.blackArmGang by intVarp("varp.blackarmgang")
internal var Player.soaState by intVarBit("varbit.shieldofarrav")
internal var Player.charlieMet by intVarBit("varbit.soa_charlie_met")
internal var Player.charliePaid by intVarBit("varbit.soa_charlie_paid")
internal var Player.charlieHint by intVarBit("varbit.soa_charlie_hint")
internal var Player.weaponsmasterDead by intVarBit("varbit.soa_weaponmaster_dead")
internal var Player.shieldGiven by intVarBit("varbit.soa_shield_given")
internal var Player.stravenTrampChat by intVarBit("varbit.soa_straven_tramp_chat")
internal var Player.stravenShieldChat by intVarBit("varbit.soa_straven_shield_chat")
internal var Player.lampUsed by intVarBit("varbit.soa_lamp_used")

internal const val SOA_STARTED = 1
internal const val SOA_COMPLETE = 2

internal const val PHOENIX_STARTED = 1
internal const val PHOENIX_BOOK_READ = 2
internal const val PHOENIX_GANGS_KNOWN = 3
internal const val PHOENIX_BARAEK_PAID = 4
internal const val PHOENIX_TASK = 8
internal const val PHOENIX_MEMBER = 9
internal const val PHOENIX_COMPLETE = 10

internal const val BLACKARM_KNOWN = 1
internal const val BLACKARM_TASK = 2
internal const val BLACKARM_MEMBER = 3
internal const val BLACKARM_COMPLETE = 4

internal const val SHIELD_GIVEN_PHOENIX = 1
internal const val SHIELD_GIVEN_BLACKARM = 2

internal const val BOOK = "obj.the_shield_of_arrav"
internal const val WEAPON_STORE_KEY = "obj.phoenixkey2"
internal const val INTEL_REPORT = "obj.intelligence_report"
internal const val PHOENIX_SHIELD = "obj.arravshield1"
internal const val BLACKARM_SHIELD = "obj.arravshield2"
internal const val CROSSBOW = "obj.phoenix_crossbow"
internal const val CERTIFICATE = "obj.arravcertificate"
internal const val PHOENIX_HALF = "obj.arravcertificate_lft"
internal const val BLACKARM_HALF = "obj.arravcertificate_rht"
internal const val LAMP = "obj.soa_lamp"
internal const val COINS = "obj.coins"

internal val Player.isPhoenixMember: Boolean
    get() = phoenixGang >= PHOENIX_MEMBER

internal val Player.isBlackArmMember: Boolean
    get() = blackArmGang >= BLACKARM_MEMBER

internal val Player.isOnQuest: Boolean
    get() = soaState == SOA_STARTED

internal val Player.isQuestComplete: Boolean
    get() = soaState >= SOA_COMPLETE

internal fun Player.owns(obj: String): Boolean =
    obj in inv || obj in worn || invMap.getOrPut("inv.bank").contains(obj)

internal fun Player.holds(obj: String): Boolean = obj in inv
