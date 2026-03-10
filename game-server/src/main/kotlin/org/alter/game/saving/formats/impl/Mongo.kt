package org.alter.game.saving.formats.impl

import com.mongodb.client.model.Filters.regex
import com.mongodb.client.model.Updates.set
import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.game.model.entity.Client
import org.alter.game.saving.formats.FormatHandler
import org.bson.Document
import org.bson.conversions.Bson

private val logger = KotlinLogging.logger {}

class Mongo(override val collectionName: String) : FormatHandler(collectionName) {

    override fun init() {
        DatabaseManager.connect()
    }

    override fun saveDocument(client: Client, document: Document) {
        if (!playerExists(client)) {
            DatabaseManager.getCollection(collectionName).insertOne(document)
        } else {
            val caseInsensitiveFilter = createCaseInsensitiveFilter(client)
            val attrs = document.get("attributes", Document::class.java)
            DatabaseManager.getCollection(collectionName).updateOne(caseInsensitiveFilter, set("attributes", attrs))
        }
    }

    override fun parseDocument(client : Client): Document {
        val caseInsensitiveFilter = createCaseInsensitiveFilter(client)
        return DatabaseManager.getCollection(collectionName).find(caseInsensitiveFilter).first()
            ?: error("Player document not found for ${client.loginUsername}")
    }

    override fun loadAll(): Map<String, Document> {
        logger.warn { "Mongo.loadAll() is not yet fully implemented" }
        return emptyMap()
    }

    override fun playerExists(client: Client): Boolean {
        val caseInsensitiveFilter = createCaseInsensitiveFilter(client)
        return DatabaseManager.getCollection(collectionName)
            .find(caseInsensitiveFilter)
            .toList()
            .isNotEmpty()
    }

    private fun createCaseInsensitiveFilter(client: Client): Bson {
        return regex("loginUsername", "^${client.loginUsername}$", "i")
    }
}