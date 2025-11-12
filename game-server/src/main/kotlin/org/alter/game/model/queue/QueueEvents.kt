package org.alter.game.model.queue

import org.alter.game.event.Event

data class IntChatInput(val amount: Int) : Event
data class StringChatInput(val text: String) : Event
data class NameChatInput(val text: String) : Event
data class ItemSearchInput(val item: Int) : Event
data class ContinueDialogue(val interfaceId: Int, val child: Int, val slot: Int) : Event