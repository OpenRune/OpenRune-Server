package org.alter.game.saving.impl

import dev.openrune.types.InvScope
import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.game.model.entity.Client
import org.alter.game.model.inv.Inventory
import org.alter.game.model.item.Item
import org.alter.game.saving.DocumentHandler
import org.bson.Document

private val logger = KotlinLogging.logger {}

class ContainersSerialisation(
    override val name: String = "containers"
) : DocumentHandler {

    override fun fromDocument(client: Client, doc: Document) {
        doc.forEach { (containerKey, value) ->
            val containerDoc = value as? Document ?: return@forEach
            val container = client.invMap.getOrPut(containerKey)
            decodeItems(container, containerDoc)
        }
    }

    private fun decodeItems(container: Inventory, containerDoc: Document) {
        containerDoc.forEach { (slotKey, value) ->
            val slot = slotKey.toIntOrNull() ?: return@forEach
            val itemDoc = value as? Document ?: return@forEach

            container[slot] = Item.fromDocument(itemDoc)
        }
    }

    override fun asDocument(client: Client): Document {
        val root = Document()

        client.invMap.backing
            .asSequence()
            .filter { (_, container) -> container.type.scope == InvScope.Perm }
            .forEach { (key, container) ->
                val items = Document()

                container.objs.forEachIndexed { slot, item ->
                    if (item != null) {
                        items[slot.toString()] = item.asDocument()
                    }
                }

                root[key] = items
            }

        return root
    }
}