package org.rsmod.api.account.saver.request

import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType

public data class AccountSaveRequest(
    val accountId: Int,
    val characterId: Int,
    val player: Player,
    val worldType: WorldType,
    val account: CharacterAccountRepository.SaveData,
    val segments: List<CharacterDataStage.Save>,
    val callback: AccountSaveCallback,
    var attempts: Int = 0,
)
