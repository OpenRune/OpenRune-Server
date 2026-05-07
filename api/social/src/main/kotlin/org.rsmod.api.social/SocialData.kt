package org.rsmod.api.social

import java.util.Locale

public class SocialData {

    public enum class PrivateChatMode(public val id: Int) {
        ON(0),
        FRIENDS(1),
        OFF(2);

        public companion object {
            public fun fromId(id: Int): PrivateChatMode =
                entries.firstOrNull { it.id == id } ?: ON
        }
    }

    public enum class ChatFilterMode(public val id: Int) {
        ON(0),
        FRIENDS(1),
        OFF(2),
        HIDE(3),
        AUTOCHAT(4);

        public companion object {
            public fun fromId(id: Int): ChatFilterMode =
                entries.firstOrNull { it.id == id } ?: ON
        }
    }

    private val friends: LinkedHashSet<String> = linkedSetOf()
    private val ignores: LinkedHashSet<String> = linkedSetOf()
    private val nameRecords: LinkedHashMap<String, SocialNameRecord> = linkedMapOf()

    public var publicChatMode: ChatFilterMode = ChatFilterMode.ON
    public var privateChatMode: PrivateChatMode = PrivateChatMode.ON
    public var tradeChatMode: ChatFilterMode = ChatFilterMode.ON

    public fun friends(): Set<String> = friends

    public fun ignores(): Set<String> = ignores

    public fun addFriend(name: String): Boolean {
        return friends.add(normalize(name))
    }

    public fun removeFriend(name: String): Boolean {
        return friends.remove(normalize(name))
    }

    public fun addIgnore(name: String): Boolean {
        friends.remove(normalize(name))
        return ignores.add(normalize(name))
    }

    public fun removeIgnore(name: String): Boolean {
        return ignores.remove(normalize(name))
    }

    public fun isFriend(name: String): Boolean {
        return friends.contains(normalize(name))
    }

    public fun isIgnoring(name: String): Boolean {
        return ignores.contains(normalize(name))
    }

    public fun setFriends(names: Collection<String>) {
        friends.clear()
        friends.addAll(names.map(::normalize).filter(String::isNotBlank))
    }

    public fun setIgnores(names: Collection<String>) {
        ignores.clear()
        ignores.addAll(names.map(::normalize).filter(String::isNotBlank))
    }

    private fun normalize(name: String): String {
        return name.trim().lowercase(Locale.getDefault())
    }

    public fun rememberName(record: SocialNameRecord) {
        nameRecords[normalize(record.canonicalName)] =
            record.copy(
                canonicalName = normalize(record.canonicalName),
                currentName = record.currentName.trim(),
                previousName = record.previousName?.trim()?.takeIf(String::isNotBlank),
            )
    }

    public fun nameRecord(name: String): SocialNameRecord? {
        return nameRecords[normalize(name)]
    }

    public fun toPersistentMap(): Map<String, Any> {
        return mapOf(
            "friends" to friends.toList(),
            "ignores" to ignores.toList(),
            "publicChatMode" to publicChatMode.id,
            "privateChatMode" to privateChatMode.id,
            "tradeChatMode" to tradeChatMode.id,
            "nameRecords" to nameRecords.values.map { record ->
                mapOf(
                    "canonicalName" to record.canonicalName,
                    "currentName" to record.currentName,
                    "previousName" to (record.previousName ?: ""),
                )
            },
        )
    }

    public companion object {
        public fun fromPersistentMap(map: Map<String, Any>): SocialData {
            val data = SocialData()

            val friends = map["friends"] as? List<*>
            val ignores = map["ignores"] as? List<*>
            val publicChatMode = (map["publicChatMode"] as? Number)?.toInt() ?: 0
            val privateChatMode = (map["privateChatMode"] as? Number)?.toInt() ?: 0
            val tradeChatMode = (map["tradeChatMode"] as? Number)?.toInt() ?: 0
            val nameRecords = map["nameRecords"] as? List<*>

            data.setFriends(friends?.mapNotNull { it as? String } ?: emptyList())
            data.setIgnores(ignores?.mapNotNull { it as? String } ?: emptyList())
            data.publicChatMode = ChatFilterMode.fromId(publicChatMode)
            data.privateChatMode = PrivateChatMode.fromId(privateChatMode)
            data.tradeChatMode = ChatFilterMode.fromId(tradeChatMode)

            nameRecords?.forEach { raw ->
                val record = raw as? Map<*, *> ?: return@forEach
                val canonicalName = record["canonicalName"] as? String ?: return@forEach
                val currentName = record["currentName"] as? String ?: canonicalName
                val previousName = record["previousName"] as? String

                data.rememberName(
                    SocialNameRecord(
                        canonicalName = canonicalName,
                        currentName = currentName,
                        previousName = previousName?.takeIf(String::isNotBlank),
                    )
                )
            }

            return data
        }
    }
}
