package org.rsmod.api.account.character

import dev.or2.central.account.AccountData

@JvmInline
public value class CharacterAccountLoginSegment(public val wrapped: AccountData) : CharacterDataStage.Segment
