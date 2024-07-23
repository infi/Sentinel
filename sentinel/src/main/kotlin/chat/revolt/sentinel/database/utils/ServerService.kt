package chat.revolt.sentinel.database.utils

import chat.revolt.sentinel.database.schemas.DbServer
import chat.revolt.sentinel.database.schemas.Servers
import chat.revolt.sentinel.lookout.RevoltAPI
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

data class PrefixConfig(
    val customPrefix: String?,
    val defaultPrefix: String,
    val usingFallbackPrefix: Boolean
)

object ServerService {
    init {
        transaction {
            SchemaUtils.create(Servers)
        }
    }

    const val DEFAULT_PREFIX = "/"
    const val DEFAULT_PREFIX_FALLBACK = "!"
    private const val AUTO_MOD = "01FHGJ3NPP7XANQQH8C2BE44ZY"

    private suspend fun shouldUseFallbackPrefix(id: String): Boolean {
        return RevoltAPI.members.getMembersForServer(id).any { it.id?.user == AUTO_MOD }
    }

    suspend fun defaultPrefixFor(id: String): String {
        if (shouldUseFallbackPrefix(id)) {
            return DEFAULT_PREFIX_FALLBACK
        }
        return DEFAULT_PREFIX
    }

    suspend fun prefixFor(id: String): String {
        return dbQuery {
            return@dbQuery DbServer.findById(id)?.customPrefix ?: defaultPrefixFor(id)
        }
    }

    suspend fun prefixConfig(id: String): PrefixConfig {
        return PrefixConfig(
            customPrefix = dbQuery {
                return@dbQuery DbServer.findById(id)?.customPrefix
            },
            defaultPrefix = defaultPrefixFor(id),
            usingFallbackPrefix = shouldUseFallbackPrefix(id)
        )
    }

    suspend fun setCustomPrefix(id: String, prefix: String?) {
        dbQuery {
            Servers.update({ Servers.id eq id }) {
                it[customPrefix] = prefix
            }
        }
    }
}