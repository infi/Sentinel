package chat.revolt.sentinel.database.schemas

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column

object Servers : IdTable<String>() {
    override val id: Column<EntityID<String>> = varchar("id", 26).entityId()
    val customPrefix: Column<String?> = varchar("custom_prefix", length = 10).nullable()
}

class DbServer(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, DbServer>(Servers)

    var customPrefix by Servers.customPrefix
}